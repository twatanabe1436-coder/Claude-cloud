package io.github.twatanabe1436.hanaso.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.twatanabe1436.hanaso.core.Conversation
import io.github.twatanabe1436.hanaso.core.EngineMode
import io.github.twatanabe1436.hanaso.core.Rating
import io.github.twatanabe1436.hanaso.core.ScriptEngine
import io.github.twatanabe1436.hanaso.core.Stats
import io.github.twatanabe1436.hanaso.ui.talk.FinishedSession
import io.github.twatanabe1436.hanaso.ui.talk.LoadState
import io.github.twatanabe1436.hanaso.ui.talk.errorText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun SummaryScreen(result: FinishedSession, onAgain: () -> Unit, onHome: () -> Unit) {
    val app = LocalApp.current
    val context = LocalContext.current
    val phrases by app.store.phrases.collectAsStateWithLifecycle()
    val savedSet = remember(phrases) { phrases.map { Stats.normalizePhrase(it.en) }.toSet() }
    val scenario = result.scenario

    fun play(text: String) {
        app.speaker.stop()
        app.speaker.say(text)
    }

    fun save(en: String, ja: String) {
        if (app.savePhrase(en, ja, scenario.titleJa) != null) context.toast("フレーズ帳に保存しました ⭐")
    }

    fun load() {
        result.summary = LoadState.Loading
        app.scope.launch {
            result.summary = try {
                val s = result.engine.summary(Conversation(scenario, result.level, result.history), result.completed)
                app.store.updateSession(result.recordId) { it.copy(score = s.score) }
                LoadState.Ready(s)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LoadState.Failed(errorText(e))
            }
        }
    }

    LaunchedEffect(result) { if (result.summary == null) load() }

    val minutes = maxOf(1L, (result.endedAt - result.startedAt) / 60_000)
    val corrections = result.turns.filter { it.feedback != null && it.feedback?.rating != Rating.GREAT }
    val summary = result.summary

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Emoji(scenario.emoji, size = 40)
                Text("おつかれさまでした！", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("${scenario.titleJa}・${result.level.displayJa}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (summary is LoadState.Ready) {
            item {
                Card {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        ScoreRing(summary.value.score)
                        Spacer(Modifier.height(10.dp))
                        Text(summary.value.headlineJa, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
            }
            summary.value.estimatedLevel?.let { estimated ->
                item {
                    Card {
                        Heading("📏 今回の英語レベル（推定）")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(estimated.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(estimated.ja, fontWeight = FontWeight.Bold)
                                Text("選んだレベル: ${result.level.displayJa}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (summary.value.levelCommentJa.isNotBlank()) {
                            Text(summary.value.levelCommentJa, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
                        }
                        Text(
                            "AI による目安です。短い会話では正確に判定できないことがあります。",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat("🗣️ ${result.turns.size}", "発話", Modifier.weight(1f))
                Stat("⏱️ $minutes", "分", Modifier.weight(1f))
                if (scenario.missions.isNotEmpty()) {
                    Stat("🎯 ${result.completed.size}/${scenario.missions.size}", "ミッション", Modifier.weight(1f))
                }
            }
        }
        when (summary) {
            is LoadState.Ready -> {
                val s = summary.value
                if (scenario.missions.isNotEmpty()) {
                    item {
                        Card {
                            Heading("🎯 ミッション")
                            scenario.missions.forEach { m ->
                                val ok = m.id in result.completed
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                                    Icon(
                                        if (ok) Icons.Filled.Check else Icons.Filled.Close,
                                        contentDescription = null,
                                        tint = if (ok) LocalGrades.current.great else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(m.ja, color = if (ok) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                item {
                    Card {
                        Heading("👍 よかった点")
                        s.goodPointsJa.forEach { Text("・$it", modifier = Modifier.padding(vertical = 3.dp)) }
                    }
                }
                item {
                    Card {
                        Heading("📈 伸ばしたいポイント")
                        s.improvePoints.forEach { p ->
                            Text(p.pointJa, modifier = Modifier.padding(top = 6.dp))
                            val ja = ScriptEngine.lookupJa(p.exampleEn).orEmpty()
                            PhraseRow(p.exampleEn, ja, onPlay = { play(p.exampleEn) }, saved = Stats.normalizePhrase(p.exampleEn) in savedSet, onSave = { save(p.exampleEn, ja) })
                        }
                    }
                }
                item {
                    Card {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Heading("⭐ 覚えたいフレーズ", Modifier.weight(1f))
                            TextButton(onClick = {
                                val added = s.keyPhrases.count { app.savePhrase(it.en, it.ja, scenario.titleJa) != null }
                                context.toast(if (added > 0) "${added}件をフレーズ帳に保存しました ⭐" else "すべて保存済みです")
                            }) { Text("すべて保存") }
                        }
                        s.keyPhrases.forEach { p ->
                            PhraseRow(p.en, p.ja, onPlay = { play(p.en) }, saved = Stats.normalizePhrase(p.en) in savedSet, onSave = { save(p.en, p.ja) })
                        }
                    }
                }
            }
            is LoadState.Failed -> item {
                Card {
                    Text(summary.message, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { load() }) { Text("もう一度") }
                }
            }
            else -> item {
                Card {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            if (result.engine.mode == EngineMode.AI) "AI コーチが振り返りを作成中…" else "振り返りを作成中…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        if (corrections.isNotEmpty()) {
            item {
                Card {
                    Heading(if (result.engine.mode == EngineMode.SCRIPT) "📖 お手本と比べよう" else "✏️ 今回の言い直し")
                    corrections.forEach { t ->
                        val fb = t.feedback ?: return@forEach
                        val ja = ScriptEngine.lookupJa(fb.natural).orEmpty()
                        Text("あなた: ${t.text}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                        PhraseRow(fb.natural, ja, onPlay = { play(fb.natural) }, saved = Stats.normalizePhrase(fb.natural) in savedSet, onSave = { save(fb.natural, ja) })
                        Text(fb.explanationJa, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (summary is LoadState.Ready) {
            item {
                Card {
                    Heading("🚀 次のチャレンジ")
                    Text(summary.value.nextChallengeJa)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)) {
                Button(onClick = onAgain) {
                    Icon(AppIcons.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("もう一度話す")
                }
                OutlinedButton(onClick = onHome) {
                    Icon(Icons.Filled.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("ホームへ")
                }
            }
        }
    }
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun Heading(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = modifier.padding(bottom = 4.dp))
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest, tonalElevation = 1.dp) {
        Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.ExtraBold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ScoreRing(score: Int) {
    val color = LocalGrades.current.ofScore(score)
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
            drawArc(color, -90f, 360f * score / 100f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$score", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
            Text("スコア", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
