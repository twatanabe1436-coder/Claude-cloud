package io.github.twatanabe1436.sodateru.core

import io.github.twatanabe1436.sodateru.core.model.CookLog
import io.github.twatanabe1436.sodateru.core.model.Recipe

/** レシピの育ち具合。作るほど・アレンジを反映するほど育つ。 */
enum class GrowthStage(val emoji: String, val label: String) {
    SEED("🌰", "たね"),
    SPROUT("🌱", "芽"),
    LEAF("🌿", "若葉"),
    TREE("🌳", "木"),
    FRUIT("🍎", "実り"),
}

data class Growth(
    val stage: GrowthStage,
    val cookCount: Int,
    val versionCount: Int,
    val bestRating: Int,
    val lastCooked: Long?,
    /** 次の段階までのポイント (最終段階なら null)。 */
    val pointsToNext: Int?,
) {
    val points: Int get() = cookCount + 2 * (versionCount - 1)
}

object GrowthCalculator {

    /** 段階が上がるポイント。作った回数 1 回 = 1、反映した版 1 つ = 2。 */
    private val thresholds = listOf(
        GrowthStage.SPROUT to 1,
        GrowthStage.LEAF to 3,
        GrowthStage.TREE to 6,
        GrowthStage.FRUIT to 12,
    )

    fun growth(recipe: Recipe, logs: List<CookLog>): Growth {
        val mine = logs.filter { it.recipeId == recipe.id }
        val points = mine.size + 2 * (recipe.versions.size - 1)
        val stage = thresholds.lastOrNull { points >= it.second }?.first ?: GrowthStage.SEED
        val next = thresholds.firstOrNull { points < it.second }?.second
        return Growth(
            stage = stage,
            cookCount = mine.size,
            versionCount = recipe.versions.size,
            bestRating = mine.maxOfOrNull { it.rating } ?: 0,
            lastCooked = mine.maxOfOrNull { it.date },
            pointsToNext = next?.let { it - points },
        )
    }
}
