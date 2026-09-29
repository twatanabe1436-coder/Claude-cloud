package io.github.twatanabe1436.biyotimer

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import io.github.twatanabe1436.biyotimer.ui.BiyoTimerRoot
import io.github.twatanabe1436.biyotimer.ui.BiyoTimerTheme

class MainActivity : ComponentActivity() {

    // 拒否されても通知が出ないだけでタイマーは動くので、結果は使わない
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // 音量ボタンで読み上げ (メディア) の音量を変えられるようにする
        volumeControlStream = AudioManager.STREAM_MUSIC
        if (savedInstanceState == null) askNotificationPermission()

        setContent {
            BiyoTimerTheme {
                BiyoTimerRoot(app)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        app.announcer.recheck()
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
