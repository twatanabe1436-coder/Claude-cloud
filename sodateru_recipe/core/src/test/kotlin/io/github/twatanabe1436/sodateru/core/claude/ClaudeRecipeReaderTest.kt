package io.github.twatanabe1436.sodateru.core.claude

import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.sun.net.httpserver.HttpServer
import io.github.twatanabe1436.sodateru.core.model.Category
import io.github.twatanabe1436.sodateru.core.model.IngredientRole
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.InetSocketAddress
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** ローカルの偽サーバーに向けて、SDK 経由のリクエスト内容と応答の読み取りを確かめる。 */
class ClaudeRecipeReaderTest {

    private lateinit var server: HttpServer
    private var status = 200
    private var responseBody = ""
    private var lastRequest: JsonObject? = null
    private var lastHeaders: Map<String, List<String>> = emptyMap()

    @BeforeTest
    fun start() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { ex ->
            val body = ex.requestBody.readBytes().decodeToString()
            if (body.isNotEmpty()) lastRequest = Json.parseToJsonElement(body).jsonObject
            lastHeaders = ex.requestHeaders.mapKeys { it.key.lowercase() }
            val bytes = responseBody.toByteArray()
            ex.responseHeaders.add("Content-Type", "application/json")
            ex.sendResponseHeaders(status, bytes.size.toLong())
            ex.responseBody.use { it.write(bytes) }
        }
        server.start()
    }

    @AfterTest
    fun stop() = server.stop(0)

    private fun reader(model: String = ClaudeRecipeReader.DEFAULT_MODEL) = ClaudeRecipeReader(
        AnthropicOkHttpClient.builder()
            .apiKey("test-key")
            .baseUrl("http://127.0.0.1:${server.address.port}")
            .maxRetries(0)
            .build(),
        model,
    )

    private fun messageWith(text: String, stopReason: String = "end_turn"): String {
        val escaped = Json.encodeToString(kotlinx.serialization.json.JsonPrimitive.serializer(), kotlinx.serialization.json.JsonPrimitive(text))
        return """
            {"id":"msg_1","type":"message","role":"assistant","model":"claude-opus-5-5",
             "content":[{"type":"text","text":$escaped}],
             "stop_reason":"$stopReason","stop_sequence":null,
             "usage":{"input_tokens":10,"output_tokens":20}}
        """.trimIndent()
    }

    private val recipeJson = """
        {"transcript":"肉じゃが\n材料…","title":"肉じゃが","servings":"2人分","category":"COOKING",
         "ingredients":[{"name":"じゃがいも","amount":"3","unit":"個","note":""},
                        {"name":"砂糖","amount":"1.5","unit":"大さじ","note":"A"},
                        {"name":"","amount":"","unit":"","note":""}],
         "steps":["切る"," 煮る "],"memo":"砂糖 大さじ2→1.5（書き込み）"}
    """.trimIndent()

    @Test
    fun sendsImageWithStructuredOutputAndParsesDraft() {
        responseBody = messageWith(recipeJson)
        val draft = reader().read(listOf(ImageInput("AAAA")), ReadMode.RECIPE)

        assertEquals("肉じゃが", draft.title)
        assertEquals(Category.COOKING, draft.category)
        assertEquals(listOf("じゃがいも", "砂糖"), draft.ingredients.map { it.name })
        assertEquals(IngredientRole.SUGAR, draft.ingredients[1].role)
        assertEquals(listOf("切る", "煮る"), draft.steps)
        assertEquals("砂糖 大さじ2→1.5（書き込み）", draft.memo)

        val req = lastRequest!!
        assertEquals("claude-opus-5-5", req["model"]!!.jsonPrimitive.content)
        val content = req["messages"]!!.jsonArray[0].jsonObject["content"]!!.jsonArray
        val image = content[0].jsonObject
        assertEquals("image", image["type"]!!.jsonPrimitive.content)
        assertEquals("image/jpeg", image["source"]!!.jsonObject["media_type"]!!.jsonPrimitive.content)
        assertEquals("AAAA", image["source"]!!.jsonObject["data"]!!.jsonPrimitive.content)
        assertEquals("text", content[1].jsonObject["type"]!!.jsonPrimitive.content)
        val output = req["output_config"]!!.jsonObject
        assertEquals("medium", output["effort"]!!.jsonPrimitive.content)
        val format = output["format"]!!.jsonObject
        assertEquals("json_schema", format["type"]!!.jsonPrimitive.content)
        assertTrue("ingredients" in format["schema"]!!.jsonObject["properties"]!!.jsonObject)
        assertEquals("default", req["fallbacks"]!!.jsonPrimitive.content)
        assertTrue(lastHeaders["anthropic-beta"].orEmpty().any { "server-side-fallback-2026-07-01" in it })
        assertEquals(listOf("test-key"), lastHeaders["x-api-key"])
    }

    @Test
    fun haikuSkipsEffortAndFallback() {
        responseBody = messageWith(recipeJson)
        reader("claude-haiku-4-5").read(listOf(ImageInput("AAAA")), ReadMode.NOTE)
        val req = lastRequest!!
        assertFalse("effort" in req["output_config"]!!.jsonObject)
        assertFalse("fallbacks" in req)
    }

    @Test
    fun mapsErrorsToFriendlyMessages() {
        status = 401
        responseBody = """{"type":"error","error":{"type":"authentication_error","message":"invalid x-api-key"}}"""
        val e = assertFailsWith<ClaudeReadException> { reader().read(listOf(ImageInput("AAAA")), ReadMode.RECIPE) }
        assertTrue("APIキー" in e.message!!)

        status = 200
        responseBody = messageWith("", stopReason = "refusal")
        assertFailsWith<ClaudeReadException> { reader().read(listOf(ImageInput("AAAA")), ReadMode.RECIPE) }

        responseBody = messageWith("not json")
        assertFailsWith<ClaudeReadException> { reader().read(listOf(ImageInput("AAAA")), ReadMode.RECIPE) }
    }
}
