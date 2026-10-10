package io.github.twatanabe1436.hanaso.core

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class XpTest {
    private val tokyo = ZoneId.of("Asia/Tokyo")
    private fun at(y: Int, m: Int, d: Int, h: Int = 12) = ZonedDateTime.of(y, m, d, h, 0, 0, 0, tokyo).toInstant().toEpochMilli()

    @Test
    fun pointsRewardBothAmountAndQuality() {
        assertEquals(10, Xp.forUtterance(Rating.GREAT))
        assertEquals(7, Xp.forUtterance(Rating.GOOD))
        assertEquals(3, Xp.forUtterance(Rating.FIX))
        assertEquals(5, Xp.forPractice(100))
        assertEquals(3, Xp.forPractice(60))
        assertEquals(1, Xp.forPractice(0))
    }

    @Test
    fun weeksStartOnMondayInJapanTime() {
        // 2026-10-04 は日曜、10-05 は月曜
        assertEquals("2026-W40", Xp.weekId(at(2026, 10, 4, 23)))
        assertEquals("2026-W41", Xp.weekId(at(2026, 10, 5, 0)))
        assertEquals("2026-W41", Xp.weekId(at(2026, 10, 11, 23)))
        // 年をまたぐ ISO 週
        assertEquals("2026-W53", Xp.weekId(at(2027, 1, 1)))
        assertEquals(at(2026, 10, 12, 0), Xp.weekEndsAt(at(2026, 10, 10)))
        assertEquals(at(2026, 10, 12, 0), Xp.weekEndsAt(at(2026, 10, 5, 0)))
    }
}

class LeagueTest {
    private lateinit var server: MockWebServer
    private val mapper = ObjectMapper()
    private var now = 1_000_000L

    private class MemoryTokens(var saved: String? = null) : LeagueTokenStore {
        override fun load() = saved
        override fun save(refreshToken: String) {
            saved = refreshToken
        }
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() = server.shutdown()

    private fun league(tokens: LeagueTokenStore): FirebaseLeague {
        val base = server.url("/").toString().trimEnd('/')
        return FirebaseLeague("hanaso-test", "KEY", tokens, base, base, base, clock = { now })
    }

    private fun json(body: String, code: Int = 200) = MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)

    @Test
    fun signsUpAnonymouslySubmitsAndReadsTheRanking() = runBlocking {
        val tokens = MemoryTokens()
        server.enqueue(json("""{"idToken":"id-1","refreshToken":"refresh-1","localId":"me","expiresIn":"3600"}"""))
        server.enqueue(json("""{"writeResults":[{}],"commitTime":"2026-10-10T00:00:00Z"}"""))
        server.enqueue(
            json(
                """[
                  {"document":{"name":"projects/hanaso-test/databases/(default)/documents/leagues/2026-W41/players/a","fields":{"name":{"stringValue":"Aki"},"xp":{"integerValue":"240"},"level":{"stringValue":"B1"}}},"readTime":"x"},
                  {"document":{"name":"projects/hanaso-test/databases/(default)/documents/leagues/2026-W41/players/me","fields":{"name":{"stringValue":"Ken"},"xp":{"integerValue":"120"},"level":{"stringValue":"A2"}}},"readTime":"x"}
                ]""",
            ),
        )
        val league = league(tokens)
        assertNull(league.uid)
        league.submit("2026-W41", "Ken", 120, Level.A2)
        assertEquals("me", league.uid)
        assertEquals("refresh-1", tokens.saved)

        val signUp = server.takeRequest()
        assertEquals("/v1/accounts:signUp?key=KEY", signUp.path)
        assertTrue(mapper.readTree(signUp.body.readUtf8())["returnSecureToken"].asBoolean())

        val commit = server.takeRequest()
        assertEquals("/v1/projects/hanaso-test/databases/(default)/documents:commit", commit.path)
        assertEquals("Bearer id-1", commit.getHeader("Authorization"))
        val update = mapper.readTree(commit.body.readUtf8())["writes"][0]["update"]
        assertEquals("projects/hanaso-test/databases/(default)/documents/leagues/2026-W41/players/me", update["name"].asText())
        assertEquals("Ken", update["fields"]["name"]["stringValue"].asText())
        assertEquals("120", update["fields"]["xp"]["integerValue"].asText())
        assertEquals("A2", update["fields"]["level"]["stringValue"].asText())

        // トークンはまだ有効なので、登録し直さずに読む
        val top = league.top("2026-W41")
        assertEquals(listOf("Aki", "Ken"), top.map { it.name })
        assertEquals(listOf(240, 120), top.map { it.xp })
        assertEquals(Level.B1, top[0].level)
        assertEquals("me", top[1].uid)
        val query = server.takeRequest()
        assertEquals("/v1/projects/hanaso-test/databases/(default)/documents/leagues/2026-W41:runQuery", query.path)
        val structured = mapper.readTree(query.body.readUtf8())["structuredQuery"]
        assertEquals("players", structured["from"][0]["collectionId"].asText())
        assertEquals("DESCENDING", structured["orderBy"][0]["direction"].asText())
        assertEquals(50, structured["limit"].asInt())
    }

    @Test
    fun emptyWeekAndTokenRefresh() = runBlocking {
        val tokens = MemoryTokens("saved-refresh")
        server.enqueue(json("""{"id_token":"id-2","refresh_token":"refresh-2","user_id":"me","expires_in":"3600"}"""))
        server.enqueue(json("""[{"readTime":"2026-10-10T00:00:00Z"}]"""))
        server.enqueue(json("""{"id_token":"id-3","refresh_token":"refresh-3","user_id":"me","expires_in":"3600"}"""))
        server.enqueue(json("""[{"readTime":"2026-10-10T00:00:00Z"}]"""))
        val league = league(tokens)

        assertTrue(league.top("2026-W41").isEmpty())
        val refresh = server.takeRequest()
        assertEquals("/v1/token?key=KEY", refresh.path)
        assertEquals("grant_type=refresh_token&refresh_token=saved-refresh", refresh.body.readUtf8())
        assertEquals("refresh-2", tokens.saved)
        server.takeRequest()

        // 1 時間たつと、更新用トークンで ID トークンを取り直す
        now += 3_600_000
        league.top("2026-W41")
        assertEquals("grant_type=refresh_token&refresh_token=refresh-2", server.takeRequest().body.readUtf8())
        assertEquals("Bearer id-3", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun invalidRefreshTokenSignsUpAgain() = runBlocking {
        val tokens = MemoryTokens("revoked")
        server.enqueue(json("""{"error":{"code":400,"message":"INVALID_REFRESH_TOKEN"}}""", 400))
        server.enqueue(json("""{"idToken":"id-4","refreshToken":"refresh-4","localId":"new-me","expiresIn":"3600"}"""))
        server.enqueue(json("""{}"""))
        league(tokens).submit("2026-W41", "Ken", 10, Level.B2)
        assertEquals("/v1/token?key=KEY", server.takeRequest().path)
        assertEquals("/v1/accounts:signUp?key=KEY", server.takeRequest().path)
        assertEquals("refresh-4", tokens.saved)
    }

    @Test
    fun errorsBecomeJapaneseMessages() = runBlocking {
        server.enqueue(json("""{"idToken":"id-5","refreshToken":"r","localId":"me","expiresIn":"3600"}"""))
        server.enqueue(json("""{"error":{"code":403,"status":"PERMISSION_DENIED"}}""", 403))
        val league = league(MemoryTokens())
        try {
            league.submit("2026-W41", "Ken", 10, Level.A1)
            fail("403 なのに成功した")
        } catch (e: LeagueException) {
            assertTrue(e.messageJa, e.messageJa.contains("参加できませんでした"))
        }

        server.shutdown()
        try {
            league("unused").top("2026-W41")
            fail("つながらないのに成功した")
        } catch (e: LeagueException) {
            assertTrue(e.messageJa, e.messageJa.contains("インターネット接続"))
        }
    }

    private fun league(saved: String) = league(MemoryTokens(saved))
}
