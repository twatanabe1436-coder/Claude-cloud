package io.github.twatanabe1436.sodateru.core

import io.github.twatanabe1436.sodateru.core.Fixtures.bakeLog
import io.github.twatanabe1436.sodateru.core.model.AppData
import io.github.twatanabe1436.sodateru.core.model.RecipeVersion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class GrowthAndCodecTest {

    @Test
    fun growthStagesFollowCooksAndVersions() {
        val recipe = Fixtures.bread()
        assertEquals(GrowthStage.SEED, GrowthCalculator.growth(recipe, emptyList()).stage)
        val oneCook = listOf(bakeLog(rating = 4, room = 25.0))
        val g1 = GrowthCalculator.growth(recipe, oneCook)
        assertEquals(GrowthStage.SPROUT, g1.stage)
        assertEquals(2, g1.pointsToNext)

        val v2 = recipe.copy(versions = recipe.versions + RecipeVersion(number = 2, createdAt = 1))
        val g2 = GrowthCalculator.growth(v2, oneCook)
        assertEquals(GrowthStage.LEAF, g2.stage)
        assertEquals(4, g2.bestRating)

        val many = (1..12).map { bakeLog(rating = 3, room = 25.0) }
        val g3 = GrowthCalculator.growth(recipe, many)
        assertEquals(GrowthStage.FRUIT, g3.stage)
        assertNull(g3.pointsToNext)
    }

    @Test
    fun backupRoundTripAndMerge() {
        val recipe = Fixtures.bread()
        val log = bakeLog(rating = 5, room = 24.0, humidity = 55.0, water = "170")
        val data = AppData(exportedAt = 10, recipes = listOf(recipe), logs = listOf(log))
        val decoded = DataCodec.decodeBackup(DataCodec.encodeBackup(data))
        assertEquals(data, decoded)

        val renamed = recipe.copy(title = "新しい名前", updatedAt = 5)
        val merged = DataCodec.merge(AppData(recipes = listOf(renamed)), data)
        assertEquals("新しい名前", merged.recipes.single().title)
        assertEquals(1, merged.logs.size)

        val newer = recipe.copy(title = "バックアップ側", updatedAt = 9)
        val merged2 = DataCodec.merge(AppData(recipes = listOf(renamed)), AppData(recipes = listOf(newer)))
        assertEquals("バックアップ側", merged2.recipes.single().title)
    }

    @Test
    fun ignoresUnknownFieldsAndRejectsGarbage() {
        val json = """{"schemaVersion":1,"recipes":[],"logs":[],"futureField":true}"""
        assertEquals(AppData(), DataCodec.decodeBackup(json).copy(exportedAt = 0))
        assertFailsWith<BackupFormatException> { DataCodec.decodeBackup("not json") }
        assertFailsWith<BackupFormatException> { DataCodec.decodeBackup("""{"schemaVersion":99}""") }
    }
}
