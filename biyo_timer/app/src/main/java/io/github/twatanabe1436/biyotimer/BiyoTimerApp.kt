package io.github.twatanabe1436.biyotimer

import android.app.Application
import android.content.Context
import io.github.twatanabe1436.biyotimer.runtime.Announcer
import io.github.twatanabe1436.biyotimer.runtime.SettingsStore
import io.github.twatanabe1436.biyotimer.runtime.TimerController

class BiyoTimerApp : Application() {

    lateinit var settingsStore: SettingsStore
        private set
    lateinit var announcer: Announcer
        private set
    lateinit var controller: TimerController
        private set

    override fun onCreate() {
        super.onCreate()
        settingsStore = SettingsStore(this)
        // 読み上げエンジンの準備には時間がかかるので、起動直後から始めておく
        announcer = Announcer(this)
        controller = TimerController(this, settingsStore, announcer)
    }
}

val Context.app: BiyoTimerApp get() = applicationContext as BiyoTimerApp
