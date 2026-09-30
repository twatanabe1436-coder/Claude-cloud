package io.github.twatanabe1436.sodateru.core

import io.github.twatanabe1436.sodateru.core.Fixtures.bakeLog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BakeAdvisorTest {

    private val recipe = Fixtures.bread()

    private fun samples(vararg logs: io.github.twatanabe1436.sodateru.core.model.CookLog) =
        BakeSamples.from(listOf(recipe), logs.toList())

    @Test
    fun withoutLogsUsesRecipeDefaults() {
        val advice = BakeAdvisor.advise(recipe, emptyList(), emptyList(), TodayConditions(roomTemp = 25.0))
        assertEquals(Confidence.NONE, advice.confidence)
        assertNull(advice.hydration)
        assertEquals(70.0, advice.baseHydration!!, 1e-9)
        // (27 - 3) * 3 - (25 + 25) = 22
        assertEquals(22.0, advice.water.waterTemp, 1e-9)
        assertTrue(advice.rise.isDefault)
        // 発酵温度が空ならレシピと同じ 28℃ で発酵させるとみなすので、レシピどおり 60 分
        assertEquals(60.0, advice.firstProof!!.value)
        // 25℃ で発酵させるなら補正する: 60 * 2^(3/10) ≈ 73.9 → 75 分
        val at25 = BakeAdvisor.advise(recipe, emptyList(), emptyList(), TodayConditions(roomTemp = 25.0, firstProofTemp = 25.0))
        assertEquals(75.0, at25.firstProof!!.value)
        assertEquals(40.0, advice.secondProof!!.value)
        assertTrue(advice.notes.any { "焼成ログがまだありません" in it })
    }

    @Test
    fun hydrationLeansTowardSimilarGoodBakes() {
        val s = samples(
            bakeLog(rating = 5, room = 28.0, humidity = 75.0, water = "160"), // 64%
            bakeLog(rating = 4, room = 27.0, humidity = 70.0, water = "162.5"), // 65%
            bakeLog(rating = 5, room = 18.0, humidity = 40.0, water = "180"), // 72%
            bakeLog(rating = 1, room = 28.0, humidity = 75.0, water = "190"), // ★1 は使わない
        )
        val humid = BakeAdvisor.advise(recipe, s, emptyList(), TodayConditions(roomTemp = 28.0, humidity = 74.0))
        val h = humid.hydration!!
        assertTrue(h.value in 64.0..65.5, "humid day hydration ${h.value}")
        assertEquals(3, h.samples)
        assertNotNull(humid.adjustedIngredients)
        assertEquals(3, humid.references.size)
        assertEquals(28.0, humid.references.first().sample.bake.roomTemp)

        val dry = BakeAdvisor.advise(recipe, s, emptyList(), TodayConditions(roomTemp = 18.0, humidity = 40.0))
        assertTrue(dry.hydration!!.value > 71.0, "dry day hydration ${dry.hydration!!.value}")
    }

    @Test
    fun proofTimeIsTemperatureCorrected() {
        val s = samples(bakeLog(rating = 5, room = 20.0, firstProof = 90, firstProofTemp = null))
        // 20℃ で 90 分 → 30℃ なら半分の 45 分
        val warm = BakeAdvisor.advise(recipe, s, emptyList(), TodayConditions(roomTemp = 20.0, firstProofTemp = 30.0))
        assertEquals(45.0, warm.firstProof!!.value)
        assertEquals(1, warm.firstProof!!.samples)
    }

    @Test
    fun blankProofTempMeansUsualProofTempOfRecipe() {
        // レシピの一次発酵は 28℃。過去の回も 28℃ で 60 分なら、室温が低くても 28℃ で発酵させる前提で 60 分
        val s = samples(bakeLog(rating = 5, room = 25.0, firstProof = 60, firstProofTemp = 28.0))
        val cold = BakeAdvisor.advise(recipe, s, emptyList(), TodayConditions(roomTemp = 18.0))
        assertEquals(60.0, cold.firstProof!!.value)
        // 二次発酵は記録がなく、レシピにも温度がないので目安の 40 分のまま
        assertEquals(40.0, cold.secondProof!!.value)
    }

    @Test
    fun adjustProofMinutesFollowsQ10() {
        assertEquals(120.0, BakeAdvisor.adjustProofMinutes(60.0, 30.0, 20.0), 1e-9)
        assertEquals(60.0, BakeAdvisor.adjustProofMinutes(60.0, 25.0, 25.0), 1e-9)
    }

    @Test
    fun reportsTrendsWhenEnoughGoodBakes() {
        val s = samples(
            bakeLog(rating = 5, room = 25.0, humidity = 40.0, water = "180"),
            bakeLog(rating = 4, room = 25.0, humidity = 50.0, water = "177.5"),
            bakeLog(rating = 5, room = 25.0, humidity = 60.0, water = "172.5"),
            bakeLog(rating = 4, room = 25.0, humidity = 70.0, water = "167.5"),
            bakeLog(rating = 5, room = 25.0, humidity = 80.0, water = "165"),
        )
        val trends = BakeAdvisor.trends(s)
        assertTrue(trends.any { it.startsWith("うまくいった回では、湿度が10%上がると、加水率は約") && "低い傾向" in it }, trends.toString())
    }
}
