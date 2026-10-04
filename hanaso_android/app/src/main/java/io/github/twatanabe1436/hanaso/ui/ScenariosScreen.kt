package io.github.twatanabe1436.hanaso.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.twatanabe1436.hanaso.core.Catalog
import io.github.twatanabe1436.hanaso.core.Category
import io.github.twatanabe1436.hanaso.core.EngineMode
import io.github.twatanabe1436.hanaso.core.Level
import io.github.twatanabe1436.hanaso.core.Scenario
import io.github.twatanabe1436.hanaso.core.Scripts

/** CEFR レベル (A1〜C2) を選ぶ。選んだレベルの目安も表示する */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelSelector(selected: Level, onSelect: (Level) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            Level.entries.forEachIndexed { i, level ->
                SegmentedButton(
                    selected = level == selected,
                    onClick = { onSelect(level) },
                    shape = SegmentedButtonDefaults.itemShape(index = i, count = Level.entries.size),
                    icon = {},
                ) { Text(level.name, maxLines = 1) }
            }
        }
        Text(
            "${selected.displayJa}：${selected.descriptionJa}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** フリートークのトピック (3列) */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FreeTalkGrid(onOpen: (Scenario) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 3,
    ) {
        Catalog.freeTalks.forEach { t ->
            Surface(
                onClick = { onOpen(t) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                tonalElevation = 1.dp,
            ) {
                Column(Modifier.padding(vertical = 14.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Emoji(t.emoji, size = 24)
                    Spacer(Modifier.height(6.dp))
                    Text(t.titleJa, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun ScenarioCard(s: Scenario, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 1.dp,
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Emoji(s.emoji, size = 30)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.titleJa, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    s.level?.let { LevelBadge(it) }
                }
                Text(s.descriptionJa, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "🎯 ミッション ${s.missions.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScenariosScreen(onOpen: (Scenario) -> Unit) {
    var levelFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var categoryFilter by rememberSaveable { mutableStateOf<String?>(null) }
    val items = Catalog.scenarios.filter {
        (levelFilter == null || it.level?.name == levelFilter) && (categoryFilter == null || it.category.name == categoryFilter)
    }

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("会話する", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        item {
            Column {
                Text("フリートーク", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
                FreeTalkGrid(onOpen)
            }
        }
        item {
            Text("ロールプレイ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
        }
        item {
            Column {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = levelFilter == null, onClick = { levelFilter = null }, label = { Text("すべて") })
                Level.entries.forEach { lv ->
                    FilterChip(selected = levelFilter == lv.name, onClick = { levelFilter = lv.name }, label = { Text(lv.name) })
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = categoryFilter == null, onClick = { categoryFilter = null }, label = { Text("全カテゴリ") })
                Category.entries.filter { it != Category.FREE }.forEach { c ->
                    FilterChip(selected = categoryFilter == c.name, onClick = { categoryFilter = c.name }, label = { Text(c.ja) })
                }
            }
            }
        }
        if (items.isEmpty()) {
            item { Text("該当するシナリオがありません", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(items, key = { it.id }) { s -> ScenarioCard(s) { onOpen(s) } }
    }
}

/** 会話を始める前の説明シート (ミッション・使えるフレーズ・レベル) */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenarioIntroSheet(
    scenario: Scenario,
    initialLevel: Level,
    mode: EngineMode,
    onPlay: (String) -> Unit,
    onDismiss: () -> Unit,
    onStart: (Level) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var level by remember(scenario.id) { mutableStateOf(initialLevel) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Emoji(scenario.emoji, size = 44)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(scenario.titleJa, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(scenario.titleEn, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        scenario.level?.let {
                            Spacer(Modifier.width(8.dp))
                            LevelBadge(it)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(scenario.descriptionJa, style = MaterialTheme.typography.bodyMedium)
            if (!scenario.isFreeTalk) {
                Spacer(Modifier.height(10.dp))
                Row {
                    RoleTag("あなた", scenario.userRoleJa, highlight = false)
                    Spacer(Modifier.width(16.dp))
                    RoleTag("相手", scenario.aiName, highlight = true)
                }
            }
            if (scenario.missions.isNotEmpty()) {
                SheetHeading("🎯 ミッション")
                scenario.missions.forEach { m ->
                    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(m.ja, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (scenario.keyPhrases.isNotEmpty()) {
                SheetHeading("🔑 使えるフレーズ")
                scenario.keyPhrases.forEach { p -> PhraseRow(p.en, p.ja, onPlay = { onPlay(p.en) }) }
            }
            val scriptRolePlay = mode == EngineMode.SCRIPT && !scenario.isFreeTalk
            if (mode == EngineMode.SCRIPT) {
                val steps = Scripts.forScenario(scenario).steps
                SheetHeading("📖 台本のお題（全 ${steps.size} 問）")
                steps.forEachIndexed { i, step ->
                    Row(Modifier.padding(vertical = 3.dp)) {
                        Text("${i + 1}.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                        Text(step.taskJa, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Text(
                    "台本モード：お題を英語で言うと、相手が台本どおりに返事をします（AI なし・無料）。" +
                        (scenario.level?.let { "台本のレベルは ${it.displayJa} です。" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
                if (scenario.isFreeTalk) {
                    SheetHeading("回答例のレベル")
                    LevelSelector(level, { level = it })
                }
            } else {
                SheetHeading(scenario.level?.let { "レベル（このシナリオの目安は ${it.name}）" } ?: "レベル")
                LevelSelector(level, { level = it })
                Text(
                    "AI が単語や文の長さをこのレベルに合わせて話します。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
            // 台本モードのロールプレイは台本のレベルで始める (自分のレベル設定は変えない)
            Button(onClick = { onStart(if (scriptRolePlay) scenario.level ?: level else level) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(AppIcons.Mic, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("会話をはじめる", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "マイクの使用を許可してください。静かな場所がおすすめです。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun SheetHeading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 18.dp, bottom = 6.dp),
    )
}

@Composable
private fun RoleTag(label: String, value: String, highlight: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (highlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
        }
        Spacer(Modifier.width(6.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
