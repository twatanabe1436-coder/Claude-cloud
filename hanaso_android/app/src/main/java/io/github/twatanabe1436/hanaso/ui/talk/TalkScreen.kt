package io.github.twatanabe1436.hanaso.ui.talk

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.twatanabe1436.hanaso.core.EngineMode
import io.github.twatanabe1436.hanaso.core.Feedback
import io.github.twatanabe1436.hanaso.core.HintSuggestion
import io.github.twatanabe1436.hanaso.core.Rating
import io.github.twatanabe1436.hanaso.core.Scenario
import io.github.twatanabe1436.hanaso.core.ScriptEngine
import io.github.twatanabe1436.hanaso.core.ScriptTask
import io.github.twatanabe1436.hanaso.core.SpeechScore
import io.github.twatanabe1436.hanaso.core.SpeechScorer
import io.github.twatanabe1436.hanaso.core.Stats
import io.github.twatanabe1436.hanaso.ui.AppIcons
import io.github.twatanabe1436.hanaso.ui.Emoji
import io.github.twatanabe1436.hanaso.ui.LevelBadge
import io.github.twatanabe1436.hanaso.ui.LocalApp
import io.github.twatanabe1436.hanaso.ui.LocalGrades
import io.github.twatanabe1436.hanaso.ui.MicPermission
import io.github.twatanabe1436.hanaso.ui.PracticeButton
import io.github.twatanabe1436.hanaso.ui.SmallChip
import io.github.twatanabe1436.hanaso.ui.rememberMicPermission
import io.github.twatanabe1436.hanaso.ui.toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TalkScreen(session: TalkSession, onQuit: () -> Unit, onFinish: () -> Unit) {
    val app = LocalApp.current
    val context = LocalContext.current
    val mic = rememberMicPermission()
    val settings by app.store.settings.collectAsStateWithLifecycle()
    val phrases by app.store.phrases.collectAsStateWithLifecycle()
    val savedSet = remember(phrases) { phrases.map { Stats.normalizePhrase(it.en) }.toSet() }
    val isSaved: (String) -> Boolean = { Stats.normalizePhrase(it) in savedSet }
    var confirmQuit by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    KeepScreenOn()
    // 戻るボタン: 話した後なら確認してからやめる
    BackHandler { if (session.learnerTurns > 0) confirmQuit = true else onQuit() }
    LaunchedEffect(session) { session.messages.collect { context.toast(it) } }

    // 新しいメッセージが来たら一番下 (reverseLayout なので 0 番目) へ
    LaunchedEffect(session.items.size) { listState.animateScrollToItem(0) }

    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    // 入力欄を閉じたとき・台本を終えたとき (入力欄が消えるとき) は、キーボードもしまう
    LaunchedEffect(session.showKeyboard, session.scriptDone) {
        if (!session.showKeyboard || session.scriptDone) {
            focusManager.clearFocus()
            keyboard?.hide()
        }
    }
    val onMic: () -> Unit = {
        // 声で答えるときはキーボードをしまう
        focusManager.clearFocus()
        keyboard?.hide()
        when {
            session.listening -> session.toggleMic()
            !app.speechInput.available -> session.startListening() // 使えない旨を表示してキーボードに切り替える
            else -> mic.request { session.startListening() }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Header(
            session = session,
            onQuit = { if (session.learnerTurns > 0) confirmQuit = true else onQuit() },
            onFinish = onFinish,
        )
        if (session.scenario.missions.isNotEmpty()) MissionPanel(session, compact = session.mode == EngineMode.SCRIPT)
        if (session.mode == EngineMode.DEMO) {
            Text(
                "🧪 デモモード（決まった返事のみ）",
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LocalGrades.current.goodContainer)
                    .padding(vertical = 3.dp),
            )
        }
        LazyColumn(
            state = listState,
            reverseLayout = true,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.Bottom),
        ) {
            items(session.items.asReversed(), key = { it.id }) { item ->
                when (item) {
                    is Scene -> SceneCard(session.scenario)
                    is AiMessage -> AiBubble(item, session, settings.showText, isSaved)
                    is LearnerMessage -> LearnerBubble(item, session, isSaved)
                    is Celebration -> CelebrationCard(item, onFinish)
                }
            }
        }
        Footer(session, onMic, onFinish)
    }

    session.hint?.let { hint ->
        ModalBottomSheet(
            onDismissRequest = { session.closeHint() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            HintSheet(hint, session, mic)
        }
    }

    if (confirmQuit) {
        AlertDialog(
            onDismissRequest = { confirmQuit = false },
            title = { Text("会話をやめますか？") },
            text = { Text("振り返りは作成されません。記録を残すには「終了」を押してください。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmQuit = false
                    onQuit()
                }) { Text("やめる") }
            },
            dismissButton = { TextButton(onClick = { confirmQuit = false }) { Text("続ける") } },
        )
    }
}

/** 会話中は画面を消さない */
@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

@Composable
private fun Header(session: TalkSession, onQuit: () -> Unit, onFinish: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, tonalElevation = 2.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onQuit) { Icon(Icons.Filled.Close, contentDescription = "やめる") }
            Emoji(session.scenario.emoji, size = 20)
            Spacer(Modifier.width(6.dp))
            Text(
                session.scenario.titleJa,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(6.dp))
            LevelBadge(session.level)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { session.toggleMute() }) {
                Icon(
                    if (session.muted) AppIcons.VolumeOff else AppIcons.VolumeUp,
                    contentDescription = if (session.muted) "自動読み上げをオンにする" else "自動読み上げをオフにする",
                )
            }
            OutlinedButton(onClick = onFinish, contentPadding = PaddingValues(horizontal = 14.dp)) { Text("終了") }
            Spacer(Modifier.width(6.dp))
        }
    }
}

/** ミッションの一覧。compact (台本モード) では達成数だけを 1 行で出す */
@Composable
private fun MissionPanel(session: TalkSession, compact: Boolean) {
    val grades = LocalGrades.current
    val missions = session.scenario.missions
    val done = missions.count { it.id in session.completed }
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
            Row {
                Text("🎯 ミッション", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("$done / ${missions.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            if (!compact) missions.forEach { m ->
                val ok = m.id in session.completed
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 1.dp)) {
                    Icon(
                        if (ok) Icons.Filled.Check else AppIcons.Flag,
                        contentDescription = if (ok) "達成" else null,
                        tint = if (ok) grades.great else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        m.ja,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (ok) grades.great else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (ok) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

/** 台本モードのお題。答えるときに目に入るよう、画面下のパネルのマイクのすぐ上に出す */
@Composable
private fun TaskPrompt(session: TalkSession, task: ScriptTask) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = scheme.primaryContainer,
        contentColor = scheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            if (task.finished) {
                Text("🎉 台本クリア！「振り返りを見る」で結果を確認しよう", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            } else {
                TaskBody(session, task)
            }
        }
    }
}

@Composable
private fun TaskBody(session: TalkSession, task: ScriptTask) {
    val scheme = MaterialTheme.colorScheme
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = { (task.number - 1).toFloat() / task.total },
                modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape),
            )
            Text(
                "${task.number} / ${task.total}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            SmallChip(
                if (session.showExample) "隠す" else if (task.open) "回答例" else "お手本",
                onClick = { session.showExample = !session.showExample },
            )
        }
        Text(task.taskJa, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
        if (task.contextJa.isNotBlank()) {
            Text("ℹ️ ${task.contextJa}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
        }
        if (session.showExample) {
            Spacer(Modifier.height(6.dp))
            Surface(shape = RoundedCornerShape(10.dp), color = scheme.surfaceContainerLowest, contentColor = scheme.onSurface) {
                Row(Modifier.fillMaxWidth().padding(start = 12.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(task.example.en, fontWeight = FontWeight.SemiBold)
                        Text(task.example.ja, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { session.play(task.example.en) }) {
                        Icon(AppIcons.VolumeUp, contentDescription = "お手本を聞く")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiBubble(m: AiMessage, session: TalkSession, showText: Boolean, isSaved: (String) -> Boolean) {
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(scheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                session.scenario.aiName.removePrefix("Dr. ").removePrefix("Ms. ").removePrefix("Officer ").take(1),
                color = scheme.onPrimaryContainer,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            modifier = Modifier.weight(1f, fill = false),
            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 18.dp),
            color = scheme.surfaceContainerLowest,
            tonalElevation = 1.dp,
            shadowElevation = 1.dp,
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                val error = m.error
                when {
                    error != null -> Text(error, color = scheme.error, style = MaterialTheme.typography.bodyMedium)
                    m.text.isEmpty() -> Text("・・・", color = scheme.onSurfaceVariant, style = MaterialTheme.typography.titleMedium)
                    !showText && !m.revealed && !m.streaming -> Text(
                        "👂 タップで表示",
                        color = scheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { m.revealed = true }.padding(vertical = 4.dp),
                    )
                    else -> Text(m.text, style = MaterialTheme.typography.bodyLarge)
                }
                if (m.showTranslation) {
                    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = scheme.outlineVariant)
                    when (val t = m.translation) {
                        is LoadState.Ready -> {
                            Text(t.value.ja, style = MaterialTheme.typography.bodyMedium)
                            t.value.words.forEach { w ->
                                Text("・${w.en}：${w.ja}", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                            }
                        }
                        is LoadState.Failed -> Text(t.message, color = scheme.error, style = MaterialTheme.typography.bodySmall)
                        else -> Text("翻訳中…", color = scheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    SmallChip("もう一度", onClick = { session.retry(m) }, icon = AppIcons.Replay)
                } else if (!m.streaming) {
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconAction("再生", AppIcons.VolumeUp) { session.play(m.text) }
                        IconAction("ゆっくり再生", emoji = "🐢") { session.play(m.text, 0.7f) }
                        IconAction("訳", AppIcons.Translate, active = m.showTranslation) { session.toggleTranslation(m) }
                        val saved = isSaved(m.text)
                        IconAction(if (saved) "保存済み" else "保存", Icons.Filled.Star, tint = if (saved) scheme.tertiary else null) {
                            val tr = m.translation
                            session.savePhrase(m.text, if (tr is LoadState.Ready) tr.value.ja else "")
                        }
                    }
                }
            }
        }
    }
}

/** 文字なしの小さな丸ボタン (説明は読み上げ・長押し用の contentDescription に入れる) */
@Composable
private fun IconAction(
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    emoji: String? = null,
    tint: androidx.compose.ui.graphics.Color? = null,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (active) scheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent,
        modifier = Modifier.size(36.dp).semantics { contentDescription = description },
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = tint ?: scheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            } else if (emoji != null) {
                Text(emoji, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun LearnerBubble(m: LearnerMessage, session: TalkSession, isSaved: (String) -> Boolean) {
    val scheme = MaterialTheme.colorScheme
    val grades = LocalGrades.current
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
        Surface(
            modifier = Modifier.widthIn(max = 320.dp),
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 6.dp, bottomEnd = 18.dp, bottomStart = 18.dp),
            color = scheme.primary,
            contentColor = scheme.onPrimary,
        ) {
            Text(m.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
        }
        Spacer(Modifier.height(6.dp))
        when (val f = m.feedback) {
            LoadState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(6.dp))
                Text("チェック中…", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            }
            is LoadState.Failed -> Text("チェックできませんでした", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            is LoadState.Ready -> {
                val (fg, bg) = grades.of(f.value.rating)
                val scripted = session.mode == EngineMode.SCRIPT
                val (label, icon) = when (f.value.rating) {
                    Rating.GREAT -> "Great!" to Icons.Filled.Check
                    Rating.GOOD -> (if (scripted) "OK!" else "もっと自然に") to AppIcons.Lightbulb
                    Rating.FIX -> (if (scripted) "もう一度" else "修正あり") to Icons.Filled.Edit
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (m.xp > 0) {
                        Text(
                            "+${m.xp} XP",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 6.dp),
                        )
                    }
                    Surface(onClick = { m.expanded = !m.expanded }, shape = CircleShape, color = bg, contentColor = fg) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (m.expanded) {
                    Spacer(Modifier.height(8.dp))
                    FeedbackCard(f.value, m.text, session, isSaved)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FeedbackCard(fb: Feedback, said: String, session: TalkSession, isSaved: (String) -> Boolean) {
    val scheme = MaterialTheme.colorScheme
    val grades = LocalGrades.current
    Surface(
        modifier = Modifier.fillMaxWidth(0.94f),
        shape = RoundedCornerShape(14.dp),
        color = scheme.surfaceContainerLowest,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (session.mode == EngineMode.SCRIPT) {
                ScriptAnswer(fb, session, isSaved)
            } else if (fb.rating == Rating.FIX && !SpeechScorer.sameSentence(fb.corrected, said)) {
                Column {
                    Label("✏️ 正しくは")
                    Text(fb.corrected, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                }
            }
            if (session.mode != EngineMode.SCRIPT && (fb.rating != Rating.GREAT || !SpeechScorer.sameSentence(fb.natural, said))) {
                Column {
                    Label("✨ 自然な言い方")
                    Text(fb.natural, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconAction("再生", AppIcons.VolumeUp) { session.play(fb.natural) }
                        val saved = isSaved(fb.natural)
                        IconAction(if (saved) "保存済み" else "保存", Icons.Filled.Star, tint = if (saved) scheme.tertiary else null) {
                            session.savePhrase(fb.natural)
                        }
                        Spacer(Modifier.width(6.dp))
                        PracticeButton(fb.natural)
                    }
                }
            }
            Surface(shape = RoundedCornerShape(10.dp), color = scheme.surfaceContainerHigh) {
                Text(fb.explanationJa, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(10.dp))
            }
            fb.mistakes.forEach { mistake ->
                Column {
                    Row {
                        Text(mistake.wrong, color = grades.fix, textDecoration = TextDecoration.LineThrough)
                        Text("  →  ")
                        Text(mistake.right, color = grades.great, fontWeight = FontWeight.Bold)
                    }
                    Text(mistake.noteJa, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** 台本モード: お手本 (言えた単語は緑、言えなかった単語は赤の下線) と練習ボタン */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScriptAnswer(fb: Feedback, session: TalkSession, isSaved: (String) -> Boolean) {
    val scheme = MaterialTheme.colorScheme
    val grades = LocalGrades.current
    val match: SpeechScore? = fb.matchScore
    Column {
        Label(if (match == null) "💬 回答例" else "📖 お手本")
        if (match == null) {
            Text(fb.natural, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                match.words.forEach { w ->
                    Text(
                        w.text,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (w.ok) grades.great else grades.fix,
                        textDecoration = if (w.ok) null else TextDecoration.Underline,
                    )
                }
            }
        }
        ScriptEngine.lookupJa(fb.natural)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant) }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconAction("再生", AppIcons.VolumeUp) { session.play(fb.natural) }
            val saved = isSaved(fb.natural)
            IconAction(if (saved) "保存済み" else "保存", Icons.Filled.Star, tint = if (saved) scheme.tertiary else null) {
                session.savePhrase(fb.natural)
            }
            Spacer(Modifier.width(6.dp))
            PracticeButton(fb.natural)
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
}

/** 会話の最初に出す場面の説明 (状況・自分の役・相手) */
@Composable
private fun SceneCard(scenario: Scenario) {
    val scheme = MaterialTheme.colorScheme
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = scheme.surfaceContainerHigh) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text("🎬 場面", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(
                scenario.backgroundJa.ifBlank { scenario.descriptionJa },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                "あなた：${scenario.userRoleJa}",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                "相手：${scenario.aiName}" + if (scenario.aiRoleJa.isBlank()) "" else "（${scenario.aiRoleJa}）",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CelebrationCard(item: Celebration, onFinish: () -> Unit) {
    val grades = LocalGrades.current
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = grades.greatContainer, contentColor = grades.great) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (item.scriptDone) {
                Text("🎉 台本クリア！", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            } else {
                Text("🎉 すべてのミッションを達成しました！", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Button(onClick = onFinish) { Text("会話を終えて振り返る") }
            }
        }
    }
}

@Composable
private fun Footer(session: TalkSession, onMic: () -> Unit, onFinish: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        color = scheme.surfaceContainerLowest,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp)) {
            // 台本モード: 言うことはマイクのすぐ上に (相手の最新のセリフはそのすぐ上の会話の一番下)
            session.task?.takeUnless { it.finished }?.let { TaskPrompt(session, it) }
            if (session.scriptDone && !session.busy) {
                // 台本を終えたら、マイクの代わりに振り返りへのボタン
                Button(onClick = onFinish, modifier = Modifier.fillMaxWidth().height(52.dp).padding(vertical = 2.dp)) {
                    Text("振り返りを見る", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                FooterControls(session, onMic)
            }
        }
    }
}

@Composable
private fun FooterControls(session: TalkSession, onMic: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val closeInput: () -> Unit = {
        focusManager.clearFocus()
        keyboard?.hide()
        session.closeKeyboard()
    }
    // 送ったらキーボードはしまう (入力欄は残すので、続けて打つときはタップ)
    val send: () -> Unit = {
        session.submitDraft()
        focusManager.clearFocus()
        keyboard?.hide()
    }
    // 「入力」で開いたときは、すぐに打てるようにフォーカスしてキーボードを出す
    LaunchedEffect(session.keyboardFocusRequest) {
        if (session.keyboardFocusRequest > 0 && session.showKeyboard) runCatching { focusRequester.requestFocus() }
    }
    Column {
        if (session.listening) {
            Surface(shape = RoundedCornerShape(12.dp), color = scheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                Text(
                    session.liveText.ifBlank { "🎤 …" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (session.liveText.isBlank()) scheme.onSurfaceVariant else scheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
        }
        session.cue?.let { cue -> CueBox(cue) { session.dismissCue() } }
        if (session.status.isNotEmpty()) {
            Text(
                session.status,
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            )
        }
        if (session.showKeyboard) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                IconButton(onClick = closeInput) {
                    Icon(Icons.Filled.Close, contentDescription = "入力を閉じる", tint = scheme.onSurfaceVariant)
                }
                OutlinedTextField(
                    value = session.draft,
                    onValueChange = { session.draft = it.take(500) },
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    placeholder = { Text("英語で入力") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send() }),
                )
                IconButton(onClick = { send() }, enabled = !session.busy) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "送信", tint = scheme.primary)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            RoundAction(AppIcons.Lightbulb, "ヒント", enabled = !session.busy) { session.openHint() }
            MicButton(session.listening, session.micLevel, enabled = !session.busy || session.listening, onClick = onMic)
            RoundAction(AppIcons.Keyboard, if (session.showKeyboard) "閉じる" else "入力", enabled = true) {
                if (session.showKeyboard) closeInput() else session.openKeyboard()
            }
        }
    }
}

@Composable
private fun CueBox(cue: HintSuggestion, onClose: () -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = LocalGrades.current.goodContainer, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(start = 12.dp, top = 8.dp, bottom = 8.dp)) {
                Text(cue.en, fontWeight = FontWeight.Bold)
                Text(cue.ja, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "閉じる") }
        }
    }
}

@Composable
private fun RoundAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
        Surface(onClick = onClick, enabled = enabled, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp))
            }
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MicButton(listening: Boolean, level: Float, enabled: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val grades = LocalGrades.current
    val scale by animateFloatAsState(if (listening) 1f + level * 0.25f else 1f, label = "mic")
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(96.dp)) {
        if (listening) {
            Box(
                Modifier
                    .size(78.dp)
                    .graphicsLayer {
                        scaleX = scale + 0.08f
                        scaleY = scale + 0.08f
                    }
                    .clip(CircleShape)
                    .background(grades.fix.copy(alpha = 0.25f)),
            )
        }
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = CircleShape,
            color = if (listening) grades.fix else scheme.primary,
            contentColor = scheme.onPrimary,
            shadowElevation = 6.dp,
            modifier = Modifier.semantics { contentDescription = if (listening) "話し終わる" else "話す" },
        ) {
            Box(Modifier.size(76.dp), contentAlignment = Alignment.Center) {
                Icon(if (listening) AppIcons.Stop else AppIcons.Mic, contentDescription = null, modifier = Modifier.size(34.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HintSheet(hint: HintState, session: TalkSession, mic: MicPermission) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 16.dp),
    ) {
        val scripted = session.mode == EngineMode.SCRIPT
        Text(
            if (hint.want.isBlank() || scripted) "ヒント：何て言えばいい？" else "英語でどう言う？",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))
        when {
            hint.loading -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 24.dp)) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text(if (hint.want.isBlank()) "ヒントを考えています…" else "英語の言い方を考えています…", color = scheme.onSurfaceVariant)
            }
            hint.error != null -> Column {
                Text(hint.error, color = scheme.error)
                TextButton(onClick = { session.loadHint(hint.want) }) { Text("もう一度") }
            }
            else -> hint.suggestions.forEach { s ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = scheme.surfaceContainerLowest,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Surface(shape = RoundedCornerShape(6.dp), color = scheme.primaryContainer) {
                            Text(s.labelJa, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(s.en, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(s.ja, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SmallChip("聞く", onClick = { session.play(s.en) }, icon = AppIcons.VolumeUp)
                            SmallChip("自分で言う", onClick = { mic.request { session.sayHintYourself(s) } }, icon = AppIcons.Mic, tint = scheme.primary)
                            SmallChip("そのまま送る", onClick = { session.sendHint(s) }, icon = Icons.AutoMirrored.Filled.Send)
                        }
                    }
                }
            }
        }
        if (!scripted) {
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            WantInput(hint, session)
        }
    }
}

@Composable
private fun WantInput(hint: HintState, session: TalkSession) {
    val scheme = MaterialTheme.colorScheme
    var want by remember(hint.want) { mutableStateOf(hint.want) }
    Column {
        Text("💬 言いたいことを日本語で入力すると、英語の言い方を提案します", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = want,
                onValueChange = { want = it.take(300) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("例：砂糖なしでお願いします") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { session.loadHint(want.trim()) }),
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { session.loadHint(want.trim()) }) { Text("英語にする") }
        }
    }
}
