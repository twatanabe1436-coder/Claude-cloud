package io.github.twatanabe1436.hanaso.core

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 画面や音声の流れを試すための「デモモード」の相手 (主にテスト用)。
 * 本物の AI ではなく、決まった返事と簡単なルールで動く。
 */
class MockEngine(private val delayMs: Long = 35) : AiEngine {

    override val mode: EngineMode = EngineMode.DEMO

    private val partnerLines = listOf(
        "I see! Could you tell me a little more about that?",
        "Oh, that's interesting. Why do you say that?",
        "Got it. And what would you like to do next?",
        "That sounds great! How do you feel about it?",
        "Okay, no problem. Is there anything else?",
    )

    private val genericPhrases = listOf(
        Phrase("Could you say that again, please?", "もう一度言ってもらえますか？"),
        Phrase("That sounds interesting! Tell me more.", "おもしろそう！もっと教えて。"),
        Phrase("What do you think about it?", "それについてどう思いますか？"),
        Phrase("I'm not sure, but I think so.", "よくわからないけど、そう思います。"),
    )

    /** よくある「直訳っぽい」言い方を自然な言い方に置き換えるルール */
    private val naturalRules = listOf(
        Regex("""\bI want\b""", RegexOption.IGNORE_CASE) to "I'd like",
        Regex("""\bgive me\b""", RegexOption.IGNORE_CASE) to "could I have",
        Regex("""\bvery very\b""", RegexOption.IGNORE_CASE) to "really",
    )

    override fun reply(conversation: Conversation): Flow<String> = flow {
        val turn = conversation.history.count { it.speaker == Speaker.LEARNER }
        val text = partnerLines[(turn - 1).mod(partnerLines.size)]
        delay(delayMs * 4)
        text.split(" ").forEachIndexed { i, word ->
            emit(if (i == 0) word else " $word")
            delay(delayMs)
        }
    }

    override suspend fun feedback(conversation: Conversation): Feedback {
        delay(delayMs * 6)
        val original = conversation.history.last().text
        val corrected = tidy(original)
        var natural = corrected
        for ((re, rep) in naturalRules) natural = re.replace(natural, rep)
        natural = natural.replaceFirstChar { it.uppercase() }
        val hadErrors = Regex("""\bi\b""").containsMatchIn(original)
        val rating = when {
            hadErrors -> Rating.FIX
            natural != corrected -> Rating.GOOD
            else -> Rating.GREAT
        }
        val learnerTurns = conversation.history.count { it.speaker == Speaker.LEARNER }
        return Feedback(
            rating = rating,
            corrected = corrected,
            natural = natural,
            explanationJa = when (rating) {
                Rating.GREAT -> "（デモ）自然に言えています！この調子で文で答えてみましょう。"
                Rating.GOOD -> "（デモ）通じますが、こう言うとより自然で丁寧に聞こえます。"
                Rating.FIX -> "（デモ）「I（私）」は文中でも必ず大文字で書きます。"
            },
            mistakes = if (hadErrors) listOf(Mistake("i", "I", "「私」は常に大文字の I")) else emptyList(),
            completedMissions = conversation.scenario.missions.take(learnerTurns).map { it.id },
        )
    }

    override suspend fun hint(conversation: Conversation, wantJa: String?): List<HintSuggestion> {
        delay(delayMs * 6)
        val pool = conversation.scenario.keyPhrases.ifEmpty { genericPhrases }
        val labels = if (wantJa.isNullOrBlank()) listOf("シンプル", "自然な言い方", "会話を進める") else listOf("シンプル", "自然な言い方", "丁寧")
        return labels.mapIndexed { i, label ->
            val p = pool[i % pool.size]
            HintSuggestion(
                labelJa = label,
                en = p.en,
                ja = if (wantJa.isNullOrBlank()) p.ja else "（デモ：「$wantJa」の英訳は API キー設定後に表示されます）",
            )
        }
    }

    override suspend fun translate(text: String): Translation {
        delay(delayMs * 4)
        return Translation("（デモモードでは翻訳できません。API キーを設定すると日本語訳が表示されます）", emptyList())
    }

    override suspend fun summary(conversation: Conversation, completedMissions: Set<String>): Summary {
        delay(delayMs * 10)
        val learnerLines = conversation.history.filter { it.speaker == Speaker.LEARNER }.map { it.text }
        val avgWords = learnerLines.sumOf { it.split(Regex("""\s+""")).size }.toDouble() / maxOf(1, learnerLines.size)
        val missions = conversation.scenario.missions
        val missionRate = if (missions.isEmpty()) 1.0 else completedMissions.size.toDouble() / missions.size
        val score = minOf(95.0, 45 + avgWords * 3 + missionRate * 25).toInt()
        return Summary(
            score = score,
            headlineJa = "（デモ）最後まで英語で会話できました！",
            goodPointsJa = listOfNotNull(
                "${learnerLines.size}回、英語で返答できました。",
                learnerLines.firstOrNull()?.let { "「$it」のように自分の言葉で話せています。" },
            ),
            improvePoints = listOf(
                ImprovePoint("一言で終わらせず、理由や詳しい情報を足してみましょう。", "I'd like a latte because I need some energy this morning."),
                ImprovePoint("丁寧に頼むときは I want より I'd like を使いましょう。", "I'd like a medium coffee, please."),
            ),
            keyPhrases = conversation.scenario.keyPhrases.ifEmpty { genericPhrases }.take(4),
            nextChallengeJa = "次は1回の返答で2文以上話すことに挑戦してみましょう。",
        )
    }

    private fun tidy(text: String): String {
        var s = text.trim().replace(Regex("""\s+"""), " ")
        s = s.replace(Regex("""\bi\b"""), "I").replace(Regex("""\bi'm\b""", RegexOption.IGNORE_CASE), "I'm")
        s = s.replaceFirstChar { it.uppercase() }
        if (!Regex("""[.!?]$""").containsMatchIn(s)) {
            val question = Regex("""^(what|where|when|why|how|who|can|could|do|does|is|are|would)\b""", RegexOption.IGNORE_CASE)
            s += if (question.containsMatchIn(s)) "?" else "."
        }
        return s
    }
}
