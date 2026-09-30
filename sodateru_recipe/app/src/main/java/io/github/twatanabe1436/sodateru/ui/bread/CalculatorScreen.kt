@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package io.github.twatanabe1436.sodateru.ui.bread

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.twatanabe1436.sodateru.AppContainer
import io.github.twatanabe1436.sodateru.core.Amounts
import io.github.twatanabe1436.sodateru.core.BakersMath
import io.github.twatanabe1436.sodateru.core.DoughTemperature
import io.github.twatanabe1436.sodateru.core.model.Category
import io.github.twatanabe1436.sodateru.core.model.Ingredient
import io.github.twatanabe1436.sodateru.core.model.MixingMethod
import io.github.twatanabe1436.sodateru.ui.BakePrefill
import io.github.twatanabe1436.sodateru.ui.Navigator
import io.github.twatanabe1436.sodateru.ui.Route
import io.github.twatanabe1436.sodateru.ui.components.BackButton
import io.github.twatanabe1436.sodateru.ui.components.BakersSummaryLine
import io.github.twatanabe1436.sodateru.ui.components.ChoiceChips
import io.github.twatanabe1436.sodateru.ui.components.Fmt
import io.github.twatanabe1436.sodateru.ui.components.IngredientList
import io.github.twatanabe1436.sodateru.ui.components.IntField
import io.github.twatanabe1436.sodateru.ui.components.NumberField
import io.github.twatanabe1436.sodateru.ui.components.SectionCard
import io.github.twatanabe1436.sodateru.ui.components.ingredientEditorItems

/** 計算ツール: 仕込み水温とベーカーズ%の配合計算。 */
@Composable
fun CalculatorScreen(container: AppContainer, navigator: Navigator) {
    var tab by rememberSaveable { mutableStateOf(0) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("計算ツール") },
                navigationIcon = { BackButton { navigator.pop() } },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).consumeWindowInsets(padding).imePadding()) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("仕込み水温") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("配合（ベーカーズ%）") })
            }
            when (tab) {
                0 -> WaterTempCalculator(container)
                else -> BakersCalculator(container, navigator)
            }
        }
    }
}

@Composable
private fun WaterTempCalculator(container: AppContainer) {
    val data by container.repository.data.collectAsStateWithLifecycle()
    val settings = container.settings.current
    var target by rememberSaveable { mutableStateOf<Double?>(settings.targetDoughTemp) }
    var room by rememberSaveable { mutableStateOf(settings.lastRoomTemp) }
    var flour by rememberSaveable { mutableStateOf<Double?>(null) }
    var method by rememberSaveable { mutableStateOf<MixingMethod?>(settings.mixingMethod) }
    var riseOverride by rememberSaveable { mutableStateOf<Double?>(null) }
    var preferment by rememberSaveable { mutableStateOf<Double?>(null) }
    val records = remember(data.logs) { data.logs.mapNotNull { it.bake } }
    val learned = DoughTemperature.learnedRise(records, method)
    val rise = riseOverride ?: learned.value

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionCard(title = "条件") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(target, { target = it }, "目標こね上げ温度", Modifier.weight(1f), "℃")
                    NumberField(room, { room = it }, "室温", Modifier.weight(1f), "℃")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(flour, { flour = it }, "粉温", Modifier.weight(1f), "℃", placeholder = "空=室温")
                    NumberField(preferment, { preferment = it }, "種の温度", Modifier.weight(1f), "℃", placeholder = "中種など")
                }
                Text("こね方", style = MaterialTheme.typography.labelLarge)
                ChoiceChips(MixingMethod.entries, method, { it.label }, { method = it; riseOverride = null })
                NumberField(
                    riseOverride ?: learned.value,
                    { riseOverride = it },
                    "上昇温度" + if (learned.isDefault) "（目安）" else "（記録${learned.samples}回の平均）",
                    Modifier.fillMaxWidth(),
                    "℃",
                )
            }
        }
        item {
            val t = target
            val r = room
            SectionCard(title = "結果", containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)) {
                if (t == null || r == null) {
                    Text("目標こね上げ温度と室温を入れてください", style = MaterialTheme.typography.bodyMedium)
                } else {
                    val f = flour ?: r
                    val result = DoughTemperature.waterTemp(t, r, f, rise, preferment)
                    Text("仕込み水 ${Fmt.temp(Math.round(result.waterTemp * 10) / 10.0)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    val formula = if (preferment == null) {
                        "（${Fmt.num(t)} − ${Fmt.num(rise)}）× 3 − （${Fmt.num(r)} + ${Fmt.num(f)}）"
                    } else {
                        "（${Fmt.num(t)} − ${Fmt.num(rise)}）× 4 − （${Fmt.num(r)} + ${Fmt.num(f)} + ${Fmt.num(preferment)}）"
                    }
                    Text(formula, style = MaterialTheme.typography.bodySmall)
                    result.warning?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
        item {
            Text(
                "こね上げ温度 ≒ （室温 + 粉温 + 水温）÷ 3 + こねによる上昇温度、という関係から逆算しています。" +
                    "種を使うときは種の温度も足して 4 で割ります。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BakersCalculator(container: AppContainer, navigator: Navigator) {
    val data by container.repository.data.collectAsStateWithLifecycle()
    val breads = remember(data.recipes) { data.recipes.filter { it.category == Category.BREAD } }
    var recipeId by rememberSaveable { mutableStateOf(breads.firstOrNull()?.id) }
    val recipe = breads.firstOrNull { it.id == recipeId }
    var items by remember(recipeId) {
        mutableStateOf(recipe?.current?.ingredients ?: listOf(Ingredient("強力粉", "250", "g", role = io.github.twatanabe1436.sodateru.core.model.IngredientRole.FLOUR)))
    }
    var targetFlour by rememberSaveable { mutableStateOf<Double?>(null) }
    var pieces by rememberSaveable { mutableStateOf<Int?>(null) }
    var pieceWeight by rememberSaveable { mutableStateOf<Double?>(null) }
    var hydration by rememberSaveable { mutableStateOf<Double?>(null) }
    var open by remember { mutableStateOf(false) }

    val result = remember(items, targetFlour, pieces, pieceWeight, hydration) {
        var list = items
        hydration?.let { list = BakersMath.withHydration(list, it) }
        val p = pieces
        val w = pieceWeight
        when {
            p != null && w != null && p > 0 && w > 0 -> list = BakersMath.scaleToTotal(list, p * w)
            targetFlour != null -> list = BakersMath.scaleToFlour(list, targetFlour!!)
        }
        list
    }
    val summary = remember(result) { BakersMath.summarize(result) }

    LazyColumn(contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Box(Modifier.padding(horizontal = 16.dp)) {
                OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(recipe?.let { "${it.title}（v${it.current.number}）の配合" } ?: "自由入力", modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = "配合を選ぶ")
                }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    breads.forEach { r ->
                        DropdownMenuItem(text = { Text(r.title) }, onClick = { recipeId = r.id; open = false })
                    }
                    DropdownMenuItem(text = { Text("自由入力") }, onClick = { recipeId = null; open = false })
                }
            }
        }
        item { Text("元の配合", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp)) }
        ingredientEditorItems(items, { items = it }, bread = true, keyPrefix = "calc")
        item {
            SectionCard(title = "変える", modifier = Modifier.padding(horizontal = 16.dp)) {
                NumberField(hydration, { hydration = it }, "加水率を変える", Modifier.fillMaxWidth(), "%", placeholder = "空=そのまま")
                NumberField(targetFlour, { targetFlour = it }, "粉の量に合わせる", Modifier.fillMaxWidth(), "g")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IntField(pieces, { pieces = it }, "個数", Modifier.weight(1f), "個")
                    NumberField(pieceWeight, { pieceWeight = it }, "1個の重さ", Modifier.weight(1f), "g")
                }
                Text(
                    "個数と重さを入れると生地の総量に、粉の量を入れると粉の合計に合わせて全体を拡大・縮小します（個数と重さが優先）。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            SectionCard(title = "計算結果", modifier = Modifier.padding(horizontal = 16.dp), containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)) {
                IngredientList(result, percentages = summary.lines.map { it.percent })
                BakersSummaryLine(summary)
                if (recipe != null) {
                    Button(onClick = {
                        navigator.push(
                            Route.EditLog(
                                recipe.id,
                                prefill = BakePrefill(
                                    roomTemp = null, humidity = null, flourTemp = null, waterTemp = null,
                                    targetDoughTemp = null, mixingMethod = null, firstProofMinutes = null, firstProofTemp = null,
                                    secondProofMinutes = null, secondProofTemp = null, ingredients = result,
                                ),
                            ),
                        )
                    }, modifier = Modifier.fillMaxWidth()) { Text("この配合で焼成ログをつける") }
                }
                Text(
                    "粉 ${Amounts.formatNumber(summary.flourGrams)}g・総量 ${Amounts.formatNumber(summary.totalGrams)}g",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
