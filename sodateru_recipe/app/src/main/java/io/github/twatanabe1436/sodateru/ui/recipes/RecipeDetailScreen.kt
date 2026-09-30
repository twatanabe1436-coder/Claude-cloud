@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package io.github.twatanabe1436.sodateru.ui.recipes

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ScatterPlot
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.twatanabe1436.sodateru.AppContainer
import io.github.twatanabe1436.sodateru.core.Amounts
import io.github.twatanabe1436.sodateru.core.BakersMath
import io.github.twatanabe1436.sodateru.core.Growth
import io.github.twatanabe1436.sodateru.core.GrowthCalculator
import io.github.twatanabe1436.sodateru.core.IngredientChange
import io.github.twatanabe1436.sodateru.core.RecipeDiff
import io.github.twatanabe1436.sodateru.core.model.BreadProcess
import io.github.twatanabe1436.sodateru.core.model.Category
import io.github.twatanabe1436.sodateru.core.model.CookLog
import io.github.twatanabe1436.sodateru.core.model.Recipe
import io.github.twatanabe1436.sodateru.core.model.RecipeVersion
import io.github.twatanabe1436.sodateru.ui.HomeTab
import io.github.twatanabe1436.sodateru.ui.Navigator
import io.github.twatanabe1436.sodateru.ui.Route
import io.github.twatanabe1436.sodateru.ui.components.BackButton
import io.github.twatanabe1436.sodateru.ui.components.BakersSummaryLine
import io.github.twatanabe1436.sodateru.ui.components.ConfirmDialog
import io.github.twatanabe1436.sodateru.ui.components.Fmt
import io.github.twatanabe1436.sodateru.ui.components.IconLabel
import io.github.twatanabe1436.sodateru.ui.components.IngredientList
import io.github.twatanabe1436.sodateru.ui.components.PhotoStrip
import io.github.twatanabe1436.sodateru.ui.components.PillRow
import io.github.twatanabe1436.sodateru.ui.components.SectionCard
import io.github.twatanabe1436.sodateru.ui.logs.LogCard
import kotlinx.coroutines.launch

private val SCALES = listOf(0.5, 1.0, 1.5, 2.0, 3.0)

private sealed interface TimelineItem {
    val date: Long

    data class VersionMade(val version: RecipeVersion, val previous: RecipeVersion?) : TimelineItem {
        override val date: Long get() = version.createdAt
    }

    data class Cooked(val log: CookLog) : TimelineItem {
        override val date: Long get() = log.date
    }
}

@Composable
fun RecipeDetailScreen(container: AppContainer, navigator: Navigator, recipeId: String) {
    val data by container.repository.data.collectAsStateWithLifecycle()
    val recipe = data.recipes.firstOrNull { it.id == recipeId }
    if (recipe == null) {
        LaunchedEffect(Unit) { navigator.pop() }
        return
    }
    val scope = rememberCoroutineScope()
    val logs = remember(data.logs, recipeId) { data.logs.filter { it.recipeId == recipeId } }
    val growth = remember(recipe, logs) { GrowthCalculator.growth(recipe, logs) }
    // null = 今の版
    var viewingNumber by rememberSaveable(recipeId) { mutableStateOf<Int?>(null) }
    var scale by rememberSaveable(recipeId) { mutableStateOf(1.0) }
    var showPercent by rememberSaveable(recipeId) { mutableStateOf(recipe.category == Category.BREAD) }
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val version = viewingNumber?.let { recipe.version(it) } ?: recipe.current
    val isCurrent = version.number == recipe.current.number
    val previous = recipe.versions.lastOrNull { it.number < version.number }
    val bread = recipe.category == Category.BREAD
    val ingredients = remember(version, scale) { BakersMath.scale(version.ingredients, scale) }
    val summary = remember(ingredients, bread) { if (bread) BakersMath.summarize(ingredients) else null }
    val changedNames = remember(previous, version) {
        if (previous == null) {
            emptySet()
        } else {
            RecipeDiff.diffIngredients(previous.ingredients, version.ingredients).mapNotNull {
                when (it) {
                    is IngredientChange.Added -> it.ingredient.name
                    is IngredientChange.Changed -> it.after.name
                    else -> null
                }
            }.toSet()
        }
    }
    val timeline = remember(recipe, logs) {
        val versions = recipe.versions.mapIndexed { i, v -> TimelineItem.VersionMade(v, recipe.versions.getOrNull(i - 1)) }
        (versions + logs.map { TimelineItem.Cooked(it) }).sortedByDescending { it.date }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(recipe.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { BackButton { navigator.pop() } },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            container.repository.saveRecipe(
                                recipe.copy(favorite = !recipe.favorite, updatedAt = System.currentTimeMillis()),
                            )
                        }
                    }) {
                        Icon(
                            if (recipe.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (recipe.favorite) "お気に入りから外す" else "お気に入りにする",
                        )
                    }
                    IconButton(onClick = { navigator.push(Route.EditRecipe(recipeId = recipe.id)) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "レシピを育てる（新しい版）")
                    }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "その他") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            if (recipe.versions.size > 1) {
                                DropdownMenuItem(
                                    text = { Text("版を比べる") },
                                    onClick = {
                                        menu = false
                                        navigator.push(Route.Diff(recipe.id, recipe.versions[recipe.versions.size - 2].number, recipe.current.number))
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("レシピを削除") },
                                onClick = { menu = false; confirmDelete = true },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navigator.push(Route.EditLog(recipe.id)) },
                icon = { Icon(Icons.Filled.EditNote, contentDescription = if (bread) "焼いた！記録する" else "作った！記録する") },
                text = { Text(if (bread) "焼いた！記録する" else "作った！記録する") },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val photos = version.photos.ifEmpty { recipe.versions.flatMap { it.photos }.distinct() }
            if (photos.isNotEmpty()) {
                item(key = "photos") { PhotoStrip(photos, container.photos, size = 160.dp) }
            }
            item(key = "meta") {
                val meta = buildList {
                    add(recipe.category.label)
                    addAll(recipe.tags.map { "#$it" })
                    if (recipe.source.isNotBlank()) add("出典: ${recipe.source}")
                }
                PillRow(meta)
            }
            item(key = "growth") { GrowthCard(recipe, growth) }
            if (bread) {
                item(key = "bread") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = {
                            navigator.breadRecipeId = recipe.id
                            navigator.goHome(HomeTab.BREAD)
                        }) {
                            IconLabel(Icons.Filled.BakeryDining, "今日の条件で仕込む")
                        }
                        OutlinedButton(onClick = { navigator.push(Route.Analysis(recipe.id)) }) {
                            IconLabel(Icons.Filled.ScatterPlot, "研究ノート")
                        }
                    }
                }
            }
            item(key = "versions") {
                VersionSelector(recipe, version.number) { n -> viewingNumber = if (n == recipe.current.number) null else n }
            }
            if (!isCurrent) {
                item(key = "old") {
                    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(12.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "v${version.number}（${Fmt.shortDate(version.createdAt)}）を表示中",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            FilledTonalButton(onClick = {
                                navigator.push(Route.Diff(recipe.id, version.number, recipe.current.number))
                            }) { Text("今の版と比べる") }
                        }
                    }
                }
            }
            if (previous != null) {
                item(key = "change") {
                    val diff = remember(previous, version) { RecipeDiff.diff(previous, version) }
                    SectionCard(
                        title = "v${version.number}で変えたこと",
                        icon = Icons.Filled.Spa,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        action = {
                            IconButton(onClick = { navigator.push(Route.Diff(recipe.id, previous.number, version.number)) }) {
                                Icon(Icons.Filled.Compare, contentDescription = "違いを見る")
                            }
                        },
                    ) {
                        if (version.changeNote.isNotBlank()) Text(version.changeNote, style = MaterialTheme.typography.bodyMedium)
                        Text(diff.summary(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item(key = "ingredients") {
                SectionCard(title = "材料" + if (version.servings.isNotBlank()) "（${version.servings}）" else "") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FlowRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SCALES.forEach { s ->
                                FilterChip(
                                    selected = scale == s,
                                    onClick = { scale = s },
                                    label = { Text("×${Amounts.formatNumber(s)}") },
                                )
                            }
                        }
                    }
                    if (bread) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("ベーカーズ%を表示", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Switch(checked = showPercent, onCheckedChange = { showPercent = it })
                        }
                    }
                    IngredientList(
                        ingredients,
                        percentages = if (bread && showPercent) summary?.lines?.map { it.percent } else null,
                        highlightNames = changedNames,
                    )
                    if (summary != null) BakersSummaryLine(summary)
                    if (changedNames.isNotEmpty()) {
                        Text(
                            "色の付いた材料は前の版から変えたもの",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            version.process?.takeIf { bread }?.let { process ->
                item(key = "process") { ProcessCard(process) }
            }
            if (version.steps.isNotEmpty()) {
                item(key = "steps-title") {
                    Text("作り方", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                itemsIndexed(version.steps, key = { i, _ -> "step-$i" }) { i, step ->
                    Row(verticalAlignment = Alignment.Top) {
                        Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape, modifier = Modifier.size(26.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("${i + 1}", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(step, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    }
                }
            }
            if (version.memo.isNotBlank()) {
                item(key = "memo") {
                    SectionCard(title = "メモ・コツ") { Text(version.memo, style = MaterialTheme.typography.bodyMedium) }
                }
            }
            item(key = "timeline-title") {
                Column(Modifier.padding(top = 8.dp)) {
                    Text("育ちの記録", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "作った記録とレシピの版が新しい順に並びます",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(timeline, key = {
                when (it) {
                    is TimelineItem.VersionMade -> "v-${it.version.number}"
                    is TimelineItem.Cooked -> "log-${it.log.id}"
                }
            }) { item ->
                when (item) {
                    is TimelineItem.VersionMade -> VersionEvent(item) {
                        val prev = item.previous
                        if (prev != null) {
                            navigator.push(Route.Diff(recipe.id, prev.number, item.version.number))
                        } else {
                            viewingNumber = if (item.version.number == recipe.current.number) null else item.version.number
                        }
                    }
                    is TimelineItem.Cooked -> LogCard(
                        log = item.log,
                        recipe = recipe,
                        photos = container.photos,
                        onClick = { navigator.push(Route.EditLog(recipe.id, item.log.id)) },
                        onApply = { navigator.push(Route.EditRecipe(recipeId = recipe.id, fromLogId = item.log.id)) },
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "レシピを削除しますか？",
            text = "「${recipe.title}」のすべての版と、作った記録 ${logs.size} 件も削除されます。元に戻せません。",
            confirmLabel = "削除する",
            destructive = true,
            onConfirm = {
                scope.launch {
                    container.repository.deleteRecipe(recipe.id)
                    navigator.pop()
                }
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun GrowthCard(recipe: Recipe, growth: Growth) {
    SectionCard(title = null, containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(growth.stage.emoji, style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("育ち具合: ${growth.stage.label}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${growth.cookCount}回作った · ${recipe.versions.size - 1}回アレンジを反映" +
                        (if (growth.bestRating > 0) " · 最高★${growth.bestRating}" else ""),
                    style = MaterialTheme.typography.bodySmall,
                )
                val next = growth.pointsToNext
                if (next != null) {
                    val total = growth.points + next
                    LinearProgressIndicator(
                        progress = { if (total == 0) 0f else growth.points.toFloat() / total },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    )
                    Text(
                        "あと${next}ポイントで次の段階（作る=1・反映=2）",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text("しっかり実りました！", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun VersionSelector(recipe: Recipe, selected: Int, onSelect: (Int) -> Unit) {
    if (recipe.versions.size <= 1) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        recipe.versions.forEach { v ->
            FilterChip(
                selected = v.number == selected,
                onClick = { onSelect(v.number) },
                label = { Text(if (v.number == recipe.current.number) "v${v.number}（今）" else "v${v.number}") },
            )
        }
    }
}

@Composable
private fun ProcessCard(process: BreadProcess) {
    val rows = buildList {
        process.targetDoughTemp?.let { add("こね上げ目標" to Fmt.temp(it)) }
        process.mixingMethod?.let { add("こね方" to it.label) }
        process.firstProofMinutes?.let { m -> add("一次発酵" to "${m}分" + (process.firstProofTemp?.let { " / ${Fmt.temp(it)}" } ?: "")) }
        process.benchMinutes?.let { add("ベンチタイム" to "${it}分") }
        process.secondProofMinutes?.let { m -> add("二次発酵" to "${m}分" + (process.secondProofTemp?.let { " / ${Fmt.temp(it)}" } ?: "")) }
        process.bakeTemp?.let { t -> add("焼成" to "${t}℃" + (process.bakeMinutes?.let { " / ${it}分" } ?: "")) }
    }
    if (rows.isEmpty()) return
    SectionCard(title = "工程の目安", icon = Icons.Filled.BakeryDining) {
        rows.forEach { (label, value) ->
            Row {
                Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun VersionEvent(item: TimelineItem.VersionMade, onClick: () -> Unit) {
    val v = item.version
    val prev = item.previous
    val summary = remember(prev, v) { prev?.let { RecipeDiff.diff(it, v).summary() } }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(if (prev == null) "🌰" else "🌱", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (prev == null) "v1 レシピを登録（${Fmt.date(v.createdAt)}）" else "v${v.number} に育てた（${Fmt.date(v.createdAt)}）",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary,
            )
            if (v.changeNote.isNotBlank()) Text(v.changeNote, style = MaterialTheme.typography.bodyMedium)
            if (summary != null) {
                Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
