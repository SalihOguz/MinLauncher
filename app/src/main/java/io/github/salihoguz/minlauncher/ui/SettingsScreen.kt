package io.github.salihoguz.minlauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.github.salihoguz.minlauncher.LockService
import io.github.salihoguz.minlauncher.domain.ClockMode
import io.github.salihoguz.minlauncher.system.Actions
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(state: UiState, viewModel: LauncherViewModel, onClose: () -> Unit) {
    val context = LocalContext.current
    val settings = state.settings
    BackHandler(onBack = onClose)

    // Re-check things that change outside the app (default launcher, lock service) on return.
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumes++ }
    val isDefault = remember(resumes) { Actions.isDefaultLauncher(context) }
    val lockRunning = remember(resumes) { LockService.isRunning }
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF2121118))
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Settings", style = Ui.drawerApp.copy(fontSize = 32.sp), modifier = Modifier.weight(1f))
                Text(
                    "Done",
                    color = Ui.dim,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clickable(onClick = onClose)
                        .padding(8.dp),
                )
            }
        }

        if (!isDefault) {
            item {
                Notice(
                    title = "Make MinLauncher your home app",
                    body = "Pressing Home still opens another launcher.",
                    actions = listOf("Choose home app" to { Actions.homeSettings(context) }),
                )
            }
        }

        item { SectionLabel("Home") }
        item {
            Choice(
                title = "Big clock",
                help = "Auto shows it while your home list has 11 rows or fewer.",
                options = listOf("Auto" to ClockMode.AUTO, "Always" to ClockMode.ALWAYS, "Never" to ClockMode.NEVER),
                selected = settings.clockMode,
                onSelect = viewModel::setClockMode,
            )
        }

        item { SectionLabel("Drawer") }
        item { Toggle("Open with the keyboard", null, settings.keyboardOnOpen, viewModel::setKeyboardOnOpen) }
        item { DimSlider(settings.drawerDim, viewModel::setDrawerDim) }
        item {
            Toggle(
                "Open the only match by itself",
                "Off keeps web search one tap away.",
                settings.openOnlyMatch,
                viewModel::setOpenOnlyMatch,
            )
        }
        item {
            Choice(
                title = "Enter key",
                help = null,
                options = listOf("Search the web" to true, "Open top app" to false),
                selected = settings.enterSearchesWeb,
                onSelect = viewModel::setEnterSearchesWeb,
            )
        }

        item { SectionLabel("Gestures") }
        item {
            Toggle(
                "Double-tap to lock",
                "Uses an accessibility service, so fingerprint and face unlock keep working.",
                settings.doubleTapToLock,
                viewModel::setDoubleTapToLock,
            )
        }
        if (settings.doubleTapToLock && !lockRunning) {
            item {
                Notice(
                    title = "Turn on the lock service",
                    body = "Accessibility › Installed apps › MinLauncher double-tap to lock. " +
                        "If it's greyed out: App info › ⋮ › Allow restricted settings, then try again.",
                    actions = listOf(
                        "Accessibility" to { Actions.accessibilitySettings(context) },
                        "App info" to { Actions.appDetailsOfLauncher(context) },
                    ),
                )
            }
        }

        item { SectionLabel("Hidden apps") }
        if (state.hidden.isEmpty()) {
            item { Text("None. Hide apps from their long-press menu.", color = Ui.faint, fontSize = 14.sp) }
        } else {
            items(state.hidden, key = { "hidden-" + it.key }) { app ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(app.label, style = Ui.drawerApp.copy(fontSize = 18.sp), modifier = Modifier.weight(1f))
                    Text(
                        "Unhide",
                        color = Ui.dim,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable { viewModel.unhide(app.key) }
                            .padding(8.dp),
                    )
                }
            }
        }

        item { SectionLabel("About") }
        item { Text("MinLauncher $version", color = Ui.faint, fontSize = 14.sp) }
    }
}

@Composable
private fun DimSlider(value: Float, onChange: (Float) -> Unit) {
    var local by remember(value) { mutableFloatStateOf(value) }
    Column(Modifier.padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Background dim", color = Ui.text, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Text("${(local * 100).roundToInt()}%", color = Ui.dim, fontSize = 14.sp)
        }
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onChange(local) },
            valueRange = 0f..0.8f,
            steps = 15,
            colors = SliderDefaults.colors(thumbColor = Ui.text, activeTrackColor = Ui.accent),
        )
    }
}

@Composable
private fun Toggle(title: String, help: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 6.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Ui.text, fontSize = 16.sp)
            if (help != null) Text(help, color = Ui.faint, fontSize = 13.sp)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Ui.accent),
        )
    }
}

@Composable
private fun <T> Choice(
    title: String,
    help: String?,
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Column(Modifier.padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = Ui.text, fontSize = 16.sp)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            options.forEach { (label, value) ->
                val isSelected = value == selected
                Text(
                    text = label,
                    color = if (isSelected) Color(0xFF1B1A22) else Ui.text,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color(0xFFF2F0F6) else Ui.text.copy(alpha = 0.1f))
                        .clickable { onSelect(value) }
                        .padding(vertical = 10.dp),
                )
            }
        }
        if (help != null) Text(help, color = Ui.faint, fontSize = 13.sp)
    }
}

@Composable
private fun Notice(title: String, body: String, actions: List<Pair<String, () -> Unit>>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Ui.accent.copy(alpha = 0.18f))
            .padding(14.dp),
    ) {
        Text(title, color = Ui.text, fontSize = 16.sp)
        Text(body, color = Ui.dim, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            actions.forEach { (label, action) ->
                Text(
                    text = label,
                    color = Color(0xFF1B1A22),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF2F0F6))
                        .clickable(onClick = action)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}
