package io.github.twatanabe1436.biyotimer.runtime

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import io.github.twatanabe1436.biyotimer.timer.Phrases
import io.github.twatanabe1436.biyotimer.timer.TimerEvent
import io.github.twatanabe1436.biyotimer.timer.TimerSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class VoiceStatus {
    INITIALIZING,
    READY,

    /** 読み上げエンジンはあるが日本語の音声が入っていない。 */
    NO_JAPANESE,

    /** 読み上げエンジン自体が使えない。 */
    UNAVAILABLE,
}

/**
 * タイマーのイベントを音声 (TextToSpeech) とビープ音、バイブレーションに変換する。
 * 日本語の音声が使えない端末ではビープ音で代用する。
 */
class Announcer(context: Context) {

    private val appContext = context.applicationContext
    private val beeper = BeepPlayer()
    private var tts: TextToSpeech? = null
    private var utteranceSeq = 0

    private val _voiceStatus = MutableStateFlow(VoiceStatus.INITIALIZING)
    val voiceStatus: StateFlow<VoiceStatus> = _voiceStatus.asStateFlow()

    init {
        connect()
    }

    /** 音声データを入れ直した後などに呼ぶ。使えない状態のときだけ接続し直す。 */
    fun recheck() {
        val status = _voiceStatus.value
        if (status == VoiceStatus.NO_JAPANESE || status == VoiceStatus.UNAVAILABLE) connect()
    }

    fun onEvent(event: TimerEvent, settings: TimerSettings) {
        when (event) {
            is TimerEvent.CountdownTick -> if (settings.countdownBeep) beeper.play(Beep.TICK)
            TimerEvent.Started ->
                announce(settings.startPhrase.takeIf { settings.startPhraseEnabled }, settings, Beep.START)
            is TimerEvent.Remaining ->
                announce(Phrases.remaining(event.seconds), settings, Beep.ANNOUNCE)
            TimerEvent.Finished -> {
                announce(settings.endPhrase.takeIf { settings.endPhraseEnabled }, settings, Beep.FINISH)
                if (settings.vibrateOnFinish) vibrate()
            }
        }
    }

    /** 設定画面の「テスト再生」。 */
    fun preview(settings: TimerSettings) {
        stop()
        announce(Phrases.remaining(10 * 60), settings, Beep.ANNOUNCE)
    }

    fun stop() {
        tts?.stop()
        beeper.stop()
    }

    private fun announce(text: String?, settings: TimerSettings, fallback: Beep) {
        if (text.isNullOrBlank() || !speak(text, settings.speechRate)) beeper.play(fallback)
    }

    private fun speak(text: String, rate: Float): Boolean {
        val engine = tts ?: return false
        if (_voiceStatus.value != VoiceStatus.READY) return false
        engine.setSpeechRate(rate)
        val id = "biyo-${utteranceSeq++}"
        return engine.speak(text, TextToSpeech.QUEUE_ADD, null, id) == TextToSpeech.SUCCESS
    }

    private fun connect() {
        // 接続し直す間も前回の状態を表示しておく (画面の警告がちらつかないように)
        tts?.shutdown()
        var created: TextToSpeech? = null
        created = TextToSpeech(appContext) { status ->
            // 古い接続からのコールバックは無視する
            if (tts === created) onInit(status)
        }
        tts = created
    }

    private fun onInit(status: Int) {
        val engine = tts
        if (engine == null || status != TextToSpeech.SUCCESS) {
            Log.w(TAG, "TextToSpeech init failed: $status")
            _voiceStatus.value = VoiceStatus.UNAVAILABLE
            return
        }
        engine.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
        val result = engine.setLanguage(Locale.JAPAN)
        _voiceStatus.value =
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                VoiceStatus.NO_JAPANESE
            } else {
                VoiceStatus.READY
            }
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= 31) {
            appContext.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Vibrator::class.java)
        }
        if (vibrator == null || !vibrator.hasVibrator()) return
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 500, 250, 500, 250, 800), -1)
        // 画面が消えていても震えるようにアラーム扱いにする
        if (Build.VERSION.SDK_INT >= 33) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(
                effect,
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(),
            )
        }
    }

    private companion object {
        const val TAG = "Announcer"
    }
}
