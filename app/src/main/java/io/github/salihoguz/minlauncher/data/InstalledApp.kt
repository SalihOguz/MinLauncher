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
) {
    val packageName: String get() = component.packageName
}

/** An installed app as the launcher shows it: with the user's rename applied. */
data class LauncherApp(
    val app: InstalledApp,
    val label: String,
) {
    val key: String get() = app.key
    val isRenamed: Boolean get() = label != app.label
}
