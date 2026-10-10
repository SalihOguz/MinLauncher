package io.github.salihoguz.minlauncher.data

import android.content.ComponentName
import android.os.UserHandle

/** One launchable activity, as Android reports it. */
data class InstalledApp(
    /** Stable id: package/class/user serial. Used as the key in saved lists. */
    val key: String,
    val label: String,
    val component: ComponentName,
    val user: UserHandle,
    val installedAt: Long,
    /** Android's app category (ApplicationInfo.CATEGORY_*), -1 when the app doesn't say. */
    val category: Int = -1,
    /** Came with the phone: can be disabled in App info, not uninstalled. */
    val isSystem: Boolean = false,
) {
    /** Games stay out of Recent by default. */
    val isGame: Boolean get() = category == 0
    val packageName: String get() = component.packageName
}

/** An installed app as the launcher shows it: with the user's rename and history applied. */
data class LauncherApp(
    val app: InstalledApp,
    val label: String,
    val searchWords: List<String> = emptyList(),
    /** Last time it was opened from MinLauncher, or null. */
    val lastOpened: Long? = null,
    val showInRecent: Boolean = !app.isGame,
    /** Installed in the last 3 days: shows a ✦ next to its name. */
    val isFresh: Boolean = false,
    /** The user folder it's filed in, or null for Unsorted. */
    val folder: String? = null,
) {
    val key: String get() = app.key
    val isRenamed: Boolean get() = label != app.label
}
