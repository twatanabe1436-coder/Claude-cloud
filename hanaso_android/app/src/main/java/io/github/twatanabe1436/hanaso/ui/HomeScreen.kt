package io.github.twatanabe1436.hanaso.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.twatanabe1436.hanaso.core.Catalog
import io.github.twatanabe1436.hanaso.core.EngineMode
import io.github.twatanabe1436.hanaso.core.Level
import io.github.twatanabe1436.hanaso.core.Scenario
import io.github.twatanabe1436.hanaso.core.Stats
import io.github.twatanabe1436.hanaso.data.SessionRecord
import io.github.twatanabe1436.hanaso.data.Totals
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun HomeScreen(
    level: Level,
    mode: EngineMode,
    totals: Totals,
    recent: List<SessionRecord>,
    onOpen: (Scenario) -> Unit,
    onOpenSettings: () -> Unit,
    onSeeAll: () -> Unit,
    weekXp: Int,
    onOpenLeague: () -> Unit,
) {
    val pick = Stats.todaysPick(level)
    // 自分のレベルに近い順 (同じ距離なら、やさしいほうが先)
    val roleplays = Catalog.scenarios.sortedWith(compareBy({ abs((it.level ?: level).ordinal - level.ordinal) }, { it.level?.ordinal ?: 0 }))

    LazyColumn(
        modifier = Modifier.testTag("home_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Hanaso", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    Text("話して、英語が口から出るように", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                SmallChip(text = "レベル: ${level.displayJa}", onClick = onOpenSettings)
            }
        }
        item { ModeCard(mode, onOpenSettings) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("🔥 ${totals.streak}", "日連続", Modifier.weight(1f))
                StatTile("💬 ${totals.sessions}", "回の会話", Modifier.weight(1f))
                StatTile("🗣️ ${totals.turns}", "回の発話", Modifier.weight(1f))
                StatTile("⭐ ${totals.phrases}", "フレーズ", Modifier.weight(1f))
            }
        }
        item { LeagueCard(weekXp, onOpenLeague) }
        item { TodayCard(pick) { onOpen(pick) } }
        item {
            Column {
                SectionTitle("フリートーク", if (mode == EngineMode.AI) "AI の Alex と自由に話そう" else "Alex と自分のことを話そう")
                FreeTalkGrid(onOpen)
            }
        }
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("ロールプレイ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    SmallChip(text = "すべて見る", onClick = onSeeAll)
                }
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(roleplays.take(8), key = { it.id }) { s -> MiniCard(s) { onOpen(s) } }
                }
            }
        }
        if (recent.isNotEmpty()) {
            item { SectionTitle("最近の会話", null) }
            items(recent.take(5), key = { it.id }) { r -> RecentRow(r) { Catalog.find(r.scenarioId)?.let(onOpen) } }
        }
    }
}

/** いまの会話モードの案内 */
@Composable
private fun ModeCard(mode: EngineMode, onOpenSettings: () -> Unit) {
    val (title, body) = when (mode) {
        EngineMode.SCRIPT -> "📖 台本モード（AI なし・無料）" to
            "日本語のお題を英語で言うと、相手が台本どおりに返事をします。お手本との一致度で判定し、何度でも無料で練習できます。" +
            "Claude の API キーを設定すると、AI と自由に会話することもできます。"
        EngineMode.AI -> "🤖 AI 会話モード" to "Claude と自由に会話します。台本モードには設定から切り替えられます。"
        EngineMode.DEMO -> "🧪 デモモード" to "決まった返事だけを返す、動作確認用のモードです。"
    }
    Card(colors = CardDefaults.cardColors(containerColor = if (mode == EngineMode.AI) MaterialTheme.colorScheme.surfaceContainerLow else LocalGrades.current.goodContainer)) {
        Column(Modifier.padding(14.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
            if (mode == EngineMode.SCRIPT) {
                OutlinedButton(onClick = onOpenSettings) { Text("会話モードの設定") }
            }
        }
    }
}

/** 今週の XP (タップでリーグへ) */
@Composable
private fun LeagueCard(weekXp: Int, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 1.dp,
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(AppIcons.Trophy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text("今週 $weekXp XP", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("リーグ ›", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String?) {
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(bottom = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (subtitle != null) {
            Spacer(Modifier.width(8.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest, tonalElevation = 1.dp) {
        Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleSmall)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TodayCard(s: Scenario, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF6D5DFC), Color(0xFF4A38E0))))
            .padding(18.dp),
    ) {
        Text("今日のおすすめ", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Emoji(s.emoji, size = 34)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(s.titleJa, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(s.titleEn, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
            }
        }
        Text(s.descriptionJa, color = Color.White.copy(alpha = 0.92f), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 12.dp))
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = primary),
        ) {
            Icon(AppIcons.Mic, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("この会話をはじめる", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MiniCard(s: Scenario, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.width(136.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(14.dp)) {
            Emoji(s.emoji, size = 30)
            Spacer(Modifier.height(6.dp))
            Text(s.titleJa, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, minLines = 2, maxLines = 2)
            Spacer(Modifier.height(6.dp))
            s.level?.let { LevelBadge(it) }
        }
    }
}

@Composable
private fun RecentRow(r: SessionRecord, onClick: () -> Unit) {
    val date = SimpleDateFormat("M/d", Locale.JAPAN).format(Date(r.endedAt))
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 1.dp,
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Emoji(r.emoji, size = 22)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(r.titleJa, style = MaterialTheme.typography.bodyLarge)
                Text("$date・${r.learnerTurns}回発話", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (r.score != null) {
                Text("${r.score}点", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.End)
            }
        }
    }
}
