package io.github.twatanabe1436.hanaso.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class StatsTest {
    private val zone = ZoneId.of("Asia/Tokyo")
    private val now = LocalDateTime.of(2026, 9, 30, 12, 0).atZone(zone).toInstant().toEpochMilli()
    private val day = 86_400_000L

    @Test
    fun streak() {
        assertEquals(0, Stats.streak(emptyList(), now, zone))
        assertEquals(3, Stats.streak(listOf(now, now - day, now - 2 * day), now, zone))
        // 今日はまだでも昨日まで続いていれば継続
        assertEquals(2, Stats.streak(listOf(now - day, now - 2 * day), now, zone))
        // 1日空いたらリセット
        assertEquals(1, Stats.streak(listOf(now, now - 2 * day), now, zone))
    }

    @Test
    fun normalizeAndPick() {
        assertEquals("i'd like a latte", Stats.normalizePhrase("I'd like a latte!"))
        val pick = Stats.todaysPick(Level.ADVANCED, now)
        assertEquals(Level.ADVANCED, pick.level)
    }
}
