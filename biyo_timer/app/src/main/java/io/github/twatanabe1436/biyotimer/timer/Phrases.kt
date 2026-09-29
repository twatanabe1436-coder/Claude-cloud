package io.github.twatanabe1436.biyotimer.timer

/** 画面表示と読み上げに使う文言。 */
object Phrases {

    /** 例: 600 → "10分", 90 → "1分30秒", 30 → "30秒" */
    fun duration(totalSec: Int): String {
        val min = totalSec / 60
        val sec = totalSec % 60
        return when {
            min == 0 -> "${sec}秒"
            sec == 0 -> "${min}分"
            else -> "${min}分${sec}秒"
        }
    }

    /** 読み上げ文。例: 300 → "残り5分" */
    fun remaining(totalSec: Int): String = "残り${duration(totalSec)}"

    /** 例: 125 → "02:05" */
    fun clock(totalSec: Int): String {
        val s = totalSec.coerceAtLeast(0)
        return "${(s / 60).toString().padStart(2, '0')}:${(s % 60).toString().padStart(2, '0')}"
    }
}
