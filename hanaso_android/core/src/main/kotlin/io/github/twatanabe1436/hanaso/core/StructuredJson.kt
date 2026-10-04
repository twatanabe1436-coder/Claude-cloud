package io.github.twatanabe1436.hanaso.core

import com.anthropic.core.JsonValue
import com.anthropic.models.beta.messages.BetaJsonOutputFormat
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

/**
 * 構造化出力の JSON スキーマと、返ってきた JSON の読み取り。
 *
 * SDK にはクラスからスキーマを自動生成する機能もあるが、その仕組み (victools) は Android にない
 * java.lang.reflect の API を使うため、スキーマは明示的に書き、JSON はリフレクションなしで読む。
 */
internal object StructuredJson {

    // ---- スキーマ (Claude の構造化出力: 全プロパティ required・additionalProperties false) ----

    private val STRING: Map<String, Any> = mapOf("type" to "string")
    private val INTEGER: Map<String, Any> = mapOf("type" to "integer")

    private fun arrayOf(items: Map<String, Any>): Map<String, Any> = mapOf("type" to "array", "items" to items)

    private fun enumOf(values: List<String>): Map<String, Any> = mapOf("type" to "string", "enum" to values)

    private fun objectOf(vararg properties: Pair<String, Map<String, Any>>): Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to properties.toMap(),
        "required" to properties.map { it.first },
        "additionalProperties" to false,
    )

    private val PHRASE = objectOf("en" to STRING, "ja" to STRING)

    val FEEDBACK = objectOf(
        "rating" to enumOf(Rating.entries.map { it.name }),
        "corrected" to STRING,
        "natural" to STRING,
        "explanationJa" to STRING,
        "mistakes" to arrayOf(objectOf("wrong" to STRING, "right" to STRING, "noteJa" to STRING)),
        "completedMissions" to arrayOf(STRING),
    )

    val HINTS = objectOf(
        "suggestions" to arrayOf(objectOf("labelJa" to STRING, "en" to STRING, "ja" to STRING)),
    )

    val TRANSLATION = objectOf(
        "ja" to STRING,
        "words" to arrayOf(PHRASE),
    )

    val SUMMARY = objectOf(
        "score" to INTEGER,
        "headlineJa" to STRING,
        "goodPointsJa" to arrayOf(STRING),
        "improvePoints" to arrayOf(objectOf("pointJa" to STRING, "exampleEn" to STRING)),
        "keyPhrases" to arrayOf(PHRASE),
        "nextChallengeJa" to STRING,
        "estimatedLevel" to enumOf(Level.entries.map { it.name }),
        "levelCommentJa" to STRING,
    )

    /** スキーマを SDK の出力形式にする */
    fun format(schema: Map<String, Any>): BetaJsonOutputFormat {
        val builder = BetaJsonOutputFormat.Schema.builder()
        schema.forEach { (key, value) -> builder.putAdditionalProperty(key, JsonValue.from(value)) }
        return BetaJsonOutputFormat.builder().schema(builder.build()).build()
    }

    // ---- 読み取り (項目が欠けていても落ちないようにする) ----

    private val mapper = ObjectMapper()

    private fun JsonNode.str(field: String): String = get(field)?.takeIf { !it.isNull }?.asText().orEmpty()

    private fun JsonNode.items(field: String): List<JsonNode> = get(field)?.takeIf { it.isArray }?.toList().orEmpty()

    private fun JsonNode.strings(field: String): List<String> = items(field).map { it.asText() }

    private fun JsonNode.phrase() = Phrase(str("en"), str("ja"))

    fun feedback(json: String): Feedback {
        val n = mapper.readTree(json)
        return Feedback(
            rating = Rating.entries.firstOrNull { it.name == n.str("rating").uppercase() } ?: Rating.GOOD,
            corrected = n.str("corrected"),
            natural = n.str("natural"),
            explanationJa = n.str("explanationJa"),
            mistakes = n.items("mistakes").map { Mistake(it.str("wrong"), it.str("right"), it.str("noteJa")) },
            completedMissions = n.strings("completedMissions"),
        )
    }

    fun hints(json: String): List<HintSuggestion> =
        mapper.readTree(json).items("suggestions").map { HintSuggestion(it.str("labelJa"), it.str("en"), it.str("ja")) }

    fun translation(json: String): Translation {
        val n = mapper.readTree(json)
        return Translation(n.str("ja"), n.items("words").map { WordNote(it.str("en"), it.str("ja")) })
    }

    fun summary(json: String): Summary {
        val n = mapper.readTree(json)
        return Summary(
            score = n.get("score")?.asInt() ?: 0,
            headlineJa = n.str("headlineJa"),
            goodPointsJa = n.strings("goodPointsJa"),
            improvePoints = n.items("improvePoints").map { ImprovePoint(it.str("pointJa"), it.str("exampleEn")) },
            keyPhrases = n.items("keyPhrases").map { it.phrase() },
            nextChallengeJa = n.str("nextChallengeJa"),
            estimatedLevel = Level.entries.firstOrNull { it.name == n.str("estimatedLevel").uppercase() },
            levelCommentJa = n.str("levelCommentJa"),
        )
    }
}
