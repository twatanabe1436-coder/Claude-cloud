package io.github.twatanabe1436.biyotimer.runtime

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.twatanabe1436.biyotimer.MainActivity
import io.github.twatanabe1436.biyotimer.R
import io.github.twatanabe1436.biyotimer.app
import io.github.twatanabe1436.biyotimer.timer.Phase
import io.github.twatanabe1436.biyotimer.timer.Phrases
import io.github.twatanabe1436.biyotimer.timer.TimerSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * 計測中だけ動くフォアグラウンドサービス。
 * 通知に残り時間を出し、CPU を起こしておくことで、画面が消えても読み上げが続く。
 * タイマーの進行そのものは [TimerController] が行う。
 */
class TimerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val controller: TimerController get() = app.controller
    private var wakeLock: PowerManager.WakeLock? = null
    private var stopJob: Job? = null
    private var inForeground = false

    override fun onCreate() {
        super.onCreate()
        createChannel()
        scope.launch {
            // 状態が変わったときと、次の読み上げが進んだときだけ通知を更新する
            controller.state.map { it.phase to it.nextAnnouncementSec }.distinctUntilChanged().collect { (phase, _) ->
                // startForeground より前に止めるとクラッシュするので、前面化してから反応する
                if (inForeground) onStateChanged(phase)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!goForeground()) return START_NOT_STICKY
        when (intent?.action) {
            ACTION_PAUSE -> controller.pause()
            ACTION_RESUME -> controller.resume()
            ACTION_RESET -> controller.reset()
        }
        onStateChanged(controller.state.value.phase)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        releaseWakeLock()
        super.onDestroy()
    }

    private fun onStateChanged(phase: Phase) {
        stopJob?.cancel()
        stopJob = null
        when (phase) {
            Phase.IDLE -> stopNow()
            Phase.COUNTDOWN, Phase.RUNNING -> {
                holdWakeLock()
                updateNotification()
            }
            Phase.PAUSED -> {
                releaseWakeLock()
                updateNotification()
            }
            Phase.FINISHED -> {
                // 「作業やめ」を読み終えるまで少し待ってから止める
                holdWakeLock()
                updateNotification()
                stopJob = scope.launch {
                    delay(FINISH_LINGER_MS)
                    stopNow()
                }
            }
        }
    }

    private fun goForeground(): Boolean {
        val notification = buildNotification(controller.state.value)
        return try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            inForeground = true
            true
        } catch (e: Exception) {
            // 前面化できなくても、アプリを開いている間はタイマーは動く
            Log.w(TAG, "startForeground failed", e)
            stopSelf()
            false
        }
    }

    private fun stopNow() {
        releaseWakeLock()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        inForeground = false
        stopSelf()
    }

    private fun updateNotification() {
        getSystemService(NotificationManager::class.java)
            ?.notify(NOTIFICATION_ID, buildNotification(controller.state.value))
    }

    private fun buildNotification(snapshot: TimerSnapshot): android.app.Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openAppIntent())
            .setShowWhen(false)

        val remaining = Phrases.clock(snapshot.remainingDisplaySec)
        val next = snapshot.nextAnnouncementSec?.let { "次の読み上げ：残り${Phrases.duration(it)}" } ?: "次の合図：終了"
        when (snapshot.phase) {
            Phase.COUNTDOWN -> builder
                .setContentTitle(title("まもなく開始"))
                .setContentText("作業時間 $remaining")
            Phase.RUNNING -> builder
                .setContentTitle(title("作業中"))
                .setContentText(next)
                // 残り時間は通知のヘッダーにカウントダウン表示される
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis() + snapshot.remainingMs)
                .addAction(0, "一時停止", serviceIntent(ACTION_PAUSE))
            Phase.PAUSED -> builder
                .setContentTitle(title("一時停止中"))
                .setContentText("残り $remaining")
                .addAction(0, "再開", serviceIntent(ACTION_RESUME))
                .addAction(0, "リセット", serviceIntent(ACTION_RESET))
            Phase.FINISHED -> builder
                .setContentTitle(title("終了"))
                .setContentText("時間になりました")
            Phase.IDLE -> builder
                .setContentTitle("待機中")
                .setContentText(remaining)
        }
        return builder.build()
    }

    /** 例: "作業中 · カッティング" */
    private fun title(state: String): String =
        app.settingsStore.current.activePreset?.let { "$state · ${it.name}" } ?: state

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun serviceIntent(action: String): PendingIntent = PendingIntent.getService(
        this,
        action.hashCode(),
        Intent(this, TimerService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "タイマー", NotificationManager.IMPORTANCE_LOW).apply {
            description = "計測中の残り時間を表示します"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    private fun holdWakeLock() {
        val lock = wakeLock ?: getSystemService(PowerManager::class.java)
            ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BiyoTimer:timer")
            ?.also {
                it.setReferenceCounted(false)
                wakeLock = it
            }
        lock?.acquire(MAX_WAKE_LOCK_MS)
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
    }

    companion object {
        const val NOTIFICATION_ID = 1
        private const val TAG = "TimerService"
        private const val CHANNEL_ID = "timer"
        private const val ACTION_PAUSE = "io.github.twatanabe1436.biyotimer.PAUSE"
        private const val ACTION_RESUME = "io.github.twatanabe1436.biyotimer.RESUME"
        private const val ACTION_RESET = "io.github.twatanabe1436.biyotimer.RESET"
        private const val FINISH_LINGER_MS = 8_000L

        /** 最長の作業時間 (99:59) より長くしておく。解放し忘れの保険。 */
        private const val MAX_WAKE_LOCK_MS = 2 * 60 * 60 * 1000L

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, TimerService::class.java))
            } catch (e: Exception) {
                Log.w(TAG, "could not start foreground service", e)
            }
        }
    }
}
