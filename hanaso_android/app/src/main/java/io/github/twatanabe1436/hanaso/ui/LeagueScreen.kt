package io.github.twatanabe1436.hanaso.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.twatanabe1436.hanaso.core.LeagueEntry
import io.github.twatanabe1436.hanaso.core.LeagueException
import io.github.twatanabe1436.hanaso.core.Xp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private const val NICKNAME_MAX = 16

/** 週ごとの XP リーグ: 今週の自分の XP と、オンラインの順位 */
@Composable
fun LeagueScreen() {
    val app = LocalApp.current
    val scope = rememberCoroutineScope()
    val settings by app.store.settings.collectAsStateWithLifecycle()
    val saved by app.store.xp.collectAsStateWithLifecycle()
    val xp = remember(saved) { app.store.currentXp() }
    val league = app.league()
    val joined = league != null && settings.leagueJoined && settings.nickname.isNotBlank()

    var board by remember { mutableStateOf<List<LeagueEntry>?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var editName by remember { mutableStateOf(false) }

    fun refresh() {
        loading = true
        error = null
        scope.launch {
            try {
                board = app.refreshLeague()
            } catch (e: CancellationException) {
                throw e
            } catch (e: LeagueException) {
                error = e.messageJa
            } catch (e: Exception) {
                error = "リーグを読み込めませんでした。もう一度お試しください。"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(joined) { if (joined) refresh() }

    val daysLeft = remember(xp.week) {
        val ms = Xp.weekEndsAt() - System.currentTimeMillis()
        ((ms + 86_399_999) / 86_400_000).coerceAtLeast(1)
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("リーグ", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("今週 ${xp.weekXp} XP", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text(
                    "累計 ${xp.totalXp} XP ・ あと $daysLeft 日でリセット",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        when {
            league == null -> Note("🌐 オンラインのリーグは準備中です。XP はこの端末に記録されています。")
            !joined -> JoinCard(initial = settings.nickname) { name ->
                app.store.updateSettings { it.copy(nickname = name, leagueJoined = true) }
            }
            else -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("今週の順位", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = { editName = true }) { Text("名前") }
                    TextButton(onClick = { refresh() }, enabled = !loading) { Text("更新") }
                }
                when {
                    loading && board == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("読み込み中…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    error != null -> Column {
                        Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { refresh() }) { Text("もう一度") }
                    }
                    else -> Board(board.orEmpty(), me = league?.uid, myName = settings.nickname, myXp = xp.weekXp)
                }
                TextButton(onClick = { app.store.updateSettings { it.copy(leagueJoined = false) } }) {
                    Text("リーグをやめる", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Text(
            "XP：Great 10 ／ OK 7 ／ もう一度 3 ／ ヒントのまま 2 ・ 台本クリア・ミッション達成 +20 ・ 言ってみる 1〜5",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (editName) {
        NameDialog(initial = settings.nickname, onDismiss = { editName = false }) { name ->
            editName = false
            app.store.updateSettings { it.copy(nickname = name) }
            refresh()
        }
    }
}

@Composable
private fun Note(text: String) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(14.dp))
    }
}

/** ニックネームを決めて参加する */
@Composable
private fun JoinCard(initial: String, onJoin: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial) }
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest, tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("🏆 リーグに参加する", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "ほかのユーザーと、今週の XP で順位を競います。公開されるのはニックネーム・今週の XP・レベルだけです。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = cleanName(it) },
                label = { Text("ニックネーム") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Button(onClick = { onJoin(name.trim()) }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Text("参加する")
            }
        }
    }
}

@Composable
private fun NameDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ニックネーム") },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = cleanName(it) }, singleLine = true)
        },
        confirmButton = { TextButton(onClick = { onSave(name.trim()) }, enabled = name.isNotBlank()) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("やめる") } },
    )
}

private fun cleanName(raw: String) = raw.replace(Regex("""[\r\n\t]"""), "").take(NICKNAME_MAX)

/** 順位表。自分の行は色を変える。自分が上位に入っていなければ最後に自分の行を出す */
@Composable
private fun Board(entries: List<LeagueEntry>, me: String?, myName: String, myXp: Int) {
    val scheme = MaterialTheme.colorScheme
    if (entries.isEmpty()) {
        Note("まだ誰もいません。練習して最初の XP を入れよう！")
        return
    }
    Column(Modifier.testTag("league_board"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        entries.forEachIndexed { i, e -> BoardRow(rank = i + 1, name = e.name, level = e.level, xp = e.xp, isMe = e.uid == me) }
        if (entries.none { it.uid == me }) {
            Text("…", color = scheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            BoardRow(rank = null, name = myName, level = null, xp = myXp, isMe = true)
        }
    }
}

@Composable
private fun BoardRow(rank: Int?, name: String, level: io.github.twatanabe1436.hanaso.core.Level?, xp: Int, isMe: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isMe) scheme.primaryContainer else scheme.surfaceContainerLowest,
        tonalElevation = if (isMe) 0.dp else 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                when (rank) {
                    1 -> "🥇"
                    2 -> "🥈"
                    3 -> "🥉"
                    null -> "–"
                    else -> "$rank"
                },
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 32.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (isMe) "$name（あなた）" else name,
                fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            level?.let {
                Spacer(Modifier.width(6.dp))
                LevelBadge(it)
            }
            Spacer(Modifier.weight(1f))
            Text("$xp XP", fontWeight = FontWeight.ExtraBold, color = scheme.primary)
        }
    }
}
