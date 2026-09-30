package io.github.twatanabe1436.hanaso

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigatorTest {
    @Test
    fun tabsResetHistoryAndBackReturnsHome() {
        val nav = Navigator()
        assertEquals(Screen.Home, nav.current)
        assertFalse(nav.canGoBack)

        nav.tab(Screen.Scenarios)
        nav.go(Screen.Talk("cafe"))
        assertEquals(Screen.Talk("cafe"), nav.current)

        nav.replace(Screen.Summary)
        assertEquals(Screen.Summary, nav.current)
        assertTrue(nav.back())
        assertEquals(Screen.Scenarios, nav.current)
        assertTrue(nav.back())
        assertEquals(Screen.Home, nav.current)
        assertFalse(nav.back())
    }
}
