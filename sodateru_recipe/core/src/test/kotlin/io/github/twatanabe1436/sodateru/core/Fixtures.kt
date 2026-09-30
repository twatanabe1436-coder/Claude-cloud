package io.github.twatanabe1436.sodateru.core

import io.github.twatanabe1436.sodateru.core.model.BakeRecord
import io.github.twatanabe1436.sodateru.core.model.BreadProcess
import io.github.twatanabe1436.sodateru.core.model.Category
import io.github.twatanabe1436.sodateru.core.model.CookLog
import io.github.twatanabe1436.sodateru.core.model.Ingredient
import io.github.twatanabe1436.sodateru.core.model.MixingMethod
import io.github.twatanabe1436.sodateru.core.model.Recipe
import io.github.twatanabe1436.sodateru.core.model.RecipeVersion

object Fixtures {
    fun g(name: String, amount: String) = Ingredient(name, amount, "g", role = IngredientRoles.guess(name))

    fun bread(id: String = "r1", water: String = "175") = Recipe(
        id = id,
        title = "山食パン",
        category = Category.BREAD,
        createdAt = 0,
        updatedAt = 0,
        versions = listOf(
            RecipeVersion(
                number = 1,
                createdAt = 0,
                ingredients = listOf(g("強力粉", "250"), g("砂糖", "15"), g("塩", "5"), g("ドライイースト", "3"), g("水", water)),
                process = BreadProcess(
                    targetDoughTemp = 27.0,
                    mixingMethod = MixingMethod.HAND,
                    firstProofMinutes = 60,
                    firstProofTemp = 28.0,
                    secondProofMinutes = 40,
                ),
            ),
        ),
    )

    private var seq = 0

    fun bakeLog(
        recipeId: String = "r1",
        rating: Int,
        room: Double?,
        humidity: Double? = null,
        water: String? = null,
        firstProof: Int? = null,
        firstProofTemp: Double? = null,
        date: Long = (++seq).toLong(),
    ) = CookLog(
        id = "log${++seq}",
        recipeId = recipeId,
        versionNumber = 1,
        date = date,
        rating = rating,
        bake = BakeRecord(
            roomTemp = room,
            humidity = humidity,
            ingredients = water?.let {
                listOf(g("強力粉", "250"), g("砂糖", "15"), g("塩", "5"), g("ドライイースト", "3"), g("水", it))
            }.orEmpty(),
            firstProofMinutes = firstProof,
            firstProofTemp = firstProofTemp,
        ),
    )
}
