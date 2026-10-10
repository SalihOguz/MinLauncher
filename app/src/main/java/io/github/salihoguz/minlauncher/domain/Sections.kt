package io.github.salihoguz.minlauncher.domain

/** What the drawer needs to know about one app to place it in Recent or New. */
data class AppHistory(
    val key: String,
    val installedAt: Long,
    /** Last time it was opened from MinLauncher, or null. */
    val lastOpened: Long?,
    val showInRecent: Boolean,
    /** When the user removed it from Recent; it comes back once opened again. */
    val removedFromRecentAt: Long? = null,
    val dismissedFromNew: Boolean = false,
)

object Sections {
    const val DAY_MS = 24L * 60 * 60 * 1000
    const val FRESH_DAYS = 3

    /** Installed in the last 3 days: gets a ✦ next to its name. */
    fun isFresh(installedAt: Long, now: Long): Boolean = now - installedAt in 0 until FRESH_DAYS * DAY_MS

    /**
     * Most recently opened first. Leaves out apps on home or pinned ([excluded]), apps with
     * "Show in Recent" off (games by default), and apps removed from Recent until they're opened again.
     */
    fun recent(apps: List<AppHistory>, excluded: Set<String>, count: Int): List<String> {
        if (count <= 0) return emptyList()
        return apps.asSequence()
            .filter { it.lastOpened != null && it.key !in excluded && it.showInRecent }
            .filter { a -> a.removedFromRecentAt == null || a.removedFromRecentAt < a.lastOpened!! }
            .sortedByDescending { it.lastOpened }
            .take(count)
            .map { it.key }
            .toList()
    }

    /**
     * Apps installed since MinLauncher started keeping track, never opened from it, newest first.
     * They stay in New however long that takes, unless removed or put on home or pinned.
     */
    fun newApps(apps: List<AppHistory>, trackingSince: Long, excluded: Set<String>): List<String> =
        apps.asSequence()
            .filter { it.installedAt > trackingSince && it.lastOpened == null && !it.dismissedFromNew }
            .filter { it.key !in excluded }
            .sortedByDescending { it.installedAt }
            .map { it.key }
            .toList()
}
