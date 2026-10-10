package io.github.salihoguz.minlauncher.data

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Keeps the list of launchable apps current, across the main and work profiles. */
class AppRepository(private val context: Context, private val scope: CoroutineScope) {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)

    private val _apps = MutableStateFlow<List<InstalledApp>?>(null)
    /** Null until the first load finishes. */
    val apps: StateFlow<List<InstalledApp>?> = _apps

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = refresh()
        override fun onPackageAdded(packageName: String, user: UserHandle) = refresh()
        override fun onPackageChanged(packageName: String, user: UserHandle) = refresh()
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = refresh()
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = refresh()
    }

    private var registered = false

    fun start() {
        if (!registered) {
            launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
            registered = true
        }
        refresh()
    }

    fun stop() {
        if (registered) {
            launcherApps.unregisterCallback(callback)
            registered = false
        }
    }

    fun refresh() {
        scope.launch { _apps.value = withContext(Dispatchers.IO) { load() } }
    }

    private fun load(): List<InstalledApp> {
        val result = ArrayList<InstalledApp>()
        for (user in userManager.userProfiles) {
            val serial = userManager.getSerialNumberForUser(user)
            val activities = runCatching { launcherApps.getActivityList(null, user) }.getOrDefault(emptyList())
            for (info in activities) {
                val component = info.componentName
                if (component.packageName == context.packageName) continue
                result += InstalledApp(
                    key = "${component.packageName}/${component.className}/$serial",
                    label = info.label?.toString()?.trim().orEmpty().ifEmpty { component.packageName },
                    component = component,
                    user = user,
                    installedAt = info.firstInstallTime,
                )
            }
        }
        return result
    }
}
