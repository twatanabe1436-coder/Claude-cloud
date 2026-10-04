package io.github.twatanabe1436.hanaso

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.twatanabe1436.hanaso.core.AiErrorKind
import io.github.twatanabe1436.hanaso.core.AiException
import io.github.twatanabe1436.hanaso.core.Catalog
import io.github.twatanabe1436.hanaso.core.ClaudeEngine
import io.github.twatanabe1436.hanaso.core.Conversation
import io.github.twatanabe1436.hanaso.core.Level
import io.github.twatanabe1436.hanaso.core.Line
import io.github.twatanabe1436.hanaso.core.Rating
import io.github.twatanabe1436.hanaso.core.Speaker
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 公式 Claude SDK (Java) が Android の実行環境 (ART) でも動くことを、
 * 端末上に立てたテスト用サーバーに対して確かめる (本物の API は呼ばない)。
 */
@RunWith(AndroidJUnit4::class)
class ClaudeEngineOnDeviceTest {
    private lateinit var server: MockWebServer
    private val cafe = Catalog.find("cafe")!!
    private val conversation = Conversation(
        cafe,
        Level.A2,
        listOf(Line(Speaker.AI, cafe.opener), Line(Speaker.LEARNER, "a latte please")),
    )

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() = server.shutdown()

    private fun engine() = ClaudeEngine.create("test-key", baseUrl = server.url("/").toString().trimEnd('/'))

    private fun sse(vararg events: String) = MockResponse()
        .setHeader("content-type", "text/event-stream")
        .setBody(events.joinToString("") { data -> "event: ${JSONObject(data).getString("type")}\ndata: $data\n\n" })

    @Test
    fun streamingReplyWorksOnDevice() = runBlocking {
        server.enqueue(
            sse(
                """{"type":"message_start","message":{"id":"m1","type":"message","role":"assistant","model":"claude-opus-5-5","content":[],"stop_reason":null,"stop_sequence":null,"usage":{"input_tokens":5,"output_tokens":1}}}""",
                """{"type":"content_block_start","index":0,"content_block":{"type":"text","text":""}}""",
                """{"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"Sure! "}}""",
                """{"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"What size?"}}""",
                """{"type":"content_block_stop","index":0}""",
                """{"type":"message_delta","delta":{"stop_reason":"end_turn","stop_sequence":null},"usage":{"output_tokens":4}}""",
                """{"type":"message_stop"}""",
            ),
        )
        assertEquals("Sure! What size?", engine().reply(conversation).toList().joinToString(""))
        val body = JSONObject(server.takeRequest().body.readUtf8())
        assertEquals("default", body.getString("fallbacks"))
        assertEquals("[start]", body.getJSONArray("messages").getJSONObject(0).getString("content"))
    }

    @Test
    fun structuredFeedbackWorksOnDevice() = runBlocking {
        val feedback = """{"rating":"FIX","corrected":"A latte, please.","natural":"Can I get a latte, please?","explanationJa":"説明","mistakes":[{"wrong":"a","right":"A","noteJa":"文頭"}],"completedMissions":["drink"]}"""
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    JSONObject()
                        .put("id", "m2").put("type", "message").put("role", "assistant").put("model", "claude-opus-5-5")
                        .put("content", org.json.JSONArray().put(JSONObject().put("type", "text").put("text", feedback)))
                        .put("stop_reason", "end_turn").put("stop_sequence", JSONObject.NULL)
                        .put("usage", JSONObject().put("input_tokens", 1).put("output_tokens", 1))
                        .toString(),
                ),
        )
        val fb = engine().feedback(conversation)
        assertEquals(Rating.FIX, fb.rating)
        assertEquals("Can I get a latte, please?", fb.natural)
        assertEquals("A", fb.mistakes.single().right)

        // JSON スキーマが端末上でも Kotlin のクラスから生成できている
        val schema = JSONObject(server.takeRequest().body.readUtf8())
            .getJSONObject("output_config").getJSONObject("format").getJSONObject("schema")
        assertTrue(schema.getJSONObject("properties").has("explanationJa"))
    }

    @Test
    fun authErrorIsReported() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(401).setHeader("content-type", "application/json")
                .setBody("""{"type":"error","error":{"type":"authentication_error","message":"invalid x-api-key"}}"""),
        )
        try {
            engine().translate("hi")
            fail("例外になるはず")
        } catch (e: AiException) {
            assertEquals(AiErrorKind.AUTH, e.kind)
        }
    }
}
