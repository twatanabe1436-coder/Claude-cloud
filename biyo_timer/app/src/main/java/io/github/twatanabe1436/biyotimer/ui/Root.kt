package io.github.twatanabe1436.biyotimer.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.twatanabe1436.biyotimer.BiyoTimerApp
import io.github.twatanabe1436.biyotimer.timer.Phase

private enum class Screen { TIMER, SETTINGS }

@Composable
fun BiyoTimerRoot(app: BiyoTimerApp) {
    val controller = app.controller
    val snapshot by controller.state.collectAsStateWithLifecycle()
    val settings by app.settingsStore.settings.collectAsStateWithLifecycle()
    val voiceStatus by app.announcer.voiceStatus.collectAsStateWithLifecycle()
    var screen by rememberSaveable { mutableStateOf(Screen.TIMER) }
    val context = LocalContext.current

    val active = snapshot.phase == Phase.COUNTDOWN ||
        snapshot.phase == Phase.RUNNING ||
        snapshot.phase == Phase.PAUSED
    KeepScreenOn(settings.keepScreenOn && active)

    when (screen) {
        Screen.TIMER -> TimerScreen(
            snapshot = snapshot,
            settings = settings,
            voiceStatus = voiceStatus,
            onStart = controller::start,
            onPause = controller::pause,
            onResume = controller::resume,
            onReset = controller::reset,
            onSelectPreset = { preset ->
                controller.updateSettings { it.copy(durationSec = preset.durationSec, presetName = preset.name) }
            },
            onSetDuration = { sec ->
                controller.updateSettings { it.copy(durationSec = sec, presetName = null) }
            },
            onOpenSettings = { screen = Screen.SETTINGS },
        )

        Screen.SETTINGS -> {
            BackHandler { screen = Screen.TIMER }
            SettingsScreen(
                settings = settings,
                voiceStatus = voiceStatus,
                onChange = controller::updateSettings,
                onPreview = { app.announcer.preview(settings) },
                onOpenVoiceSettings = { openVoiceSettings(context) },
                onInstallVoiceData = { installVoiceData(context) },
                onBack = { screen = Screen.TIMER },
            )
        }
    }
}

/** タイマー作動中に画面が消えないようにする。 */
@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}

private fun openVoiceSettings(context: Context) {
    // 「テキスト読み上げの出力」画面。機種によって無いので、無ければ端末の設定を開く
    val opened = tryStart(context, Intent("com.android.settings.TTS_SETTINGS")) ||
        tryStart(context, Intent(Settings.ACTION_SETTINGS))
    if (!opened) Toast.makeText(context, "設定画面を開けませんでした", Toast.LENGTH_SHORT).show()
}

private fun installVoiceData(context: Context) {
    if (!tryStart(context, Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA))) openVoiceSettings(context)
}

private fun tryStart(context: Context, intent: Intent): Boolean = try {
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
} catch (e: ActivityNotFoundException) {
    false
} catch (e: SecurityException) {
    false
}
