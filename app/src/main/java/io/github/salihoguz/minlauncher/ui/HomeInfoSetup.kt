package io.github.salihoguz.minlauncher.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.salihoguz.minlauncher.data.PhoneCalendar
import io.github.salihoguz.minlauncher.data.Place
import io.github.salihoguz.minlauncher.data.WeatherMode
import io.github.salihoguz.minlauncher.system.Actions
import kotlinx.coroutines.launch

/** Choose where the weather line gets its weather: approximate location, a city, or none. */
@Composable
fun WeatherSetupDialog(viewModel: LauncherViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Place>?>(null) }
    var searching by remember { mutableStateOf(false) }

    fun useLocation() {
        viewModel.setWeatherMode(WeatherMode.LOCATION)
        onDismiss()
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) useLocation() else Actions.toast(context, "Without location access, type a city instead")
    }
    fun search() {
        if (query.isBlank() || searching) return
        searching = true
        scope.launch {
            results = viewModel.searchPlaces(query)
            searching = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Weather") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Shown under the date. Forecasts come from Open-Meteo; only a rough position (about 10 km) is sent.",
                    color = Ui.dim,
                    fontSize = 13.sp,
                )
                Chip("Use my approximate location", selected = true) {
                    if (viewModel.hasLocationPermission()) useLocation()
                    else permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    label = { Text("Or type a city") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { search() }),
                )
                val found = results
                when {
                    searching -> Text("Searching…", color = Ui.dim, fontSize = 13.sp)
                    found != null && found.isEmpty() -> Text("No places found. Check the spelling, or you may be offline.", color = Ui.dim, fontSize = 13.sp)
                    found != null -> LazyColumn(Modifier.heightIn(max = 240.dp)) {
                        items(found) { place ->
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setWeatherCity(place)
                                        onDismiss()
                                    }
                                    .padding(vertical = 8.dp),
                            ) {
                                Text(place.name, fontSize = 16.sp)
                                if (place.detail.isNotBlank()) Text(place.detail, color = Ui.dim, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(enabled = query.isNotBlank() && !searching, onClick = ::search) { Text("Search") } },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    viewModel.setWeatherMode(WeatherMode.OFF)
                    onDismiss()
                }) { Text("No weather") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

/** Pick the calendars the home line reads. None picked: no calendar line. */
@Composable
fun CalendarPickerDialog(viewModel: LauncherViewModel, selected: Set<Long>, onDismiss: () -> Unit) {
    var granted by remember { mutableStateOf(viewModel.hasCalendarPermission()) }
    var calendars by remember { mutableStateOf<List<PhoneCalendar>?>(null) }
    var picked by remember { mutableStateOf(selected) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(granted) {
        if (granted) calendars = viewModel.loadCalendars() else permission.launch(Manifest.permission.READ_CALENDAR)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Calendar line") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Shows today's next event under the date, and nothing on a free day. MinLauncher only reads.",
                    color = Ui.dim,
                    fontSize = 13.sp,
                )
                val list = calendars
                when {
                    !granted -> Chip("Allow calendar access", selected = true) {
                        permission.launch(Manifest.permission.READ_CALENDAR)
                    }
                    list == null -> Text("Loading…", color = Ui.dim, fontSize = 13.sp)
                    list.isEmpty() -> Text("No calendars on this phone.", color = Ui.dim, fontSize = 13.sp)
                    else -> LazyColumn(Modifier.heightIn(max = 320.dp)) {
                        items(list, key = { it.id }) { calendar ->
                            val checked = calendar.id in picked
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { picked = if (checked) picked - calendar.id else picked + calendar.id }
                                    .padding(vertical = 2.dp),
                            ) {
                                Box(
                                    Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(calendar.color or 0xFF000000.toInt())),
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(calendar.name, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(calendar.account, color = Ui.dim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { picked = if (it) picked + calendar.id else picked - calendar.id },
                                    colors = CheckboxDefaults.colors(checkedColor = Ui.accent),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.setCalendars(picked)
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
