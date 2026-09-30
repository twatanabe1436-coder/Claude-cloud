package io.github.twatanabe1436.sodateru.core

import io.github.twatanabe1436.sodateru.core.model.Ingredient
import io.github.twatanabe1436.sodateru.core.model.RecipeVersion

sealed interface IngredientChange {
    data class Same(val ingredient: Ingredient) : IngredientChange
    data class Added(val ingredient: Ingredient) : IngredientChange
    data class Removed(val ingredient: Ingredient) : IngredientChange
    data class Changed(val before: Ingredient, val after: Ingredient) : IngredientChange
}

enum class LineChangeType { SAME, ADDED, REMOVED }

data class LineChange(val type: LineChangeType, val text: String)

data class VersionDiff(
    val ingredients: List<IngredientChange>,
    val steps: List<LineChange>,
    val servings: Pair<String, String>?,
    val memo: Pair<String, String>?,
) {
    val changedIngredientCount: Int get() = ingredients.count { it !is IngredientChange.Same }
    val changedStepCount: Int get() = steps.count { it.type != LineChangeType.SAME }
    val hasChanges: Boolean
        get() = changedIngredientCount > 0 || changedStepCount > 0 || servings != null || memo != null

    /** 「強力粉 250g→260g、塩 追加」のような一行の要約。 */
    fun summary(limit: Int = 3): String {
        val parts = ingredients.mapNotNull {
            when (it) {
                is IngredientChange.Changed -> "${it.after.name} ${Amounts.display(it.before)}→${Amounts.display(it.after)}"
                is IngredientChange.Added -> "${it.ingredient.name}を追加"
                is IngredientChange.Removed -> "${it.ingredient.name}をなくした"
                is IngredientChange.Same -> null
            }
        }.toMutableList()
        if (changedStepCount > 0) parts += "手順を変更"
        if (servings != null) parts += "分量 ${servings.first}→${servings.second}"
        if (parts.isEmpty()) return if (memo != null) "メモを変更" else "変更なし"
        val shown = parts.take(limit).joinToString("、")
        return if (parts.size > limit) "$shown ほか${parts.size - limit}件" else shown
    }
}

/** 2 つの版を比べる。材料は名前で対応づけ、手順は行単位の差分 (最長共通部分列) で比べる。 */
object RecipeDiff {

    private fun key(name: String): String = Amounts.normalize(name).replace(" ", "").lowercase()

    fun diff(before: RecipeVersion, after: RecipeVersion): VersionDiff = VersionDiff(
        ingredients = diffIngredients(before.ingredients, after.ingredients),
        steps = diffLines(before.steps.map(String::trim), after.steps.map(String::trim)),
        servings = (before.servings to after.servings).takeIf { it.first.trim() != it.second.trim() },
        memo = (before.memo to after.memo).takeIf { it.first.trim() != it.second.trim() },
    )

    fun diffIngredients(before: List<Ingredient>, after: List<Ingredient>): List<IngredientChange> {
        val remaining = before.toMutableList()
        val result = mutableListOf<IngredientChange>()
        for (ing in after) {
            val idx = remaining.indexOfFirst { key(it.name) == key(ing.name) }
            if (idx < 0) {
                result += IngredientChange.Added(ing)
                continue
            }
            val old = remaining.removeAt(idx)
            val same = Amounts.normalize(old.amount) == Amounts.normalize(ing.amount) &&
                old.unit.trim() == ing.unit.trim() &&
                old.note.trim() == ing.note.trim()
            result += if (same) IngredientChange.Same(ing) else IngredientChange.Changed(old, ing)
        }
        // なくなった材料は、元の並びで直前にあった材料の後ろに差し込む
        for (old in remaining) {
            val prevIndex = before.indexOf(old) - 1
            val anchor = if (prevIndex >= 0) key(before[prevIndex].name) else null
            val pos = if (anchor == null) 0 else result.indexOfFirst { ingredientKey(it) == anchor }.let { if (it < 0) result.size else it + 1 }
            result.add(pos, IngredientChange.Removed(old))
        }
        return result
    }

    private fun ingredientKey(change: IngredientChange): String = when (change) {
        is IngredientChange.Same -> key(change.ingredient.name)
        is IngredientChange.Added -> key(change.ingredient.name)
        is IngredientChange.Removed -> key(change.ingredient.name)
        is IngredientChange.Changed -> key(change.after.name)
    }

    fun diffLines(before: List<String>, after: List<String>): List<LineChange> {
        val n = before.size
        val m = after.size
        val lcs = Array(n + 1) { IntArray(m + 1) }
        for (i in n - 1 downTo 0) {
            for (j in m - 1 downTo 0) {
                lcs[i][j] = if (before[i] == after[j]) lcs[i + 1][j + 1] + 1 else maxOf(lcs[i + 1][j], lcs[i][j + 1])
            }
        }
        val out = mutableListOf<LineChange>()
        var i = 0
        var j = 0
        while (i < n && j < m) {
            when {
                before[i] == after[j] -> {
                    out += LineChange(LineChangeType.SAME, after[j]); i++; j++
                }
                lcs[i + 1][j] >= lcs[i][j + 1] -> {
                    out += LineChange(LineChangeType.REMOVED, before[i]); i++
                }
                else -> {
                    out += LineChange(LineChangeType.ADDED, after[j]); j++
                }
            }
        }
        while (i < n) out += LineChange(LineChangeType.REMOVED, before[i++])
        while (j < m) out += LineChange(LineChangeType.ADDED, after[j++])
        return out
    }
}
