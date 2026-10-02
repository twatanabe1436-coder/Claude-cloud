package io.github.twatanabe1436.hanaso

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.twatanabe1436.hanaso.core.AiEngine
import io.github.twatanabe1436.hanaso.core.ClaudeEngine
import io.github.twatanabe1436.hanaso.core.EngineMode
import io.github.twatanabe1436.hanaso.core.Level
import io.github.twatanabe1436.hanaso.core.Scenario
import io.github.twatanabe1436.hanaso.core.ScriptEngine
import io.github.twatanabe1436.hanaso.data.SavedPhrase
import io.github.twatanabe1436.hanaso.data.Settings
import io.github.twatanabe1436.hanaso.data.Store
import io.github.twatanabe1436.hanaso.speech.AndroidSpeechInput
import io.github.twatanabe1436.hanaso.speech.SpeechInput
import io.github.twatanabe1436.hanaso.speech.SpeechOutput
import io.github.twatanabe1436.hanaso.speech.TtsSpeaker
import io.github.twatanabe1436.hanaso.ui.talk.FinishedSession
import io.github.twatanabe1436.hanaso.ui.talk.TalkSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** 画面。一覧系はタブ、会話と振り返りは全画面。 */
sealed interface Screen {
    data object Home : Screen
    data object Scenarios : Screen
    data object Phrases : Screen
    data object Settings : Screen
    data class Talk(val scenarioId: String) : Screen
    data object Summary : Screen
}

/** 画面の履歴。画面回転でも消えないよう Application に置く。 */
class Navigator {
    val stack = mutableStateListOf<Screen>(Screen.Home)
    val current: Screen get() = stack.last()
    val canGoBack: Boolean get() = stack.size > 1

    fun go(screen: Screen) {
        stack.add(screen)
    }

    /** タブの切り替え (履歴をリセット) */
    fun tab(screen: Screen) {
        stack.clear()
        if (screen != Screen.Home) stack.add(Screen.Home)
        stack.add(screen)
    }

    fun replace(screen: Screen) {
        stack[stack.lastIndex] = screen
    }

    fun back(): Boolean {
        if (!canGoBack) return false
        stack.removeAt(stack.lastIndex)
        return true
    }
}

class HanasoApp : Application() {

    lateinit var store: Store
        private set

    /** 読み上げ。テストでは偽物に差し替える */
    lateinit var speaker: SpeechOutput

    /** 音声認識。テストでは偽物に差し替える */
    lateinit var speechInput: SpeechInput

    /** テスト用: 設定すると API キーや設定に関係なくこの相手を使う */
    var engineOverride: AiEngine? by mutableStateOf(null)

    /** 台本モード (AI なし) */
    val scriptEngine = ScriptEngine()

    val nav = Navigator()
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** 進行中の会話 (画面回転でも続くよう Application に置く) */
    var talk: TalkSession? by mutableStateOf(null)
        private set

    /** 直前に終えた会話 (振り返り画面用) */
    var finished: FinishedSession? by mutableStateOf(null)

    private var cachedEngine: Pair<String, ClaudeEngine>? = null

    override fun onCreate() {
        super.onCreate()
        store = Store(this)
        // 読み上げエンジンの準備には時間がかかるので、起動直後から始めておく
        speaker = TtsSpeaker(this) { store.settings.value.speechRate }
        speechInput = AndroidSpeechInput(this)
    }

    /** 会話の相手: API キーがあって「AI と会話」がオンなら Claude、それ以外は台本モード */
    fun engine(): AiEngine {
        engineOverride?.let { return it }
        return claudeEngine()?.takeIf { store.settings.value.aiConversation } ?: scriptEngine
    }

    /** API キーがあれば Claude (会話の設定に関係なく。接続テスト用) */
    fun claudeEngine(): ClaudeEngine? {
        val key = store.apiKey.value
        if (key.isBlank()) return null
        val model = store.settings.value.model
        val cacheKey = "$model|$key"
        cachedEngine?.let { (k, e) -> if (k == cacheKey) return e }
        return ClaudeEngine.create(key, model).also { cachedEngine = cacheKey to it }
    }

    /** 会話を始めたときに使われる相手の種類 (画面の案内用) */
    fun modeFor(apiKey: String, settings: Settings): EngineMode = engineOverride?.mode
        ?: if (apiKey.isNotBlank() && settings.aiConversation) EngineMode.AI else EngineMode.SCRIPT

    fun startTalk(scenario: Scenario, level: Level) {
        talk?.close()
        talk = TalkSession(scenario, level, this).also { it.start() }
        nav.go(Screen.Talk(scenario.id))
    }

    fun closeTalk() {
        talk?.close()
        talk = null
    }

    /** 会話を終えて振り返り画面へ */
    fun finishTalk() {
        val session = talk ?: return
        val result = session.finish()
        talk = null
        if (result == null) {
            nav.back()
        } else {
            finished = result
            nav.replace(Screen.Summary)
        }
    }

    /** フレーズ帳に保存する。訳がなければ台本から探すか、AI 会話中なら裏で翻訳して埋める */
    fun savePhrase(en: String, ja: String, source: String): SavedPhrase? {
        val p = store.addPhrase(en, ja.ifBlank { ScriptEngine.lookupJa(en).orEmpty() }, source) ?: return null
        val engine = engine()
        if (p.ja.isBlank() && engine.mode == EngineMode.AI) {
            scope.launch {
                runCatching { engine.translate(en) }.onSuccess { tr ->
                    store.updatePhrase(p.id) { it.copy(ja = tr.ja) }
                }
            }
        }
        return p
    }
}

val Context.app: HanasoApp get() = applicationContext as HanasoApp
