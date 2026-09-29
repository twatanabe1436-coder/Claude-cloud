package io.github.twatanabe1436.biyotimer.timer

/** 画面上部のワンタップで時間を切り替えるボタン。 */
data class Preset(val name: String, val durationSec: Int)

object Presets {
    /** 実技試験の一般的な時間の目安。年度によって変わりうるので時間は自由に変更できる。 */
    val all: List<Preset> = listOf(
        Preset("カッティング", 20 * 60),
        Preset("ワインディング", 20 * 60),
        Preset("オールウェーブ", 25 * 60),
        Preset("準備", 7 * 60),
        Preset("拭き取り", 60),
    )
}

/** 保存されるユーザー設定。 */
data class TimerSettings(
    val durationSec: Int = 20 * 60,
    /** 最後に選んだプリセット名。時間を手入力したら null。 */
    val presetName: String? = Presets.all.first().name,
    val countdownSec: Int = 3,
    val countdownBeep: Boolean = true,
    val announceAtSec: Set<Int> = DEFAULT_ANNOUNCEMENTS,
    val startPhraseEnabled: Boolean = true,
    val startPhrase: String = DEFAULT_START_PHRASE,
    val endPhraseEnabled: Boolean = true,
    val endPhrase: String = DEFAULT_END_PHRASE,
    val speechRate: Float = 1.0f,
    val vibrateOnFinish: Boolean = true,
    val keepScreenOn: Boolean = true,
) {
    fun toConfig(): TimerConfig = TimerConfig(
        durationSec = durationSec,
        countdownSec = countdownSec,
        announceAtSec = announceAtSec,
    )

    /** 選択中のプリセット。名前と時間の両方が一致するときだけ返す。 */
    val activePreset: Preset?
        get() = Presets.all.firstOrNull { it.name == presetName && it.durationSec == durationSec }

    /** 値を有効範囲に収める。保存・読み込みのたびに通す。 */
    fun sanitized(): TimerSettings = copy(
        durationSec = durationSec.coerceIn(1, TimerConfig.MAX_DURATION_SEC),
        countdownSec = countdownSec.coerceIn(0, TimerConfig.MAX_COUNTDOWN_SEC),
        announceAtSec = announceAtSec.filter { it in 1..TimerConfig.MAX_DURATION_SEC }.toSet(),
        startPhrase = startPhrase.take(MAX_PHRASE_LENGTH),
        endPhrase = endPhrase.take(MAX_PHRASE_LENGTH),
        speechRate = speechRate.coerceIn(MIN_SPEECH_RATE, MAX_SPEECH_RATE),
    )

    companion object {
        val DEFAULT_ANNOUNCEMENTS: Set<Int> = setOf(10 * 60, 5 * 60, 60)

        /** 設定画面に常に並べる読み上げタイミングの候補 (秒)。 */
        val ANNOUNCEMENT_CHOICES: List<Int> =
            listOf(30 * 60, 20 * 60, 15 * 60, 10 * 60, 5 * 60, 3 * 60, 2 * 60, 60, 30, 10)

        val COUNTDOWN_CHOICES: List<Int> = listOf(0, 3, 5, 10)

        const val DEFAULT_START_PHRASE = "作業はじめ"
        const val DEFAULT_END_PHRASE = "作業やめ"
        const val MAX_PHRASE_LENGTH = 40
        const val MIN_SPEECH_RATE = 0.5f
        const val MAX_SPEECH_RATE = 1.5f

        fun encodeSeconds(values: Set<Int>): String = values.sortedDescending().joinToString(",")

        /** [encodeSeconds] の逆。null (未保存) なら null を返し、空文字は空集合として扱う。 */
        fun decodeSeconds(text: String?): Set<Int>? =
            text?.split(',')?.mapNotNull { it.trim().toIntOrNull() }?.filter { it > 0 }?.toSet()
    }
}
