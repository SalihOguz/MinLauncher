package io.github.salihoguz.minlauncher.domain

/** A change the Stats tab offers. Each has one action and "Not now". */
sealed interface Suggestion {
    /** Stable id, so "Not now" can hide it for a while. */
    val id: String

    /** An app you keep typing to open: pin it. */
    data class Pin(val key: String, val times: Int) : Suggestion {
        override val id get() = "pin:$key"
    }

    /** A pinned app you haven't opened in a month: unpin it. */
    data class Unpin(val key: String) : Suggestion {
        override val id get() = "unpin:$key"
    }

    /** A home app you rarely open: move it to Pinned and free a home row. */
    data class HomeToPinned(val key: String, val times: Int) : Suggestion {
        override val id get() = "home-to-pinned:$key"
    }

    /** Recent is barely used: turn it off. */
    data class RecentOff(val percent: Int) : Suggestion {
        override val id get() = "recent-off"
    }

    /** Apps unused for 60+ days: look through them in Library. */
    data class ReviewUnused(val count: Int) : Suggestion {
        override val id get() = "review-unused"
    }
}

data class SuggestionInput(
    val now: Long,
    /** When MinLauncher started recording when apps were opened. */
    val trackingSince: Long,
    val home: List<String>,
    /** Pinned apps shown in the drawer (not on home). */
    val pinned: List<String>,
    /** When each app was put on home or pinned; missing means before this was recorded. */
    val placedAt: Map<String, Long>,
    val lastOpened: Map<String, Long>,
    val launches: List<Launch>,
    val visits: List<Visit>,
    val recentCount: Int,
    val unusedCount: Int,
    /** When "Not now" was pressed, by suggestion id. */
    val snoozedAt: Map<String, Long>,
)

object Suggestions {
    const val SNOOZE_DAYS = 30
    const val MAX = 5

    private const val PIN_WINDOW_DAYS = 14
    private const val PIN_MIN_TYPED = 5
    private const val IDLE_DAYS = 30
    private const val HOME_MAX_OPENS = 2
    private const val RECENT_MIN_VISITS = 40
    private const val RECENT_MAX_SHARE = 3
    private const val REVIEW_MIN_APPS = 3

    fun compute(input: SuggestionInput): List<Suggestion> {
        val day = Sections.DAY_MS
        val now = input.now
        val idleCutoff = now - IDLE_DAYS * day
        val homeSet = input.home.toSet()
        val pinnedSet = input.pinned.toSet()
        // Placement times only exist from v0.4 on; older placements count from when tracking began.
        fun placed(key: String) = input.placedAt[key] ?: input.trackingSince
        val result = ArrayList<Suggestion>()

        Stats.launchCounts(input.launches, now - PIN_WINDOW_DAYS * day, LaunchSource.SEARCH).entries
            .filter { it.value >= PIN_MIN_TYPED && it.key !in homeSet && it.key !in pinnedSet }
            .sortedByDescending { it.value }
            .take(2)
            .forEach { result += Suggestion.Pin(it.key, it.value) }

        input.pinned
            .filter { key -> placed(key) <= idleCutoff && (input.lastOpened[key] ?: input.trackingSince) <= idleCutoff }
            .take(2)
            .forEach { result += Suggestion.Unpin(it) }

        // Needs a month of launch records to judge.
        val logSince = input.launches.minOfOrNull { it.time } ?: now
        if (logSince <= idleCutoff) {
            val opens = Stats.launchCounts(input.launches, idleCutoff)
            input.home
                .filter { key -> placed(key) <= idleCutoff && (opens[key] ?: 0) <= HOME_MAX_OPENS }
                .sortedBy { opens[it] ?: 0 }
                .take(2)
                .forEach { result += Suggestion.HomeToPinned(it, opens[it] ?: 0) }
        }

        if (input.recentCount > 0) {
            val visits = Stats.visits(input.visits, idleCutoff)
            val share = visits.share(VisitOutcome.RECENT)
            if (visits.total >= RECENT_MIN_VISITS && share < RECENT_MAX_SHARE) result += Suggestion.RecentOff(share)
        }

        if (input.unusedCount >= REVIEW_MIN_APPS) result += Suggestion.ReviewUnused(input.unusedCount)

        return result
            .filter { s -> input.snoozedAt[s.id]?.let { now - it >= SNOOZE_DAYS * day } ?: true }
            .take(MAX)
    }
}
