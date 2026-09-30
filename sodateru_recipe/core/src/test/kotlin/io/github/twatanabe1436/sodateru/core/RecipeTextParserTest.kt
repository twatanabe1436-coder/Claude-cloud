package io.github.twatanabe1436.sodateru.core

import io.github.twatanabe1436.sodateru.core.model.IngredientRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RecipeTextParserTest {

    @Test
    fun parsesSingleIngredientLines() {
        val a = RecipeTextParser.parseIngredientLine("強力粉 …… 250g")!!
        assertEquals("強力粉", a.name)
        assertEquals("250", a.amount)
        assertEquals("g", a.unit)
        assertEquals(IngredientRole.FLOUR, a.role)

        val b = RecipeTextParser.parseIngredientLine("・砂糖　大さじ１と１/２")!!
        assertEquals("砂糖", b.name)
        assertEquals("1と1/2", b.amount)
        assertEquals("大さじ", b.unit)

        val c = RecipeTextParser.parseIngredientLine("玉ねぎ 1/2個（みじん切り）")!!
        assertEquals("玉ねぎ", c.name)
        assertEquals("1/2", c.amount)
        assertEquals("個", c.unit)
        assertEquals("みじん切り", c.note)

        val d = RecipeTextParser.parseIngredientLine("塩こしょう 少々")!!
        assertEquals("塩こしょう", d.name)
        assertEquals("少々", d.amount)
        assertEquals("", d.unit)

        val e = RecipeTextParser.parseIngredientLine("水180ml")!!
        assertEquals("水", e.name)
        assertEquals("180", e.amount)
        assertEquals("ml", e.unit)

        val f = RecipeTextParser.parseIngredientLine("A 醤油 大匙2")!!
        assertEquals("醤油", f.name)
        assertEquals("大さじ", f.unit)

        assertNull(RecipeTextParser.parseIngredientLine("フライパンで焼く"))
        assertNull(RecipeTextParser.parseIngredientLine("250g"))
    }

    @Test
    fun parsesRecipeWithHeaders() {
        val text = """
            ふわふわ食パン
            材料（1斤分）
            強力粉 250g
            砂糖 20g
            塩 5g
            ドライイースト 3g
            水 180ml
            作り方
            1. 材料をすべてボウルに入れて
            こねる。
            2. 一次発酵を60分。
            ③ 焼く
            ポイント
            夏は水を冷やす
        """.trimIndent()
        val d = RecipeTextParser.parse(text)
        assertEquals("ふわふわ食パン", d.title)
        assertEquals("1斤分", d.servings)
        assertEquals(listOf("強力粉", "砂糖", "塩", "ドライイースト", "水"), d.ingredients.map { it.name })
        assertEquals(listOf("材料をすべてボウルに入れてこねる。", "一次発酵を60分。", "焼く"), d.steps)
        assertEquals("夏は水を冷やす", d.memo)
        assertEquals(text, d.transcript)
    }

    @Test
    fun matchesQuantitiesReadAsSeparateColumn() {
        val text = """
            【材料】2人分
            鶏もも肉
            醤油
            みりん
            300g
            大さじ2
            大さじ1
            【作り方】
            鶏肉を焼く
            タレを絡める
        """.trimIndent()
        val d = RecipeTextParser.parse(text)
        assertEquals("2人分", d.servings)
        assertEquals(listOf("300", "2", "1"), d.ingredients.map { it.amount })
        assertEquals(listOf("g", "大さじ", "大さじ"), d.ingredients.map { it.unit })
        assertEquals(listOf("鶏肉を焼く", "タレを絡める").joinToString(""), d.steps.joinToString(""))
    }

    @Test
    fun guessesSectionsWithoutHeaders() {
        val text = """
            豚の生姜焼き
            豚ロース 200g
            しょうが 1かけ
            1 豚肉に下味をつける
            2 焼いてタレを絡める
        """.trimIndent()
        val d = RecipeTextParser.parse(text)
        assertEquals("豚の生姜焼き", d.title)
        assertEquals(2, d.ingredients.size)
        assertEquals(listOf("豚肉に下味をつける", "焼いてタレを絡める"), d.steps)
    }
}
