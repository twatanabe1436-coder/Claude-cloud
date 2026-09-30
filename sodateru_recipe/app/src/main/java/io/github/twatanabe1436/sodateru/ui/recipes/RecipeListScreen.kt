@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package io.github.twatanabe1436.sodateru.ui.recipes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.twatanabe1436.sodateru.AppContainer
import io.github.twatanabe1436.sodateru.core.Growth
import io.github.twatanabe1436.sodateru.core.GrowthCalculator
import io.github.twatanabe1436.sodateru.core.model.Category
import io.github.twatanabe1436.sodateru.core.model.Recipe
import io.github.twatanabe1436.sodateru.data.SampleData
import io.github.twatanabe1436.sodateru.ui.Navigator
import io.github.twatanabe1436.sodateru.ui.Route
import io.github.twatanabe1436.sodateru.ui.components.EmptyState
import io.github.twatanabe1436.sodateru.ui.components.Fmt
import io.github.twatanabe1436.sodateru.ui.components.PhotoThumb
import io.github.twatanabe1436.sodateru.ui.components.SmallStars
import kotlinx.coroutines.launch

private enum class SortOrder(val label: String) {
    RECENT_COOKED("最近作った順"),
    UPDATED("更新順"),
    MOST_COOKED("よく作る順"),
    TITLE("名前順"),
}

private data class RecipeEntry(val recipe: Recipe, val growth: Growth)

private const val FAVORITES = "FAV"

@Composable
fun RecipeListScreen(container: AppContainer, navigator: Navigator, bottomBar: @Composable () -> Unit) {
    val data by container.repository.data.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var sort by rememberSaveable { mutableStateOf(SortOrder.RECENT_COOKED) }
    var sortMenu by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }

    val entries = remember(data, query, filter, sort) {
        val q = query.trim()
        data.recipes
            .filter { r ->
                when (filter) {
                    null -> true
                    FAVORITES -> r.favorite
                    else -> r.category.name == filter
                }
            }
            .filter { r ->
                q.isEmpty() || q in r.title || r.tags.any { q in it } ||
                    r.current.ingredients.any { q in it.name }
            }
            .map { RecipeEntry(it, GrowthCalculator.growth(it, data.logs)) }
            .let { list ->
                when (sort) {
                    SortOrder.RECENT_COOKED -> list.sortedByDescending { it.growth.lastCooked ?: it.recipe.updatedAt }
                    SortOrder.UPDATED -> list.sortedByDescending { it.recipe.updatedAt }
                    SortOrder.MOST_COOKED -> list.sortedByDescending { it.growth.cookCount }
                    SortOrder.TITLE -> list.sortedBy { it.recipe.title }
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("そだてるレシピ", fontWeight = FontWeight.Bold) },
                actions = {
                    Box {
                        IconButton(onClick = { sortMenu = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "並び替え")
                        }
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            SortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(if (order == sort) "✓ ${order.label}" else order.label) },
                                    onClick = { sort = order; sortMenu = false },
                                )
                            }
                        }
                    }
                },
            )
        },
        bottomBar = bottomBar,
        floatingActionButton = {
            if (data.recipes.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showAdd = true },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("レシピを追加") },
                )
            }
        },
    ) { padding ->
        if (data.recipes.isEmpty()) {
            EmptyState(
                emoji = "🌱",
                title = "レシピを育てよう",
                message = "作るたびのアレンジを記録して、レシピに反映していくノートです。" +
                    "パンは室温・湿度と配合・発酵時間の記録から、今日の仕込みを提案します。",
                modifier = Modifier.padding(padding),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { showAdd = true }) { Text("最初のレシピを追加") }
                    OutlinedButton(onClick = {
                        scope.launch { container.repository.mergeIn(SampleData.create()) }
                    }) { Text("サンプルを入れて試してみる") }
                }
            }
            return@Scaffold
        }
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 88.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "search") {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("料理名・材料・タグで探す") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item(key = "filters") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("すべて") })
                    Category.entries.forEach { c ->
                        FilterChip(
                            selected = filter == c.name,
                            onClick = { filter = if (filter == c.name) null else c.name },
                            label = { Text(c.label) },
                        )
                    }
                    FilterChip(
                        selected = filter == FAVORITES,
                        onClick = { filter = if (filter == FAVORITES) null else FAVORITES },
                        label = { Text("お気に入り") },
                        leadingIcon = { Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    )
                }
            }
            if (entries.isEmpty()) {
                item(key = "none") {
                    Text(
                        "条件に合うレシピがありません",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
            items(entries, key = { it.recipe.id }) { entry ->
                RecipeCard(entry, container) { navigator.push(Route.RecipeDetail(entry.recipe.id)) }
            }
        }
    }
    if (showAdd) AddRecipeDialog(navigator) { showAdd = false }
}

@Composable
private fun RecipeCard(entry: RecipeEntry, container: AppContainer, onClick: () -> Unit) {
    val recipe = entry.recipe
    val growth = entry.growth
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val cover = recipe.coverPhoto
            Box(
                Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                if (cover != null) {
                    PhotoThumb(cover, container.photos, Modifier.size(72.dp), maxPx = 240)
                } else {
                    Text(growth.stage.emoji, style = MaterialTheme.typography.headlineMedium)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        recipe.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (recipe.favorite) {
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Filled.Favorite, contentDescription = "お気に入り", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                }
                Text(
                    buildString {
                        append(recipe.category.label)
                        append(" · v").append(recipe.current.number)
                        append(" · ").append(growth.cookCount).append("回作った")
                        growth.lastCooked?.let { append(" · ").append(Fmt.ago(it)) }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${growth.stage.emoji} ${growth.stage.label}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(Modifier.width(8.dp))
                    SmallStars(growth.bestRating)
                }
                val change = recipe.current.changeNote
                if (recipe.versions.size > 1 && change.isNotBlank()) {
                    Text(
                        "最近の変更: $change",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun AddRecipeDialog(navigator: Navigator, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("レシピを追加") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onDismiss(); navigator.push(Route.EditRecipe()) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Icon(Icons.Filled.EditNote, contentDescription = null)
                    Text("  手で入力する", modifier = Modifier.weight(1f))
                }
                OutlinedButton(
                    onClick = { onDismiss(); navigator.push(Route.Scan()) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Icon(Icons.Filled.DocumentScanner, contentDescription = null)
                    Text("  写真から読み取る", modifier = Modifier.weight(1f))
                }
                Text(
                    "本やノートの写真から、材料と作り方を文字に起こします",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("閉じる") } },
    )
}
