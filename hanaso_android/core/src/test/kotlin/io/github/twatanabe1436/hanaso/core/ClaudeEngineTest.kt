package io.github.twatanabe1436.hanaso.core

import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * Claude API の代わりにローカルの MockWebServer を立てて、
 * SDK が実際に送るリクエストと、応答の読み取りを確かめる (本物の API は呼ばない)。
 */
class ClaudeEngineTest {
    private lateinit var server: MockWebServer
    private val json = ObjectMapper()
    private val scenario = Catalog.find("cafe")!!
    private val conversation = Conversation(
        scenario = scenario,
        level = Level.BEGINNER,
        history = listOf(Line(Speaker.AI, scenario.opener), Line(Speaker.LEARNER, "a latte please")),
    )

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() = server.shutdown()

    private fun engine(model: String = ClaudeEngine.DEFAULT_MODEL) = ClaudeEngine(
        AnthropicOkHttpClient.builder()
            .apiKey("test-key")
            .baseUrl(server.url("/").toString().trimEnd('/'))
            .maxRetries(0)
            .build(),
        model = model,
    )

    private fun RecordedRequest.bodyJson(): JsonNode = json.readTree(body.readUtf8())

    private fun sse(vararg events: Pair<String, String>) = MockResponse()
        .setHeader("content-type", "text/event-stream")
        .setBody(events.joinToString("") { (name, data) -> "event: $name\ndata: $data\n\n" })

    private fun streamOf(texts: List<String>, stopReason: String) = sse(
        "message_start" to """{"type":"message_start","message":{"id":"msg_1","type":"message","role":"assistant","model":"claude-opus-5-5","content":[],"stop_reason":null,"stop_sequence":null,"usage":{"input_tokens":12,"output_tokens":1}}}""",
        "content_block_start" to """{"type":"content_block_start","index":0,"content_block":{"type":"thinking","thinking":"","signature":""}}""",
        "content_block_delta" to """{"type":"content_block_delta","index":0,"delta":{"type":"signature_delta","signature":"sig"}}""",
        "content_block_stop" to """{"type":"content_block_stop","index":0}""",
        "content_block_start" to """{"type":"content_block_start","index":1,"content_block":{"type":"text","text":""}}""",
        *texts.map { "content_block_delta" to """{"type":"content_block_delta","index":1,"delta":{"type":"text_delta","text":${json.writeValueAsString(it)}}}""" }.toTypedArray(),
        "content_block_stop" to """{"type":"content_block_stop","index":1}""",
        "message_delta" to """{"type":"message_delta","delta":{"stop_reason":"$stopReason","stop_sequence":null},"usage":{"output_tokens":9}}""",
        "message_stop" to """{"type":"message_stop"}""",
    )

    private fun jsonMessage(text: String, stopReason: String = "end_turn") = MockResponse()
        .setHeader("content-type", "application/json")
        .setBody(
            """{"id":"msg_2","type":"message","role":"assistant","model":"claude-opus-5-5",
            "content":[{"type":"thinking","thinking":"","signature":"sig"},{"type":"text","text":${json.writeValueAsString(text)}}],
            "stop_reason":"$stopReason","stop_sequence":null,"usage":{"input_tokens":20,"output_tokens":30}}""",
        )

    private fun error(status: Int, type: String, message: String) = MockResponse()
        .setResponseCode(status)
        .setHeader("content-type", "application/json")
        .setBody("""{"type":"error","error":{"type":"$type","message":"$message"}}""")

    @Test
    fun replyStreamsTextAndSendsExpectedRequest() = runBlocking {
        server.enqueue(streamOf(listOf("Sure! ", "What size ", "would you like?"), "end_turn"))

        val deltas = engine().reply(conversation).toList()
        assertEquals(listOf("Sure! ", "What size ", "would you like?"), deltas)

        val req = server.takeRequest()
        assertEquals("/v1/messages", req.requestUrl!!.encodedPath)
        assertEquals("test-key", req.getHeader("x-api-key"))
        assertTrue(req.getHeader("anthropic-beta")!!.contains(ClaudeEngine.FALLBACK_BETA))
        val body = req.bodyJson()
        assertEquals("claude-opus-5-5", body["model"].asText())
        assertEquals(16000, body["max_tokens"].asInt())
        assertTrue(body["stream"].asBoolean())
        assertEquals("default", body["fallbacks"].asText())
        assertEquals("low", body["output_config"]["effort"].asText())
        assertEquals("ephemeral", body["cache_control"]["type"].asText())
        assertTrue(body["system"].asText().contains(scenario.aiRole))
        val messages = body["messages"]
        assertEquals(3, messages.size())
        assertEquals("user", messages[0]["role"].asText())
        assertEquals("[start]", messages[0]["content"].asText())
        assertEquals("assistant", messages[1]["role"].asText())
        assertEquals(scenario.opener, messages[1]["content"].asText())
        assertEquals("a latte please", messages[2]["content"].asText())
    }

    @Test
    fun replyRefusalBecomesAiException() = runBlocking {
        server.enqueue(streamOf(listOf("Hel"), "refusal"))
        try {
            engine().reply(conversation).toList()
            fail("例外になるはず")
        } catch (e: AiException) {
            assertEquals(AiErrorKind.REFUSAL, e.kind)
        }
    }

    @Test
    fun feedbackUsesStructuredOutputAndParsesKotlinClass() = runBlocking {
        server.enqueue(
            jsonMessage(
                """{"rating":"GOOD","corrected":"A latte, please.","natural":"Can I get a latte, please?",
                "explanationJa":"Can I get ...? がカフェの定番です。","mistakes":[],"completedMissions":["drink"]}""",
            ),
        )

        val fb = engine().feedback(conversation)
        assertEquals(Rating.GOOD, fb.rating)
        assertEquals("Can I get a latte, please?", fb.natural)
        assertEquals(listOf("drink"), fb.completedMissions)

        val body = server.takeRequest().bodyJson()
        assertFalse(body.has("stream") && body["stream"].asBoolean())
        val outputConfig = body["output_config"]
        assertEquals("low", outputConfig["effort"].asText())
        assertEquals("json_schema", outputConfig["format"]["type"].asText())
        val schema = outputConfig["format"]["schema"]
        val props = schema["properties"]
        for (name in listOf("rating", "corrected", "natural", "explanationJa", "mistakes", "completedMissions")) {
            assertTrue("スキーマに $name がない: $schema", props.has(name))
        }
        assertEquals(listOf("GREAT", "GOOD", "FIX"), props["rating"]["enum"].map { it.asText() })
        assertTrue(body["messages"][0]["content"].asText().contains("<utterance_to_review>\na latte please\n</utterance_to_review>"))
        assertEquals("default", body["fallbacks"].asText())
    }

    @Test
    fun hintTranslateAndSummary() = runBlocking {
        server.enqueue(jsonMessage("""{"suggestions":[{"labelJa":"シンプル","en":"A latte, please.","ja":"ラテをください。"}]}"""))
        server.enqueue(jsonMessage("""{"ja":"おはようございます！","words":[{"en":"get started","ja":"始める"}]}"""))
        server.enqueue(
            jsonMessage(
                """{"score":140,"headlineJa":"よくできました","goodPointsJa":["a"],"improvePoints":[{"pointJa":"b","exampleEn":"c"}],
                "keyPhrases":[{"en":"For here, please.","ja":"店内で"}],"nextChallengeJa":"d"}""",
            ),
        )
        val e = engine()
        assertEquals("A latte, please.", e.hint(conversation, "ラテ").single().en)
        assertTrue(server.takeRequest().bodyJson()["messages"][0]["content"].asText().contains("<learner_wants_to_say>\nラテ\n</learner_wants_to_say>"))

        assertEquals("始める", e.translate("Good morning!").words.single().ja)
        server.takeRequest()

        val summary = e.summary(conversation, setOf("drink"))
        assertEquals("スコアは 0-100 に丸める", 100, summary.score)
        val body = server.takeRequest().bodyJson()
        assertEquals("medium", body["output_config"]["effort"].asText())
        assertTrue(body["messages"][0]["content"].asText().contains("- [x] ドリンクをサイズ付きで注文する"))
    }

    @Test
    fun structuredRefusal() = runBlocking {
        server.enqueue(jsonMessage("", stopReason = "refusal"))
        try {
            engine().translate("hi")
            fail("例外になるはず")
        } catch (e: AiException) {
            assertEquals(AiErrorKind.REFUSAL, e.kind)
        }
    }

    @Test
    fun httpErrorsAreClassified() = runBlocking {
        val cases = listOf(
            error(401, "authentication_error", "invalid x-api-key") to AiErrorKind.AUTH,
            error(429, "rate_limit_error", "slow down") to AiErrorKind.BUSY,
            error(529, "overloaded_error", "Overloaded") to AiErrorKind.BUSY,
            error(400, "invalid_request_error", "Your credit balance is too low") to AiErrorKind.BAD_REQUEST,
        )
        for ((response, kind) in cases) {
            server.enqueue(response)
            try {
                engine().translate("hi")
                fail("例外になるはず: $kind")
            } catch (e: AiException) {
                assertEquals(kind, e.kind)
                if (kind == AiErrorKind.BAD_REQUEST) assertTrue(e.detail!!.contains("credit balance"))
            }
        }
    }

    @Test
    fun networkErrorIsClassified() = runBlocking {
        val url = server.url("/").toString().trimEnd('/')
        server.shutdown()
        val e = ClaudeEngine(AnthropicOkHttpClient.builder().apiKey("k").baseUrl(url).maxRetries(0).build())
        try {
            e.translate("hi")
            fail("例外になるはず")
        } catch (ex: AiException) {
            assertEquals(AiErrorKind.NETWORK, ex.kind)
        }
    }

    @Test
    fun haikuSendsNoEffortOrFallbacks() = runBlocking {
        server.enqueue(jsonMessage("""{"ja":"やあ","words":[]}"""))
        engine(model = "claude-haiku-4-5").translate("hi")
        val req = server.takeRequest()
        val body = req.bodyJson()
        assertEquals("claude-haiku-4-5", body["model"].asText())
        assertNull(body["fallbacks"])
        assertNull(body["output_config"]["effort"])
        assertFalse(req.getHeader("anthropic-beta").orEmpty().contains(ClaudeEngine.FALLBACK_BETA))
    }
}
