package io.github.twatanabe1436.hanaso.core

/**
 * 学習者のレベル (CEFR A1〜C2)。AI 会話では相手の語彙・文法・文の長さが変わり、
 * 台本モードではシナリオの目安と、フリートークの回答例の難しさに使う。
 */
enum class Level(
    /** 日本語の呼び名 */
    val ja: String,
    /** できることの目安 (画面表示用) */
    val descriptionJa: String,
    /** プロンプトに入れるレベル名 */
    val label: String,
    /** AI 会話の相手の話し方 */
    val partnerGuide: String,
) {
    A1(
        ja = "入門",
        descriptionJa = "あいさつや自己紹介など、ごく基本的な単語と表現でやりとりできる",
        label = "CEFR A1 (beginner)",
        partnerGuide = "Use only very basic, high-frequency words (CEFR A1 vocabulary, roughly the 500-800 most common English words) " +
            "and mostly the simple present tense. Keep sentences very short (usually under 8 words). " +
            "No idioms, phrasal verbs, or slang. Ask only one simple question per turn, often a yes/no or either/or question. " +
            "Keep each turn to 1-2 short sentences.",
    ),
    A2(
        ja = "初級",
        descriptionJa = "買い物・道案内・食事など、身近な場面で短い文のやりとりができる",
        label = "CEFR A2 (elementary)",
        partnerGuide = "Use common everyday words (CEFR A2 vocabulary, roughly the 1,000-1,500 most common English words), " +
            "simple present, past, and future (going to / will), and short sentences (usually under 10 words). " +
            "No idioms or slang; only the most common phrasal verbs. Ask only one simple question per turn. " +
            "Keep each turn to 1-2 short sentences.",
    ),
    B1(
        ja = "中級",
        descriptionJa = "旅行や仕事の場面で、経験や理由を添えて話せる",
        label = "CEFR B1 (intermediate)",
        partnerGuide = "Use natural everyday English with CEFR B1 vocabulary: common phrasal verbs and contractions are fine, " +
            "but avoid idioms and rare words. Use a normal range of tenses. Ask about experiences, reasons, and plans. " +
            "Keep each turn to 1-3 sentences.",
    ),
    B2(
        ja = "中上級",
        descriptionJa = "幅広い話題で、意見を述べて理由を説明し、議論できる",
        label = "CEFR B2 (upper intermediate)",
        partnerGuide = "Use natural, fluent English with CEFR B2 vocabulary, including common idioms, phrasal verbs, " +
            "and complex sentences. Ask follow-up questions that make the learner give opinions, explain, and compare. " +
            "Keep each turn to 2-3 sentences.",
    ),
    C1(
        ja = "上級",
        descriptionJa = "複雑な話題でも、自然な言い回しで流暢に、論理立てて話せる",
        label = "CEFR C1 (advanced)",
        partnerGuide = "Speak naturally like an educated native speaker with CEFR C1 vocabulary: idiomatic expressions, " +
            "precise word choice, and complex sentences. Push the learner to justify, hypothesize, and handle counterarguments. " +
            "Keep each turn to 2-3 sentences.",
    ),
    C2(
        ja = "最上級",
        descriptionJa = "ネイティブに近い正確さで、細かなニュアンスまで言い分けられる",
        label = "CEFR C2 (proficient)",
        partnerGuide = "Speak like an articulate native speaker without simplifying anything (CEFR C2): sophisticated, precise " +
            "vocabulary, idioms, understatement, irony, and subtle nuance. Challenge the learner's ideas and ask probing questions. " +
            "Keep each turn to 2-4 sentences.",
    );

    /** 画面表示用 (例: 「A2 初級」) */
    val displayJa: String get() = "$name $ja"

    /** A / B / C の大きな段階 */
    val band: Char get() = name[0]

    companion object {
        val DEFAULT = A2

        /** 保存されていた名前から戻す (0.2.0 までの 3 段階の名前にも対応) */
        fun fromName(name: String?): Level = when (name) {
            "BEGINNER" -> A2
            "INTERMEDIATE" -> B1
            "ADVANCED" -> C1
            else -> entries.firstOrNull { it.name == name } ?: DEFAULT
        }
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

/** 英文と日本語訳。level は台本の回答例のレベルの目安 (なければ null) */
data class Phrase(val en: String, val ja: String, val level: Level? = null)

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
    /** 相手の役 (画面表示用) */
    val aiRoleJa: String = "",
    /** 会話画面の最初に出す場面の説明。お題に答えるのに必要な事情も書く (画面表示用) */
    val backgroundJa: String = "",
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
    /** AI が推定した、今回の発話の CEFR レベル (台本モードでは null) */
    val estimatedLevel: Level? = null,
    /** 推定の理由と、次のレベルに必要なこと (日本語) */
    val levelCommentJa: String = "",
)
