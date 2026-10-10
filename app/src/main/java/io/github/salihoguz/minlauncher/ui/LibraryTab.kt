@file:OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package io.github.salihoguz.minlauncher.ui

import android.os.Process
import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.github.salihoguz.minlauncher.data.InstalledApp
import io.github.salihoguz.minlauncher.data.LauncherApp
import io.github.salihoguz.minlauncher.domain.Sections
import io.github.salihoguz.minlauncher.domain.Stats
import io.github.salihoguz.minlauncher.system.Actions
import java.text.Collator

enum class LibrarySort(val title: String) { LEAST_USED("Least used"), NEWEST("Newest"), LARGEST("Largest"), NAME("A–Z") }

/** Every app with full-colour icons, to look through and clean up. */
@Composable
fun LibraryTab(
    state: UiState,
    data: ManageData,
    facts: AppFacts,
    filter: LibraryFilter,
    onFilter: (LibraryFilter) -> Unit,
    onAppDetails: (LauncherApp) -> Unit,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val collator = remember { Collator.getInstance() }
    var sort by rememberSaveable { mutableStateOf(LibrarySort.LEAST_USED) }
    var selected by remember { mutableStateOf(emptySet<String>()) }
    // Apps still to remove, one Android confirmation at a time.
    var queue by remember { mutableStateOf(emptyList<InstalledApp>()) }

    val everything = remember(state.apps, state.hidden) { state.apps + state.hidden }
    val shown = remember(everything, facts, filter, sort, data.sizes) {
        val base = when (filter) {
            LibraryFilter.ALL -> everything
            LibraryFilter.UNUSED -> facts.unused
            LibraryFilter.HIDDEN -> state.hidden
        }
        when (sort) {
            LibrarySort.LEAST_USED -> base.sortedWith(
                compareBy<LauncherApp>({ facts.use(it) }, { facts.lastUsed(it) ?: 0L }).thenBy(collator) { it.label },
            )
            LibrarySort.NEWEST -> base.sortedByDescending { it.app.installedAt }
            LibrarySort.LARGEST -> base.sortedByDescending { data.sizes[it.key] ?: -1L }
            LibrarySort.NAME -> base.sortedWith { a, b -> collator.compare(a.label, b.label) }
        }
    }

    fun remove(app: InstalledApp) = if (app.isSystem) Actions.appInfo(context, app) else Actions.uninstall(context, app)

    // Each confirmation hands control back here; then the next one starts.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val next = queue.firstOrNull() ?: return@LifecycleEventEffect
        queue = queue.drop(1)
        remove(next)
    }
    BackHandler(enabled = selected.isNotEmpty()) { selected = emptySet() }

    Column(Modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip("All ${everything.size}", selected = filter == LibraryFilter.ALL) { onFilter(LibraryFilter.ALL) }
                Chip("Unused 60+ days ${facts.unused.size}", selected = filter == LibraryFilter.UNUSED) {
                    onFilter(LibraryFilter.UNUSED)
                }
                Chip("Hidden ${state.hidden.size}", selected = filter == LibraryFilter.HIDDEN) { onFilter(LibraryFilter.HIDDEN) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Sort", color = Ui.faint, fontSize = 13.sp, modifier = Modifier.padding(end = 10.dp))
                LibrarySort.entries.forEach { entry ->
                    Text(
                        text = entry.title,
                        color = if (entry == sort) Ui.text else Ui.faint,
                        fontSize = 14.sp,
                        fontWeight = if (entry == sort) FontWeight.Medium else FontWeight.Normal,
                        modifier = Modifier
                            .clickable { sort = entry }
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                    )
                }
            }
            if (data.loaded && !data.usageAccess) {
                Text(
                    "Without usage access, “used” means opened from MinLauncher and sizes leave out app data. Turn it on ›",
                    color = Ui.faint,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { Actions.usageAccess(context) },
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 16.dp),
            modifier = Modifier.weight(1f),
        ) {
            if (shown.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = emptyText(filter, state, facts),
                        color = Ui.dim,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
            }
            items(shown, key = { it.key }) { app ->
                LibraryRow(
                    app = app,
                    subtitle = buildList {
                        add(facts.usageText(app))
                        data.sizes[app.key]?.let { add(Formatter.formatShortFileSize(context, it)) }
                        if (app.key in state.settings.hidden) add("hidden")
                        if (app.app.isSystem) add("system")
                        if (app.app.user != Process.myUserHandle()) add("work")
                    }.joinToString(" · "),
                    selecting = selected.isNotEmpty(),
                    isSelected = app.key in selected,
                    onClick = {
                        when {
                            selected.isEmpty() -> onAppDetails(app)
                            app.key in selected -> selected = selected - app.key
                            else -> selected = selected + app.key
                        }
                    },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        selected = selected + app.key
                    },
                )
            }
        }

        if (selected.isNotEmpty()) {
            val picked = everything.filter { it.key in selected }.map { it.app }
            SelectionBar(
                count = picked.size,
                systemCount = picked.count { it.isSystem },
                onCancel = { selected = emptySet() },
                onRemove = {
                    selected = emptySet()
                    queue = picked.drop(1)
                    picked.firstOrNull()?.let { remove(it) }
                },
            )
        }
    }
}

private fun emptyText(filter: LibraryFilter, state: UiState, facts: AppFacts): String = when (filter) {
    LibraryFilter.ALL -> "No apps."
    LibraryFilter.HIDDEN -> "No hidden apps. Hide apps from their long-press menu."
    LibraryFilter.UNUSED -> {
        val days = ((facts.now - state.settings.trackingSince) / Sections.DAY_MS).coerceAtLeast(0)
        if (days < Stats.UNUSED_DAYS) {
            "Nothing yet. MinLauncher has kept track for $days days; apps show up here after " +
                "${Stats.UNUSED_DAYS} days without use. With usage access they show up right away."
        } else {
            "Every app was used in the last ${Stats.UNUSED_DAYS} days."
        }
    }
}

@Composable
private fun LibraryRow(
    app: LauncherApp,
    subtitle: String,
    selecting: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 8.dp),
    ) {
        if (selecting) {
            Box(
                Modifier
                    .padding(end = 12.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Ui.accent else Color.Transparent)
                    .border(1.5.dp, if (isSelected) Ui.accent else Ui.faint, CircleShape),
            )
        }
        AppIcon(app.app, color = true, size = 36.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(app.label, style = Ui.drawerApp.copy(fontSize = 19.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = Ui.faint, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SelectionBar(count: Int, systemCount: Int, onCancel: () -> Unit, onRemove: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(Ui.sheet)
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        if (systemCount > 0) {
            Text(
                if (systemCount == 1) "1 came with the phone: it opens App info, where you can disable it."
                else "$systemCount came with the phone: they open App info, where you can disable them.",
                color = Ui.faint,
                fontSize = 12.sp,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$count selected", color = Ui.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text(
                "Cancel",
                color = Ui.dim,
                fontSize = 15.sp,
                modifier = Modifier
                    .clickable(onClick = onCancel)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            )
            Text(
                "Uninstall $count",
                color = Color(0xFF1B1A22),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF2F0F6))
                    .clickable(onClick = onRemove)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
        Text("Android asks to confirm each one.", color = Ui.faint, fontSize = 12.sp)
    }
}
