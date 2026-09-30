@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package io.github.twatanabe1436.sodateru.ui.recipes

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.twatanabe1436.sodateru.AppContainer
import io.github.twatanabe1436.sodateru.core.Amounts
import io.github.twatanabe1436.sodateru.core.IngredientChange
import io.github.twatanabe1436.sodateru.core.LineChangeType
import io.github.twatanabe1436.sodateru.core.RecipeDiff
import io.github.twatanabe1436.sodateru.ui.Navigator
import io.github.twatanabe1436.sodateru.ui.components.BackButton
import io.github.twatanabe1436.sodateru.ui.components.Fmt
import io.github.twatanabe1436.sodateru.ui.components.SectionCard

/** 2 つの版の違い (材料の増減・量の変化・手順の差分)。 */
@Composable
fun VersionDiffScreen(container: AppContainer, navigator: Navigator, recipeId: String, from: Int, to: Int) {
    val data by container.repository.data.collectAsStateWithLifecycle()
    val recipe = data.recipes.firstOrNull { it.id == recipeId }
    if (recipe == null) {
        LaunchedEffect(Unit) { navigator.pop() }
        return
    }
    var a by rememberSaveable { mutableStateOf(from) }
    var b by rememberSaveable { mutableStateOf(to) }
    val before = recipe.version(a) ?: recipe.versions.first()
    val after = recipe.version(b) ?: recipe.current
    val diff = remember(before, after) { RecipeDiff.diff(before, after) }
    val added = MaterialTheme.colorScheme.secondaryContainer
    val removed = MaterialTheme.colorScheme.errorContainer

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("版を比べる") },
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
            item {
                Column {
                    Text("比べる版", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        recipe.versions.forEach { v ->
                            FilterChip(selected = v.number == a, onClick = { a = v.number }, label = { Text("前: v${v.number}") })
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        recipe.versions.forEach { v ->
                            FilterChip(selected = v.number == b, onClick = { b = v.number }, label = { Text("後: v${v.number}") })
                        }
                    }
                }
            }
            item {
                SectionCard(title = "v${before.number}（${Fmt.shortDate(before.createdAt)}）→ v${after.number}（${Fmt.shortDate(after.createdAt)}）") {
                    if (after.changeNote.isNotBlank()) Text(after.changeNote, style = MaterialTheme.typography.bodyMedium)
                    Text(diff.summary(limit = 10), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    diff.servings?.let { (x, y) -> Text("分量: $x → $y", style = MaterialTheme.typography.bodyMedium) }
                }
            }
            item { Text("材料", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(diff.ingredients) { change ->
                when (change) {
                    is IngredientChange.Same -> DiffRow("", change.ingredient.name, Amounts.display(change.ingredient), null)
                    is IngredientChange.Added -> DiffRow("＋", change.ingredient.name, Amounts.display(change.ingredient), added)
                    is IngredientChange.Removed -> DiffRow("−", change.ingredient.name, Amounts.display(change.ingredient), removed, strike = true)
                    is IngredientChange.Changed -> DiffRow(
                        "→",
                        change.after.name,
                        "${Amounts.display(change.before)} → ${Amounts.display(change.after)}" +
                            if (change.before.note != change.after.note && change.after.note.isNotBlank()) "（${change.after.note}）" else "",
                        added.copy(alpha = 0.6f),
                    )
                }
            }
            item { Text("作り方", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(diff.steps) { line ->
                when (line.type) {
                    LineChangeType.SAME -> DiffRow("", line.text, "", null)
                    LineChangeType.ADDED -> DiffRow("＋", line.text, "", added)
                    LineChangeType.REMOVED -> DiffRow("−", line.text, "", removed, strike = true)
                }
            }
            diff.memo?.let { (x, y) ->
                item {
                    SectionCard(title = "メモ") {
                        if (x.isNotBlank()) Text(x, textDecoration = TextDecoration.LineThrough, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (y.isNotBlank()) Text(y)
                    }
                }
            }
        }
    }
}

@Composable
private fun DiffRow(mark: String, name: String, value: String, background: Color?, strike: Boolean = false) {
    Surface(
        color = background ?: Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(mark, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp))
            Text(
                name,
                modifier = Modifier.weight(1f),
                textDecoration = if (strike) TextDecoration.LineThrough else null,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (value.isNotEmpty()) {
                Text(
                    value,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (strike) TextDecoration.LineThrough else null,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
