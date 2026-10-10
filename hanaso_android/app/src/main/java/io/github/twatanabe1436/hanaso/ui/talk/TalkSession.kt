package io.github.twatanabe1436.hanaso.ui.talk

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import io.github.twatanabe1436.hanaso.HanasoApp
import io.github.twatanabe1436.hanaso.core.AiEngine
import io.github.twatanabe1436.hanaso.core.AiErrorKind
import io.github.twatanabe1436.hanaso.core.AiException
import io.github.twatanabe1436.hanaso.core.Conversation
import io.github.twatanabe1436.hanaso.core.EngineMode
import io.github.twatanabe1436.hanaso.core.Feedback
import io.github.twatanabe1436.hanaso.core.HintSuggestion
import io.github.twatanabe1436.hanaso.core.Level
import io.github.twatanabe1436.hanaso.core.Line
import io.github.twatanabe1436.hanaso.core.Rating
import io.github.twatanabe1436.hanaso.core.Scenario
import io.github.twatanabe1436.hanaso.core.ScriptEngine
import io.github.twatanabe1436.hanaso.core.ScriptTask
import io.github.twatanabe1436.hanaso.core.SentenceChunker
import io.github.twatanabe1436.hanaso.core.Speaker
import io.github.twatanabe1436.hanaso.core.Summary
import io.github.twatanabe1436.hanaso.core.Transcript
import io.github.twatanabe1436.hanaso.core.Translation
import io.github.twatanabe1436.hanaso.core.Xp
import io.github.twatanabe1436.hanaso.core.hasJapanese
import io.github.twatanabe1436.hanaso.data.SessionRecord
import io.github.twatanabe1436.hanaso.speech.SpeechInput
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.util.UUID

/** 画面に出す日本語のエラーメッセージ (API の詳細があれば添える) */
fun errorText(e: Throwable): String = when (e) {
    is AiException -> if (e.detail.isNullOrBlank()) e.message.orEmpty() else "${e.message}\n(${e.detail})"
    else -> AiErrorKind.UNKNOWN.messageJa
}

sealed interface ChatItem {
    val id: Long
}

class AiMessage(override val id: Long, val snapshot: List<Line>) : ChatItem {
    var text by mutableStateOf("")
    var streaming by mutableStateOf(true)
    var error by mutableStateOf<String?>(null)
    var translation by mutableStateOf<LoadState<Translation>?>(null)
    var showTranslation by mutableStateOf(false)
    var revealed by mutableStateOf(false)
}

/**
 * @param voice 声で話した (キーボードではない)
 */
class LearnerMessage(override val id: Long, text: String, val voice: Boolean = false, heard: String? = null) : ChatItem {
    /** 表示する発話 (聞き間違いを直したもの) */
    var text by mutableStateOf(text)

    /** 音声認識そのままの文。聞き間違いを直して text と語が変わったときだけ入る */
    var heard by mutableStateOf(heard)

    var feedback by mutableStateOf<LoadState<Feedback>>(LoadState.Loading)
    var expanded by mutableStateOf(false)

    /** この発話でもらった XP */
    var xp by mutableIntStateOf(0)
}

/** 会話の最初に出す、場面の説明 */
class Scene(override val id: Long) : ChatItem

/** ミッションをすべて達成した (AI 会話)、または台本を最後まで終えた (台本モード) */
class Celebration(override val id: Long, val scriptDone: Boolean = false) : ChatItem

sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>
    data class Ready<T>(val value: T) : LoadState<T>
    data class Failed(val message: String) : LoadState<Nothing>
}

data class HintState(
    val want: String,
    val loading: Boolean = true,
    val suggestions: List<HintSuggestion> = emptyList(),
    val error: String? = null,
)

/** 学習者の1回の発話と、その添削 */
class Turn(var text: String) {
    var feedback: Feedback? = null
}

/** 終えた会話 (振り返り画面用) */
class FinishedSession(
    val recordId: String,
    /** 会話した相手 (振り返りも同じ相手に作ってもらう) */
    val engine: AiEngine,
    val scenario: Scenario,
    val level: Level,
    val history: List<Line>,
    val turns: List<Turn>,
    val completed: Set<String>,
    val startedAt: Long,
    val endedAt: Long,
) {
    var summary by mutableStateOf<LoadState<Summary>?>(null)
}

/**
 * 1回の会話。相手の返事 (ストリーミング + 文ごとの読み上げ)、発話ごとの添削、ミッション、ヒント、
 * ハンズフリー (読み上げが終わったら自動でマイク)、台本モードのお題を管理する。状態はすべてメインスレッドで更新する。
 */
class TalkSession(
    val scenario: Scenario,
    val level: Level,
    private val app: HanasoApp,
) {
    private val engine = app.engine()
    val mode: EngineMode = engine.mode
    private val script = engine as? ScriptEngine
    private val speaker get() = app.speaker
    private val input get() = app.speechInput
    private val settings get() = app.store.settings.value
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var nextId = 0L
    private val history = mutableListOf<Line>()
    private val turns = mutableListOf<Turn>()
    private val startedAt = System.currentTimeMillis()
    private var closed = false
    private var celebrated = false
    private var replyJob: Job? = null
    private var hintJob: Job? = null

    val items = mutableStateListOf<ChatItem>()
    var busy by mutableStateOf(false)
        private set
    var listening by mutableStateOf(false)
        private set
    var liveText by mutableStateOf("")
        private set
    var micLevel by mutableFloatStateOf(0f)
        private set
    var status by mutableStateOf("")
        private set
    var muted by mutableStateOf(!settings.autoSpeak)
        private set
    var completed by mutableStateOf(emptySet<String>())
        private set
    var hint by mutableStateOf<HintState?>(null)
        private set
    var cue by mutableStateOf<HintSuggestion?>(null)
        private set
    var draft by mutableStateOf("")
    var showKeyboard by mutableStateOf(!input.available)
        private set

    /** 入力欄を開いたときに増える (画面側で入力欄にフォーカスしてキーボードを出す) */
    var keyboardFocusRequest by mutableIntStateOf(0)
        private set

    /** 台本モードのいまのお題 (AI 会話では null) */
    var task by mutableStateOf<ScriptTask?>(null)
        private set

    /** お題カードでお手本を表示中 */
    var showExample by mutableStateOf(false)

    /** 台本を最後まで終えた */
    val scriptDone: Boolean get() = task?.finished == true

    /** これまでの会話をすべて表示する。false なら、いまのやりとりだけを大きく表示する */
    var showHistory by mutableStateOf(false)

    /** 場面の説明を開いているか (最初に答えたら 1 行にたたむ) */
    var sceneExpanded by mutableStateOf(true)

    /**
     * 画面に出す項目。ふだんは、場面と、自分の最後の発話 (判定つき) から後ろだけ
     * (= 相手の最新のセリフ)。履歴を開いているときはすべて。
     */
    val visibleItems: List<ChatItem>
        get() {
            if (showHistory) return items.toList()
            val lastLearner = items.indexOfLast { it is LearnerMessage }
            if (lastLearner < 0) return items.toList()
            return items.filterIsInstance<Scene>() + items.subList(lastLearner, items.size)
        }

    /** 履歴を開くと、いまより多くの項目が見える */
    val hasHistory: Boolean get() = items.indexOfLast { it is LearnerMessage } > 1

    /** 相手のセリフに日本語訳を自動で付けるか。台本モードの訳は台本から出せる (無料・すぐ) ので常に付ける (聞き取り練習中は除く) */
    private val autoTranslate: Boolean
        get() = settings.autoTranslate || (mode == EngineMode.SCRIPT && settings.showText)

    /** マイクの権限があるか (ハンズフリーで自動開始してよいか) */
    private fun micPermitted(): Boolean =
        ContextCompat.checkSelfPermission(app, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)

    /** トーストで出す短いお知らせ */
    val messages: SharedFlow<String> = _messages

    val learnerTurns: Int get() = turns.size

    fun start() {
        items += Scene(nextId++)
        val opener = AiMessage(nextId++, emptyList())
        opener.text = scenario.opener
        opener.streaming = false
        items += opener
        history += Line(Speaker.AI, scenario.opener)
        if (autoTranslate) loadTranslation(opener)
        refreshTask()
        speakAll(scenario.opener) { afterAiSpoke() }
    }

    /** 台本モード: 会話の履歴から、いまのお題を求め直す */
    private fun refreshTask() {
        val scripted = script ?: return
        val next = scripted.task(Conversation(scenario, level, history.toList()))
        if (next.number != task?.number || next.finished != task?.finished) showExample = false
        task = next
        if (next.finished && !celebrated) {
            celebrated = true
            items += Celebration(nextId++, scriptDone = true)
            app.awardXp(Xp.SCRIPT_CLEAR)
        }
    }

    // ---- 音声 ----

    fun play(text: String, rate: Float? = null) {
        speaker.stop()
        speaker.say(text, rate)
    }

    private fun speakAll(text: String, then: () -> Unit) {
        if (muted) return then()
        speaker.say(text)
        speaker.whenIdle(then)
    }

    fun toggleMute() {
        muted = !muted
        app.store.updateSettings { it.copy(autoSpeak = !muted) }
        if (muted) speaker.stop()
        _messages.tryEmit(if (muted) "自動読み上げ: オフ" else "自動読み上げ: オン")
    }

    /** AI が話し終わったあと: ハンズフリーなら自動でマイクを開始 */
    private fun afterAiSpoke() {
        if (closed || busy || listening || hint != null || scriptDone) return
        if (settings.handsFree && input.available && micPermitted()) startListening()
    }

    // ---- 音声入力 ----

    fun toggleMic() {
        if (listening) input.stop() else startListening()
    }

    fun startListening() {
        if (busy || closed) return
        if (!input.available) {
            showKeyboard = true
            _messages.tryEmit("この端末では音声認識が使えません。キーボードで入力してください。")
            return
        }
        speaker.stop()
        // 声で答えるときは文字の入力欄を閉じる
        showKeyboard = false
        // 聞き間違いの自動修正がオフなら、認識のヒントも渡さない (聞こえたとおりに出す)
        val autoFix = settings.autoFix
        val expected = if (autoFix) expectedEnglish() else emptyList()
        val context = if (autoFix) contextEnglish() else emptyList()
        listening = true
        liveText = ""
        // ハンズフリー会話は話し終わり (無音) で自動的に区切る。それ以外は設定に従う
        val untilStopped = settings.tapToFinish && !settings.handsFree
        status = if (untilStopped) "言い終わったら、もう一度タップ" else ""
        input.start(settings.silenceMs, untilStopped, object : SpeechInput.Callback {
            override fun onPartial(text: String) {
                liveText = text
            }

            override fun onLevel(rmsDb: Float) {
                micLevel = ((rmsDb + 2f) / 12f).coerceIn(0f, 1f)
            }

            override fun onError(messageJa: String) {
                _messages.tryEmit(messageJa)
            }

            override fun onEnd(text: String) {
                endListening()
                if (closed) return
                // 聞き間違いを、お題で言いそうな語に直す (文法の間違いはそのまま)
                val fixed = if (text.isBlank()) "" else if (autoFix) Transcript.fix(text, expected, context) else text.trim()
                when {
                    fixed.isBlank() -> status = "聞き取れませんでした"
                    settings.autoSend -> send(fixed, heard = text, voice = true)
                    else -> {
                        draft = fixed
                        showKeyboard = true
                    }
                }
            }

            override fun onCancel() = endListening()
        }, hints = expected + context)
    }

    /** いま言うはずの英語 (聞き間違いはこれに寄せて直す): 見ているヒントと、台本のお題のお手本・キーワード */
    private fun expectedEnglish(): List<String> = buildList {
        cue?.let { add(it.en) }
        task?.takeUnless { it.finished }?.let { addAll(it.vocabulary) }
    }

    /** 会話に出てきた英語 (固有名詞の書き方をそろえる・認識のヒントにする): 相手の直前のセリフ、名前、キーフレーズ */
    private fun contextEnglish(): List<String> = buildList {
        history.lastOrNull { it.speaker == Speaker.AI }?.let { add(it.text) }
        add(scenario.aiName)
        addAll(scenario.keyPhrases.map { it.en })
    }

    /** 文字の入力欄を開く (開いたらキーボードも出す) */
    fun openKeyboard() {
        showKeyboard = true
        keyboardFocusRequest++
    }

    fun closeKeyboard() {
        showKeyboard = false
    }

    private fun endListening() {
        listening = false
        micLevel = 0f
        liveText = ""
        status = ""
    }

    fun submitDraft() {
        val text = draft.trim()
        if (text.isEmpty()) return
        draft = ""
        if (text.hasJapanese()) openHint(text) else send(text)
    }

    // ---- 送信 → 添削 & AI の返事 ----

    /**
     * @param fromHint ヒントをそのまま送った (XP は少なめ)
     * @param heard 音声認識そのままの文 (声で話したとき)
     */
    fun send(text: String, fromHint: Boolean = false, heard: String? = null, voice: Boolean = heard != null) {
        if (busy || closed) return
        if (text.hasJapanese()) {
            openHint(text)
            return
        }
        cue = null
        // 最初に答えたら、場面の説明はたたむ (タップで開ける)
        if (turns.isEmpty()) sceneExpanded = false
        val line = history.size
        history += Line(Speaker.LEARNER, text)
        val turn = Turn(text)
        turns += turn
        val message = LearnerMessage(nextId++, text, voice, heard?.takeIf { Transcript.wordsChanged(it, text) })
        items += message
        val snapshot = history.toList()

        // 添削は返事と並行して取得する
        scope.launch {
            try {
                val fb = engine.feedback(Conversation(scenario, level, snapshot))
                turn.feedback = fb
                // AI 会話: AI が文脈から聞き間違いを直していたら、その文にする (文法の直しが混ざっていたら使わない)
                if (voice && settings.autoFix && fb.heard.isNotBlank() && Transcript.acceptRepair(message.text, fb.heard)) {
                    if (message.heard == null) message.heard = message.text
                    message.text = fb.heard
                    turn.text = fb.heard
                    if (history.getOrNull(line)?.text == text) history[line] = Line(Speaker.LEARNER, fb.heard)
                }
                message.feedback = LoadState.Ready(fb)
                message.xp = if (fromHint) HINT_XP else Xp.forUtterance(fb.rating)
                app.awardXp(message.xp)
                // 修正があるときは自動で開いて気づけるようにする
                if (fb.rating == Rating.FIX) message.expanded = true
                val valid = scenario.missions.map { it.id }.toSet()
                val newly = fb.completedMissions.filter { it in valid }.toSet()
                if (!completed.containsAll(newly)) {
                    completed = completed + newly
                    // 台本モードは「最後まで終えた」ときにお祝いする
                    if (script == null) checkAllMissions()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message.feedback = LoadState.Failed(errorText(e))
            }
        }
        requestReply(snapshot)
    }

    private fun checkAllMissions() {
        if (celebrated || scenario.missions.isEmpty()) return
        if (completed.containsAll(scenario.missions.map { it.id })) {
            celebrated = true
            items += Celebration(nextId++)
            app.awardXp(Xp.ALL_MISSIONS)
        }
    }

    private fun requestReply(snapshot: List<Line>) {
        busy = true
        // 考え中は吹き出しの「・・・」で伝わるので、文字は出さない
        status = ""
        val bubble = AiMessage(nextId++, snapshot)
        items += bubble
        speaker.stop()
        val chunker = SentenceChunker()
        replyJob = scope.launch {
            try {
                engine.reply(Conversation(scenario, level, snapshot)).collect { delta ->
                    bubble.text += delta
                    // 文が完成したものから順に読み上げを始める
                    if (!muted) chunker.push(delta).forEach { speaker.say(it) }
                }
                val full = bubble.text.trim()
                if (full.isEmpty()) throw AiException(AiErrorKind.BAD_OUTPUT)
                bubble.text = full
                bubble.streaming = false
                history += Line(Speaker.AI, full)
                busy = false
                status = ""
                refreshTask()
                if (!muted) chunker.flush().forEach { speaker.say(it) }
                if (autoTranslate) loadTranslation(bubble)
                speaker.whenIdle { afterAiSpoke() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                speaker.stop()
                busy = false
                status = ""
                bubble.streaming = false
                bubble.error = errorText(e)
            }
        }
    }

    fun retry(message: AiMessage) {
        if (busy || closed) return
        items.remove(message)
        requestReply(message.snapshot)
    }

    // ---- 翻訳・フレーズ保存 ----

    fun toggleTranslation(message: AiMessage) {
        if (message.showTranslation) {
            message.showTranslation = false
            return
        }
        message.showTranslation = true
        if (message.translation !is LoadState.Ready) loadTranslation(message)
    }

    private fun loadTranslation(message: AiMessage) {
        message.showTranslation = true
        message.translation = LoadState.Loading
        scope.launch {
            message.translation = try {
                LoadState.Ready(engine.translate(message.text))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LoadState.Failed(errorText(e))
            }
        }
    }

    fun savePhrase(en: String, ja: String = "") {
        if (app.savePhrase(en, ja, scenario.titleJa) != null) _messages.tryEmit("フレーズ帳に保存しました ⭐")
    }

    fun isSaved(en: String): Boolean = app.store.hasPhrase(en)

    // ---- ヒント ----

    fun openHint(want: String = "") {
        if (busy || closed) return
        input.cancel()
        loadHint(want)
    }

    fun loadHint(want: String) {
        hintJob?.cancel()
        hint = HintState(want = want)
        val snapshot = history.toList()
        hintJob = scope.launch {
            try {
                val suggestions = engine.hint(Conversation(scenario, level, snapshot), want.ifBlank { null })
                hint = hint?.copy(loading = false, suggestions = suggestions, error = null)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                hint = hint?.copy(loading = false, error = errorText(e))
            }
        }
    }

    fun closeHint() {
        hintJob?.cancel()
        hint = null
    }

    /** ヒントを見ながら自分の声で言う */
    fun sayHintYourself(suggestion: HintSuggestion) {
        closeHint()
        cue = suggestion
        _messages.tryEmit("ヒントを見ながら、自分の声で言ってみましょう！")
        startListening()
    }

    fun sendHint(suggestion: HintSuggestion) {
        closeHint()
        send(suggestion.en, fromHint = true)
    }

    fun dismissCue() {
        cue = null
    }

    // ---- 終了 ----

    /** アプリが裏に回ったとき: マイクと読み上げを止める */
    fun pause() {
        input.cancel()
        speaker.stop()
    }

    /** 会話を終えて記録を保存する。まだ話していなければ null */
    fun finish(): FinishedSession? {
        replyJob?.cancel()
        val result = if (turns.isEmpty()) {
            null
        } else {
            val endedAt = System.currentTimeMillis()
            val record = SessionRecord(
                id = UUID.randomUUID().toString(),
                scenarioId = scenario.id,
                titleJa = scenario.titleJa,
                emoji = scenario.emoji,
                level = level,
                startedAt = startedAt,
                endedAt = endedAt,
                learnerTurns = turns.size,
                missionsDone = completed.size,
                missionsTotal = scenario.missions.size,
            )
            app.store.addSession(record)
            FinishedSession(
                recordId = record.id,
                engine = engine,
                scenario = scenario,
                level = level,
                history = history.toList(),
                turns = turns.toList(),
                completed = completed,
                startedAt = startedAt,
                endedAt = endedAt,
            )
        }
        close()
        return result
    }

    fun close() {
        if (closed) return
        closed = true
        scope.cancel()
        input.cancel()
        speaker.stop()
    }
}

/** ヒントをそのまま送ったときの XP (自分で言ったときより少なめ) */
private const val HINT_XP = 2
