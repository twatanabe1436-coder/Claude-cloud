@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package io.github.twatanabe1436.sodateru.ui.bread

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.twatanabe1436.sodateru.AppContainer
import io.github.twatanabe1436.sodateru.core.BakeMetric
import io.github.twatanabe1436.sodateru.core.BakeMetrics
import io.github.twatanabe1436.sodateru.core.BakeSamples
import io.github.twatanabe1436.sodateru.core.MetricPoint
import io.github.twatanabe1436.sodateru.core.model.Category
import io.github.twatanabe1436.sodateru.ui.Navigator
import io.github.twatanabe1436.sodateru.ui.Route
import io.github.twatanabe1436.sodateru.ui.components.BackButton
import io.github.twatanabe1436.sodateru.ui.components.Fmt
import io.github.twatanabe1436.sodateru.ui.components.RatingLegend
import io.github.twatanabe1436.sodateru.ui.components.ScatterChart
import io.github.twatanabe1436.sodateru.ui.components.SectionCard
import io.github.twatanabe1436.sodateru.ui.components.SmallStars

private val PRESETS = listOf(
    BakeMetric.HUMIDITY to BakeMetric.HYDRATION,
    BakeMetric.ROOM_TEMP to BakeMetric.FIRST_PROOF,
    BakeMetric.DOUGH_TEMP to BakeMetric.FIRST_PROOF,
    BakeMetric.HYDRATION to BakeMetric.RATING,
    BakeMetric.ROOM_TEMP to BakeMetric.WATER_TEMP,
)

/** 研究ノート: 焼成ログの 2 つの項目の関係を散布図で見る。 */
@Composable
fun AnalysisScreen(container: AppContainer, navigator: Navigator, initialRecipeId: String?) {
    val data by container.repository.data.collectAsStateWithLifecycle()
    val breads = remember(data.recipes) { data.recipes.filter { it.category == Category.BREAD } }
    var recipeId by rememberSaveable { mutableStateOf(initialRecipeId) }
    var x by rememberSaveable { mutableStateOf(BakeMetric.HUMIDITY) }
    var y by rememberSaveable { mutableStateOf(BakeMetric.HYDRATION) }
    var selected by remember { mutableStateOf<MetricPoint?>(null) }

    val samples = remember(data, recipeId) {
        BakeSamples.from(data.recipes, data.logs.filter { recipeId == null || it.recipeId == recipeId })
    }
    val comparison = remember(samples, x, y) { BakeMetrics.compare(samples, x, y) }
    val titles = remember(data.recipes) { data.recipes.associate { it.id to it.title } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("研究ノート") },
                navigationIcon = { BackButton { navigator.pop() } },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "filters") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Picker(
                        label = "レシピ",
                        value = recipeId?.let { titles[it] } ?: "すべてのパン",
                        options = listOf<Pair<String?, String>>(null to "すべてのパン") + breads.map { it.id to it.title },
                        onSelect = { recipeId = it; selected = null },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Picker(
                            label = "横軸",
                            value = x.label,
                            options = BakeMetric.entries.map { it to it.label },
                            onSelect = { x = it; selected = null },
                            modifier = Modifier.weight(1f),
                        )
                        Picker(
                            label = "縦軸",
                            value = y.label,
                            options = BakeMetric.entries.map { it to it.label },
                            onSelect = { y = it; selected = null },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PRESETS.forEach { (px, py) ->
                            AssistChip(
                                onClick = { x = px; y = py; selected = null },
                                label = { Text("${px.label}×${py.label}") },
                            )
                        }
                    }
                }
            }
            item(key = "chart") {
                SectionCard(title = "${x.label} と ${y.label}") {
                    if (comparison.points.size < 2) {
                        Text(
                            "両方の値が入った焼成ログが2回以上になるとグラフが表示されます（今は${comparison.points.size}回）。",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        ScatterChart(
                            points = comparison.points,
                            fit = comparison.fit,
                            xUnit = "${x.label}（${x.unit}）",
                            yUnit = y.unit,
                            selected = selected,
                            onSelect = { selected = it },
                        )
                        RatingLegend()
                        Text(
                            comparison.describe(x, y) ?: "記録が3回以上になると傾向を計算します",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "※ 記録の数が少ないうちは偶然の影響が大きいので、参考程度に見てください",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            selected?.let { p ->
                item(key = "selected") {
                    SectionCard(title = "選んだ回", containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${Fmt.date(p.sample.log.date)}  ${titles[p.sample.log.recipeId].orEmpty()}", style = MaterialTheme.typography.labelLarge)
                                Text("${x.label} ${Fmt.num(p.x)}${x.unit} ・ ${y.label} ${Fmt.num(p.y)}${y.unit}", style = MaterialTheme.typography.bodyMedium)
                                if (p.sample.log.notes.isNotBlank()) Text(p.sample.log.notes, style = MaterialTheme.typography.bodySmall)
                            }
                            SmallStars(p.sample.rating)
                            TextButton(onClick = { navigator.push(Route.EditLog(p.sample.log.recipeId, p.sample.log.id)) }) { Text("開く") }
                        }
                    }
                }
            }
            if (comparison.points.isNotEmpty()) {
                item(key = "table-head") {
                    Column {
                        Text("データ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(Modifier.padding(vertical = 6.dp)) {
                            Text("日付", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(96.dp))
                            Text(x.label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                            Text(y.label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                            Text("評価", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(64.dp))
                        }
                        HorizontalDivider()
                    }
                }
                items(comparison.points.sortedByDescending { it.sample.log.date }, key = { it.sample.log.id }) { p ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { selected = p }
                            .padding(vertical = 6.dp),
                    ) {
                        Text(Fmt.date(p.sample.log.date).substringBefore("（"), style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(96.dp))
                        Text("${Fmt.num(p.x)}${x.unit}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text("${Fmt.num(p.y)}${y.unit}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Box(Modifier.width(64.dp)) { SmallStars(p.sample.rating) }
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> Picker(
    label: String,
    value: String,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, maxLines = 1)
            }
            Icon(Icons.Filled.ArrowDropDown, contentDescription = "${label}を選ぶ")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (v, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { onSelect(v); open = false })
            }
        }
    }
}
