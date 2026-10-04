package io.github.twatanabe1436.hanaso.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

object Stats {
    /** 今日 (または昨日) から遡って、会話した日が何日続いているか */
    fun streak(timestamps: List<Long>, now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): Int {
        fun day(ts: Long): LocalDate = Instant.ofEpochMilli(ts).atZone(zone).toLocalDate()
        val days = timestamps.map(::day).toSet()
        var cursor = day(now)
        if (cursor !in days) cursor = cursor.minusDays(1) // 今日まだでも昨日まで続いていれば継続
        var count = 0
        while (cursor in days) {
            count++
            cursor = cursor.minusDays(1)
        }
        return count
    }

    /** フレーズ帳の重複チェック用 (大文字小文字・記号を無視) */
    fun normalizePhrase(s: String): String = s.lowercase().replace(Regex("[^a-z0-9']+"), " ").trim()

    /** 「今日のおすすめ」: 自分のレベル (なければいちばん近いレベル) のシナリオから日替わりで選ぶ */
    fun todaysPick(level: Level, now: Long = System.currentTimeMillis()): Scenario {
        val nearest = Catalog.scenarios.minOf { s -> s.level?.let { abs(it.ordinal - level.ordinal) } ?: Int.MAX_VALUE }
        val pool = Catalog.scenarios.filter { s -> s.level?.let { abs(it.ordinal - level.ordinal) } == nearest }
        val day = Math.floorDiv(now, 86_400_000L)
        return pool[Math.floorMod(day, pool.size.toLong()).toInt()]
    }
}
