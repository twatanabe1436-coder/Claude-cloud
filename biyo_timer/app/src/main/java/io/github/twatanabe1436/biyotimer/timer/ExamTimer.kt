package io.github.twatanabe1436.biyotimer.timer

/**
 * 1 回の計測に使う設定。時間はすべて秒。
 *
 * @property durationSec 作業時間。
 * @property countdownSec スタートを押してから作業開始までの猶予 (0 ならすぐ開始)。
 * @property announceAtSec 読み上げを行う「残り時間」の一覧。作業時間以上の値は無視する。
 */
data class TimerConfig(
    val durationSec: Int,
    val countdownSec: Int = 3,
    val announceAtSec: Set<Int> = emptySet(),
) {
    init {
        require(durationSec in 1..MAX_DURATION_SEC) { "durationSec out of range: $durationSec" }
        require(countdownSec in 0..MAX_COUNTDOWN_SEC) { "countdownSec out of range: $countdownSec" }
    }

    companion object {
        /** 表示が MM:SS の 2 桁に収まる上限 (99:59)。 */
        const val MAX_DURATION_SEC = 99 * 60 + 59
        const val MAX_COUNTDOWN_SEC = 60
    }
}

enum class Phase { IDLE, COUNTDOWN, RUNNING, PAUSED, FINISHED }

sealed interface TimerEvent {
    /** 開始前カウントダウンの 1 秒ごとの合図。[secondsLeft] は 3, 2, 1 のように減っていく。 */
    data class CountdownTick(val secondsLeft: Int) : TimerEvent

    /** 作業開始。 */
    data object Started : TimerEvent

    /** 残り時間の読み上げタイミングに達した。 */
    data class Remaining(val seconds: Int) : TimerEvent

    /** 作業時間が終わった。 */
    data object Finished : TimerEvent
}

data class TimerSnapshot(
    val phase: Phase,
    val durationMs: Long,
    val remainingMs: Long,
    /** [Phase.COUNTDOWN] のときの表示用の残り秒。それ以外は 0。 */
    val countdownSecondsLeft: Int,
    /** 次に読み上げる残り時間 (秒)。もう無ければ null。 */
    val nextAnnouncementSec: Int?,
) {
    val elapsedMs: Long get() = durationMs - remainingMs

    /** 表示用の残り秒。切り上げなので 20:00 は開始から 1 秒経つまで表示される。 */
    val remainingDisplaySec: Int get() = ((remainingMs + 999) / 1000).toInt()

    /** 表示用の経過秒 (切り捨て)。remainingDisplaySec との合計は常に作業時間になる。 */
    val elapsedDisplaySec: Int get() = (durationMs / 1000).toInt() - remainingDisplaySec

    /** 残りの割合 (1 → 0)。 */
    val remainingFraction: Float
        get() = if (durationMs <= 0L) 0f else (remainingMs.toFloat() / durationMs).coerceIn(0f, 1f)
}

/**
 * 実技試験向けのカウントダウンタイマー本体。
 *
 * Android に依存しない純粋なロジックで、現在時刻 (単調増加するミリ秒) を引数で受け取る。
 * 呼び出し側は [tick] を短い間隔で呼び、返ってきたイベントに応じて音声を鳴らす。
 * スレッドセーフではないので、同じスレッドから操作すること。
 */
class ExamTimer(config: TimerConfig) {

    var config: TimerConfig = config
        private set

    var phase: Phase = Phase.IDLE
        private set

    /** 読み上げ対象の残り秒 (降順)。作業時間以上のものは除外済み。 */
    private var announcements: List<Int> = announcementsFor(config)
    private var nextAnnouncementIndex = 0

    private var countdownStartMs = 0L
    private var lastCountdownTick = 0

    /** 現在の走行区間の開始時刻。 */
    private var segmentStartMs = 0L

    /** 一時停止までに走った合計時間。 */
    private var accumulatedMs = 0L

    private val durationMs: Long get() = config.durationSec * 1000L

    /** 待機中か終了後だけ設定を差し替えられる。差し替えると待機状態に戻る。 */
    fun updateConfig(newConfig: TimerConfig) {
        check(phase == Phase.IDLE || phase == Phase.FINISHED) { "cannot change config while $phase" }
        config = newConfig
        announcements = announcementsFor(newConfig)
        reset()
    }

    fun start(nowMs: Long): List<TimerEvent> {
        if (phase != Phase.IDLE && phase != Phase.FINISHED) return emptyList()
        accumulatedMs = 0L
        nextAnnouncementIndex = 0
        if (config.countdownSec == 0) {
            beginRunning(nowMs)
            return listOf(TimerEvent.Started)
        }
        phase = Phase.COUNTDOWN
        countdownStartMs = nowMs
        lastCountdownTick = config.countdownSec
        return listOf(TimerEvent.CountdownTick(config.countdownSec))
    }

    /** 作業中のみ一時停止できる。停止直前までに来ていた読み上げは返り値で通知する。 */
    fun pause(nowMs: Long): List<TimerEvent> {
        if (phase != Phase.RUNNING) return emptyList()
        val events = tick(nowMs)
        if (phase == Phase.RUNNING) {
            accumulatedMs += nowMs - segmentStartMs
            phase = Phase.PAUSED
        }
        return events
    }

    fun resume(nowMs: Long) {
        if (phase != Phase.PAUSED) return
        segmentStartMs = nowMs
        phase = Phase.RUNNING
    }

    fun reset() {
        phase = Phase.IDLE
        accumulatedMs = 0L
        nextAnnouncementIndex = 0
        lastCountdownTick = 0
    }

    fun tick(nowMs: Long): List<TimerEvent> = when (phase) {
        Phase.COUNTDOWN -> tickCountdown(nowMs)
        Phase.RUNNING -> tickRunning(nowMs)
        else -> emptyList()
    }

    fun snapshot(nowMs: Long): TimerSnapshot {
        val remaining = durationMs - elapsedMs(nowMs)
        val countdownLeft = if (phase == Phase.COUNTDOWN) {
            val left = config.countdownSec - ((nowMs - countdownStartMs) / 1000L).toInt()
            left.coerceIn(1, config.countdownSec)
        } else {
            0
        }
        return TimerSnapshot(
            phase = phase,
            durationMs = durationMs,
            remainingMs = remaining,
            countdownSecondsLeft = countdownLeft,
            nextAnnouncementSec = announcements.getOrNull(nextAnnouncementIndex),
        )
    }

    private fun tickCountdown(nowMs: Long): List<TimerEvent> {
        val countdownMs = config.countdownSec * 1000L
        val elapsed = nowMs - countdownStartMs
        if (elapsed >= countdownMs) {
            // 遅れて tick が来ても、作業開始はカウントダウン終了の瞬間とする。
            beginRunning(countdownStartMs + countdownMs)
            return listOf(TimerEvent.Started) + tickRunning(nowMs)
        }
        val left = config.countdownSec - (elapsed / 1000L).toInt()
        if (left < lastCountdownTick) {
            lastCountdownTick = left
            return listOf(TimerEvent.CountdownTick(left))
        }
        return emptyList()
    }

    private fun tickRunning(nowMs: Long): List<TimerEvent> {
        val remaining = durationMs - elapsedMs(nowMs)
        if (remaining <= 0L) {
            accumulatedMs = durationMs
            nextAnnouncementIndex = announcements.size
            phase = Phase.FINISHED
            return listOf(TimerEvent.Finished)
        }
        // tick が大きく遅れて複数の読み上げ時刻をまたいだ場合は、最新のものだけを読み上げる。
        var crossed: Int? = null
        while (nextAnnouncementIndex < announcements.size &&
            announcements[nextAnnouncementIndex] * 1000L >= remaining
        ) {
            crossed = announcements[nextAnnouncementIndex]
            nextAnnouncementIndex++
        }
        return if (crossed != null) listOf(TimerEvent.Remaining(crossed)) else emptyList()
    }

    private fun beginRunning(startMs: Long) {
        phase = Phase.RUNNING
        segmentStartMs = startMs
        accumulatedMs = 0L
    }

    private fun elapsedMs(nowMs: Long): Long {
        val elapsed = when (phase) {
            Phase.RUNNING -> accumulatedMs + (nowMs - segmentStartMs)
            Phase.PAUSED -> accumulatedMs
            Phase.FINISHED -> durationMs
            Phase.IDLE, Phase.COUNTDOWN -> 0L
        }
        return elapsed.coerceIn(0L, durationMs)
    }

    private companion object {
        fun announcementsFor(config: TimerConfig): List<Int> =
            config.announceAtSec.filter { it in 1 until config.durationSec }.sortedDescending()
    }
}
