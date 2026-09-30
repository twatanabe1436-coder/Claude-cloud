package io.github.twatanabe1436.sodateru.core

import io.github.twatanabe1436.sodateru.core.model.BakeRecord
import io.github.twatanabe1436.sodateru.core.model.MixingMethod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DoughTemperatureTest {

    @Test
    fun waterTempStraightDough() {
        // (27 - 3) * 3 - (25 + 24) = 23
        val r = DoughTemperature.waterTemp(27.0, roomTemp = 25.0, flourTemp = 24.0, rise = 3.0)
        assertEquals(23.0, r.waterTemp, 1e-9)
        assertNull(r.warning)
    }

    @Test
    fun waterTempWithPreferment() {
        // (27 - 3) * 4 - (25 + 24 + 26) = 21
        val r = DoughTemperature.waterTemp(27.0, 25.0, 24.0, 3.0, prefermentTemp = 26.0)
        assertEquals(21.0, r.waterTemp, 1e-9)
    }

    @Test
    fun warnsOnExtremes() {
        assertNotNull(DoughTemperature.waterTemp(27.0, 32.0, 32.0, 6.0).warning) // 63-64 = -1
        assertNotNull(DoughTemperature.waterTemp(28.0, 10.0, 10.0, 1.0).warning) // 81-20 = 61
    }

    @Test
    fun learnsRiseFromRecords() {
        val records = listOf(
            // 26 - (24 + 24 + 21) / 3 = 3
            BakeRecord(roomTemp = 24.0, flourTemp = 24.0, waterTemp = 21.0, doughTemp = 26.0, mixingMethod = MixingMethod.HAND),
            // 粉温がなければ室温とみなす: 28 - (20 + 20 + 29) / 3 = 5
            BakeRecord(roomTemp = 20.0, waterTemp = 29.0, doughTemp = 28.0, mixingMethod = MixingMethod.HAND),
            // 27 - (25 + 25 + 10) / 3 = 7
            BakeRecord(roomTemp = 25.0, waterTemp = 10.0, doughTemp = 27.0, mixingMethod = MixingMethod.BREAD_MACHINE),
            // 上昇 30℃ は測り間違いとして除外
            BakeRecord(roomTemp = 20.0, waterTemp = 20.0, doughTemp = 50.0, mixingMethod = MixingMethod.HAND),
        )
        assertEquals(3.0, DoughTemperature.observedRise(records[0])!!, 1e-9)
        val hand = DoughTemperature.learnedRise(records, MixingMethod.HAND)
        assertEquals(2, hand.samples)
        assertEquals((3.0 + 5.0) / 2, hand.value, 1e-9)

        val mixer = DoughTemperature.learnedRise(records, MixingMethod.MIXER)
        assertTrue(mixer.isDefault)
        assertEquals(MixingMethod.MIXER.defaultRise, mixer.value)

        val any = DoughTemperature.learnedRise(records, null)
        assertEquals(3, any.samples)
    }
}
