package io.github.salihoguz.minlauncher.system

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import io.github.salihoguz.minlauncher.LockService
import io.github.salihoguz.minlauncher.data.InstalledApp

/** Everything the launcher asks Android to do. */
object Actions {

    private const val BRAVE = "com.brave.browser"

    fun launch(context: Context, app: InstalledApp) {
        runCatching {
            context.getSystemService(LauncherApps::class.java)
                .startMainActivity(app.component, app.user, null, null)
        }.onFailure { toast(context, "Couldn't open ${app.label}") }
    }

    fun appInfo(context: Context, app: InstalledApp) {
        runCatching {
            context.getSystemService(LauncherApps::class.java)
                .startAppDetailsActivity(app.component, app.user, null, null)
        }.onFailure { toast(context, "Couldn't open App info") }
    }

    fun uninstall(context: Context, app: InstalledApp) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.packageName, null))
        start(context, intent, "Couldn't start uninstall")
    }

    /** Searches in Brave with its own search engine; falls back to Brave Search in any browser. */
    fun webSearch(context: Context, query: String) {
        val text = query.trim()
        if (text.isEmpty()) return
        val inBrave = Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, text).setPackage(BRAVE)
        val url = Uri.parse("https://search.brave.com/search?q=" + Uri.encode(text))
        val attempts = listOf(
            inBrave,
            Intent(Intent.ACTION_VIEW, url).setPackage(BRAVE),
            Intent(Intent.ACTION_VIEW, url),
        )
        for (intent in attempts) {
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (e: ActivityNotFoundException) {
            } catch (e: SecurityException) {
            }
        }
        toast(context, "No browser found")
    }

    fun camera(context: Context) =
        start(context, Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA), "No camera app found")

    fun dialer(context: Context) = start(context, Intent(Intent.ACTION_DIAL), "No phone app found")

    fun clock(context: Context) = start(context, Intent(AlarmClock.ACTION_SHOW_ALARMS), "No clock app found")

    fun calendar(context: Context) {
        val uri = ContentUris.withAppendedId(
            CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build(),
            System.currentTimeMillis(),
        )
        start(context, Intent(Intent.ACTION_VIEW, uri), "No calendar app found")
    }

    @SuppressLint("WrongConstant")
    fun expandNotifications(context: Context) {
        val opened = runCatching {
            val service = context.getSystemService("statusbar")
            Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel").invoke(service)
        }.isSuccess
        if (!opened) LockService.openNotifications()
    }

    /** Returns false when the lock service isn't turned on yet. */
    fun lockScreen(): Boolean = LockService.lockScreen()

    fun accessibilitySettings(context: Context) =
        start(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), "Couldn't open accessibility settings")

    fun appDetailsOfLauncher(context: Context) = start(
        context,
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
        "Couldn't open App info",
    )

    fun homeSettings(context: Context) = start(context, Intent(Settings.ACTION_HOME_SETTINGS), "Couldn't open home app settings")

    fun isDefaultLauncher(context: Context): Boolean {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val info = context.packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
        return info?.activityInfo?.packageName == context.packageName
    }

    fun toast(context: Context, message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    private fun start(context: Context, intent: Intent, failure: String) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            toast(context, failure)
        } catch (e: SecurityException) {
            toast(context, failure)
        }
    }
}
