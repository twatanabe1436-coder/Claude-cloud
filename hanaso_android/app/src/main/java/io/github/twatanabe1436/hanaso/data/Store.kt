package io.github.twatanabe1436.hanaso.data

import android.content.Context
import io.github.twatanabe1436.hanaso.core.ClaudeEngine
import io.github.twatanabe1436.hanaso.core.LeagueTokenStore
import io.github.twatanabe1436.hanaso.core.Level
import io.github.twatanabe1436.hanaso.core.Stats
import io.github.twatanabe1436.hanaso.core.Xp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Settings(
    val level: Level = Level.DEFAULT,
    /** 読み上げ速度 (1.0 が標準) */
    val speechRate: Float = 0.95f,
    /** AI のセリフを自動で読み上げる */
    val autoSpeak: Boolean = true,
    /** AI のセリフを文字で表示する (オフで聞き取り練習) */
    val showText: Boolean = true,
    /** 読み上げが終わったら自動でマイクを開始 */
    val handsFree: Boolean = false,
    /** 話し終わったら自動で送信 */
    val autoSend: Boolean = true,
    /** マイクをもう一度押すまで聞き続ける (途中で黙っても確定しない)。ハンズフリー会話では無音で自動判定 */
    val tapToFinish: Boolean = true,
    /** 音声認識の聞き間違いを、お題や文脈に合う語に自動で直す (文法の間違いは直さない) */
    val autoFix: Boolean = true,
    /** AI のセリフに自動で日本語訳を付ける */
    val autoTranslate: Boolean = false,
    /** 話し終わりと判断するまでの無音時間 */
    val silenceMs: Long = 2000,
    /** 使う Claude のモデル */
    val model: String = ClaudeEngine.DEFAULT_MODEL,
    /** API キーがあるとき AI と会話する (オフ、またはキーがなければ台本モード) */
    val aiConversation: Boolean = true,
    /** リーグに出すニックネーム */
    val nickname: String = "",
    /** オンラインのリーグに参加している (ニックネームと今週の XP・レベルを送る) */
    val leagueJoined: Boolean = false,
)

/** 練習でためた XP。week は週の ID (Xp.weekId)、weekXp はその週の分 */
data class XpState(val week: String, val weekXp: Int, val totalXp: Int)

data class SavedPhrase(
    val id: String,
    val en: String,
    val ja: String,
    val source: String,
    val addedAt: Long,
    val practiceCount: Int = 0,
    val bestScore: Int? = null,
)

data class SessionRecord(
    val id: String,
    val scenarioId: String,
    val titleJa: String,
    val emoji: String,
    val level: Level,
    val startedAt: Long,
    val endedAt: Long,
    val learnerTurns: Int,
    val missionsDone: Int,
    val missionsTotal: Int,
    val score: Int? = null,
)

data class Totals(val streak: Int, val sessions: Int, val turns: Int, val phrases: Int)

/**
 * 端末内 (SharedPreferences) に保存するデータ: 設定・フレーズ帳・会話の記録・API キー (暗号化)。
 * 読み書きはメインスレッドから行う (量が少ないので apply() で十分速い)。
 */
class Store(context: Context) {
    private val prefs = context.getSharedPreferences("hanaso", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private val _phrases = MutableStateFlow(loadPhrases())
    val phrases: StateFlow<List<SavedPhrase>> = _phrases.asStateFlow()

    private val _sessions = MutableStateFlow(loadSessions())
    val sessions: StateFlow<List<SessionRecord>> = _sessions.asStateFlow()

    private val _apiKey = MutableStateFlow(prefs.getString(KEY_API, null)?.let { SecretBox.decrypt(it) }.orEmpty())
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    // ---- 設定 ----

    fun updateSettings(transform: (Settings) -> Settings) {
        val s = transform(_settings.value)
        _settings.value = s
        prefs.edit().putString(KEY_SETTINGS, JSONObject().apply {
            put("level", s.level.name)
            put("speechRate", s.speechRate.toDouble())
            put("autoSpeak", s.autoSpeak)
            put("showText", s.showText)
            put("handsFree", s.handsFree)
            put("autoSend", s.autoSend)
            put("tapToFinish", s.tapToFinish)
            put("autoFix", s.autoFix)
            put("autoTranslate", s.autoTranslate)
            put("silenceMs", s.silenceMs)
            put("model", s.model)
            put("aiConversation", s.aiConversation)
            put("nickname", s.nickname)
            put("leagueJoined", s.leagueJoined)
        }.toString()).apply()
    }

    private fun loadSettings(): Settings {
        val o = prefs.getString(KEY_SETTINGS, null)?.let { runCatching { JSONObject(it) }.getOrNull() } ?: return Settings()
        val d = Settings()
        return Settings(
            level = Level.fromName(o.optString("level", d.level.name)),
            speechRate = o.optDouble("speechRate", d.speechRate.toDouble()).toFloat(),
            autoSpeak = o.optBoolean("autoSpeak", d.autoSpeak),
            showText = o.optBoolean("showText", d.showText),
            handsFree = o.optBoolean("handsFree", d.handsFree),
            autoSend = o.optBoolean("autoSend", d.autoSend),
            tapToFinish = o.optBoolean("tapToFinish", d.tapToFinish),
            autoFix = o.optBoolean("autoFix", d.autoFix),
            autoTranslate = o.optBoolean("autoTranslate", d.autoTranslate),
            silenceMs = o.optLong("silenceMs", d.silenceMs),
            model = o.optString("model", d.model).ifBlank { d.model },
            aiConversation = o.optBoolean("aiConversation", d.aiConversation),
            nickname = o.optString("nickname", d.nickname),
            leagueJoined = o.optBoolean("leagueJoined", d.leagueJoined),
        )
    }

    // ---- XP (週ごとのリーグ用) ----

    private val _xp = MutableStateFlow(loadXp())
    val xp: StateFlow<XpState> = _xp.asStateFlow()

    /** 今の週の XP (週が変わっていれば今週の分は 0) */
    fun currentXp(now: Long = System.currentTimeMillis()): XpState {
        val week = Xp.weekId(now)
        val saved = _xp.value
        return if (saved.week == week) saved else saved.copy(week = week, weekXp = 0)
    }

    fun addXp(points: Int, now: Long = System.currentTimeMillis()): XpState {
        val current = currentXp(now)
        val next = current.copy(
            weekXp = (current.weekXp + points).coerceAtMost(Xp.WEEKLY_CAP),
            totalXp = current.totalXp + points,
        )
        _xp.value = next
        prefs.edit().putString(KEY_XP, JSONObject().apply {
            put("week", next.week)
            put("weekXp", next.weekXp)
            put("totalXp", next.totalXp)
        }.toString()).apply()
        return next
    }

    private fun loadXp(): XpState {
        val o = prefs.getString(KEY_XP, null)?.let { runCatching { JSONObject(it) }.getOrNull() }
        return XpState(o?.optString("week").orEmpty(), o?.optInt("weekXp") ?: 0, o?.optInt("totalXp") ?: 0)
    }

    /** オンラインのリーグの匿名ログイン (更新用トークン)。暗号化して保存する */
    val leagueTokens = object : LeagueTokenStore {
        override fun load(): String? = prefs.getString(KEY_LEAGUE_TOKEN, null)?.let { SecretBox.decrypt(it) }?.takeIf { it.isNotBlank() }

        override fun save(refreshToken: String) {
            prefs.edit().putString(KEY_LEAGUE_TOKEN, SecretBox.encrypt(refreshToken)).apply()
        }
    }

    // ---- API キー ----

    fun setApiKey(key: String) {
        val trimmed = key.trim()
        _apiKey.value = trimmed
        prefs.edit().apply {
            if (trimmed.isEmpty()) remove(KEY_API) else putString(KEY_API, SecretBox.encrypt(trimmed))
        }.apply()
    }

    // ---- フレーズ帳 ----

    fun hasPhrase(en: String): Boolean {
        val n = Stats.normalizePhrase(en)
        return _phrases.value.any { Stats.normalizePhrase(it.en) == n }
    }

    /** @return 追加したフレーズ (既にあれば null) */
    fun addPhrase(en: String, ja: String, source: String): SavedPhrase? {
        if (en.isBlank() || hasPhrase(en)) return null
        val p = SavedPhrase(UUID.randomUUID().toString(), en.trim(), ja, source, System.currentTimeMillis())
        savePhrases(listOf(p) + _phrases.value)
        return p
    }

    fun updatePhrase(id: String, transform: (SavedPhrase) -> SavedPhrase) =
        savePhrases(_phrases.value.map { if (it.id == id) transform(it) else it })

    fun removePhrase(id: String) = savePhrases(_phrases.value.filterNot { it.id == id })

    private fun savePhrases(list: List<SavedPhrase>) {
        _phrases.value = list
        val arr = JSONArray()
        list.forEach { p ->
            arr.put(JSONObject().apply {
                put("id", p.id)
                put("en", p.en)
                put("ja", p.ja)
                put("source", p.source)
                put("addedAt", p.addedAt)
                put("practiceCount", p.practiceCount)
                if (p.bestScore != null) put("bestScore", p.bestScore)
            })
        }
        prefs.edit().putString(KEY_PHRASES, arr.toString()).apply()
    }

    private fun loadPhrases(): List<SavedPhrase> = readArray(KEY_PHRASES) { o ->
        SavedPhrase(
            id = o.getString("id"),
            en = o.getString("en"),
            ja = o.optString("ja"),
            source = o.optString("source"),
            addedAt = o.optLong("addedAt"),
            practiceCount = o.optInt("practiceCount"),
            bestScore = if (o.has("bestScore")) o.getInt("bestScore") else null,
        )
    }

    // ---- 会話の記録 ----

    fun addSession(record: SessionRecord) = saveSessions((listOf(record) + _sessions.value).take(MAX_SESSIONS))

    fun updateSession(id: String, transform: (SessionRecord) -> SessionRecord) =
        saveSessions(_sessions.value.map { if (it.id == id) transform(it) else it })

    private fun saveSessions(list: List<SessionRecord>) {
        _sessions.value = list
        val arr = JSONArray()
        list.forEach { s ->
            arr.put(JSONObject().apply {
                put("id", s.id)
                put("scenarioId", s.scenarioId)
                put("titleJa", s.titleJa)
                put("emoji", s.emoji)
                put("level", s.level.name)
                put("startedAt", s.startedAt)
                put("endedAt", s.endedAt)
                put("learnerTurns", s.learnerTurns)
                put("missionsDone", s.missionsDone)
                put("missionsTotal", s.missionsTotal)
                if (s.score != null) put("score", s.score)
            })
        }
        prefs.edit().putString(KEY_SESSIONS, arr.toString()).apply()
    }

    private fun loadSessions(): List<SessionRecord> = readArray(KEY_SESSIONS) { o ->
        SessionRecord(
            id = o.getString("id"),
            scenarioId = o.getString("scenarioId"),
            titleJa = o.optString("titleJa"),
            emoji = o.optString("emoji"),
            level = Level.fromName(o.optString("level")),
            startedAt = o.optLong("startedAt"),
            endedAt = o.optLong("endedAt"),
            learnerTurns = o.optInt("learnerTurns"),
            missionsDone = o.optInt("missionsDone"),
            missionsTotal = o.optInt("missionsTotal"),
            score = if (o.has("score")) o.getInt("score") else null,
        )
    }

    fun totals(): Totals {
        val sessions = _sessions.value
        return Totals(
            streak = Stats.streak(sessions.map { it.endedAt }),
            sessions = sessions.size,
            turns = sessions.sumOf { it.learnerTurns },
            phrases = _phrases.value.size,
        )
    }

    /** 会話の記録・フレーズ帳・設定・XP を消す (API キーは残す) */
    fun resetAll() {
        prefs.edit().remove(KEY_SETTINGS).remove(KEY_PHRASES).remove(KEY_SESSIONS).remove(KEY_XP).remove(KEY_LEAGUE_TOKEN).apply()
        _settings.value = Settings()
        _phrases.value = emptyList()
        _sessions.value = emptyList()
        _xp.value = XpState("", 0, 0)
    }

    private fun <T> readArray(key: String, parse: (JSONObject) -> T): List<T> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i -> runCatching { parse(arr.getJSONObject(i)) }.getOrNull() }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val KEY_SETTINGS = "settings"
        const val KEY_PHRASES = "phrases"
        const val KEY_SESSIONS = "sessions"
        const val KEY_API = "api_key"
        const val KEY_XP = "xp"
        const val KEY_LEAGUE_TOKEN = "league_token"
        const val MAX_SESSIONS = 100
    }
}
