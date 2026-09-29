package io.github.twatanabe1436.biyotimer.timer

import io.github.twatanabe1436.biyotimer.timer.TimerEvent.CountdownTick
import io.github.twatanabe1436.biyotimer.timer.TimerEvent.Finished
import io.github.twatanabe1436.biyotimer.timer.TimerEvent.Remaining
import io.github.twatanabe1436.biyotimer.timer.TimerEvent.Started
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamTimerTest {

    private val t0 = 1_000_000L

    private fun examTimer(
        durationSec: Int = 20 * 60,
        countdownSec: Int = 3,
        announce: Set<Int> = setOf(600, 300, 60),
    ) = ExamTimer(TimerConfig(durationSec, countdownSec, announce))

    /** [from] から [to] まで 50ms 刻みで tick したときのイベントを時刻付きで集める。 */
    private fun ExamTimer.run(from: Long, to: Long, step: Long = 50L): List<Pair<Long, TimerEvent>> {
        val out = mutableListOf<Pair<Long, TimerEvent>>()
        var now = from
        while (now <= to) {
            tick(now).forEach { out += now to it }
            now += step
        }
        return out
    }

    @Test
    fun countdownTicksEverySecondThenStarts() {
        val timer = examTimer()
        assertEquals(listOf(CountdownTick(3)), timer.start(t0))
        assertEquals(Phase.COUNTDOWN, timer.phase)

        val events = timer.run(t0 + 50, t0 + 3_000)
        assertEquals(
            listOf(
                t0 + 1_000 to CountdownTick(2),
                t0 + 2_000 to CountdownTick(1),
                t0 + 3_000 to Started,
            ),
            events,
        )
        assertEquals(Phase.RUNNING, timer.phase)
    }

    @Test
    fun countdownSnapshotShowsSecondsLeft() {
        val timer = examTimer()
        timer.start(t0)
        assertEquals(3, timer.snapshot(t0).countdownSecondsLeft)
        assertEquals(3, timer.snapshot(t0 + 999).countdownSecondsLeft)
        assertEquals(2, timer.snapshot(t0 + 1_000).countdownSecondsLeft)
        assertEquals(1, timer.snapshot(t0 + 2_999).countdownSecondsLeft)
        // 作業時間はまだ減っていない
        assertEquals(20 * 60 * 1000L, timer.snapshot(t0 + 2_000).remainingMs)
    }

    @Test
    fun zeroCountdownStartsImmediately() {
        val timer = examTimer(countdownSec = 0)
        assertEquals(listOf(Started), timer.start(t0))
        assertEquals(Phase.RUNNING, timer.phase)
        assertEquals(20 * 60 * 1000L - 500, timer.snapshot(t0 + 500).remainingMs)
    }

    @Test
    fun lateTickDuringCountdownAnchorsStartAtCountdownEnd() {
        val timer = examTimer()
        timer.start(t0)
        assertEquals(listOf(Started), timer.tick(t0 + 3_400))
        // 作業開始は t0+3000 とみなすので、すでに 400ms 経過している
        assertEquals(20 * 60 * 1000L - 400, timer.snapshot(t0 + 3_400).remainingMs)
    }

    @Test
    fun announcesRemainingTimesAndFinishes() {
        val timer = examTimer(countdownSec = 0)
        timer.start(t0)
        val events = timer.run(t0, t0 + 20 * 60 * 1000L + 1_000)
        assertEquals(
            listOf(
                t0 + 10 * 60 * 1000L to Remaining(600),
                t0 + 15 * 60 * 1000L to Remaining(300),
                t0 + 19 * 60 * 1000L to Remaining(60),
                t0 + 20 * 60 * 1000L to Finished,
            ),
            events,
        )
        assertEquals(Phase.FINISHED, timer.phase)
        assertEquals(0L, timer.snapshot(t0 + 21 * 60 * 1000L).remainingMs)
        assertTrue(timer.tick(t0 + 22 * 60 * 1000L).isEmpty())
    }

    @Test
    fun announcementJustBeforeThresholdDoesNotFire() {
        val timer = examTimer(countdownSec = 0, announce = setOf(600))
        timer.start(t0)
        assertTrue(timer.tick(t0 + 600_000L - 1).isEmpty())
        assertEquals(listOf(Remaining(600)), timer.tick(t0 + 600_000L))
        assertTrue(timer.tick(t0 + 600_050L).isEmpty())
    }

    @Test
    fun announcementsAtOrAboveDurationAreIgnored() {
        // 7 分のタイマーでは「残り10分」は読み上げない。作業時間ちょうどの値も開始直後に鳴らさない。
        val timer = examTimer(durationSec = 7 * 60, countdownSec = 0, announce = setOf(600, 420, 300))
        timer.start(t0)
        assertEquals(300, timer.snapshot(t0).nextAnnouncementSec)
        val events = timer.run(t0, t0 + 7 * 60 * 1000L).map { it.second }
        assertEquals(listOf(Remaining(300), Finished), events)
    }

    @Test
    fun pauseFreezesTimeAndResumeContinues() {
        val timer = examTimer(countdownSec = 0, announce = setOf(60))
        timer.start(t0)
        timer.pause(t0 + 60_000L)
        assertEquals(Phase.PAUSED, timer.phase)
        val frozen = timer.snapshot(t0 + 60_000L).remainingMs
        assertEquals(frozen, timer.snapshot(t0 + 500_000L).remainingMs)
        assertTrue(timer.tick(t0 + 500_000L).isEmpty())

        // 5 分止めてから再開。残り 1 分の読み上げは再開後 18 分で来る。
        val resumeAt = t0 + 360_000L
        timer.resume(resumeAt)
        assertEquals(Phase.RUNNING, timer.phase)
        val events = timer.run(resumeAt, resumeAt + 19 * 60 * 1000L)
        assertEquals(
            listOf(
                resumeAt + 18 * 60 * 1000L to Remaining(60),
                resumeAt + 19 * 60 * 1000L to Finished,
            ),
            events,
        )
    }

    @Test
    fun pauseReportsAnnouncementThatWasDue() {
        val timer = examTimer(countdownSec = 0, announce = setOf(600))
        timer.start(t0)
        assertEquals(listOf(Remaining(600)), timer.pause(t0 + 600_000L))
        assertEquals(Phase.PAUSED, timer.phase)
    }

    @Test
    fun pauseIsIgnoredOutsideRunning() {
        val timer = examTimer()
        timer.start(t0)
        assertTrue(timer.pause(t0 + 500).isEmpty())
        assertEquals(Phase.COUNTDOWN, timer.phase)
    }

    @Test
    fun delayedTickAnnouncesOnlyLatestCrossedTime() {
        val timer = examTimer(countdownSec = 0)
        timer.start(t0)
        // 残り 4 分の時点まで tick が来なかった: 10分・5分は飛ばして 5 分だけ読む
        assertEquals(listOf(Remaining(300)), timer.tick(t0 + 16 * 60 * 1000L))
        assertEquals(60, timer.snapshot(t0 + 16 * 60 * 1000L).nextAnnouncementSec)
    }

    @Test
    fun delayedTickPastTheEndOnlyFinishes() {
        val timer = examTimer(countdownSec = 0)
        timer.start(t0)
        assertEquals(listOf(Finished), timer.tick(t0 + 30 * 60 * 1000L))
    }

    @Test
    fun displaySecondsRoundRemainingUpAndElapsedDown() {
        val timer = examTimer(durationSec = 600, countdownSec = 0)
        timer.start(t0)
        val atStart = timer.snapshot(t0 + 1)
        assertEquals(600, atStart.remainingDisplaySec)
        assertEquals(0, atStart.elapsedDisplaySec)
        val afterOneSecond = timer.snapshot(t0 + 1_000)
        assertEquals(599, afterOneSecond.remainingDisplaySec)
        assertEquals(1, afterOneSecond.elapsedDisplaySec)
        assertEquals(1f, timer.snapshot(t0).remainingFraction)
        assertEquals(0.5f, timer.snapshot(t0 + 300_000).remainingFraction)
    }

    @Test
    fun nextAnnouncementAdvances() {
        val timer = examTimer(countdownSec = 0)
        assertEquals(600, timer.snapshot(t0).nextAnnouncementSec)
        timer.start(t0)
        timer.tick(t0 + 10 * 60 * 1000L)
        assertEquals(300, timer.snapshot(t0 + 10 * 60 * 1000L).nextAnnouncementSec)
        timer.tick(t0 + 20 * 60 * 1000L)
        assertNull(timer.snapshot(t0 + 20 * 60 * 1000L).nextAnnouncementSec)
    }

    @Test
    fun resetDuringCountdownReturnsToIdle() {
        val timer = examTimer()
        timer.start(t0)
        timer.reset()
        assertEquals(Phase.IDLE, timer.phase)
        assertTrue(timer.tick(t0 + 5_000).isEmpty())
        assertEquals(20 * 60 * 1000L, timer.snapshot(t0 + 5_000).remainingMs)
    }

    @Test
    fun canRestartAfterFinishing() {
        val timer = examTimer(durationSec = 5, countdownSec = 0, announce = emptySet())
        timer.start(t0)
        assertEquals(listOf(Finished), timer.tick(t0 + 5_000))
        assertEquals(listOf(Started), timer.start(t0 + 10_000))
        assertEquals(5_000L - 1_000, timer.snapshot(t0 + 11_000).remainingMs)
    }

    @Test
    fun startIsIgnoredWhileRunning() {
        val timer = examTimer(countdownSec = 0)
        timer.start(t0)
        assertTrue(timer.start(t0 + 1_000).isEmpty())
        assertEquals(20 * 60 * 1000L - 2_000, timer.snapshot(t0 + 2_000).remainingMs)
    }

    @Test(expected = IllegalStateException::class)
    fun configCannotChangeWhileRunning() {
        val timer = examTimer(countdownSec = 0)
        timer.start(t0)
        timer.updateConfig(TimerConfig(60))
    }

    @Test
    fun updateConfigWhenFinishedGoesBackToIdle() {
        val timer = examTimer(durationSec = 5, countdownSec = 0)
        timer.start(t0)
        timer.tick(t0 + 5_000)
        timer.updateConfig(TimerConfig(90, countdownSec = 3, announceAtSec = setOf(60, 30)))
        assertEquals(Phase.IDLE, timer.phase)
        assertEquals(90_000L, timer.snapshot(t0 + 6_000).remainingMs)
        assertEquals(60, timer.snapshot(t0 + 6_000).nextAnnouncementSec)
    }

    @Test(expected = IllegalArgumentException::class)
    fun zeroDurationIsRejected() {
        TimerConfig(0)
    }
}
