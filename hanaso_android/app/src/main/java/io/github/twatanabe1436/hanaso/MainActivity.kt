package io.github.twatanabe1436.hanaso

import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import io.github.twatanabe1436.hanaso.ui.HanasoRoot
import io.github.twatanabe1436.hanaso.ui.HanasoTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // 音量ボタンで読み上げ (メディア) の音量を変えられるようにする
        volumeControlStream = AudioManager.STREAM_MUSIC

        setContent {
            HanasoTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    HanasoRoot(app)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        app.speaker.recheck()
    }

    override fun onStop() {
        super.onStop()
        // 画面回転以外で裏に回ったら、マイクと読み上げを止める
        if (!isChangingConfigurations) app.talk?.pause()
    }
}
