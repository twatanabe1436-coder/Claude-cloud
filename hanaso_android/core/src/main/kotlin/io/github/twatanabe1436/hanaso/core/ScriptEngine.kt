package io.github.twatanabe1436.hanaso.core

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 台本モードの1ステップ。学習者は taskJa (日本語のお題) を英語で言い、
 * 合格したら相手が reply を返して次のお題へ進む。最後のステップの reply が締めのセリフ。
 */
data class ScriptStep(
    /** 日本語のお題 (例: 「ラテのMサイズを注文しよう」) */
    val taskJa: String,
    /** お手本 (先頭が代表)。採点はいちばん近いものと比べる */
    val answers: List<Phrase>,
    /** 合格したあと (または言い直しの回数を使い切ったあと) の相手のセリフ */
    val reply: Phrase,
    /**
     * 意味が伝わったかを見るキーワード。要素ごとに「|」区切りの候補のどれかが発話に含まれていれば OK。
     * 候補は複数語でもよく、末尾の * は前方一致 (recommend* → recommended)。
     */
    val keywords: List<String> = emptyList(),
    /** 合格すると達成になるミッションの id */
    val mission: String? = null,
    /** 表現のポイント (日本語) */
    val tipJa: String = "",
    /** 自分のことを答える自由回答のお題。英語でひとこと以上 (2 語以上) 話せば合格 */
    val open: Boolean = false,
    /** お題に答えるのに必要な事情 (画面ではお題の下に出す)。お題だけで分かるときは空 */
    val contextJa: String = "",
)

/** シナリオ 1 つ分の台本。最初のセリフはシナリオの opener (その日本語訳が openerJa)。 */
data class Script(val scenarioId: String, val openerJa: String, val steps: List<ScriptStep>)

/** 画面に出す「いまのお題」 */
data class ScriptTask(
    /** 何問目か (1 から) */
    val number: Int,
    val total: Int,
    val taskJa: String,
    /** お題に答えるのに必要な事情 (なければ空) */
    val contextJa: String,
    val open: Boolean,
    /** このお題で言い直した回数 */
    val retries: Int,
    /** 代表のお手本 */
    val example: Phrase,
    /** 最後のお題まで終わった */
    val finished: Boolean,
)

/** 1 回の発話の判定 */
data class ScriptJudgement(
    val passed: Boolean,
    val rating: Rating,
    /** いちばん近いお手本 (自由回答では先頭の例) */
    val closest: Phrase,
    /** お手本との一致度 (自由回答では null) */
    val match: SpeechScore?,
    val keywordsOk: Boolean,
    /** 英単語の数 */
    val words: Int,
)

/**
 * AI を使わない「台本モード」の会話相手。お題ごとに決まったセリフを返し、発話はお手本との一致度と
 * キーワードで判定する。状態は持たず、会話の履歴を最初から再生して「いまどのお題か」を求める
 * (返事と添削を並行して取得しても、やり直しても結果が食い違わない)。
 */
class ScriptEngine(private val delayMs: Long = 20) : AiEngine {

    override val mode: EngineMode = EngineMode.SCRIPT

    companion object {
        /** 1 つのお題で言える回数。使い切ると、お手本を見せて次のお題へ進む */
        const val MAX_TRIES = 3

        private const val PASS_SCORE = 60
        private const val PASS_SCORE_WITH_KEYWORDS = 80
        private const val GREAT_SCORE = 85
        private const val OPEN_MIN_WORDS = 2
        private const val OPEN_GREAT_WORDS = 6

        val RETRY_LINES = listOf(
            Phrase("Sorry, could you say that again?", "すみません、もう一度言ってもらえますか？"),
            Phrase("Hmm, I didn't quite catch that. One more time, please?", "うーん、よく聞き取れませんでした。もう一度お願いできますか？"),
        )
        val OPEN_RETRY_LINES = listOf(
            Phrase("Oh, could you tell me a little more?", "へえ、もう少しくわしく教えてもらえますか？"),
            Phrase("Could you say that in a full sentence?", "文の形で言ってもらえますか？"),
        )
        val AFTER_END = Phrase("Thanks again! It was really nice talking with you.", "改めてありがとう！お話しできて本当によかったです。")

        private val DELIMITERS = Regex("[-:/]")

        /** 台本にある英文 → 日本語訳 */
        private val dictionary: Map<String, String> by lazy {
            val map = HashMap<String, String>()
            fun put(p: Phrase) = map.putIfAbsent(Stats.normalizePhrase(p.en), p.ja)
            for (script in Scripts.all.values) {
                Catalog.find(script.scenarioId)?.let { put(Phrase(it.opener, script.openerJa)) }
                for (step in script.steps) {
                    put(step.reply)
                    step.answers.forEach(::put)
                }
            }
            (RETRY_LINES + OPEN_RETRY_LINES + AFTER_END).forEach(::put)
            (Catalog.scenarios + Catalog.freeTalks).flatMap { it.keyPhrases }.forEach(::put)
            map
        }

        /** お手本・回答例を、学習者のレベルに近い順に並べる (レベルの付いていないものは元の順のまま) */
        fun examplesFor(step: ScriptStep, level: Level): List<Phrase> =
            step.answers.sortedBy { a -> a.level?.let { abs(it.ordinal - level.ordinal) } ?: 0 }

        /** 台本にある英文なら日本語訳を返す */
        fun lookupJa(en: String): String? = dictionary[Stats.normalizePhrase(en)]

        /** お題に対する発話を判定する */
        fun judge(step: ScriptStep, said: String): ScriptJudgement {
            val words = said.split(Regex("""\s+""")).count { w -> w.any { it in 'a'..'z' || it in 'A'..'Z' } }
            if (step.open) {
                val passed = words >= OPEN_MIN_WORDS
                val likeExample = step.answers.any { SpeechScorer.score(it.en, said).score >= GREAT_SCORE }
                val rating = when {
                    passed && (words >= OPEN_GREAT_WORDS || likeExample) -> Rating.GREAT
                    passed -> Rating.GOOD
                    else -> Rating.FIX
                }
                return ScriptJudgement(passed, rating, step.answers.first(), null, keywordsOk = false, words = words)
            }
            val (closest, match) = step.answers.map { it to SpeechScorer.score(it.en, said) }.maxBy { it.second.score }
            val keywordsOk = step.keywords.isNotEmpty() && keywordsSatisfied(step.keywords, said)
            val passed = if (step.keywords.isEmpty()) match.score >= PASS_SCORE else keywordsOk || match.score >= PASS_SCORE_WITH_KEYWORDS
            val rating = when {
                passed && match.score >= GREAT_SCORE -> Rating.GREAT
                passed -> Rating.GOOD
                else -> Rating.FIX
            }
            return ScriptJudgement(passed, rating, closest, match, keywordsOk, words)
        }

        fun keywordsSatisfied(keywords: List<String>, said: String): Boolean {
            val tokens = SpeechScorer.tokens(said.replace(DELIMITERS, " "))
            return keywords.all { group -> group.split("|").any { containsPhrase(tokens, it.trim()) } }
        }

        private fun containsPhrase(tokens: List<String>, alternative: String): Boolean {
            val prefix = alternative.endsWith("*")
            val pattern = SpeechScorer.tokens(alternative.removeSuffix("*").replace(DELIMITERS, " "))
            if (pattern.isEmpty() || pattern.size > tokens.size) return false
            return (0..tokens.size - pattern.size).any { i ->
                pattern.indices.all { k ->
                    val token = tokens[i + k]
                    if (prefix && k == pattern.lastIndex) token.startsWith(pattern[k]) else token == pattern[k]
                }
            }
        }
    }

    /** 学習者の 1 回の発話 (step は何問目への答えか。台本が終わったあとの発話は judgement が null) */
    private class Attempt(val step: Int, val said: String, val judgement: ScriptJudgement?, val moveOn: Boolean)

    private class Progress(val script: Script, val attempts: List<Attempt>, val step: Int, val retries: Int) {
        val finished: Boolean get() = step >= script.steps.size
    }

    fun script(scenario: Scenario): Script = Scripts.forScenario(scenario)

    private fun progress(conversation: Conversation): Progress {
        val script = script(conversation.scenario)
        var step = 0
        var retries = 0
        val attempts = mutableListOf<Attempt>()
        for (line in conversation.history) {
            if (line.speaker != Speaker.LEARNER) continue
            if (step >= script.steps.size) {
                attempts += Attempt(step, line.text, null, moveOn = false)
                continue
            }
            val judgement = judge(script.steps[step], line.text)
            val moveOn = judgement.passed || retries + 1 >= MAX_TRIES
            attempts += Attempt(step, line.text, judgement, moveOn)
            if (moveOn) {
                step++
                retries = 0
            } else {
                retries++
            }
        }
        return Progress(script, attempts, step, retries)
    }

    /** いまのお題 */
    fun task(conversation: Conversation): ScriptTask {
        val p = progress(conversation)
        val steps = p.script.steps
        if (p.finished) {
            val example = examplesFor(steps.last(), conversation.level).first()
            return ScriptTask(steps.size, steps.size, "台本クリア！", "", open = false, retries = 0, example = example, finished = true)
        }
        val step = steps[p.step]
        val example = examplesFor(step, conversation.level).first()
        return ScriptTask(p.step + 1, steps.size, step.taskJa, step.contextJa, step.open, p.retries, example, finished = false)
    }

    /** 学習者の最後の発話に対する相手のセリフ */
    fun nextLine(conversation: Conversation): Phrase {
        val p = progress(conversation)
        val last = p.attempts.lastOrNull() ?: return Phrase(conversation.scenario.opener, p.script.openerJa)
        if (last.judgement == null) return AFTER_END
        val step = p.script.steps[last.step]
        if (last.moveOn) return step.reply
        val lines = if (step.open) OPEN_RETRY_LINES else RETRY_LINES
        return lines[(p.retries - 1).coerceIn(0, lines.lastIndex)]
    }

    override fun reply(conversation: Conversation): Flow<String> = flow {
        val text = nextLine(conversation).en
        delay(delayMs * 8)
        text.split(" ").forEachIndexed { i, word ->
            emit(if (i == 0) word else " $word")
            delay(delayMs)
        }
    }

    override suspend fun feedback(conversation: Conversation): Feedback {
        delay(delayMs)
        val p = progress(conversation)
        val last = p.attempts.lastOrNull() ?: throw AiException(AiErrorKind.BAD_REQUEST)
        val missions = p.attempts
            .filter { it.judgement?.passed == true }
            .mapNotNull { p.script.steps[it.step].mission }
            .distinct()
        val j = last.judgement
            ?: return Feedback(
                rating = Rating.GOOD,
                corrected = last.said,
                natural = last.said,
                explanationJa = "台本はここまでです。右上の「終了」から振り返りを見てみましょう。",
                mistakes = emptyList(),
                completedMissions = missions,
            )
        val step = p.script.steps[last.step]
        val message = if (step.open) {
            when (j.rating) {
                Rating.GREAT -> "自分の言葉でしっかり答えられました！"
                Rating.GOOD -> "答えられました！理由やくわしい情報を 1 文足すと、もっと会話が広がります。"
                Rating.FIX -> "もう少し長く、文の形で答えてみよう。下の例も参考にしてね。"
            }
        } else {
            when {
                j.rating == Rating.GREAT -> "完ぺきです！お手本どおりに言えました。"
                j.rating == Rating.GOOD && (j.match?.score ?: 0) >= PASS_SCORE ->
                    "通じました！色の薄い単語まで言えると、お手本どおりです。"
                j.rating == Rating.GOOD -> "通じました！自分なりの言い方で伝えられています。お手本の言い方も覚えておこう。"
                else -> "お題の内容が伝わらなかったようです。お手本を参考にもう一度言ってみよう。"
            }
        }
        val explanation = buildString {
            append(message)
            if (!j.passed && last.moveOn) append("\n（お手本を確認して、次のお題に進みます）")
            if (step.tipJa.isNotBlank()) append("\n💡 ").append(step.tipJa)
        }
        return Feedback(
            rating = j.rating,
            corrected = last.said,
            // 自由回答は、学習者のレベルに合った回答例を見せる
            natural = if (step.open) examplesFor(step, conversation.level).first().en else j.closest.en,
            explanationJa = explanation,
            mistakes = emptyList(),
            completedMissions = missions,
            matchScore = j.match,
        )
    }

    override suspend fun hint(conversation: Conversation, wantJa: String?): List<HintSuggestion> {
        delay(delayMs)
        val p = progress(conversation)
        if (p.finished) {
            return conversation.scenario.keyPhrases.take(3).map { HintSuggestion("覚えたいフレーズ", it.en, it.ja) }
        }
        val step = p.script.steps[p.step]
        val level = conversation.level
        return examplesFor(step, level).take(3).mapIndexed { i, a ->
            val label = when {
                step.open && a.level != null -> when {
                    a.level.band == level.band -> "あなたのレベル（${a.level.band}）"
                    a.level.band < level.band -> "やさしめ（${a.level.band}）"
                    else -> "チャレンジ（${a.level.band}）"
                }
                step.open -> "例${i + 1}"
                i == 0 -> "お手本"
                else -> "言い換え"
            }
            HintSuggestion(label, a.en, a.ja)
        }
    }

    override suspend fun translate(text: String): Translation {
        delay(delayMs)
        val ja = lookupJa(text) ?: "（台本モードでは、台本にない文は翻訳できません）"
        return Translation(ja, emptyList())
    }

    override suspend fun summary(conversation: Conversation, completedMissions: Set<String>): Summary {
        delay(delayMs * 4)
        val p = progress(conversation)
        val steps = p.script.steps
        val byStep = p.attempts.filter { it.judgement != null }.groupBy { it.step }
        val passes = steps.indices.associateWith { i -> byStep[i].orEmpty().firstOrNull { it.judgement!!.passed } }

        // お題ごとの点数: 一発で Great 100 点、合格 85 点 (言い直し 1 回につき -10)、合格できず 40 点、未回答 0 点
        val points = steps.indices.map { i ->
            val tries = byStep[i].orEmpty()
            val pass = passes[i]
            when {
                tries.isEmpty() -> 0
                pass == null -> 40
                else -> (if (pass.judgement!!.rating == Rating.GREAT) 100 else 85) - 10 * tries.indexOf(pass)
            }
        }
        val score = points.average().roundToInt().coerceIn(0, 100)
        val cleared = passes.values.count { it != null }

        val headline = when {
            !p.finished -> "${cleared} 個のお題をクリア！次は最後まで話してみよう"
            score >= 90 -> "すばらしい！台本をほぼ完ぺきに話せました"
            score >= 70 -> "よくできました！最後まで英語で会話できました"
            else -> "最後まで話せました！お手本を見ながらもう一度挑戦しよう"
        }

        val good = mutableListOf("${steps.size} 個のお題のうち ${cleared} 個をクリアしました。")
        steps.indices
            .mapNotNull { i -> byStep[i]?.firstOrNull()?.takeIf { it.judgement!!.rating == Rating.GREAT } }
            .take(2)
            .forEach { a ->
                good += if (steps[a.step].open) "「${a.said}」と自分の言葉で話せました。" else "「${a.said}」とお手本どおりに言えました。"
            }
        if (cleared == 0) good += "最初の一歩を踏み出せました。続けることがいちばんの近道です。"

        val improve = mutableListOf<ImprovePoint>()
        // 合格できなかった・言い直したお題を優先し、次に「通じたけどお手本と違う」お題
        steps.indices.filter { i -> byStep[i].orEmpty().let { it.isNotEmpty() && (passes[i] == null || it.size > 1) } }
            .forEach { i -> improve += ImprovePoint("「${steps[i].taskJa}」はこう言えます。", examplesFor(steps[i], conversation.level).first().en) }
        steps.indices.filter { i -> !steps[i].open && passes[i]?.judgement?.rating == Rating.GOOD && byStep[i].orEmpty().size == 1 }
            .forEach { i -> improve += ImprovePoint("「${steps[i].taskJa}」はお手本の言い方も覚えよう。", steps[i].answers.first().en) }
        if (improve.isEmpty()) {
            val alt = steps.firstOrNull { it.answers.size > 1 }
            if (alt != null) improve += ImprovePoint("同じ内容を、別の言い方でも言えるようにしてみよう。", alt.answers[1].en)
        }

        val keyPhrases = conversation.scenario.keyPhrases.ifEmpty { steps.map { examplesFor(it, conversation.level).first() } }.take(4)
        val next = when {
            !p.finished -> "次は最後のお題まで話してみよう。詰まったら「ヒント」でお手本を確認できます。"
            score >= 85 -> "次はヒントを見ずに、言い換えの表現でも話してみよう。"
            else -> "ヒントのお手本を声に出して練習してから、もう一度挑戦しよう。"
        }
        return Summary(
            score = score,
            headlineJa = headline,
            goodPointsJa = good.take(3),
            improvePoints = improve.take(3),
            keyPhrases = keyPhrases,
            nextChallengeJa = next,
        )
    }
}
