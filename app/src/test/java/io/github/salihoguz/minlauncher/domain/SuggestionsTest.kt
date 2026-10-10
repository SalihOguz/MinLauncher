package io.github.salihoguz.minlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionsTest {

    private val day = Sections.DAY_MS
    private val now = 1_000 * day

    private fun input(
        home: List<String> = emptyList(),
        pinned: List<String> = emptyList(),
        placedAt: Map<String, Long> = emptyMap(),
        lastOpened: Map<String, Long> = emptyMap(),
        launches: List<Launch> = emptyList(),
        visits: List<Visit> = emptyList(),
        recentCount: Int = 5,
        unusedCount: Int = 0,
        snoozedAt: Map<String, Long> = emptyMap(),
        trackingSince: Long = now - 100 * day,
    ) = SuggestionInput(
        now = now,
        trackingSince = trackingSince,
        home = home,
        pinned = pinned,
        placedAt = placedAt,
        lastOpened = lastOpened,
        launches = launches,
        visits = visits,
        recentCount = recentCount,
        unusedCount = unusedCount,
        snoozedAt = snoozedAt,
    )

    private fun typed(key: String, times: Int) = (1..times).map { Launch(now - it * day / 2, key, LaunchSource.SEARCH) }

    @Test
    fun suggestsPinningAnAppYouKeepTyping() {
        val result = Suggestions.compute(input(launches = typed("bank", 6) + typed("maps", 2)))
        assertEquals(listOf(Suggestion.Pin("bank", 6)), result)
        // Not when it's already on home or pinned.
        assertTrue(Suggestions.compute(input(home = listOf("bank"), launches = typed("bank", 6))).none { it is Suggestion.Pin })
    }

    @Test
    fun suggestsUnpinningIdlePinsButNotFreshOnes() {
        val result = Suggestions.compute(
            input(
                pinned = listOf("idle", "fresh", "used"),
                placedAt = mapOf("fresh" to now - 2 * day),
                lastOpened = mapOf("idle" to now - 45 * day, "used" to now - 3 * day),
            ),
        )
        assertEquals(listOf(Suggestion.Unpin("idle")), result)
    }

    @Test
    fun suggestsMovingRarelyOpenedHomeAppsOnlyWithAMonthOfRecords() {
        val launches = listOf(Launch(now - 40 * day, "x", LaunchSource.HOME)) +
            (1..10).map { Launch(now - it * day, "busy", LaunchSource.HOME) } +
            listOf(Launch(now - 3 * day, "quiet", LaunchSource.HOME))
        val result = Suggestions.compute(input(home = listOf("busy", "quiet"), launches = launches))
        assertEquals(listOf(Suggestion.HomeToPinned("quiet", 1)), result)

        val tooNew = launches.filter { it.time > now - 20 * day }
        assertTrue(Suggestions.compute(input(home = listOf("busy", "quiet"), launches = tooNew)).isEmpty())
    }

    @Test
    fun suggestsTurningOffRecentWhenBarelyUsed() {
        val visits = (1..50).map { Visit(now - it * day / 2, if (it == 1) VisitOutcome.RECENT else VisitOutcome.SEARCH, false) }
        assertEquals(listOf(Suggestion.RecentOff(2)), Suggestions.compute(input(visits = visits)))
        assertTrue(Suggestions.compute(input(visits = visits, recentCount = 0)).isEmpty())
        assertTrue(Suggestions.compute(input(visits = visits.take(20))).isEmpty())
    }

    @Test
    fun notNowHidesASuggestionFor30Days() {
        val base = input(unusedCount = 7)
        assertEquals(listOf(Suggestion.ReviewUnused(7)), Suggestions.compute(base))
        val snoozed = base.copy(snoozedAt = mapOf("review-unused" to now - 10 * day))
        assertTrue(Suggestions.compute(snoozed).isEmpty())
        val expired = base.copy(snoozedAt = mapOf("review-unused" to now - 31 * day))
        assertEquals(1, Suggestions.compute(expired).size)
    }
}
