package io.github.twatanabe1436.sodateru

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import io.github.twatanabe1436.sodateru.ui.AppRoot
import io.github.twatanabe1436.sodateru.ui.Navigator
import io.github.twatanabe1436.sodateru.ui.theme.SodateruTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as SodateruApp).container
        setContent {
            SodateruTheme {
                val navigator = remember { Navigator() }
                AppRoot(container, navigator)
            }
        }
    }
}
