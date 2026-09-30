package io.github.twatanabe1436.hanaso.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import io.github.twatanabe1436.hanaso.HanasoApp
import io.github.twatanabe1436.hanaso.core.Level
import io.github.twatanabe1436.hanaso.core.SpeechScore
import io.github.twatanabe1436.hanaso.core.SpeechScorer
import io.github.twatanabe1436.hanaso.speech.SpeechInput

val LocalApp = staticCompositionLocalOf<HanasoApp> { error("HanasoApp が提供されていません") }

fun Context.toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

/** 小さな丸いボタン (再生・保存・訳 など) */
@Composable
fun SmallChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    highlighted: Boolean = false,
    tint: Color? = null,
    enabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    val content = tint ?: if (highlighted) scheme.onPrimary else scheme.onSurfaceVariant
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = CircleShape,
        color = if (highlighted) scheme.primary else scheme.surface,
        contentColor = content,
        border = BorderStroke(1.dp, if (tint != null) tint else if (highlighted) scheme.primary else scheme.outlineVariant),
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun LevelBadge(level: Level, modifier: Modifier = Modifier) {
    val grades = LocalGrades.current
    val (fg, bg) = when (level) {
        Level.BEGINNER -> grades.great to grades.greatContainer
        Level.INTERMEDIATE -> grades.good to grades.goodContainer
        Level.ADVANCED -> grades.fix to grades.fixContainer
    }
    Surface(modifier = modifier, shape = CircleShape, color = bg, contentColor = fg) {
        Text(
            level.ja,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** 英文 + 日本語訳 + 再生ボタン (+ 保存ボタン) の1行 */
@Composable
fun PhraseRow(
    en: String,
    ja: String,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
    saved: Boolean? = null,
    onSave: () -> Unit = {},
) {
    Row(modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(en, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            if (ja.isNotBlank()) {
                Text(ja, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        IconButton(onClick = onPlay) {
            Icon(AppIcons.VolumeUp, contentDescription = "再生", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (saved != null) {
            IconButton(onClick = onSave) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = if (saved) "保存済み" else "フレーズ帳に保存",
                    tint = if (saved) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}

/** 発音チェックの結果: 点数と、言えた単語 (緑) / 言えなかった単語 (赤) */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScoreResultView(result: SpeechScore, heard: String, modifier: Modifier = Modifier) {
    val grades = LocalGrades.current
    val (color, label) = when {
        result.score >= 90 -> grades.great to "すばらしい！"
        result.score >= 60 -> grades.good to "おしい！"
        else -> grades.fix to "もう一度！"
    }
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${result.score}", style = MaterialTheme.typography.headlineSmall, color = color, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.width(8.dp))
                Text(label, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 3.dp))
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                result.words.forEach { w ->
                    Text(
                        w.text,
                        color = if (w.ok) grades.great else grades.fix,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (w.ok) null else TextDecoration.Underline,
                    )
                }
            }
            Text(
                "聞き取り: " + if (heard.isBlank()) "（聞き取れませんでした）" else "“$heard”",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** マイクの権限。許可されていなければ確認ダイアログを出してから action を実行する */
class MicPermission(private val context: Context) {
    internal var launcher: ManagedActivityResultLauncher<String, Boolean>? = null
    internal var pending: (() -> Unit)? = null

    val granted: Boolean
        get() = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    fun request(action: () -> Unit) {
        if (granted) {
            action()
            return
        }
        pending = action
        launcher?.launch(Manifest.permission.RECORD_AUDIO)
    }
}

@Composable
fun rememberMicPermission(): MicPermission {
    val context = LocalContext.current
    val permission = remember { MicPermission(context) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        val action = permission.pending
        permission.pending = null
        if (ok) action?.invoke() else context.toast("マイクが許可されていないため音声で話せません。キーボード入力は使えます。")
    }
    SideEffect { permission.launcher = launcher }
    return permission
}

/** 「言ってみる」: お手本の文を話して、どの単語が言えたかを採点する */
@Composable
fun PracticeButton(
    target: String,
    modifier: Modifier = Modifier,
    label: String = "言ってみる",
    onResult: (SpeechScore) -> Unit = {},
) {
    val app = LocalApp.current
    val context = LocalContext.current
    val mic = rememberMicPermission()
    var listening by remember { mutableStateOf(false) }
    var live by remember { mutableStateOf("") }
    var result by remember(target) { mutableStateOf<Pair<SpeechScore, String>?>(null) }

    // 画面から消えたら認識を止める
    DisposableEffect(Unit) {
        onDispose { if (listening) app.speechInput.cancel() }
    }

    fun start() {
        app.speaker.stop()
        result = null
        live = ""
        listening = true
        app.speechInput.start(1500, object : SpeechInput.Callback {
            override fun onPartial(text: String) {
                live = text
            }

            override fun onError(messageJa: String) = context.toast(messageJa)

            override fun onEnd(text: String) {
                listening = false
                live = ""
                val r = SpeechScorer.score(target, text)
                result = r to text
                onResult(r)
            }

            override fun onCancel() {
                listening = false
                live = ""
            }
        })
    }

    Column(modifier) {
        SmallChip(
            text = if (listening) "話し終わったらタップ" else label,
            icon = AppIcons.Mic,
            highlighted = listening,
            onClick = {
                when {
                    listening -> app.speechInput.stop()
                    !app.speechInput.available -> context.toast("この端末では音声認識が使えません。")
                    else -> mic.request { start() }
                }
            },
        )
        if (listening) {
            Text(
                live.ifBlank { "🎤 どうぞ…" },
                modifier = Modifier.padding(top = 6.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        result?.let { (r, heard) -> ScoreResultView(r, heard, Modifier.padding(top = 8.dp)) }
    }
}

/** 絵文字を大きめに表示する */
@Composable
fun Emoji(emoji: String, size: Int = 28, modifier: Modifier = Modifier) {
    Text(emoji, modifier = modifier, fontSize = size.sp)
}
