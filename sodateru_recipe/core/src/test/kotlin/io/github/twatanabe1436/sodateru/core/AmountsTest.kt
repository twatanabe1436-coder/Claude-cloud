package io.github.twatanabe1436.sodateru.core

import io.github.twatanabe1436.sodateru.core.model.Ingredient
import io.github.twatanabe1436.sodateru.core.model.IngredientRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AmountsTest {

    @Test
    fun parsesNumbersFractionsAndFullWidth() {
        assertEquals(200.0, Amounts.parse("200"))
        assertEquals(1.5, Amounts.parse("1.5"))
        assertEquals(0.5, Amounts.parse("1/2"))
        assertEquals(1.5, Amounts.parse("1と1/2"))
        assertEquals(250.0, Amounts.parse("２５０"))
        assertEquals(0.5, Amounts.parse("½"))
        assertEquals(1.5, Amounts.parse("1½"))
        assertEquals(0.5, Amounts.parse("1/2強"))
        assertNull(Amounts.parse("少々"))
        assertNull(Amounts.parse("2〜3"))
        assertNull(Amounts.parse(""))
        assertNull(Amounts.parse("1/0"))
    }

    @Test
    fun parsesRanges() {
        assertEquals(2.0 to 3.0, Amounts.parseRange("2〜3"))
        assertEquals(2.0 to 3.0, Amounts.parseRange("2~3"))
        assertEquals(0.5 to 1.0, Amounts.parseRange("1/2-1"))
        assertNull(Amounts.parseRange("3"))
    }

    @Test
    fun formatsFractionsForSpoonsAndDecimalsForGrams() {
        assertEquals("1/2", Amounts.format(0.5, "大さじ"))
        assertEquals("1と1/2", Amounts.format(1.5, "大さじ"))
        assertEquals("1/3", Amounts.format(0.34, "個"))
        assertEquals("2", Amounts.format(2.0, "個"))
        assertEquals("1", Amounts.format(0.98, "個"))
        assertEquals("375", Amounts.format(375.2, "g"))
        assertEquals("12.5", Amounts.format(12.5, "g"))
        assertEquals("2.8", Amounts.format(2.83, "g"))
        assertEquals("0.75", Amounts.format(0.75, "g"))
    }

    @Test
    fun scalesAmounts() {
        assertEquals("375", Amounts.scale("250", 1.5, "g"))
        assertEquals("3/4", Amounts.scale("1/2", 1.5, "大さじ"))
        assertEquals("3〜4.5", Amounts.scale("2〜3", 1.5, "g"))
        assertEquals("少々", Amounts.scale("少々", 2.0, ""))
    }

    @Test
    fun displaysPrefixUnitsFirst() {
        assertEquals("大さじ1", Amounts.display(Ingredient("砂糖", "1", "大さじ")))
        assertEquals("200g", Amounts.display(Ingredient("強力粉", "200", "g")))
        assertEquals("少々", Amounts.display(Ingredient("塩", "少々", "")))
        assertEquals("", Amounts.display(Ingredient("水", "", "")))
    }

    @Test
    fun convertsToGrams() {
        assertEquals(250.0, Amounts.grams(Ingredient("強力粉", "250", "g")))
        assertEquals(1000.0, Amounts.grams(Ingredient("強力粉", "1", "kg")))
        assertEquals(180.0, Amounts.grams(Ingredient("水", "180", "ml", role = IngredientRole.LIQUID)))
        assertEquals(15.0, Amounts.grams(Ingredient("牛乳", "1", "大さじ", role = IngredientRole.LIQUID)))
        assertNull(Amounts.grams(Ingredient("砂糖", "1", "大さじ", role = IngredientRole.SUGAR)))
        assertNull(Amounts.grams(Ingredient("卵", "1", "個")))
    }
}
