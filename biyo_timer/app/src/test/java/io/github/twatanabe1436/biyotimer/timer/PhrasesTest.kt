package io.github.twatanabe1436.biyotimer.timer

import org.junit.Assert.assertEquals
import org.junit.Test

class PhrasesTest {

    @Test
    fun remainingPhrases() {
        assertEquals("残り10分", Phrases.remaining(600))
        assertEquals("残り1分", Phrases.remaining(60))
        assertEquals("残り30秒", Phrases.remaining(30))
        assertEquals("残り1分30秒", Phrases.remaining(90))
    }

    @Test
    fun clockFormat() {
        assertEquals("20:00", Phrases.clock(1200))
        assertEquals("00:05", Phrases.clock(5))
        assertEquals("99:59", Phrases.clock(99 * 60 + 59))
        assertEquals("00:00", Phrases.clock(-3))
    }
}
