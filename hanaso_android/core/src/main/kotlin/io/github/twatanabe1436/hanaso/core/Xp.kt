package io.github.twatanabe1436.hanaso.core

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * 練習でもらえるポイント (XP)。話した量と判定の両方で増える。
 * 週ごとのリーグはこの合計で競う (日本時間の月曜 0 時にリセット)。
 */
object Xp {
    /** リーグの週の区切りに使う時間帯 */
    val LEAGUE_ZONE: ZoneId = ZoneId.of("Asia/Tokyo")

    /** 台本を最後までクリアしたとき */
    const val SCRIPT_CLEAR = 20

    /** AI 会話でミッションをすべて達成したとき */
    const val ALL_MISSIONS = 20

    /** 1 週間に数える上限 (サーバー側のルールと同じ) */
    const val WEEKLY_CAP = 20_000

    /** 1 回話したとき (判定が良いほど多い) */
    fun forUtterance(rating: Rating): Int = when (rating) {
        Rating.GREAT -> 10
        Rating.GOOD -> 7
        Rating.FIX -> 3
    }

    /** 「言ってみる」・フラッシュカードの発音チェック */
    fun forPractice(score: Int): Int = when {
        score >= 90 -> 5
        score >= 60 -> 3
        else -> 1
    }

    /** 週の ID (例: "2026-W41")。ISO 週 (月曜はじまり) */
    fun weekId(now: Long = System.currentTimeMillis(), zone: ZoneId = LEAGUE_ZONE): String {
        val date = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return String.format(Locale.ROOT, "%d-W%02d", date.get(IsoFields.WEEK_BASED_YEAR), date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))
    }

    /** この週が終わる時刻 (次の月曜 0 時) */
    fun weekEndsAt(now: Long = System.currentTimeMillis(), zone: ZoneId = LEAGUE_ZONE): Long {
        val date = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return date.with(TemporalAdjusters.next(DayOfWeek.MONDAY)).atStartOfDay(zone).toInstant().toEpochMilli()
    }
}
