package io.github.twatanabe1436.biyotimer

import android.Manifest
import android.app.NotificationManager
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.twatanabe1436.biyotimer.runtime.TimerService
import io.github.twatanabe1436.biyotimer.timer.TimerSettings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 実機/エミュレータで画面を操作して、主要な流れがクラッシュせずに動くことを確かめる。
 * 途中の画面は /data/local/tmp/biyo_timer_shots に保存し、CI で取り出して確認する。
 */
@RunWith(AndroidJUnit4::class)
class SmokeTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val app: BiyoTimerApp get() = ApplicationProvider.getApplicationContext()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        if (Build.VERSION.SDK_INT >= 33) {
            instrumentation.uiAutomation.grantRuntimePermission(app.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        shell("mkdir -p $SHOT_DIR")
        onMain {
            app.controller.reset()
            app.controller.updateSettings { TimerSettings() }
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario.close()
        onMain { app.controller.reset() }
    }

    @Test
    fun idleScreenShowsExamDefaults() {
        compose.onNodeWithText("20:00").assertExists()
        compose.onNodeWithText("カッティング 20分").assertExists()
        compose.onNodeWithText("読み上げ：残り10分・5分・1分").assertExists()
        compose.onNodeWithText("スタート").assertExists()
        screenshot("01_idle")
    }

    @Test
    fun startCountdownPauseResumeAndReset() {
        compose.onNodeWithText("スタート").performClick()
        compose.onNodeWithText("まもなく開始").assertExists()
        screenshot("02_countdown")

        waitForText("作業中", timeoutMs = 6_000)
        waitUntil("通知が出ていない", 5_000) { timerNotificationShown() }
        compose.onNodeWithText("一時停止").assertExists()
        screenshot("03_running")

        compose.onNodeWithText("一時停止").performClick()
        waitForText("一時停止中")
        compose.onNodeWithText("再開").assertExists()
        screenshot("04_paused")

        compose.onNodeWithText("再開").performClick()
        waitForText("作業中")

        compose.onNodeWithText("一時停止").performClick()
        compose.onNodeWithText("リセット").performClick()
        waitForText("待機中")
        compose.onNodeWithText("20:00").assertExists()
        waitUntil("リセット後も通知が残っている", 5_000) { !timerNotificationShown() }
    }

    @Test
    fun cancelDuringCountdown() {
        compose.onNodeWithText("スタート").performClick()
        compose.onNodeWithText("キャンセル").performClick()
        waitForText("待機中")
        compose.onNodeWithText("スタート").assertExists()
    }

    @Test
    fun shortTimerFinishes() {
        onMain {
            app.controller.updateSettings {
                it.copy(durationSec = 3, countdownSec = 0, presetName = null, announceAtSec = setOf(1))
            }
        }
        compose.onNodeWithText("00:03").assertExists()
        compose.onNodeWithText("スタート").performClick()
        waitForText("終了", timeoutMs = 8_000)
        compose.onNodeWithText("00:00").assertExists()
        compose.onNodeWithText("もう一度").assertExists()
        screenshot("05_finished")
    }

    @Test
    fun presetAndCustomDuration() {
        compose.onNodeWithText("オールウェーブ 25分").performClick()
        compose.onNodeWithText("25:00").assertExists()

        compose.onNodeWithText("時間を指定").performClick()
        compose.onNodeWithText("分").performTextReplacement("5")
        compose.onNodeWithText("秒").performTextReplacement("30")
        screenshot("06_duration_dialog")
        compose.onNodeWithText("OK").performClick()

        compose.onNodeWithText("05:30").assertExists()
        compose.onNodeWithText("カスタム 5分30秒").assertExists()
        assertEquals(330, app.settingsStore.current.durationSec)
    }

    @Test
    fun settingsScreen() {
        compose.onNodeWithContentDescription("設定").performClick()
        compose.onNodeWithText("読み上げるタイミング").assertExists()
        screenshot("07_settings")

        compose.onNodeWithText("残り3分").performClick()
        assertTrue(180 in app.settingsStore.current.announceAtSec)

        compose.onNodeWithText("テスト再生").performScrollTo().performClick()
        compose.onNodeWithText("タイマー作動中は画面を消さない").performScrollTo()
        screenshot("08_settings_bottom")

        compose.onNodeWithContentDescription("戻る").performClick()
        compose.onNodeWithText("読み上げ：残り10分・5分・3分・1分").assertExists()
    }

    private fun waitForText(text: String, timeoutMs: Long = 3_000) {
        compose.waitUntil(timeoutMs) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitUntil(message: String, timeoutMs: Long, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!condition()) {
            if (System.currentTimeMillis() > deadline) throw AssertionError(message)
            Thread.sleep(100)
        }
    }

    private fun timerNotificationShown(): Boolean =
        app.getSystemService(NotificationManager::class.java)
            ?.activeNotifications
            ?.any { it.id == TimerService.NOTIFICATION_ID } == true

    private fun onMain(block: () -> Unit) = instrumentation.runOnMainSync(block)

    private fun screenshot(name: String) {
        compose.waitForIdle()
        shell("screencap -p $SHOT_DIR/$name.png")
    }

    private fun shell(command: String) {
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command))
            .use { it.readBytes() }
    }

    private companion object {
        const val SHOT_DIR = "/data/local/tmp/biyo_timer_shots"
    }
}
