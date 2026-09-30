package io.github.twatanabe1436.sodateru.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.twatanabe1436.sodateru.AppContainer
import io.github.twatanabe1436.sodateru.core.model.Category
import io.github.twatanabe1436.sodateru.core.model.Ingredient
import io.github.twatanabe1436.sodateru.core.model.MixingMethod
import io.github.twatanabe1436.sodateru.ui.bread.AnalysisScreen
import io.github.twatanabe1436.sodateru.ui.bread.BreadLabScreen
import io.github.twatanabe1436.sodateru.ui.bread.CalculatorScreen
import io.github.twatanabe1436.sodateru.ui.logs.LogEditScreen
import io.github.twatanabe1436.sodateru.ui.logs.LogListScreen
import io.github.twatanabe1436.sodateru.ui.recipes.RecipeDetailScreen
import io.github.twatanabe1436.sodateru.ui.recipes.RecipeEditScreen
import io.github.twatanabe1436.sodateru.ui.recipes.RecipeListScreen
import io.github.twatanabe1436.sodateru.ui.recipes.ScanScreen
import io.github.twatanabe1436.sodateru.ui.recipes.VersionDiffScreen
import io.github.twatanabe1436.sodateru.ui.settings.SettingsScreen

/** 「今日の提案」から焼成ログを始めるときに引き継ぐ値。 */
data class BakePrefill(
    val roomTemp: Double?,
    val humidity: Double?,
    val flourTemp: Double?,
    val waterTemp: Double?,
    val targetDoughTemp: Double?,
    val mixingMethod: MixingMethod?,
    val firstProofMinutes: Int?,
    val firstProofTemp: Double?,
    val secondProofMinutes: Int?,
    val secondProofTemp: Double?,
    val ingredients: List<Ingredient>?,
)

sealed interface Route {
    data object Home : Route
    data class RecipeDetail(val id: String) : Route

    /**
     * レシピの作成・編集。[recipeId] が null なら新規。
     * 既存レシピでは新しい版を作る ([fromLogId] があればその記録のアレンジを反映する)。
     */
    data class EditRecipe(
        val recipeId: String? = null,
        val fromLogId: String? = null,
        val useDraft: Boolean = false,
        val category: Category? = null,
    ) : Route

    data class Diff(val recipeId: String, val from: Int, val to: Int) : Route
    data class EditLog(val recipeId: String, val logId: String? = null, val prefill: BakePrefill? = null) : Route
    data class Scan(val category: Category? = null) : Route
    data class Analysis(val recipeId: String? = null) : Route
    data object Calculator : Route
}

enum class HomeTab(val label: String, val icon: ImageVector) {
    RECIPES("レシピ", Icons.AutoMirrored.Filled.MenuBook),
    BREAD("パン研究", Icons.Filled.BakeryDining),
    LOGS("記録", Icons.Filled.History),
    SETTINGS("設定", Icons.Filled.Settings),
}

internal data class Entry(val key: Int, val route: Route)

/** 画面の積み重ね。Activity は構成変更で作り直さない設定なので、メモリ上に持つだけでよい。 */
class Navigator {
    private var nextKey = 1
    private val entries = mutableStateListOf(Entry(0, Route.Home))
    var tab by mutableStateOf(HomeTab.RECIPES)

    /** パン研究タブで選んでいるレシピ (レシピ詳細の「今日の条件で仕込む」から切り替える)。 */
    var breadRecipeId by mutableStateOf<String?>(null)

    internal val stackEntries: List<Entry> get() = entries
    val current: Route get() = entries.last().route
    val depth: Int get() = entries.size
    internal var onRemoved: (Int) -> Unit = {}

    fun push(route: Route) {
        entries.add(Entry(nextKey++, route))
    }

    fun pop(): Boolean {
        if (entries.size <= 1) return false
        onRemoved(entries.removeAt(entries.lastIndex).key)
        return true
    }

    /** 今の画面を別の画面に置き換える (保存後に詳細画面へ移る、など)。 */
    fun replace(route: Route) {
        val removed = entries.removeAt(entries.lastIndex)
        onRemoved(removed.key)
        entries.add(Entry(nextKey++, route))
    }

    fun goHome(tab: HomeTab? = null) {
        while (entries.size > 1) pop()
        if (tab != null) this.tab = tab
    }
}

@Composable
fun AppRoot(container: AppContainer, navigator: Navigator) {
    val loaded by container.repository.loaded.collectAsStateWithLifecycle()
    if (!loaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val holder = rememberSaveableStateHolder()
    navigator.onRemoved = { holder.removeState(it) }
    BackHandler(enabled = navigator.depth > 1) { navigator.pop() }

    val entry = navigator.stackEntries.last()
    holder.SaveableStateProvider(entry.key) {
        when (val route = entry.route) {
            Route.Home -> HomeScreen(container, navigator)
            is Route.RecipeDetail -> RecipeDetailScreen(container, navigator, route.id)
            is Route.EditRecipe -> RecipeEditScreen(container, navigator, route)
            is Route.Diff -> VersionDiffScreen(container, navigator, route.recipeId, route.from, route.to)
            is Route.EditLog -> LogEditScreen(container, navigator, route)
            is Route.Scan -> ScanScreen(container, navigator, route.category)
            is Route.Analysis -> AnalysisScreen(container, navigator, route.recipeId)
            Route.Calculator -> CalculatorScreen(container, navigator)
        }
    }
}

@Composable
private fun HomeScreen(container: AppContainer, navigator: Navigator) {
    val holder = rememberSaveableStateHolder()
    val bottomBar: @Composable () -> Unit = {
        NavigationBar {
            HomeTab.entries.forEach { tab ->
                NavigationBarItem(
                    selected = navigator.tab == tab,
                    onClick = { navigator.tab = tab },
                    icon = { Icon(tab.icon, contentDescription = null) },
                    label = { Text(tab.label) },
                )
            }
        }
    }
    holder.SaveableStateProvider(navigator.tab.name) {
        when (navigator.tab) {
            HomeTab.RECIPES -> RecipeListScreen(container, navigator, bottomBar)
            HomeTab.BREAD -> BreadLabScreen(container, navigator, bottomBar)
            HomeTab.LOGS -> LogListScreen(container, navigator, bottomBar)
            HomeTab.SETTINGS -> SettingsScreen(container, navigator, bottomBar)
        }
    }
}
