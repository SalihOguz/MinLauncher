package io.github.salihoguz.minlauncher.ui

import android.os.Process
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.salihoguz.minlauncher.data.LauncherApp
import io.github.salihoguz.minlauncher.domain.Sections
import io.github.salihoguz.minlauncher.domain.Stats

enum class ManageTab(val title: String) { LIBRARY("Library"), STATS("Stats"), SETTINGS("Settings") }

enum class LibraryFilter { ALL, UNUSED, HIDDEN }

/** Library, Stats and Settings. Opened by holding empty space on home, or from the drawer. */
@Composable
fun ManageScreen(
    state: UiState,
    viewModel: LauncherViewModel,
    tab: ManageTab,
    onTab: (ManageTab) -> Unit,
    filter: LibraryFilter,
    onFilter: (LibraryFilter) -> Unit,
    onAppDetails: (LauncherApp) -> Unit,
    onClose: () -> Unit,
) {
    val data by viewModel.manage.collectAsStateWithLifecycle()
    // Also runs when the screen opens: usage access, uninstalls and app use change while away.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshManage() }
    BackHandler(onBack = onClose)
    val facts = remember(state, data) { AppFacts(state, data, System.currentTimeMillis()) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xF2121118))
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        ) {
            ManageTab.entries.forEach { entry ->
                Text(
                    text = entry.title,
                    style = Ui.drawerApp.copy(fontSize = 22.sp, color = if (entry == tab) Ui.text else Ui.faint),
                    modifier = Modifier
                        .clickable { onTab(entry) }
                        .padding(end = 18.dp, top = 6.dp, bottom = 6.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                "Done",
                color = Ui.dim,
                fontSize = 16.sp,
                modifier = Modifier
                    .clickable(onClick = onClose)
                    .padding(8.dp),
            )
        }
        Box(Modifier.weight(1f)) {
            when (tab) {
                ManageTab.LIBRARY -> LibraryTab(state, data, facts, filter, onFilter, onAppDetails)
                ManageTab.STATS -> StatsTab(
                    state = state,
                    data = data,
                    facts = facts,
                    viewModel = viewModel,
                    onReviewUnused = {
                        onFilter(LibraryFilter.UNUSED)
                        onTab(ManageTab.LIBRARY)
                    },
                )
                ManageTab.SETTINGS -> SettingsTab(
                    state = state,
                    data = data,
                    viewModel = viewModel,
                    onShowHidden = {
                        onFilter(LibraryFilter.HIDDEN)
                        onTab(ManageTab.LIBRARY)
                    },
                )
            }
        }
    }
}

/**
 * What's known about how each app is used. Android's records (with usage access) cover the
 * main profile; otherwise only opens from MinLauncher count.
 */
class AppFacts(private val state: UiState, private val data: ManageData, val now: Long) {
    private val me = Process.myUserHandle()
    private val opens30d = Stats.launchCounts(data.launches, now - 30 * Sections.DAY_MS)

    /** Android's records cover this app. */
    fun tracked(app: LauncherApp): Boolean = data.usageAccess && app.app.user == me

    fun lastUsed(app: LauncherApp): Long? =
        listOfNotNull(app.lastOpened, if (tracked(app)) data.lastUsed[app.app.packageName] else null).maxOrNull()

    /** How much it was used in 30 days: time on screen, or opens from MinLauncher. */
    fun use(app: LauncherApp): Long =
        if (tracked(app)) data.screenTime30d[app.app.packageName] ?: 0L else (opens30d[app.key] ?: 0).toLong()

    /** Whether "unused" can be told yet for apps never seen in use. */
    fun knownSince(app: LauncherApp): Long = if (tracked(app)) 0L else state.settings.trackingSince

    val unused: List<LauncherApp> = (state.apps + state.hidden)
        .filter { Stats.isUnused(lastUsed(it), it.app.installedAt, now, knownSince(it)) }

    /** "2 h 5 m in 30 days", "used yesterday", "opened 3 days ago". */
    fun usageText(app: LauncherApp): String {
        if (tracked(app)) {
            val time = data.screenTime30d[app.app.packageName] ?: 0L
            val last = lastUsed(app)
            return when {
                time >= 60_000 -> "${Stats.duration(time)} in 30 days"
                last != null -> "used ${relativeDays(last, now)}"
                else -> "no use on record"
            }
        }
        return app.lastOpened?.let { "opened ${relativeDays(it, now)}" } ?: "not opened from MinLauncher"
    }
}
