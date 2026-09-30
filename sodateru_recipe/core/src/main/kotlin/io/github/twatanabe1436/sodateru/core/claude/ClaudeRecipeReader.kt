package io.github.twatanabe1436.sodateru.core.claude

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.errors.AnthropicIoException
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.errors.BadRequestException
import com.anthropic.errors.InternalServerException
import com.anthropic.errors.NotFoundException
import com.anthropic.errors.PermissionDeniedException
import com.anthropic.errors.RateLimitException
import com.anthropic.errors.UnauthorizedException
import com.anthropic.models.messages.Base64ImageSource
import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.ImageBlockParam
import com.anthropic.models.messages.JsonOutputFormat
import com.anthropic.models.messages.Message
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.OutputConfig
import com.anthropic.models.messages.StopReason
import io.github.twatanabe1436.sodateru.core.DataCodec
import io.github.twatanabe1436.sodateru.core.IngredientRoles
import io.github.twatanabe1436.sodateru.core.RecipeDraft
import io.github.twatanabe1436.sodateru.core.model.Category
import io.github.twatanabe1436.sodateru.core.model.Ingredient
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import java.time.Duration

/** 読み取りに失敗したときの、利用者に見せる説明つき例外。 */
class ClaudeReadException(message: String, cause: Throwable? = null) : Exception(message, cause)

enum class ReadMode {
    /** レシピ (本・ノート・メモ) として材料と手順に振り分ける。 */
    RECIPE,

    /** 作った記録のメモ (アレンジや感想の手書き) を文字に起こす。 */
    NOTE,
}

/** 読み取りに送る画像 1 枚 (base64、JPEG)。 */
data class ImageInput(val base64: String)

data class ModelOption(val id: String, val label: String)

/**
 * Claude API で写真のレシピ・手書きノートを読み取る (高精度モード)。
 * API キーは利用者のもので、端末から直接 Anthropic の API を呼ぶ。
 */
class ClaudeRecipeReader(
    private val client: AnthropicClient,
    private val model: String = DEFAULT_MODEL,
) {
    constructor(apiKey: String, model: String = DEFAULT_MODEL) : this(
        AnthropicOkHttpClient.builder()
            .apiKey(apiKey)
            .timeout(Duration.ofSeconds(180))
            .maxRetries(2)
            .build(),
        model,
    )

    /** 画像を読み取ってレシピの下書きにする。通信するので UI スレッドでは呼ばないこと。 */
    fun read(images: List<ImageInput>, mode: ReadMode): RecipeDraft {
        require(images.isNotEmpty()) { "images must not be empty" }
        val message = call { client.messages().create(buildParams(images, mode, model)) }
        return parseMessage(message)
    }

    /** API キーが使えるか確かめる (モデル情報の取得だけなので料金はかからない)。 */
    fun verify() {
        call { client.models().retrieve(model) }
    }

    private fun <T> call(block: () -> T): T = try {
        block()
    } catch (e: UnauthorizedException) {
        throw ClaudeReadException("APIキーが正しくないか、無効になっています。設定を確認してください", e)
    } catch (e: PermissionDeniedException) {
        throw ClaudeReadException("このAPIキーでは利用できません（権限がありません）", e)
    } catch (e: NotFoundException) {
        throw ClaudeReadException("モデル「$model」が見つかりません。設定で別のモデルを選んでください", e)
    } catch (e: RateLimitException) {
        throw ClaudeReadException("利用の上限に達しました。少し待ってからもう一度お試しください", e)
    } catch (e: BadRequestException) {
        val detail = e.message.orEmpty()
        if ("credit" in detail.lowercase()) {
            throw ClaudeReadException("APIのクレジット残高が足りません。Anthropic Console で残高を確認してください", e)
        }
        throw ClaudeReadException("読み取りのリクエストが受け付けられませんでした（$detail）", e)
    } catch (e: InternalServerException) {
        throw ClaudeReadException("Claude 側で一時的なエラーが起きました。しばらくしてからお試しください", e)
    } catch (e: AnthropicServiceException) {
        throw ClaudeReadException("読み取りに失敗しました（エラー ${e.statusCode()}）", e)
    } catch (e: AnthropicIoException) {
        throw ClaudeReadException("インターネットに接続できませんでした。電波の良い場所でお試しください", e)
    }

    companion object {
        const val DEFAULT_MODEL = "claude-opus-5-5"

        val MODELS = listOf(
            ModelOption("claude-opus-5-5", "Claude Opus 5.5（いちばん高精度・標準）"),
            ModelOption("claude-sonnet-5-5", "Claude Sonnet 5.5（速くて安い）"),
            ModelOption("claude-haiku-4-5", "Claude Haiku 4.5（最も安い）"),
        )

        /** 安全上の理由で応答が止まったとき、別モデルで自動的にやり直すサーバー側の仕組み (beta)。 */
        private const val FALLBACK_BETA = "server-side-fallback-2026-07-01"

        private val SYSTEM_PROMPT = """
            あなたは家庭のレシピ帳をデジタル化する手伝いをしています。
            写真には印刷されたレシピ本、手書きのノート、付箋のメモなどが写っています。
            書かれている内容を正確に読み取ってください。読めない文字は推測で埋めず「〔?〕」と書いてください。
        """.trimIndent()

        private val RECIPE_INSTRUCTIONS = """
            写真のレシピを読み取り、指定の JSON 形式で返してください。
            - transcript: 写真の文字を、読み取れた順にそのまま書き起こす（改行は保つ）
            - title: 料理名。書かれていなければ内容から短く付ける
            - servings: 「2人分」「1斤分」「18cm型1台分」など。なければ空文字
            - category: 料理なら COOKING、パンなら BREAD、お菓子なら SWEETS
            - ingredients: 材料 1 つにつき name / amount / unit / note
              - amount には数字の部分だけ（「200」「1/2」「1と1/2」）。「少々」「適量」はそのまま amount に入れ unit は空
              - unit は「g」「ml」「大さじ」「小さじ」「カップ」「個」など。「大さじ1」は amount=1, unit=大さじ
              - 切り方などの補足は note
              - 「A」「合わせ調味料」のようなグループ名は note に入れる
            - steps: 作り方を順番に 1 手順 1 要素で。先頭の番号は付けない
            - memo: コツ・ポイント・保存方法・メモ書きなど、材料と手順以外の内容
            - 手書きの修正（取り消し線・書き足し・矢印）があれば修正後の内容を材料や手順に使い、
              修正前の値は memo に「砂糖 大さじ2→1.5（書き込み）」のように残す
        """.trimIndent()

        private val NOTE_INSTRUCTIONS = """
            写真は料理やパン作りの記録ノート・メモです。書かれている文字をそのまま transcript に書き起こしてください（改行は保つ）。
            材料や手順が書かれていれば ingredients / steps にも入れ、アレンジや感想は memo にまとめてください。
            当てはまらない項目は空文字・空配列にしてください。category は分からなければ COOKING。
        """.trimIndent()

        private fun str(): Map<String, Any> = mapOf("type" to "string")

        /** 構造化出力の JSON スキーマ。 */
        private val SCHEMA_PROPERTIES: Map<String, Any> = mapOf(
            "transcript" to str(),
            "title" to str(),
            "servings" to str(),
            "category" to mapOf("type" to "string", "enum" to listOf("COOKING", "BREAD", "SWEETS")),
            "ingredients" to mapOf(
                "type" to "array",
                "items" to mapOf(
                    "type" to "object",
                    "properties" to mapOf("name" to str(), "amount" to str(), "unit" to str(), "note" to str()),
                    "required" to listOf("name", "amount", "unit", "note"),
                    "additionalProperties" to false,
                ),
            ),
            "steps" to mapOf("type" to "array", "items" to str()),
            "memo" to str(),
        )

        fun supportsEffort(model: String): Boolean = !model.startsWith("claude-haiku")

        fun supportsFallback(model: String): Boolean =
            model == "claude-opus-5-5" || model == "claude-sonnet-5-5" || model == "claude-fable-5-1"

        fun buildParams(images: List<ImageInput>, mode: ReadMode, model: String): MessageCreateParams {
            val schema = JsonOutputFormat.Schema.builder()
                .putAdditionalProperty("type", JsonValue.from("object"))
                .putAdditionalProperty("properties", JsonValue.from(SCHEMA_PROPERTIES))
                .putAdditionalProperty("required", JsonValue.from(SCHEMA_PROPERTIES.keys.toList()))
                .putAdditionalProperty("additionalProperties", JsonValue.from(false))
                .build()
            val outputConfig = OutputConfig.builder()
                .format(JsonOutputFormat.builder().schema(schema).build())
                .apply { if (supportsEffort(model)) effort(OutputConfig.Effort.MEDIUM) }
                .build()

            val blocks = images.map { image ->
                ContentBlockParam.ofImage(
                    ImageBlockParam.builder()
                        .source(
                            Base64ImageSource.builder()
                                .mediaType(Base64ImageSource.MediaType.IMAGE_JPEG)
                                .data(image.base64)
                                .build(),
                        )
                        .build(),
                )
            } + ContentBlockParam.ofText(
                when (mode) {
                    ReadMode.RECIPE -> RECIPE_INSTRUCTIONS
                    ReadMode.NOTE -> NOTE_INSTRUCTIONS
                },
            )

            return MessageCreateParams.builder()
                .model(model)
                .maxTokens(16000L)
                .system(SYSTEM_PROMPT)
                .outputConfig(outputConfig)
                .addUserMessageOfBlockParams(blocks)
                .apply {
                    if (supportsFallback(model)) {
                        putAdditionalHeader("anthropic-beta", FALLBACK_BETA)
                        putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
                    }
                }
                .build()
        }

        fun parseMessage(message: Message): RecipeDraft {
            when (message.stopReason().orElse(null)) {
                StopReason.REFUSAL ->
                    throw ClaudeReadException("この写真は読み取れませんでした（応答が途中で止まりました）")
                StopReason.MAX_TOKENS ->
                    throw ClaudeReadException("内容が長すぎて読み取りが途中で切れました。写真を分けてお試しください")
                else -> Unit
            }
            val text = message.content().mapNotNull { block -> block.text().orElse(null)?.text() }.joinToString("")
            if (text.isBlank()) throw ClaudeReadException("読み取り結果が空でした。もう一度お試しください")
            return parseResponseText(text)
        }

        fun parseResponseText(text: String): RecipeDraft {
            val parsed = try {
                DataCodec.json.decodeFromString(ClaudeRecipeJson.serializer(), text.trim())
            } catch (e: SerializationException) {
                throw ClaudeReadException("読み取り結果の形式が想定と違いました。もう一度お試しください", e)
            } catch (e: IllegalArgumentException) {
                throw ClaudeReadException("読み取り結果の形式が想定と違いました。もう一度お試しください", e)
            }
            return RecipeDraft(
                title = parsed.title.trim(),
                servings = parsed.servings.trim(),
                category = Category.entries.firstOrNull { it.name == parsed.category },
                ingredients = parsed.ingredients.filter { it.name.isNotBlank() }.map {
                    Ingredient(
                        name = it.name.trim(),
                        amount = it.amount.trim(),
                        unit = it.unit.trim(),
                        note = it.note.trim(),
                        role = IngredientRoles.guess(it.name),
                    )
                },
                steps = parsed.steps.map(String::trim).filter(String::isNotEmpty),
                memo = parsed.memo.trim(),
                transcript = parsed.transcript.trim(),
            )
        }
    }
}

@Serializable
internal data class ClaudeIngredientJson(
    val name: String = "",
    val amount: String = "",
    val unit: String = "",
    val note: String = "",
)

@Serializable
internal data class ClaudeRecipeJson(
    val transcript: String = "",
    val title: String = "",
    val servings: String = "",
    val category: String = "",
    val ingredients: List<ClaudeIngredientJson> = emptyList(),
    val steps: List<String> = emptyList(),
    val memo: String = "",
)
