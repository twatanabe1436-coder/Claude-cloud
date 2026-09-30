package io.github.twatanabe1436.hanaso.speech

import android.content.Context
import android.media.AudioAttributes
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class VoiceStatus {
    INITIALIZING,
    READY,

    /** 読み上げエンジンはあるが英語の音声が入っていない */
    NO_ENGLISH,

    /** 読み上げエンジン自体が使えない */
    UNAVAILABLE,
}

/** 英語の読み上げ。テストでは偽物に差し替える。メインスレッドから呼ぶ。 */
interface SpeechOutput {
    val status: StateFlow<VoiceStatus>

    /** 読み上げキューに追加する (文単位で順番に読む) */
    fun say(text: String, rate: Float? = null)

    /** 読み上げを止めてキューを空にする */
    fun stop()

    /** キューが空になったら (すぐ空なら今すぐ) callback を呼ぶ。stop() で取り消される */
    fun whenIdle(callback: () -> Unit)

    /** 音声データを入れ直した後などに呼ぶ */
    fun recheck() {}
}

/**
 * Android 標準の TextToSpeech を使った読み上げ。
 * AI の返事は文ごとに届くので、キューの数を数えて「全部読み終わった」を検出する (ハンズフリー会話用)。
 */
class TtsSpeaker(context: Context, private val defaultRate: () -> Float) : SpeechOutput {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var seq = 0
    private var generation = 0
    private var pending = 0
    private var idleCallbacks = mutableListOf<() -> Unit>()

    private val _status = MutableStateFlow(VoiceStatus.INITIALIZING)
    override val status: StateFlow<VoiceStatus> = _status.asStateFlow()

    init {
        connect()
    }

    override fun recheck() {
        val s = _status.value
        if (s == VoiceStatus.NO_ENGLISH || s == VoiceStatus.UNAVAILABLE) connect()
    }

    override fun say(text: String, rate: Float?) {
        val engine = tts
        if (engine == null || _status.value != VoiceStatus.READY || text.isBlank()) return
        engine.setSpeechRate(rate ?: defaultRate())
        val id = "g$generation-${seq++}"
        pending++
        if (engine.speak(text, TextToSpeech.QUEUE_ADD, null, id) != TextToSpeech.SUCCESS) done(generation)
    }

    override fun stop() {
        generation++
        pending = 0
        idleCallbacks = mutableListOf()
        tts?.stop()
    }

    override fun whenIdle(callback: () -> Unit) {
        if (pending == 0) callback() else idleCallbacks += callback
    }

    /** 読み上げ1件が終わった (メインスレッドで呼ぶ) */
    private fun done(gen: Int) {
        if (gen != generation) return // stop() より前のもの
        pending = maxOf(0, pending - 1)
        if (pending == 0) {
            val callbacks = idleCallbacks
            idleCallbacks = mutableListOf()
            callbacks.forEach { it() }
        }
    }

    private fun onUtteranceFinished(utteranceId: String?) {
        val gen = utteranceId?.substringAfter('g')?.substringBefore('-')?.toIntOrNull() ?: return
        main.post { done(gen) }
    }

    private fun connect() {
        tts?.shutdown()
        var created: TextToSpeech? = null
        created = TextToSpeech(appContext) { status ->
            // 古い接続からのコールバックは無視する
            if (tts === created) main.post { onInit(status) }
        }
        tts = created
    }

    private fun onInit(status: Int) {
        val engine = tts
        if (engine == null || status != TextToSpeech.SUCCESS) {
            Log.w(TAG, "TextToSpeech init failed: $status")
            _status.value = VoiceStatus.UNAVAILABLE
            return
        }
        engine.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) = onUtteranceFinished(utteranceId)

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = onUtteranceFinished(utteranceId)
            override fun onError(utteranceId: String?, errorCode: Int) = onUtteranceFinished(utteranceId)
            override fun onStop(utteranceId: String?, interrupted: Boolean) = onUtteranceFinished(utteranceId)
        })
        val result = engine.setLanguage(Locale.US)
        _status.value = if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            VoiceStatus.NO_ENGLISH
        } else {
            VoiceStatus.READY
        }
    }

    private companion object {
        const val TAG = "TtsSpeaker"
    }
}
