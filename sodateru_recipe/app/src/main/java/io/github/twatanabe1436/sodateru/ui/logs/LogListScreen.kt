@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package io.github.twatanabe1436.sodateru.ui.logs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import io.github.twatanabe1436.sodateru.core.model.Category
import io.github.twatanabe1436.sodateru.ui.Navigator
import io.github.twatanabe1436.sodateru.ui.Route
import io.github.twatanabe1436.sodateru.ui.components.EmptyState
import io.github.twatanabe1436.sodateru.ui.components.Fmt

/** すべての「作った記録」を新しい順に。 */
@Composable
fun LogListScreen(container: AppContainer, navigator: Navigator, bottomBar: @Composable () -> Unit) {
    val data by container.repository.data.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf<Category?>(null) }
    val recipes = remember(data.recipes) { data.recipes.associateBy { it.id } }
    val logs = remember(data, filter) {
        data.logs
            .filter { log -> filter == null || recipes[log.recipeId]?.category == filter }
            .sortedByDescending { it.date }
    }
    val groups = remember(logs) { logs.groupBy { Fmt.month(it.date) } }

    Scaffold(
        topBar = { TopAppBar(title = { Text("記録", fontWeight = FontWeight.Bold) }) },
        bottomBar = bottomBar,
    ) { padding ->
        if (data.logs.isEmpty()) {
            EmptyState(
                emoji = "📓",
                title = "まだ記録がありません",
                message = "レシピの画面の「作った！記録する」から、アレンジや感想、パンなら焼いたときの条件を残せます。",
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "filters") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("すべて") })
                    Category.entries.forEach { c ->
                        FilterChip(selected = filter == c, onClick = { filter = if (filter == c) null else c }, label = { Text(c.label) })
                    }
                }
            }
            groups.forEach { (month, list) ->
                item(key = "m-$month") {
                    Text(
                        "$month（${list.size}回）",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(list, key = { it.id }) { log ->
                    val recipe = recipes[log.recipeId]
                    LogCard(
                        log = log,
                        recipe = recipe,
                        photos = container.photos,
                        showRecipeTitle = true,
                        onClick = { navigator.push(Route.EditLog(log.recipeId, log.id)) },
                        onApply = recipe?.let { r -> { navigator.push(Route.EditRecipe(recipeId = r.id, fromLogId = log.id)) } },
                    )
                }
            }
        }
    }
}
