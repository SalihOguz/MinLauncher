package io.github.salihoguz.minlauncher.domain

/** How a drawer visit ended. */
enum class VisitOutcome { PINNED, RECENT, NEW, LIST, SEARCH, WEB, FOLDER, RAIL, NONE }

/** Where an app was opened from in MinLauncher. */
enum class LaunchSource {
    HOME, PINNED, RECENT, NEW, LIST, SEARCH, FOLDER, RAIL, SHEET;

    companion object {
        /** Drawer launches carry how the visit ended. */
        fun of(outcome: VisitOutcome): LaunchSource = when (outcome) {
            VisitOutcome.PINNED -> PINNED
            VisitOutcome.RECENT -> RECENT
            VisitOutcome.NEW -> NEW
            VisitOutcome.SEARCH -> SEARCH
            VisitOutcome.FOLDER -> FOLDER
            VisitOutcome.RAIL -> RAIL
            VisitOutcome.LIST, VisitOutcome.WEB, VisitOutcome.NONE -> LIST
        }
    }
}

data class Visit(val time: Long, val outcome: VisitOutcome, val keyboardClosedFirst: Boolean)

data class Launch(val time: Long, val key: String, val source: LaunchSource)

data class VisitSummary(
    val total: Int,
    val byOutcome: Map<VisitOutcome, Int>,
    val keyboardClosedFirst: Int,
) {
    fun share(outcome: VisitOutcome): Int = if (total == 0) 0 else (byOutcome[outcome] ?: 0) * 100 / total
}

/** One entry from Android's usage events, reduced to what screen time needs. */
data class UsageEvent(val time: Long, val type: Type, val packageName: String, val activity: String = packageName) {
    enum class Type { RESUMED, PAUSED, SCREEN_OFF, UNLOCKED }
}

data class DayUsage(val totalMs: Long, val unlocks: Int, val perApp: Map<String, Long>)

object Stats {
    const val UNUSED_DAYS = 60

    fun visits(visits: List<Visit>, since: Long): VisitSummary {
        val recent = visits.filter { it.time >= since }
        return VisitSummary(
            total = recent.size,
            byOutcome = recent.groupingBy { it.outcome }.eachCount(),
            keyboardClosedFirst = recent.count { it.keyboardClosedFirst },
        )
    }

    /** How often each app was opened since [since], optionally from one place only. */
    fun launchCounts(launches: List<Launch>, since: Long, source: LaunchSource? = null): Map<String, Int> =
        launches.asSequence()
            .filter { it.time >= since && (source == null || it.source == source) }
            .groupingBy { it.key }
            .eachCount()

    /** Most opened first; ties go to the one opened most recently. */
    fun mostOpened(launches: List<Launch>, since: Long, limit: Int): List<Pair<String, Int>> {
        val last = HashMap<String, Long>()
        launches.forEach { if (it.time >= since) last[it.key] = maxOf(last[it.key] ?: 0L, it.time) }
        return launchCounts(launches, since).entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenByDescending { last[it.key] ?: 0L })
            .take(limit)
            .map { it.key to it.value }
    }

    /**
     * Time spent in each app and the number of unlocks between [dayStart] and [now].
     * An app counts while one of its activities is the most recently resumed one, until the
     * screen goes off. Packages in [ignore] (the launcher itself) don't count.
     */
    fun day(events: List<UsageEvent>, dayStart: Long, now: Long, ignore: Set<String>): DayUsage {
        val perApp = HashMap<String, Long>()
        fun add(pkg: String, ms: Long) {
            if (pkg !in ignore && ms > 0) perApp[pkg] = (perApp[pkg] ?: 0L) + ms
        }
        val resumed = LinkedHashMap<String, String>() // activity -> package
        val seen = HashSet<String>()
        var current: String? = null
        var since = dayStart
        var unlocks = 0
        fun switchTo(pkg: String?, time: Long) {
            if (pkg == current) return
            current?.let { add(it, time - since) }
            current = pkg
            since = time
        }
        for (event in events.sortedBy { it.time }) {
            val time = event.time.coerceIn(dayStart, now)
            when (event.type) {
                UsageEvent.Type.RESUMED -> {
                    seen += event.packageName
                    resumed.remove(event.activity)
                    resumed[event.activity] = event.packageName
                    switchTo(event.packageName, time)
                }
                UsageEvent.Type.PAUSED -> {
                    // Paused without being resumed today: it was already open when the day began.
                    if (event.activity !in resumed && event.packageName !in seen && current == null) {
                        add(event.packageName, time - dayStart)
                    }
                    seen += event.packageName
                    resumed.remove(event.activity)
                    switchTo(resumed.values.lastOrNull(), time)
                }
                UsageEvent.Type.SCREEN_OFF -> {
                    resumed.clear()
                    switchTo(null, time)
                }
                UsageEvent.Type.UNLOCKED -> unlocks++
            }
        }
        switchTo(null, now)
        return DayUsage(totalMs = perApp.values.sum(), unlocks = unlocks, perApp = perApp)
    }

    /**
     * Not used in the last [days] days. [knownSince] is how far back use is known: apps never
     * seen used only count once that reaches back far enough. Apps installed more recently
     * than that are never "unused".
     */
    fun isUnused(lastUsed: Long?, installedAt: Long, now: Long, knownSince: Long, days: Int = UNUSED_DAYS): Boolean {
        val cutoff = now - days * Sections.DAY_MS
        if (installedAt > cutoff) return false
        return if (lastUsed != null && lastUsed > 0) lastUsed < cutoff else knownSince <= cutoff
    }

    /** "2 h 14 m", "14 m", "under 1 m". */
    fun duration(ms: Long): String {
        val minutes = ms / 60_000
        return when {
            minutes < 1 -> "under 1 m"
            minutes < 60 -> "$minutes m"
            minutes % 60 == 0L -> "${minutes / 60} h"
            else -> "${minutes / 60} h ${minutes % 60} m"
        }
    }
}
