package io.github.twatanabe1436.biyotimer.runtime

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import io.github.twatanabe1436.biyotimer.timer.ExamTimer
import io.github.twatanabe1436.biyotimer.timer.Phase
import io.github.twatanabe1436.biyotimer.timer.TimerEvent
import io.github.twatanabe1436.biyotimer.timer.TimerSettings
import io.github.twatanabe1436.biyotimer.timer.TimerSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * アプリ全体で 1 つのタイマー。画面・通知のどちらからもここを操作する。
 * メインスレッドで [ExamTimer] を短い間隔で進め、イベントを [Announcer] に渡す。
 * 計測中は [TimerService] を起動して、画面が消えても動き続けるようにする。
 */
class TimerController(
    private val context: Context,
    private val settingsStore: SettingsStore,
    private val announcer: Announcer,
) {
    private val handler = Handler(Looper.getMainLooper())
    private val timer = ExamTimer(settingsStore.current.toConfig())

    private val _state = MutableStateFlow(timer.snapshot(now()))
    val state: StateFlow<TimerSnapshot> = _state.asStateFlow()

    private val ticker = object : Runnable {
        override fun run() {
            handle(timer.tick(now()))
            if (timer.phase == Phase.COUNTDOWN || timer.phase == Phase.RUNNING) {
                handler.postDelayed(this, TICK_INTERVAL_MS)
            }
        }
    }

    fun start() {
        if (timer.phase != Phase.IDLE && timer.phase != Phase.FINISHED) return
        timer.updateConfig(settingsStore.current.toConfig())
        val events = timer.start(now())
        TimerService.start(context)
        handle(events)
        restartTicker()
    }

    fun pause() {
        if (timer.phase != Phase.RUNNING) return
        handler.removeCallbacks(ticker)
        handle(timer.pause(now()))
    }

    fun resume() {
        if (timer.phase != Phase.PAUSED) return
        timer.resume(now())
        publish()
        restartTicker()
    }

    /** 計測をやめて待機状態に戻す。カウントダウン中のキャンセルもこれ。 */
    fun reset() {
        handler.removeCallbacks(ticker)
        announcer.stop()
        timer.reset()
        timer.updateConfig(settingsStore.current.toConfig())
        publish()
    }

    /** 設定を保存する。時間などの変更は待機中 (または終了後) なら即座に反映する。 */
    fun updateSettings(transform: (TimerSettings) -> TimerSettings) {
        settingsStore.update(transform)
        if (timer.phase == Phase.IDLE || timer.phase == Phase.FINISHED) {
            val config = settingsStore.current.toConfig()
            if (config != timer.config) {
                timer.updateConfig(config)
                publish()
            }
        }
    }

    private fun handle(events: List<TimerEvent>) {
        val settings = settingsStore.current
        events.forEach { announcer.onEvent(it, settings) }
        publish()
    }

    private fun publish() {
        _state.value = timer.snapshot(now())
    }

    private fun restartTicker() {
        handler.removeCallbacks(ticker)
        handler.postDelayed(ticker, TICK_INTERVAL_MS)
    }

    private fun now(): Long = SystemClock.elapsedRealtime()

    private companion object {
        /** 読み上げの遅れは最大この程度。表示も 20fps で更新される。 */
        const val TICK_INTERVAL_MS = 50L
    }
}
