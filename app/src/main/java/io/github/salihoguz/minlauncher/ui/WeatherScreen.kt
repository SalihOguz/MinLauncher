package io.github.salihoguz.minlauncher.ui

import android.text.format.DateFormat
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.salihoguz.minlauncher.domain.Weather
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** Now, the next 24 hours and the week. Opened by tapping the weather line on home. */
@Composable
fun WeatherScreen(viewModel: LauncherViewModel, fahrenheit: Boolean, onSetUp: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val report by viewModel.weather.collectAsStateWithLifecycle()
    BackHandler(onBack = onClose)
    LaunchedEffect(Unit) { viewModel.refreshWeather() }

    val is24 = DateFormat.is24HourFormat(context)
    val hourFormat = remember(is24) { SimpleDateFormat(if (is24) "HH" else "h a", Locale.getDefault()) }
    val timeFormat = remember(is24) { SimpleDateFormat(if (is24) "HH:mm" else "h:mm a", Locale.getDefault()) }
    val dayFormat = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    fun temp(celsius: Double) = Weather.temperature(celsius, fahrenheit)

    LazyColumn(
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF2121118))
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        item(key = "top") {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    report?.place ?: "Weather",
                    color = Ui.dim,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onSetUp)
                        .padding(vertical = 8.dp),
                )
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

        val r = report
        if (r == null) {
            item(key = "empty") {
                Text("No forecast yet. It needs an internet connection.", color = Ui.dim, fontSize = 15.sp)
            }
            return@LazyColumn
        }
        val now = System.currentTimeMillis()
        val today = r.days.firstOrNull { now >= it.date && now < it.date + 24 * 60 * 60 * 1000L } ?: r.days.firstOrNull()

        item(key = "now") {
            Column {
                Text(temp(r.current.temperature), style = Ui.homeApp.copy(fontSize = 66.sp))
                Text(
                    "${Weather.describe(r.current.code)} · feels like ${temp(r.current.feelsLike)}",
                    style = Ui.homeApp.copy(fontSize = 18.sp),
                )
                val details = buildList {
                    today?.let { add("H ${temp(it.max)}  L ${temp(it.min)}") }
                    add("wind ${r.current.windKmh.roundToInt()} km/h")
                    add("humidity ${r.current.humidity}%")
                }.joinToString(" · ")
                Text(details, color = Ui.dim, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }

        val hours = r.hours.filter { it.time >= now - 60 * 60 * 1000L }.take(24)
        if (hours.isNotEmpty()) {
            item(key = "hours-label") { SectionLabel("Next 24 hours") }
            item(key = "hours") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    items(hours, key = { it.time }) { hour ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(hourFormat.format(Date(hour.time)), color = Ui.dim, fontSize = 13.sp)
                            Text(temp(hour.temperature), color = Ui.text, fontSize = 18.sp, modifier = Modifier.padding(vertical = 2.dp))
                            Text(
                                if (hour.rainChance >= 20) "${hour.rainChance}%" else " ",
                                color = Ui.dim,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
        }

        if (r.days.isNotEmpty()) {
            item(key = "days-label") { SectionLabel("This week") }
            items(r.days, key = { "day-${it.date}" }) { day ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(
                        if (day == today) "Today" else dayFormat.format(Date(day.date)),
                        color = Ui.text,
                        fontSize = 16.sp,
                        modifier = Modifier.width(64.dp),
                    )
                    Text(
                        Weather.describe(day.code),
                        color = Ui.dim,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(if (day.rainChance >= 20) "${day.rainChance}%" else "", color = Ui.dim, fontSize = 13.sp)
                    Spacer(Modifier.width(14.dp))
                    Text("${temp(day.min)} / ${temp(day.max)}", color = Ui.text, fontSize = 15.sp)
                }
            }
        }

        today?.let { day ->
            item(key = "sun") {
                Text(
                    "Sunrise ${timeFormat.format(Date(day.sunrise))} · Sunset ${timeFormat.format(Date(day.sunset))}",
                    color = Ui.dim,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }

        item(key = "source") {
            val minutes = ((now - r.fetchedAt) / 60_000).coerceAtLeast(0)
            val age = when {
                minutes < 1 -> "just now"
                minutes < 60 -> "$minutes min ago"
                else -> "${minutes / 60} h ago"
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 16.dp)) {
                Text("Updated $age · Open-Meteo", color = Ui.faint, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text(
                    "Refresh",
                    color = Ui.dim,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable { viewModel.refreshWeather(force = true) }
                        .padding(8.dp),
                )
            }
        }
    }
}
