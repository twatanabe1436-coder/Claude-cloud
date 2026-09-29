package io.github.twatanabe1436.biyotimer.runtime

import android.content.Context
import androidx.core.content.edit
import io.github.twatanabe1436.biyotimer.timer.TimerSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** [TimerSettings] を SharedPreferences に保存し、変更を StateFlow で配信する。 */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(load())

    val settings: StateFlow<TimerSettings> = _settings.asStateFlow()
    val current: TimerSettings get() = _settings.value

    fun update(transform: (TimerSettings) -> TimerSettings) {
        val next = transform(_settings.value).sanitized()
        if (next == _settings.value) return
        _settings.value = next
        save(next)
    }

    private fun load(): TimerSettings {
        val d = TimerSettings()
        return TimerSettings(
            durationSec = prefs.getInt(KEY_DURATION, d.durationSec),
            presetName = prefs.getString(KEY_PRESET, d.presetName)?.takeIf { it.isNotEmpty() },
            countdownSec = prefs.getInt(KEY_COUNTDOWN, d.countdownSec),
            countdownBeep = prefs.getBoolean(KEY_COUNTDOWN_BEEP, d.countdownBeep),
            announceAtSec = TimerSettings.decodeSeconds(prefs.getString(KEY_ANNOUNCE, null))
                ?: d.announceAtSec,
            startPhraseEnabled = prefs.getBoolean(KEY_START_ENABLED, d.startPhraseEnabled),
            startPhrase = prefs.getString(KEY_START_PHRASE, null) ?: d.startPhrase,
            endPhraseEnabled = prefs.getBoolean(KEY_END_ENABLED, d.endPhraseEnabled),
            endPhrase = prefs.getString(KEY_END_PHRASE, null) ?: d.endPhrase,
            speechRate = prefs.getFloat(KEY_SPEECH_RATE, d.speechRate),
            vibrateOnFinish = prefs.getBoolean(KEY_VIBRATE, d.vibrateOnFinish),
            keepScreenOn = prefs.getBoolean(KEY_KEEP_SCREEN_ON, d.keepScreenOn),
        ).sanitized()
    }

    private fun save(s: TimerSettings) {
        prefs.edit {
            putInt(KEY_DURATION, s.durationSec)
            // null (手入力) は空文字で保存し、未保存 (= 初期値) と区別する
            putString(KEY_PRESET, s.presetName ?: "")
            putInt(KEY_COUNTDOWN, s.countdownSec)
            putBoolean(KEY_COUNTDOWN_BEEP, s.countdownBeep)
            putString(KEY_ANNOUNCE, TimerSettings.encodeSeconds(s.announceAtSec))
            putBoolean(KEY_START_ENABLED, s.startPhraseEnabled)
            putString(KEY_START_PHRASE, s.startPhrase)
            putBoolean(KEY_END_ENABLED, s.endPhraseEnabled)
            putString(KEY_END_PHRASE, s.endPhrase)
            putFloat(KEY_SPEECH_RATE, s.speechRate)
            putBoolean(KEY_VIBRATE, s.vibrateOnFinish)
            putBoolean(KEY_KEEP_SCREEN_ON, s.keepScreenOn)
        }
    }

    private companion object {
        const val KEY_DURATION = "duration_sec"
        const val KEY_PRESET = "preset_name"
        const val KEY_COUNTDOWN = "countdown_sec"
        const val KEY_COUNTDOWN_BEEP = "countdown_beep"
        const val KEY_ANNOUNCE = "announce_at_sec"
        const val KEY_START_ENABLED = "start_phrase_enabled"
        const val KEY_START_PHRASE = "start_phrase"
        const val KEY_END_ENABLED = "end_phrase_enabled"
        const val KEY_END_PHRASE = "end_phrase"
        const val KEY_SPEECH_RATE = "speech_rate"
        const val KEY_VIBRATE = "vibrate_on_finish"
        const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
    }
}
