package io.github.salihoguz.minlauncher.ui

import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.salihoguz.minlauncher.domain.Sections
import io.github.salihoguz.minlauncher.domain.Stats
import io.github.salihoguz.minlauncher.domain.Suggestion
import io.github.salihoguz.minlauncher.domain.SuggestionInput
import io.github.salihoguz.minlauncher.domain.Suggestions
import io.github.salihoguz.minlauncher.domain.VisitOutcome
import io.github.salihoguz.minlauncher.system.Actions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** How the launcher and the phone get used, and the changes that could help. Nothing leaves the phone. */
@Composable
fun StatsTab(
    state: UiState,
    data: ManageData,
    facts: AppFacts,
    viewModel: LauncherViewModel,
    onReviewUnused: () -> Unit,
) {
    val context = LocalContext.current
    val now = facts.now
    val monthAgo = now - 30 * Sections.DAY_MS
    val byKey = remember(state.apps) { state.apps.associateBy { it.key } }

    val suggestions = remember(state, data, facts) {
        Suggestions.compute(
            SuggestionInput(
                now = now,
                trackingSince = state.settings.trackingSince,
                home = state.home.map { it.key },
                pinned = state.pinned.map { it.key },
                placedAt = state.settings.placedAt,
                lastOpened = state.settings.lastOpened,
                launches = data.launches,
                visits = data.visits,
                recentCount = state.settings.recentCount,
                unusedCount = facts.unused.size,
                snoozedAt = state.settings.snoozedSuggestions,
            ),
        ).filter { s ->
            when (s) {
                is Suggestion.Pin -> s.key in byKey
                is Suggestion.Unpin -> s.key in byKey
                is Suggestion.HomeToPinned -> s.key in byKey
                else -> true
            }
        }
    }
    val visits = remember(data.visits, monthAgo) { Stats.visits(data.visits, monthAgo) }
    val mostOpened = remember(data.launches, byKey, monthAgo) {
        Stats.mostOpened(data.launches, monthAgo, 12).mapNotNull { (key, count) -> byKey[key]?.let { it to count } }.take(8)
    }
    val countingSince = data.launches.minOfOrNull { it.time }
    val todayApps = remember(data.today, state.apps) {
        val labels = state.apps.associate { it.app.packageName to it.label }
        data.today?.perApp.orEmpty().entries
            .sortedByDescending { it.value }
            .take(5)
            .map { (pkg, ms) -> Triple(pkg, labels[pkg] ?: packageLabel(context.packageManager, pkg), ms) }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (suggestions.isNotEmpty()) {
            item(key = "suggestions-label") { SectionLabel("Suggestions") }
            items(suggestions, key = { it.id }) { s ->
                fun label(key: String) = byKey[key]?.label ?: "this app"
                val notNow = { viewModel.snoozeSuggestion(s.id) }
                when (s) {
                    is Suggestion.Pin -> SuggestionCard(
                        title = "Pin ${label(s.key)}?",
                        body = "You typed its name to open it ${s.times} times in the last 2 weeks.",
                        action = "Pin",
                        onAction = { viewModel.pin(s.key) },
                        onNotNow = notNow,
                    )
                    is Suggestion.Unpin -> SuggestionCard(
                        title = "Unpin ${label(s.key)}?",
                        body = "Pinned, but not opened from MinLauncher in over 30 days.",
                        action = "Unpin",
                        onAction = { viewModel.unpin(s.key) },
                        onNotNow = notNow,
                    )
                    is Suggestion.HomeToPinned -> SuggestionCard(
                        title = "Move ${label(s.key)} to Pinned?",
                        body = "Opened from home ${times(s.times)} in 30 days. Pinned keeps it one swipe away " +
                            "and frees a row on home.",
                        action = "Move",
                        onAction = { viewModel.moveHomeToPinned(s.key) },
                        onNotNow = notNow,
                    )
                    is Suggestion.RecentOff -> SuggestionCard(
                        title = "Turn off Recent?",
                        body = "Only ${s.percent}% of drawer visits in 30 days ended in Recent.",
                        action = "Turn off",
                        onAction = { viewModel.setRecentCount(0) },
                        onNotNow = notNow,
                    )
                    is Suggestion.ReviewUnused -> SuggestionCard(
                        title = "Look through ${s.count} unused apps",
                        body = "Not used in ${Stats.UNUSED_DAYS} days or more.",
                        action = "Review",
                        onAction = {
                            viewModel.snoozeSuggestion(s.id)
                            onReviewUnused()
                        },
                        onNotNow = notNow,
                    )
                }
            }
        }

        item(key = "today-label") { SectionLabel("Today") }
        val today = data.today
        if (data.usageAccess && today != null) {
            item(key = "today") {
                Column(Modifier.padding(bottom = 8.dp)) {
                    Text(Stats.duration(today.totalMs), style = Ui.drawerApp.copy(fontSize = 44.sp))
                    Text(
                        "in apps · ${today.unlocks} ${if (today.unlocks == 1) "unlock" else "unlocks"}",
                        color = Ui.dim,
                        fontSize = 14.sp,
                    )
                }
            }
            val longest = todayApps.maxOfOrNull { it.third } ?: 0L
            items(todayApps, key = { "today-" + it.first }) { (_, label, ms) ->
                BarRow(label, Stats.duration(ms), ms.toFloat() / longest.coerceAtLeast(1))
            }
        } else if (data.loaded) {
            item(key = "today-off") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Screen time and unlocks come from Android's usage records. Turn on usage access to see them here.",
                        color = Ui.dim,
                        fontSize = 14.sp,
                    )
                    Chip("Turn on usage access", selected = true) { Actions.usageAccess(context) }
                }
            }
        }

        item(key = "drawer-label") { SectionLabel("Drawer · last 30 days") }
        if (visits.total == 0) {
            item(key = "drawer-empty") { Text("No drawer visits yet.", color = Ui.dim, fontSize = 14.sp) }
        } else {
            item(key = "drawer-total") {
                Text(
                    "${visits.total} visits. How each one ended:",
                    color = Ui.dim,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            val rows = visits.byOutcome.entries.sortedByDescending { it.value }
            val most = rows.firstOrNull()?.value ?: 1
            items(rows, key = { "visit-" + it.key.name }) { (outcome, count) ->
                BarRow(outcomeLabel(outcome), "$count · ${visits.share(outcome)}%", count.toFloat() / most)
            }
            item(key = "keyboard") {
                val share = visits.keyboardClosedFirst * 100 / visits.total
                Text(
                    "You put the keyboard away before choosing in $share% of visits.",
                    color = Ui.dim,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        item(key = "opened-label") { SectionLabel("Most opened from MinLauncher · 30 days") }
        if (mostOpened.isEmpty()) {
            item(key = "opened-empty") {
                Text("Nothing yet. Counting started with this version.", color = Ui.dim, fontSize = 14.sp)
            }
        } else {
            val most = mostOpened.first().second
            items(mostOpened, key = { "opened-" + it.first.key }) { (app, count) ->
                BarRow(app.label, "$count", count.toFloat() / most)
            }
            if (countingSince != null && countingSince > monthAgo) {
                item(key = "opened-since") {
                    val date = SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(countingSince))
                    Text("Counting since $date.", color = Ui.faint, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }

        item(key = "footer") {
            Text(
                "Everything here stays on this phone.",
                color = Ui.faint,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 20.dp),
            )
        }
    }
}

private fun times(n: Int) = if (n == 1) "once" else "$n times"

private fun outcomeLabel(outcome: VisitOutcome) = when (outcome) {
    VisitOutcome.SEARCH -> "Typed a name"
    VisitOutcome.PINNED -> "Pinned"
    VisitOutcome.RECENT -> "Recent"
    VisitOutcome.NEW -> "New"
    VisitOutcome.FOLDER -> "A folder"
    VisitOutcome.RAIL -> "A–Z rail"
    VisitOutcome.LIST -> "Scrolled to it"
    VisitOutcome.WEB -> "Web search"
    VisitOutcome.NONE -> "Left without opening"
}

private fun packageLabel(pm: PackageManager, pkg: String): String =
    runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)

/** A label, then a bar sized against the largest in its list, with the value at its tip. */
@Composable
private fun BarRow(label: String, value: String, fraction: Float) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(label, color = Ui.text, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        ) {
            // Leaves room for the value; rounded only at the data end.
            Box(
                Modifier
                    .fillMaxWidth(0.78f * fraction.coerceIn(0.02f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
                    .background(Ui.accent),
            )
            Spacer(Modifier.width(8.dp))
            Text(value, color = Ui.dim, fontSize = 13.sp, maxLines = 1)
        }
    }
}

@Composable
private fun SuggestionCard(title: String, body: String, action: String, onAction: () -> Unit, onNotNow: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Ui.text.copy(alpha = 0.07f))
            .padding(14.dp),
    ) {
        Text(title, color = Ui.text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Text(body, color = Ui.dim, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(action, selected = true, onClick = onAction)
            Chip("Not now", selected = false, onClick = onNotNow)
        }
    }
}
