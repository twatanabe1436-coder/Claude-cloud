@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package io.github.twatanabe1436.sodateru.ui.bread

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.ScatterPlot
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
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
import io.github.twatanabe1436.sodateru.core.Amounts
import io.github.twatanabe1436.sodateru.core.BakeAdvice
import io.github.twatanabe1436.sodateru.core.BakeAdvisor
import io.github.twatanabe1436.sodateru.core.BakeSamples
import io.github.twatanabe1436.sodateru.core.BakersMath
import io.github.twatanabe1436.sodateru.core.Confidence
import io.github.twatanabe1436.sodateru.core.DoughTemperature
import io.github.twatanabe1436.sodateru.core.TodayConditions
import io.github.twatanabe1436.sodateru.core.model.Category
import io.github.twatanabe1436.sodateru.core.model.IngredientRole
import io.github.twatanabe1436.sodateru.core.model.MixingMethod
import io.github.twatanabe1436.sodateru.core.model.Recipe
import io.github.twatanabe1436.sodateru.ui.BakePrefill
import io.github.twatanabe1436.sodateru.ui.Navigator
import io.github.twatanabe1436.sodateru.ui.Route
import io.github.twatanabe1436.sodateru.ui.components.ChoiceChips
import io.github.twatanabe1436.sodateru.ui.components.EmptyState
import io.github.twatanabe1436.sodateru.ui.components.Fmt
import io.github.twatanabe1436.sodateru.ui.components.InfoPill
import io.github.twatanabe1436.sodateru.ui.components.NumberField
import io.github.twatanabe1436.sodateru.ui.components.SectionCard
import io.github.twatanabe1436.sodateru.ui.components.SmallStars
import io.github.twatanabe1436.sodateru.ui.logs.LogCard
import kotlin.math.roundToInt

/**
 * パン研究: 今日の室温・湿度から仕込みを提案し、こね方の学習結果・研究ノート・計算ツールへつなぐ。
 */
@Composable
fun BreadLabScreen(container: AppContainer, navigator: Navigator, bottomBar: @Composable () -> Unit) {
    val data by container.repository.data.collectAsStateWithLifecycle()
    val settings by container.settings.settings.collectAsStateWithLifecycle()
    val breads = remember(data.recipes) { data.recipes.filter { it.category == Category.BREAD } }
    val lastBaked = remember(data.logs) {
        data.logs.filter { it.bake != null }.groupBy { it.recipeId }.mapValues { (_, l) -> l.maxOf { it.date } }
    }
    val recipe = breads.firstOrNull { it.id == navigator.breadRecipeId }
        ?: breads.maxByOrNull { lastBaked[it.id] ?: it.updatedAt }

    var room by rememberSaveable { mutableStateOf(settings.lastRoomTemp) }
    var humidity by rememberSaveable { mutableStateOf(settings.lastHumidity) }
    var flour by rememberSaveable { mutableStateOf<Double?>(null) }
    var proofTemp by rememberSaveable { mutableStateOf<Double?>(null) }
    var method by rememberSaveable { mutableStateOf<MixingMethod?>(null) }

    val allRecords = remember(data.logs) { data.logs.mapNotNull { it.bake } }
    val samples = remember(data, recipe) {
        if (recipe == null) emptyList() else BakeSamples.from(data.recipes, data.logs.filter { it.recipeId == recipe.id })
    }
    val effectiveMethod = method ?: recipe?.current?.process?.mixingMethod ?: settings.mixingMethod
    val advice = remember(recipe, samples, allRecords, room, humidity, flour, proofTemp, effectiveMethod, settings.targetDoughTemp) {
        val r = room ?: return@remember null
        BakeAdvisor.advise(
            recipe,
            samples,
            allRecords,
            TodayConditions(
                roomTemp = r,
                humidity = humidity,
                flourTemp = flour,
                firstProofTemp = proofTemp,
                mixingMethod = effectiveMethod,
                targetDoughTemp = recipe?.current?.process?.targetDoughTemp ?: settings.targetDoughTemp,
            ),
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("パン研究", fontWeight = FontWeight.Bold) }) },
        bottomBar = bottomBar,
    ) { padding ->
        if (breads.isEmpty()) {
            EmptyState(
                emoji = "🍞",
                title = "パンのレシピを登録しましょう",
                message = "種類を「パン」にしたレシピで焼成ログ（室温・湿度・配合・発酵時間・出来栄え）を記録すると、" +
                    "今日の条件に合わせた加水・仕込み水温・発酵時間を提案します。",
                modifier = Modifier.padding(padding),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(onClick = { navigator.push(Route.EditRecipe(category = Category.BREAD)) }) { Text("パンのレシピを追加") }
                    TextButton(onClick = { navigator.push(Route.Calculator) }) { Text("計算ツールだけ使う") }
                }
            }
            return@Scaffold
        }
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "today") {
                SectionCard(title = "今日の仕込み", icon = Icons.Filled.Thermostat) {
                    RecipePicker(breads, recipe) { navigator.breadRecipeId = it.id }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField(room, { room = it }, "室温", Modifier.weight(1f), "℃")
                        NumberField(humidity, { humidity = it }, "湿度", Modifier.weight(1f), "%")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField(flour, { flour = it }, "粉温", Modifier.weight(1f), "℃", placeholder = "空=室温")
                        NumberField(proofTemp, { proofTemp = it }, "発酵させる温度", Modifier.weight(1f), "℃", placeholder = "空=いつもの温度")
                    }
                    ChoiceChips(MixingMethod.entries, effectiveMethod, { it.label }, { method = it })
                    if (room == null) {
                        Text(
                            "室温を入れると、過去の焼成ログから今日の配合と仕込み水温を提案します",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (advice != null && recipe != null) {
                item(key = "advice") {
                    AdviceCard(recipe, advice, room, humidity, flour) { prefill ->
                        container.settings.update { it.copy(lastRoomTemp = room, lastHumidity = humidity) }
                        navigator.push(Route.EditLog(recipe.id, prefill = prefill))
                    }
                }
                if (advice.references.isNotEmpty()) {
                    item(key = "refs") {
                        SectionCard(title = "条件が近かった過去の回", icon = Icons.Filled.Insights) {
                            advice.references.forEach { ref ->
                                val s = ref.sample
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(Fmt.date(s.log.date), style = MaterialTheme.typography.labelLarge)
                                            Spacer(Modifier.width(6.dp))
                                            SmallStars(s.rating)
                                        }
                                        Text(
                                            listOfNotNull(
                                                s.bake.roomTemp?.let { "室温${Fmt.temp(it)}" },
                                                s.bake.humidity?.let { "湿度${Fmt.num(it)}%" },
                                                s.hydration?.let { "加水${Fmt.percent(it)}" },
                                                s.bake.firstProofMinutes?.let { "一次${it}分" },
                                            ).joinToString("・"),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                        if (s.log.notes.isNotBlank()) {
                                            Text(s.log.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                        }
                                    }
                                    TextButton(onClick = { navigator.push(Route.EditLog(recipe.id, s.log.id)) }) { Text("見る") }
                                }
                            }
                        }
                    }
                }
            }
            item(key = "rise") { RiseCard(allRecords) }
            item(key = "tools") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { navigator.push(Route.Analysis(recipe?.id)) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.ScatterPlot, contentDescription = null)
                        Text(" 研究ノート")
                    }
                    OutlinedButton(onClick = { navigator.push(Route.Calculator) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Calculate, contentDescription = null)
                        Text(" 計算ツール")
                    }
                }
            }
            val recent = data.logs.filter { it.bake != null }.sortedByDescending { it.date }.take(5)
            if (recent.isNotEmpty()) {
                item(key = "recent-title") {
                    Text("最近の焼成ログ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(recent, key = { it.id }) { log ->
                    val r = data.recipes.firstOrNull { it.id == log.recipeId }
                    LogCard(
                        log = log,
                        recipe = r,
                        photos = container.photos,
                        showRecipeTitle = true,
                        onClick = { navigator.push(Route.EditLog(log.recipeId, log.id)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RecipePicker(recipes: List<Recipe>, selected: Recipe?, onSelect: (Recipe) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selected?.title ?: "レシピを選ぶ", modifier = Modifier.weight(1f), maxLines = 1)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = "レシピを選ぶ")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            recipes.forEach { r ->
                DropdownMenuItem(text = { Text(r.title) }, onClick = { onSelect(r); open = false })
            }
        }
    }
}

@Composable
private fun AdviceCard(
    recipe: Recipe,
    advice: BakeAdvice,
    room: Double?,
    humidity: Double?,
    flour: Double?,
    onStart: (BakePrefill) -> Unit,
) {
    val water = (advice.water.waterTemp * 2).roundToInt() / 2.0
    SectionCard(
        title = "今日の提案",
        icon = Icons.Filled.AutoAwesome,
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        action = { InfoPill("確かさ: ${advice.confidence.label}", color = confidenceColor(advice.confidence)) },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("仕込み水の温度", style = MaterialTheme.typography.labelLarge)
                Text(Fmt.temp(water), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                val r = room ?: 0.0
                Text(
                    "（こね上げ${Fmt.temp(advice.targetDoughTemp)} − 上昇${Fmt.num(advice.rise.value)}℃）× 3 − （室温${Fmt.temp(r)} + 粉温${Fmt.temp(flour ?: r)}）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        advice.water.warning?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        HorizontalDivider()

        val hydration = advice.hydration
        if (hydration != null) {
            Text("加水率", style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(Fmt.percent(hydration.value), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                advice.baseHydration?.let { base ->
                    val diff = hydration.value - base
                    Text(
                        "  レシピ ${Fmt.percent(base)}（${if (diff >= 0) "+" else "−"}${Fmt.percent(kotlin.math.abs(diff))}）",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Text(hydration.basis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            advice.adjustedIngredients?.let { adjusted ->
                val base = recipe.current.ingredients
                adjusted.forEachIndexed { i, ing ->
                    if (ing.role == IngredientRole.LIQUID && ing.amount != base.getOrNull(i)?.amount) {
                        val before = base.getOrNull(i)?.let { Amounts.display(it) } ?: ""
                        Text("・${ing.name}: $before → ${Amounts.display(ing)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            val base = advice.baseHydration
            Text(
                if (base != null) "加水率はレシピどおり（${Fmt.percent(base)}）。記録が増えると条件に合わせて提案します" else "加水率の提案には、材料を「粉」「水分」の役割付きでグラム入力してください",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        HorizontalDivider()
        Row {
            advice.firstProof?.let { LabeledSuggestion("一次発酵", "${it.value.roundToInt()}分", it.basis, Modifier.weight(1f)) }
            advice.secondProof?.let { LabeledSuggestion("二次発酵", "${it.value.roundToInt()}分", it.basis, Modifier.weight(1f)) }
        }
        advice.trends.forEach { Text("📈 $it", style = MaterialTheme.typography.bodySmall) }
        advice.notes.forEach { Text("💡 $it", style = MaterialTheme.typography.bodySmall) }
        Button(
            onClick = {
                onStart(
                    BakePrefill(
                        roomTemp = room,
                        humidity = humidity,
                        flourTemp = flour,
                        waterTemp = water,
                        targetDoughTemp = advice.targetDoughTemp,
                        mixingMethod = advice.rise.method,
                        firstProofMinutes = advice.firstProof?.value?.roundToInt(),
                        firstProofTemp = null,
                        secondProofMinutes = advice.secondProof?.value?.roundToInt(),
                        secondProofTemp = null,
                        ingredients = advice.adjustedIngredients,
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.EditNote, contentDescription = null)
            Text(" この内容で焼成ログをつける")
        }
    }
}

@Composable
private fun LabeledSuggestion(label: String, value: String, basis: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(basis, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun confidenceColor(c: Confidence) = when (c) {
    Confidence.HIGH -> MaterialTheme.colorScheme.secondaryContainer
    Confidence.MEDIUM -> MaterialTheme.colorScheme.tertiaryContainer
    else -> MaterialTheme.colorScheme.surfaceContainerHighest
}

/** こね方ごとに学習した上昇温度。 */
@Composable
private fun RiseCard(records: List<io.github.twatanabe1436.sodateru.core.model.BakeRecord>) {
    SectionCard(title = "こね方の学習（上昇温度）", icon = Icons.Filled.Insights) {
        Text(
            "こねている間に生地が何℃温まるか。仕込み水温とこね上げ温度を記録すると、あなたのこね方の値に置き換わります。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MixingMethod.entries.forEach { m ->
                val est = DoughTemperature.learnedRise(records, m)
                OutlinedCard {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(m.label, style = MaterialTheme.typography.labelMedium)
                        Text("+${Fmt.num(est.value)}℃", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            if (est.isDefault) "目安" else "${est.samples}回の平均",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (est.isDefault) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
        }
        // 配合の記録がある回の数も添える
        val withFormula = records.count { BakersMath.hydration(it.ingredients) != null }
        if (withFormula > 0) {
            Text("配合を記録した焼成ログ: ${withFormula}回", style = MaterialTheme.typography.labelSmall)
        }
    }
}
