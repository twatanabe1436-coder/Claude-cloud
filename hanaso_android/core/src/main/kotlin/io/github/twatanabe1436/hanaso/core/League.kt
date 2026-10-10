package io.github.twatanabe1436.hanaso.core

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** 週ごとのリーグの 1 人分 */
data class LeagueEntry(val uid: String, val name: String, val xp: Int, val level: Level?)

/** オンラインのリーグ。実装は Firebase (FirebaseLeague)、テストでは偽物に差し替える */
interface LeagueService {
    /** この端末のプレイヤー ID (まだサーバーに登録していなければ null) */
    val uid: String?

    /** その週の自分の XP を送る (上書き) */
    suspend fun submit(week: String, name: String, xp: Int, level: Level)

    /** その週の XP の上位 */
    suspend fun top(week: String, limit: Int = 50): List<LeagueEntry>
}

/** リーグとのやりとりの失敗。messageJa は画面に出す */
class LeagueException(val messageJa: String, cause: Throwable? = null) : Exception(messageJa, cause)

/** 匿名ログインの更新用トークンを端末に保存する口 (アプリ側で実装する) */
interface LeagueTokenStore {
    fun load(): String?
    fun save(refreshToken: String)
}

/**
 * Firebase (匿名ログイン + Firestore) を使うリーグ。SDK は使わず REST API を呼ぶ。
 *
 * データは leagues/{週}/players/{プレイヤー ID} に { name, xp, level } で置く。
 * 誰が何を書けるかは Firestore のルール (firebase/firestore.rules) で決める: 読むのはログインした人、
 * 書くのは自分の分だけで、項目と値の範囲も制限する。プロジェクト ID と API キーは秘密ではない。
 */
class FirebaseLeague(
    private val projectId: String,
    private val apiKey: String,
    private val tokens: LeagueTokenStore,
    private val authUrl: String = "https://identitytoolkit.googleapis.com",
    private val tokenUrl: String = "https://securetoken.googleapis.com",
    private val firestoreUrl: String = "https://firestore.googleapis.com",
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = System::currentTimeMillis,
) : LeagueService {

    private val mapper = ObjectMapper()
    private val mutex = Mutex()
    private var idToken: String? = null
    private var expiresAt = 0L

    @Volatile
    override var uid: String? = null
        private set

    private val documents get() = "projects/$projectId/databases/(default)/documents"

    override suspend fun submit(week: String, name: String, xp: Int, level: Level) = guard {
        val token = token()
        val body = mapper.createObjectNode()
        body.putArray("writes").addObject().putObject("update").apply {
            put("name", "$documents/leagues/$week/players/$uid")
            putObject("fields").apply {
                putObject("name").put("stringValue", name)
                putObject("xp").put("integerValue", xp.coerceIn(0, Xp.WEEKLY_CAP).toString())
                putObject("level").put("stringValue", level.name)
            }
        }
        post("$firestoreUrl/v1/$documents:commit", json = body.toString(), bearer = token)
        Unit
    }

    override suspend fun top(week: String, limit: Int): List<LeagueEntry> = guard {
        val token = token()
        val query = mapper.createObjectNode()
        query.putObject("structuredQuery").apply {
            putArray("from").addObject().put("collectionId", "players")
            putArray("orderBy").addObject().apply {
                putObject("field").put("fieldPath", "xp")
                put("direction", "DESCENDING")
            }
            put("limit", limit)
        }
        val result = post("$firestoreUrl/v1/$documents/leagues/$week:runQuery", json = query.toString(), bearer = token)
        // 結果は配列。該当がなければ document のない要素 (readTime だけ) が 1 つ返る
        result.mapNotNull { it.get("document") }.map { doc ->
            val fields = doc.get("fields")
            fun field(name: String, type: String) = fields?.get(name)?.get(type)?.asText()
            LeagueEntry(
                uid = doc.get("name").asText().substringAfterLast('/'),
                name = field("name", "stringValue").orEmpty(),
                xp = field("xp", "integerValue")?.toIntOrNull() ?: 0,
                level = field("level", "stringValue")?.let { name -> Level.entries.firstOrNull { it.name == name } },
            )
        }
    }

    /** ID トークン (1 時間有効)。期限が近ければ更新し、まだ登録していなければ匿名で登録する */
    private suspend fun token(): String = mutex.withLock {
        idToken?.takeIf { clock() < expiresAt - 60_000 }?.let { return it }
        val saved = tokens.load()
        val refreshed = saved?.let { refreshToken ->
            try {
                post("$tokenUrl/v1/token?key=$apiKey", form = "grant_type=refresh_token&refresh_token=" + URLEncoder.encode(refreshToken, "UTF-8"))
            } catch (e: HttpError) {
                // 無効になったトークン (400) なら登録し直す
                if (e.code == 400) null else throw e
            }
        }
        if (refreshed != null) {
            remember(refreshed.text("id_token"), refreshed.text("refresh_token"), refreshed.text("user_id"), refreshed.text("expires_in"))
        } else {
            val created = post("$authUrl/v1/accounts:signUp?key=$apiKey", json = """{"returnSecureToken":true}""")
            remember(created.text("idToken"), created.text("refreshToken"), created.text("localId"), created.text("expiresIn"))
        }
    }

    private fun remember(id: String, refresh: String, user: String, expiresIn: String): String {
        if (id.isBlank() || user.isBlank()) throw LeagueException(UNEXPECTED)
        idToken = id
        uid = user
        expiresAt = clock() + (expiresIn.toLongOrNull() ?: 3600L) * 1000
        if (refresh.isNotBlank()) tokens.save(refresh)
        return id
    }

    private fun JsonNode.text(field: String): String = get(field)?.asText().orEmpty()

    private class HttpError(val code: Int, val body: String) : Exception("HTTP $code: $body")

    private suspend fun post(url: String, json: String? = null, form: String? = null, bearer: String? = null): JsonNode = withContext(io) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.setRequestProperty(
                "Content-Type",
                if (form != null) "application/x-www-form-urlencoded" else "application/json; charset=utf-8",
            )
            bearer?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
            connection.outputStream.use { it.write((form ?: json ?: "").toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw HttpError(code, text)
            mapper.readTree(text.ifBlank { "{}" })
        } finally {
            connection.disconnect()
        }
    }

    /** 通信の失敗を、画面に出せる LeagueException にする */
    private suspend fun <T> guard(block: suspend () -> T): T = try {
        block()
    } catch (e: LeagueException) {
        throw e
    } catch (e: HttpError) {
        throw LeagueException(
            when (e.code) {
                401, 403 -> "リーグのサーバーに参加できませんでした。少し待ってからもう一度お試しください。"
                in 500..599 -> "リーグのサーバーが混み合っています。少し待ってからもう一度お試しください。"
                else -> UNEXPECTED
            },
            e,
        )
    } catch (e: IOException) {
        throw LeagueException("リーグに接続できませんでした。インターネット接続を確認してください。", e)
    }

    private companion object {
        const val UNEXPECTED = "リーグとのやりとりで問題が起きました。もう一度お試しください。"
    }
}
