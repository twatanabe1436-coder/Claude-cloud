package io.github.twatanabe1436.hanaso.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.twatanabe1436.hanaso.core.SpeechScore
import io.github.twatanabe1436.hanaso.core.SpeechScorer
import io.github.twatanabe1436.hanaso.core.Xp
import io.github.twatanabe1436.hanaso.data.SavedPhrase
import io.github.twatanabe1436.hanaso.speech.SpeechInput

@Composable
fun PhrasesScreen(onStartTalk: () -> Unit) {
    val app = LocalApp.current
    val phrases by app.store.phrases.collectAsStateWithLifecycle()
    var flashcards by remember { mutableStateOf<List<SavedPhrase>?>(null) }
    var deleting by remember { mutableStateOf<SavedPhrase?>(null) }

    fun record(p: SavedPhrase, score: Int) = app.store.updatePhrase(p.id) {
        it.copy(practiceCount = it.practiceCount + 1, bestScore = maxOf(it.bestScore ?: 0, score))
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("フレーズ帳", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("${phrases.size} フレーズ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (phrases.isNotEmpty()) {
                    Button(onClick = { flashcards = phrases.shuffled().take(10) }) { Text("🃏 フラッシュカード") }
                }
            }
        }
        if (phrases.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Emoji("⭐", size = 44)
                    Spacer(Modifier.height(8.dp))
                    Text("まだフレーズがありません", fontWeight = FontWeight.Bold)
                    Text(
                        "会話中の「保存」ボタンや、振り返り画面の「覚えたいフレーズ」から保存できます。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                    Button(onClick = onStartTalk) { Text("会話をはじめる") }
                }
            }
        }
        items(phrases, key = { it.id }) { p ->
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest, tonalElevation = 1.dp) {
                Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PhraseRow(p.en, p.ja, onPlay = {
                            app.speaker.stop()
                            app.speaker.say(p.en)
                        }, modifier = Modifier.weight(1f))
                        IconButton(onClick = { deleting = p }) {
                            Icon(Icons.Filled.Delete, contentDescription = "削除", tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                    val meta = listOfNotNull(
                        p.source.ifBlank { null },
                        p.bestScore?.let { "ベスト ${it}点・${p.practiceCount}回練習" },
                    ).joinToString("　")
                    if (meta.isNotEmpty()) {
                        Text(meta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(6.dp))
                    PracticeButton(p.en, onResult = { record(p, it.score) })
                }
            }
        }
    }

    flashcards?.let { deck ->
        FlashcardsDialog(deck, onResult = { p, score -> record(p, score) }, onClose = { flashcards = null })
    }

    deleting?.let { p ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("フレーズを削除しますか？") },
            text = { Text(p.en) },
            confirmButton = {
                TextButton(onClick = {
                    app.store.removePhrase(p.id)
                    deleting = null
                }) { Text("削除") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("キャンセル") } },
        )
    }
}

/** 日本語を見て英語で言うフラッシュカード練習 */
@Composable
private fun FlashcardsDialog(deck: List<SavedPhrase>, onResult: (SavedPhrase, Int) -> Unit, onClose: () -> Unit) {
    val app = LocalApp.current
    val context = LocalContext.current
    val mic = rememberMicPermission()
    var index by remember { mutableIntStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<Pair<SpeechScore, String>?>(null) }
    var listening by remember { mutableStateOf(false) }
    var live by remember { mutableStateOf("") }
    val card = deck[index]

    DisposableEffect(Unit) {
        onDispose {
            app.speechInput.cancel()
            app.speaker.stop()
        }
    }

    fun listen() {
        app.speaker.stop()
        listening = true
        live = ""
        app.speechInput.start(1500, app.store.settings.value.tapToFinish, object : SpeechInput.Callback {
            override fun onPartial(text: String) {
                live = text
            }

            override fun onError(messageJa: String) = context.toast(messageJa)

            override fun onEnd(text: String) {
                listening = false
                val r = SpeechScorer.score(card.en, text)
                result = r to text
                revealed = true
                app.awardXp(Xp.forPractice(r.score))
                onResult(card, r.score)
                app.speaker.say(card.en)
            }

            override fun onCancel() {
                listening = false
            }
        })
    }

    fun next() {
        app.speechInput.cancel()
        if (index + 1 >= deck.size) {
            context.toast("おつかれさまでした！")
            onClose()
            return
        }
        index++
        revealed = false
        result = null
        live = ""
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${index + 1} / ${deck.size}", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "閉じる") }
                }
                Spacer(Modifier.height(48.dp))
                Text("英語で言ってみよう", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(
                    card.ja.ifBlank { "（訳なし）" },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(28.dp))
                Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (revealed) {
                        Text(card.en, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        result?.let { (r, heard) -> ScoreResultView(r, heard, Modifier.padding(top = 12.dp).fillMaxWidth()) }
                    } else {
                        Text("？", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.outline)
                    }
                    if (listening) {
                        Text(live.ifBlank { "🎤 どうぞ…" }, modifier = Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        revealed = true
                        app.speaker.stop()
                        app.speaker.say(card.en)
                    }) { Text("答えを見る") }
                    Surface(
                        onClick = {
                            when {
                                listening -> app.speechInput.stop()
                                !app.speechInput.available -> context.toast("この端末では音声認識が使えません。")
                                else -> mic.request { listen() }
                            }
                        },
                        shape = CircleShape,
                        color = if (listening) LocalGrades.current.fix else MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.semantics { contentDescription = if (listening) "話し終わる" else "話す" },
                    ) {
                        Box(Modifier.size(76.dp), contentAlignment = Alignment.Center) {
                            Icon(if (listening) AppIcons.Stop else AppIcons.Mic, contentDescription = null, modifier = Modifier.size(34.dp))
                        }
                    }
                    TextButton(onClick = { next() }) {
                        Text(if (index + 1 >= deck.size) "終わる" else "次へ")
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    }
                }
                Spacer(Modifier.width(1.dp))
            }
        }
    }
}
