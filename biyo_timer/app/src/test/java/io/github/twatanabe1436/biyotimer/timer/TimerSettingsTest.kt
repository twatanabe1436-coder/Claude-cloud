package io.github.twatanabe1436.biyotimer.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimerSettingsTest {

    @Test
    fun secondsRoundTrip() {
        val values = setOf(60, 600, 300)
        assertEquals("600,300,60", TimerSettings.encodeSeconds(values))
        assertEquals(values, TimerSettings.decodeSeconds("600,300,60"))
    }

    @Test
    fun decodeDistinguishesMissingFromEmpty() {
        assertNull(TimerSettings.decodeSeconds(null))
        assertEquals(emptySet<Int>(), TimerSettings.decodeSeconds(""))
        assertEquals(setOf(30), TimerSettings.decodeSeconds(" 30, x, -5,0"))
    }

    @Test
    fun sanitizedClampsValues() {
        val s = TimerSettings(
            durationSec = 200 * 60,
            countdownSec = -1,
            announceAtSec = setOf(0, 60, 999_999),
            speechRate = 9f,
            startPhrase = "あ".repeat(100),
        ).sanitized()
        assertEquals(TimerConfig.MAX_DURATION_SEC, s.durationSec)
        assertEquals(0, s.countdownSec)
        assertEquals(setOf(60), s.announceAtSec)
        assertEquals(TimerSettings.MAX_SPEECH_RATE, s.speechRate)
        assertEquals(TimerSettings.MAX_PHRASE_LENGTH, s.startPhrase.length)
    }

    @Test
    fun activePresetNeedsMatchingNameAndDuration() {
        assertEquals("カッティング", TimerSettings().activePreset?.name)
        // カッティングとワインディングは同じ 20 分なので、名前で区別する
        assertEquals("ワインディング", TimerSettings(presetName = "ワインディング").activePreset?.name)
        assertNull(TimerSettings(durationSec = 19 * 60).activePreset)
        assertNull(TimerSettings(presetName = null).activePreset)
    }

    @Test
    fun defaultsMatchExamAnnouncements() {
        val config = TimerSettings().toConfig()
        assertEquals(20 * 60, config.durationSec)
        assertEquals(3, config.countdownSec)
        assertEquals(setOf(600, 300, 60), config.announceAtSec)
    }
}
