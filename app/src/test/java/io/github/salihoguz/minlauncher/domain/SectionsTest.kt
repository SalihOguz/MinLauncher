package io.github.salihoguz.minlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SectionsTest {

    private val day = Sections.DAY_MS
    private val now = 1_000 * day

    private fun app(
        key: String,
        installedDaysAgo: Int = 100,
        openedDaysAgo: Int? = null,
        showInRecent: Boolean = true,
        removedDaysAgo: Int? = null,
        dismissed: Boolean = false,
    ) = AppHistory(
        key = key,
        installedAt = now - installedDaysAgo * day,
        lastOpened = openedDaysAgo?.let { now - it * day },
        showInRecent = showInRecent,
        removedFromRecentAt = removedDaysAgo?.let { now - it * day },
        dismissedFromNew = dismissed,
    )

    @Test
    fun recentIsMostRecentFirstAndCapped() {
        val apps = (1..8).map { app("a$it", openedDaysAgo = it) }
        assertEquals(listOf("a1", "a2", "a3", "a4", "a5"), Sections.recent(apps, emptySet(), 5))
        assertEquals(emptyList<String>(), Sections.recent(apps, emptySet(), 0))
    }

    @Test
    fun recentSkipsHomePinnedHiddenFromRecentAndNeverOpened() {
        val apps = listOf(
            app("home", openedDaysAgo = 1),
            app("game", openedDaysAgo = 2, showInRecent = false),
            app("never"),
            app("kept", openedDaysAgo = 3),
        )
        assertEquals(listOf("kept"), Sections.recent(apps, setOf("home"), 5))
    }

    @Test
    fun removedFromRecentComesBackOnlyAfterOpeningAgain() {
        val removedAfterOpen = app("x", openedDaysAgo = 5, removedDaysAgo = 2)
        val openedAfterRemove = app("y", openedDaysAgo = 1, removedDaysAgo = 2)
        assertEquals(listOf("y"), Sections.recent(listOf(removedAfterOpen, openedAfterRemove), emptySet(), 5))
    }

    @Test
    fun newAppsAreInstalledSinceTrackingAndNeverOpened() {
        val trackingSince = now - 30 * day
        val apps = listOf(
            app("old", installedDaysAgo = 200),
            app("newest", installedDaysAgo = 1),
            app("waiting", installedDaysAgo = 20),
            app("opened", installedDaysAgo = 2, openedDaysAgo = 1),
            app("dismissed", installedDaysAgo = 3, dismissed = true),
            app("pinned", installedDaysAgo = 4),
        )
        assertEquals(listOf("newest", "waiting"), Sections.newApps(apps, trackingSince, setOf("pinned")))
    }

    @Test
    fun freshForThreeDays() {
        assertTrue(Sections.isFresh(now - 1 * day, now))
        assertTrue(Sections.isFresh(now - 3 * day + 1, now))
        assertFalse(Sections.isFresh(now - 3 * day, now))
        assertFalse(Sections.isFresh(now + day, now))
    }
}
