package io.github.twatanabe1436.hanaso.core

/** 学習者のレベル。AI の話し方 (文の長さ・語彙) が変わる。 */
enum class Level(val ja: String, val label: String, val partnerGuide: String) {
    BEGINNER(
        ja = "初級",
        label = "beginner (CEFR A1-A2)",
        partnerGuide = "Use short, simple sentences (usually under 10 words) and very common words. " +
            "No idioms or slang. Ask only one simple question per turn. Keep each turn to 1-2 short sentences.",
    ),
    INTERMEDIATE(
        ja = "中級",
        label = "intermediate (CEFR B1-B2)",
        partnerGuide = "Use natural everyday English, including common phrasal verbs and contractions, " +
            "but avoid rare idioms. Keep each turn to 1-3 sentences.",
    ),
    ADVANCED(
        ja = "上級",
        label = "advanced (CEFR C1)",
        partnerGuide = "Speak naturally like a native speaker, with idioms and natural contractions. " +
            "Ask follow-up questions that make the learner explain, give reasons, or tell stories. " +
            "Keep each turn to 2-3 sentences.",
    );

    companion object {
        fun fromName(name: String?): Level = entries.firstOrNull { it.name == name } ?: BEGINNER
    }
}

enum class Category(val ja: String) {
    DAILY("日常"),
    TRAVEL("旅行"),
    SOCIAL("交流"),
    WORK("仕事"),
    FREE("フリートーク"),
}

data class Mission(val id: String, val ja: String)

data class Phrase(val en: String, val ja: String)

/**
 * 会話のシナリオ。setting / aiRole / userRole はプロンプトにそのまま入る英語、*Ja は画面表示用。
 * フリートークは missions が空で、level は null。
 */
data class Scenario(
    val id: String,
    val category: Category,
    val emoji: String,
    val titleJa: String,
    val titleEn: String,
    val level: Level?,
    val descriptionJa: String,
    val setting: String,
    val aiName: String,
    val aiRole: String,
    val userRole: String,
    val userRoleJa: String,
    val opener: String,
    val missions: List<Mission> = emptyList(),
    val keyPhrases: List<Phrase> = emptyList(),
) {
    val isFreeTalk: Boolean get() = category == Category.FREE
}

enum class Speaker { AI, LEARNER }

data class Line(val speaker: Speaker, val text: String)

/** AI に渡す会話の状態。history の先頭は AI の最初のセリフ (opener)。 */
data class Conversation(
    val scenario: Scenario,
    val level: Level,
    val history: List<Line>,
)

// ---- AI の構造化出力 (JSON スキーマと読み取りは StructuredJson.kt) ----

enum class Rating { GREAT, GOOD, FIX }

data class Mistake(val wrong: String, val right: String, val noteJa: String)

data class Feedback(
    val rating: Rating,
    val corrected: String,
    val natural: String,
    val explanationJa: String,
    val mistakes: List<Mistake>,
    val completedMissions: List<String>,
    /** 台本モードのみ: お手本 (natural) のどの単語を言えたか */
    val matchScore: SpeechScore? = null,
)

data class HintSuggestion(val labelJa: String, val en: String, val ja: String)

data class WordNote(val en: String, val ja: String)

data class Translation(val ja: String, val words: List<WordNote>)

data class ImprovePoint(val pointJa: String, val exampleEn: String)

data class Summary(
    val score: Int,
    val headlineJa: String,
    val goodPointsJa: List<String>,
    val improvePoints: List<ImprovePoint>,
    val keyPhrases: List<Phrase>,
    val nextChallengeJa: String,
)
