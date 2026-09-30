package io.github.twatanabe1436.hanaso.core

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.errors.AnthropicInvalidDataException
import com.anthropic.errors.AnthropicIoException
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.errors.BadRequestException
import com.anthropic.errors.InternalServerException
import com.anthropic.errors.NoCredentialsException
import com.anthropic.errors.PermissionDeniedException
import com.anthropic.errors.RateLimitException
import com.anthropic.errors.UnauthorizedException
import com.anthropic.helpers.BetaMessageAccumulator
import com.anthropic.models.beta.messages.BetaCacheControlEphemeral
import com.anthropic.models.beta.messages.BetaOutputConfig
import com.anthropic.models.beta.messages.BetaStopReason
import com.anthropic.models.beta.messages.MessageCreateParams
import com.anthropic.models.beta.messages.StructuredOutputConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Claude API (公式 Java SDK) を使う AI。
 * 会話の返事はストリーミング、フィードバック等は構造化出力 (Kotlin のクラスから JSON スキーマを自動生成) で受け取る。
 */
class ClaudeEngine(
    private val client: AnthropicClient,
    val model: String = DEFAULT_MODEL,
    /** 会話・添削・ヒント・翻訳の思考の深さ。低いほど速くて安い */
    private val fastEffort: BetaOutputConfig.Effort = BetaOutputConfig.Effort.LOW,
    /** 振り返りの思考の深さ */
    private val summaryEffort: BetaOutputConfig.Effort = BetaOutputConfig.Effort.MEDIUM,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : AiEngine {

    override val isDemo: Boolean = false

    companion object {
        const val DEFAULT_MODEL = "claude-opus-5-5"

        /** 安全分類器が応答を断った場合に、推奨モデルで自動的にやり直すサーバー側フォールバック */
        const val FALLBACK_BETA = "server-side-fallback-2026-07-01"

        fun create(apiKey: String, model: String = DEFAULT_MODEL, baseUrl: String? = null): ClaudeEngine {
            val builder = AnthropicOkHttpClient.builder().apiKey(apiKey)
            if (baseUrl != null) builder.baseUrl(baseUrl)
            return ClaudeEngine(builder.build(), model)
        }

        /** effort (思考の深さ) を受け付けないモデル */
        internal fun supportsEffort(model: String) = !model.contains("haiku")

        /** fallbacks: "default" を受け付けるモデル */
        internal fun supportsFallbacks(model: String) =
            Regex("^claude-(opus-5|fable-5-1|sonnet-5-5)").containsMatchIn(model)
    }

    private fun baseParams(system: String): MessageCreateParams.Builder {
        val builder = MessageCreateParams.builder()
            .model(model)
            .maxTokens(16_000L)
            .system(system)
        if (supportsFallbacks(model)) builder.addBeta(FALLBACK_BETA).fallbacksDefault()
        return builder
    }

    override fun reply(conversation: Conversation): Flow<String> = flow {
        val builder = baseParams(Prompts.partnerSystem(conversation.scenario, conversation.level))
            // 会話が続くほど前半が共通になるので、自動キャッシュで入力コストを下げる
            .cacheControl(BetaCacheControlEphemeral.builder().build())
        if (supportsEffort(model)) builder.outputConfig(BetaOutputConfig.builder().effort(fastEffort).build())
        for (line in Prompts.partnerMessages(conversation.history)) {
            if (line.speaker == Speaker.AI) builder.addAssistantMessage(line.text) else builder.addUserMessage(line.text)
        }
        val params = builder.build()

        val response = try {
            client.beta().messages().createStreaming(params)
        } catch (e: Exception) {
            throw toAiException(e)
        }
        // 画面を閉じるなどでキャンセルされたら通信を切る (読み込み待ちで止まっているスレッドを起こす)
        val closeOnCancel = currentCoroutineContext().job.invokeOnCompletion { response.close() }
        val accumulator = BetaMessageAccumulator.create()
        try {
            response.use {
                val events = it.stream().iterator()
                while (events.hasNext()) {
                    val event = accumulator.accumulate(events.next())
                    // フォールバックが途中で起きても、テキストは同じストリームで続きから届く
                    val text = event.contentBlockDelta().flatMap { d -> d.delta().text() }.orElse(null)?.text()
                    if (!text.isNullOrEmpty()) emit(text)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            throw toAiException(e)
        } finally {
            closeOnCancel.dispose()
        }
        val message = try {
            accumulator.message()
        } catch (e: Exception) {
            throw AiException(AiErrorKind.BAD_OUTPUT, e)
        }
        if (message.stopReason().orElse(null) == BetaStopReason.REFUSAL) throw AiException(AiErrorKind.REFUSAL)
    }.flowOn(io)

    /** 構造化出力 (JSON) で1回呼び出す */
    private suspend fun <T : Any> structured(
        type: Class<T>,
        system: String,
        prompt: String,
        effort: BetaOutputConfig.Effort,
    ): T = withContext(io) {
        val outputConfig = StructuredOutputConfig.builder<T>().format(type)
        if (supportsEffort(model)) outputConfig.effort(effort)
        val params = baseParams(system).addUserMessage(prompt).outputConfig(outputConfig.build()).build()
        val message = try {
            client.beta().messages().create(params)
        } catch (e: Exception) {
            throw toAiException(e)
        }
        if (message.stopReason().orElse(null) == BetaStopReason.REFUSAL) throw AiException(AiErrorKind.REFUSAL)
        try {
            message.content().firstNotNullOfOrNull { it.text().orElse(null) }?.text()
                ?: throw AiException(AiErrorKind.BAD_OUTPUT)
        } catch (e: AiException) {
            throw e
        } catch (e: Exception) {
            throw AiException(AiErrorKind.BAD_OUTPUT, e)
        }
    }

    override suspend fun feedback(conversation: Conversation): Feedback =
        structured(Feedback::class.java, Prompts.FEEDBACK_SYSTEM, Prompts.feedbackPrompt(conversation), fastEffort)

    override suspend fun hint(conversation: Conversation, wantJa: String?): List<HintSuggestion> =
        structured(Hints::class.java, Prompts.HINT_SYSTEM, Prompts.hintPrompt(conversation, wantJa), fastEffort).suggestions

    override suspend fun translate(text: String): Translation =
        structured(Translation::class.java, Prompts.TRANSLATE_SYSTEM, Prompts.translatePrompt(text), fastEffort)

    override suspend fun summary(conversation: Conversation, completedMissions: Set<String>): Summary {
        val s = structured(
            Summary::class.java,
            Prompts.SUMMARY_SYSTEM,
            Prompts.summaryPrompt(conversation, completedMissions),
            summaryEffort,
        )
        return s.copy(score = s.score.coerceIn(0, 100))
    }
}

/** SDK の例外を、画面に出せる種類に分類する (具体的なクラスから順に判定) */
internal fun toAiException(e: Throwable): AiException = when (e) {
    is AiException -> e
    is UnauthorizedException, is PermissionDeniedException, is NoCredentialsException -> AiException(AiErrorKind.AUTH, e)
    is RateLimitException, is InternalServerException -> AiException(AiErrorKind.BUSY, e)
    is BadRequestException -> AiException(AiErrorKind.BAD_REQUEST, e, detail = e.message?.take(300))
    is AnthropicServiceException ->
        if (e.statusCode() >= 500) AiException(AiErrorKind.BUSY, e)
        else AiException(AiErrorKind.UNKNOWN, e, detail = e.message?.take(300))
    is AnthropicIoException, is IOException -> AiException(AiErrorKind.NETWORK, e)
    is AnthropicInvalidDataException -> AiException(AiErrorKind.BAD_OUTPUT, e)
    else -> AiException(AiErrorKind.UNKNOWN, e, detail = e.message?.take(300))
}
