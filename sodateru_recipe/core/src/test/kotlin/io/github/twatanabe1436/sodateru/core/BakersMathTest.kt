package io.github.twatanabe1436.sodateru.core

import io.github.twatanabe1436.sodateru.core.model.Ingredient
import io.github.twatanabe1436.sodateru.core.model.IngredientRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class BakersMathTest {

    private fun ing(name: String, amount: String, unit: String = "g") =
        Ingredient(name, amount, unit, role = IngredientRoles.guess(name))

    private val shokupan = listOf(
        ing("強力粉", "250"),
        ing("砂糖", "20"),
        ing("塩", "5"),
        ing("ドライイースト", "3"),
        ing("バター", "15"),
        ing("水", "175"),
        Ingredient("卵", "1", "個"),
    )

    @Test
    fun guessesRoles() {
        assertEquals(IngredientRole.FLOUR, IngredientRoles.guess("強力粉"))
        assertEquals(IngredientRole.FLOUR, IngredientRoles.guess("全粒粉"))
        assertEquals(IngredientRole.FLOUR, IngredientRoles.guess("リスドォル"))
        assertEquals(IngredientRole.SUGAR, IngredientRoles.guess("粉糖"))
        assertEquals(IngredientRole.OTHER, IngredientRoles.guess("片栗粉"))
        assertEquals(IngredientRole.OTHER, IngredientRoles.guess("スキムミルク"))
        assertEquals(IngredientRole.YEAST, IngredientRoles.guess("インスタントドライイースト"))
        assertEquals(IngredientRole.SALT, IngredientRoles.guess("塩"))
        assertEquals(IngredientRole.OTHER, IngredientRoles.guess("塩こしょう"))
        assertEquals(IngredientRole.FAT, IngredientRoles.guess("無塩バター"))
        assertEquals(IngredientRole.FAT, IngredientRoles.guess("オリーブオイル"))
        assertEquals(IngredientRole.OTHER, IngredientRoles.guess("醤油"))
        assertEquals(IngredientRole.LIQUID, IngredientRoles.guess("水"))
        assertEquals(IngredientRole.LIQUID, IngredientRoles.guess("牛乳"))
        assertEquals(IngredientRole.SUGAR, IngredientRoles.guess("水あめ"))
        assertEquals(IngredientRole.OTHER, IngredientRoles.guess("水菜"))
        assertEquals(IngredientRole.OTHER, IngredientRoles.guess("卵"))
    }

    @Test
    fun summarizesBakersPercent() {
        val s = BakersMath.summarize(shokupan)
        assertEquals(250.0, s.flourGrams)
        assertEquals(468.0, s.totalGrams)
        assertEquals(70.0, s.hydration!!, 1e-9)
        assertEquals(2.0, s.salt!!, 1e-9)
        assertEquals(1.2, s.yeast!!, 1e-9)
        assertEquals(8.0, s.sugar!!, 1e-9)
        assertEquals(6.0, s.fat!!, 1e-9)
        assertEquals(100.0, s.lines[0].percent!!, 1e-9)
        assertNull(s.lines[6].grams)
        assertEquals(1, s.unconvertedCount)
    }

    @Test
    fun noFlourMeansNoPercent() {
        val s = BakersMath.summarize(listOf(ing("砂糖", "10")))
        assertFalse(s.hasFlour)
        assertNull(s.hydration)
        assertNull(s.lines[0].percent)
    }

    @Test
    fun scalesToFlourAndTotal() {
        val scaled = BakersMath.scaleToFlour(shokupan, 300.0)
        assertEquals("300", scaled[0].amount)
        assertEquals("6", scaled[2].amount)
        assertEquals("3.6", scaled[3].amount)
        assertEquals("210", scaled[5].amount)
        assertEquals("1.2", scaled[6].amount)

        val total = BakersMath.scaleToTotal(shokupan, 936.0)
        assertEquals("500", total[0].amount)
    }

    @Test
    fun adjustsHydrationOnlyOnLiquids() {
        val adjusted = BakersMath.withHydration(shokupan, 66.0)
        assertEquals("165", adjusted[5].amount)
        assertEquals("250", adjusted[0].amount)
        assertEquals(66.0, BakersMath.hydration(adjusted)!!, 1e-9)
    }
}
