package io.github.salihoguz.minlauncher.domain

import io.github.salihoguz.minlauncher.domain.UsageEvent.Type.PAUSED
import io.github.salihoguz.minlauncher.domain.UsageEvent.Type.RESUMED
import io.github.salihoguz.minlauncher.domain.UsageEvent.Type.SCREEN_OFF
import io.github.salihoguz.minlauncher.domain.UsageEvent.Type.UNLOCKED
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsTest {

    private val day = Sections.DAY_MS
    private val minute = 60_000L
    private val dayStart = 1_000 * day
    private val now = dayStart + 12 * 60 * minute

    private fun at(minutes: Int) = dayStart + minutes * minute

    @Test
    fun visitSummaryCountsRecentVisitsOnly() {
        val visits = listOf(
            Visit(at(0) - 40 * day, VisitOutcome.SEARCH, false),
            Visit(at(1), VisitOutcome.SEARCH, true),
            Visit(at(2), VisitOutcome.PINNED, false),
            Visit(at(3), VisitOutcome.SEARCH, true),
            Visit(at(4), VisitOutcome.NONE, false),
        )
        val summary = Stats.visits(visits, at(0) - 30 * day)
        assertEquals(4, summary.total)
        assertEquals(2, summary.byOutcome[VisitOutcome.SEARCH])
        assertEquals(2, summary.keyboardClosedFirst)
        assertEquals(50, summary.share(VisitOutcome.SEARCH))
        assertEquals(0, summary.share(VisitOutcome.RECENT))
    }

    @Test
    fun mostOpenedRanksByCountThenRecency() {
        val launches = listOf(
            Launch(at(1), "a", LaunchSource.HOME),
            Launch(at(2), "b", LaunchSource.SEARCH),
            Launch(at(3), "a", LaunchSource.PINNED),
            Launch(at(4), "c", LaunchSource.HOME),
            Launch(at(5), "b", LaunchSource.SEARCH),
        )
        assertEquals(listOf("b" to 2, "a" to 2), Stats.mostOpened(launches, at(0), 2))
        assertEquals(mapOf("b" to 2), Stats.launchCounts(launches, at(0), LaunchSource.SEARCH))
    }

    @Test
    fun dayAddsForegroundTimeAndUnlocks() {
        val events = listOf(
            UsageEvent(at(10), UNLOCKED, "android"),
            UsageEvent(at(10), RESUMED, "launcher"),
            UsageEvent(at(11), PAUSED, "launcher"),
            UsageEvent(at(11), RESUMED, "chat"),
            UsageEvent(at(41), PAUSED, "chat"),
            UsageEvent(at(41), RESUMED, "maps"),
            UsageEvent(at(51), SCREEN_OFF, "android"),
            UsageEvent(at(100), UNLOCKED, "android"),
            UsageEvent(at(100), RESUMED, "maps"),
        )
        val usage = Stats.day(events, dayStart, now = at(105), ignore = setOf("launcher"))
        assertEquals(30 * minute, usage.perApp["chat"])
        assertEquals(15 * minute, usage.perApp["maps"])
        assertEquals(45 * minute, usage.totalMs)
        assertEquals(2, usage.unlocks)
        assertFalse("launcher" in usage.perApp)
    }

    @Test
    fun dayCountsAnAppOpenSinceMidnightAndSwitchesWithinAnApp() {
        val events = listOf(
            UsageEvent(at(20), PAUSED, "reader", "reader/Main"),
            // Inside one app, the next screen may resume before the last one pauses.
            UsageEvent(at(30), RESUMED, "mail", "mail/Inbox"),
            UsageEvent(at(35), RESUMED, "mail", "mail/Message"),
            UsageEvent(at(35), PAUSED, "mail", "mail/Inbox"),
            UsageEvent(at(40), PAUSED, "mail", "mail/Message"),
        )
        val usage = Stats.day(events, dayStart, now = at(60), ignore = emptySet())
        assertEquals(20 * minute, usage.perApp["reader"])
        assertEquals(10 * minute, usage.perApp["mail"])
    }

    @Test
    fun unusedNeedsEnoughHistory() {
        val today = now
        val old = today - 200 * day
        assertTrue(Stats.isUnused(today - 61 * day, old, today, knownSince = 0))
        assertFalse(Stats.isUnused(today - 10 * day, old, today, knownSince = 0))
        // Never seen used: only unused once use has been known for 60 days.
        assertTrue(Stats.isUnused(null, old, today, knownSince = today - 90 * day))
        assertFalse(Stats.isUnused(null, old, today, knownSince = today - 20 * day))
        // Recently installed apps are never unused.
        assertFalse(Stats.isUnused(null, today - 5 * day, today, knownSince = 0))
    }

    @Test
    fun durationsReadNaturally() {
        assertEquals("under 1 m", Stats.duration(20_000))
        assertEquals("14 m", Stats.duration(14 * minute))
        assertEquals("2 h", Stats.duration(120 * minute))
        assertEquals("2 h 14 m", Stats.duration(134 * minute))
    }
}
