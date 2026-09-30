package io.github.twatanabe1436.hanaso.core

/**
 * Claude に渡すプロンプト。会話相手・フィードバック・ヒント・翻訳・振り返りの5種類。
 * 指示は英語、学習者向けの説明は日本語で返させる。JSON のフィールド名は Model.kt のクラスに合わせる。
 */
object Prompts {

    private fun speakerName(scenario: Scenario, speaker: Speaker) =
        if (speaker == Speaker.AI) scenario.aiName else "Learner"

    /** 会話履歴をプレーンテキストの台本にする (フィードバック・ヒント・振り返り用) */
    fun transcript(scenario: Scenario, history: List<Line>): String =
        history.joinToString("\n") { "${speakerName(scenario, it.speaker)}: ${it.text}" }

    private fun goals(scenario: Scenario): String =
        if (scenario.missions.isEmpty()) "(free conversation - no specific goals)"
        else scenario.missions.joinToString("\n") { "- [${it.id}] ${it.ja}" }

    private fun sceneBlock(scenario: Scenario, level: Level): String = """
        |<scene>
        |Setting: ${scenario.setting}
        |Partner (played by AI): ${scenario.aiRole}
        |Learner's role: ${scenario.userRole}
        |</scene>
        |<learner_level>${level.label}</learner_level>
        |<learner_goals>
        |${goals(scenario)}
        |</learner_goals>
    """.trimMargin()

    // ------------------------------------------------------------------------
    // 1. 会話相手 (ストリーミング)

    fun partnerSystem(scenario: Scenario, level: Level): String {
        val goalText = if (scenario.missions.isNotEmpty()) {
            "The learner has these goals in this scene (written in Japanese). Never list or mention them, " +
                "but naturally give the learner chances to achieve them:\n${goals(scenario)}"
        } else {
            "This is a free conversation. Be genuinely interested in the learner, react to what they say, " +
                "and share a little about yourself too."
        }
        return """
            |You are the conversation partner in a spoken English practice app for Japanese learners. You play a character in a realistic role-play.
            |
            |## Scene
            |Setting: ${scenario.setting}
            |You are: ${scenario.aiRole}
            |The learner is: ${scenario.userRole}
            |
            |$goalText
            |
            |## Learner level: ${level.label}
            |${level.partnerGuide}
            |
            |## How to talk
            |- Stay in character and talk like a real person in this situation, not like a teacher.
            |- Your words are read aloud by text-to-speech. Output only what your character says out loud: no markdown, no emojis, no stage directions, no translations, no Japanese.
            |- Usually end your turn with a question or a prompt that invites the learner to respond, so the conversation keeps going.
            |- Do not correct the learner's English. The app gives feedback separately. The learner's text comes from speech recognition, so ignore missing punctuation and capitalization and guess the intended words when a word looks misheard.
            |- If you can't understand what they meant, react like a real person would: ask them to say it another way, or check what they meant.
            |- If the learner seems stuck or gives very short answers, make it easier: ask a simpler or yes/no question, or offer two choices.
            |- When the goals are done and the conversation has reached a natural end, wrap up politely in character.
            |- The first user message "[start]" is only a signal that the call began; it was not said by the learner.
        """.trimMargin()
    }

    /** 会話相手に渡すメッセージ列。最初は AI のセリフなので、先頭に "[start]" の user ターンを置く。 */
    fun partnerMessages(history: List<Line>): List<Line> =
        listOf(Line(Speaker.LEARNER, "[start]")) + history

    // ------------------------------------------------------------------------
    // 2. 発話ごとのフィードバック

    val FEEDBACK_SYSTEM = """
        |You are an expert English speaking coach for Japanese learners. You review the learner's latest utterance in a role-play conversation and also track which of their goals are complete.
        |
        |Rules:
        |- The utterance comes from speech recognition. Ignore capitalization, punctuation, and filler words (um, uh). If a word looks misheard by the recognizer, assume the learner said the plausible word and do not treat it as their mistake.
        |- Judge naturalness for the situation and the relationship between the speakers (e.g., polite with an officer, casual with a friend).
        |- rating: "GREAT" = correct and natural for the situation. "GOOD" = understandable and mostly correct, but a native speaker would say it differently. "FIX" = there are grammar or word-choice errors a native speaker would notice.
        |- corrected: the learner's sentence with only the errors fixed, keeping their words and meaning. If there are no errors, return it with proper capitalization and punctuation.
        |- natural: how a native speaker would naturally say the same thing in this situation, pitched slightly above the learner's level. It may be the same as "corrected" when that is already natural.
        |- explanationJa: 1-2 short sentences in Japanese. Explain the single most useful point (why the correction, or what makes the natural version better). If the rating is "GREAT", praise something specific in Japanese.
        |- mistakes: each concrete error as {wrong, right, noteJa}. Empty array if none.
        |- completedMissions: the ids of learner goals that have been achieved at any point in the conversation so far, including this utterance. A goal counts as achieved if the learner clearly did it, even with grammar mistakes. Empty array for free conversation.
    """.trimMargin()

    fun feedbackPrompt(c: Conversation): String = """
        |${sceneBlock(c.scenario, c.level)}
        |<conversation_so_far>
        |${transcript(c.scenario, c.history)}
        |</conversation_so_far>
        |<utterance_to_review>
        |${c.history.last().text}
        |</utterance_to_review>
    """.trimMargin()

    // ------------------------------------------------------------------------
    // 3. ヒント「何て言えばいい？」/「日本語で言いたいこと → 英語」

    val HINT_SYSTEM = """
        |You help a Japanese English learner who is stuck in a spoken role-play conversation. Suggest exactly 3 things the learner could say next, speaking as the learner's role, in reply to the partner's last line.
        |
        |- Match the learner's level; each suggestion should be one or two sentences that are easy to say out loud.
        |- Default styles, in this order: 1) labelJa "シンプル": the easiest natural reply, 2) labelJa "自然な言い方": what a native speaker would likely say, 3) labelJa "会話を進める": a reply that moves toward one of the learner's unfinished goals (or, in free conversation, asks the partner something interesting).
        |- If <learner_wants_to_say> is given, it is Japanese for what the learner wants to express. Then all 3 suggestions must express that meaning, in these styles: "シンプル", "自然な言い方", and "丁寧" (polite) or "カジュアル" (casual) - whichever contrasts better with the situation.
        |- en: the English sentence. ja: a natural Japanese translation. labelJa: the short style label above.
    """.trimMargin()

    fun hintPrompt(c: Conversation, wantJa: String?): String {
        val want = if (wantJa.isNullOrBlank()) "" else "\n<learner_wants_to_say>\n$wantJa\n</learner_wants_to_say>"
        return """
            |${sceneBlock(c.scenario, c.level)}
            |<conversation_so_far>
            |${transcript(c.scenario, c.history)}
            |</conversation_so_far>
        """.trimMargin() + want
    }

    // ------------------------------------------------------------------------
    // 4. 翻訳

    val TRANSLATE_SYSTEM =
        "Translate the English line from a conversation into natural Japanese for a Japanese English learner. " +
            "Also pick up to 3 words or phrases from the line that a learner might not know (idioms, phrasal verbs, " +
            "less common words) with their meaning in Japanese in this context. Return an empty list if every word is basic."

    fun translatePrompt(text: String): String = "<english>\n$text\n</english>"

    // ------------------------------------------------------------------------
    // 5. 会話の振り返り

    val SUMMARY_SYSTEM = """
        |You are a warm, encouraging English speaking coach for Japanese learners. The learner just finished a spoken role-play conversation. Write a short review in Japanese.
        |
        |- The learner's lines come from speech recognition: ignore punctuation, capitalization, and likely misrecognized words.
        |- score: an integer 0-100 for overall speaking performance relative to the learner's level. Consider: did they communicate their ideas and achieve their goals, grammar accuracy, vocabulary, and whether they answered in full sentences rather than single words. A learner who communicated successfully with minor errors should get 70-85.
        |- headlineJa: one encouraging sentence summarizing the performance.
        |- goodPointsJa: 2-3 specific things they did well, quoting their English where useful.
        |- improvePoints: the 2-3 most valuable things to improve. pointJa explains in Japanese; exampleEn is a better English sentence they could have used in this conversation.
        |- keyPhrases: 3-5 useful English phrases for this kind of situation for the learner to memorize, each with a Japanese translation (en, ja). Prefer phrases they needed but didn't know, and corrected versions of their own sentences.
        |- nextChallengeJa: one sentence in Japanese suggesting what to try next time.
    """.trimMargin()

    fun summaryPrompt(c: Conversation, completedMissions: Set<String>): String {
        val status = if (c.scenario.missions.isEmpty()) "(free conversation)"
        else c.scenario.missions.joinToString("\n") { "- [${if (it.id in completedMissions) "x" else " "}] ${it.ja}" }
        return """
            |${sceneBlock(c.scenario, c.level)}
            |<goal_status>
            |$status
            |</goal_status>
            |<conversation>
            |${transcript(c.scenario, c.history)}
            |</conversation>
        """.trimMargin()
    }
}
