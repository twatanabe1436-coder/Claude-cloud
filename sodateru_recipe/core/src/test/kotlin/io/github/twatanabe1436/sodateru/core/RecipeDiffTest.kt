package io.github.twatanabe1436.sodateru.core

import io.github.twatanabe1436.sodateru.core.model.Ingredient
import io.github.twatanabe1436.sodateru.core.model.RecipeVersion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RecipeDiffTest {

    private val v1 = RecipeVersion(
        number = 1,
        createdAt = 0,
        servings = "2人分",
        ingredients = listOf(
            Ingredient("豚こま肉", "200", "g"),
            Ingredient("玉ねぎ", "1/2", "個"),
            Ingredient("砂糖", "2", "大さじ"),
            Ingredient("醤油", "2", "大さじ"),
        ),
        steps = listOf("玉ねぎを切る", "肉を炒める", "調味料を入れて煮る"),
    )

    @Test
    fun detectsIngredientChanges() {
        val v2 = v1.copy(
            number = 2,
            ingredients = listOf(
                Ingredient("豚こま肉", "200", "g"),
                Ingredient("砂糖", "1.5", "大さじ"),
                Ingredient("醤油", "2", "大さじ"),
                Ingredient("しょうが", "1", "かけ"),
            ),
            steps = listOf("肉を炒める", "しょうがを加える", "調味料を入れて煮る"),
        )
        val d = RecipeDiff.diff(v1, v2)
        assertIs<IngredientChange.Same>(d.ingredients[0])
        assertIs<IngredientChange.Removed>(d.ingredients[1])
        val changed = assertIs<IngredientChange.Changed>(d.ingredients[2])
        assertEquals("2", changed.before.amount)
        assertEquals("1.5", changed.after.amount)
        assertIs<IngredientChange.Added>(d.ingredients[4])
        assertEquals(3, d.changedIngredientCount)

        assertEquals(
            listOf(LineChangeType.REMOVED, LineChangeType.SAME, LineChangeType.ADDED, LineChangeType.SAME),
            d.steps.map { it.type },
        )
        assertTrue(d.hasChanges)
        assertEquals("玉ねぎをなくした、砂糖 大さじ2→大さじ1.5、しょうがを追加 ほか1件", d.summary())
    }

    @Test
    fun sameVersionHasNoChanges() {
        val d = RecipeDiff.diff(v1, v1.copy(number = 2))
        assertFalse(d.hasChanges)
        assertEquals("変更なし", d.summary())
    }

    @Test
    fun servingsChangeIsReported() {
        val d = RecipeDiff.diff(v1, v1.copy(servings = "4人分"))
        assertEquals("2人分" to "4人分", d.servings)
        assertEquals("分量 2人分→4人分", d.summary())
    }
}
