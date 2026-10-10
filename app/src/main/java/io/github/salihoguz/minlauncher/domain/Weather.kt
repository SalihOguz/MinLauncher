package io.github.salihoguz.minlauncher.domain

import kotlin.math.roundToInt

/** A forecast from Open-Meteo, reduced to what the home line and the weather screen show. Times in ms. */
data class WeatherReport(
    val place: String,
    val fetchedAt: Long,
    val current: CurrentWeather,
    val hours: List<HourWeather>,
    val days: List<DayWeather>,
)

data class CurrentWeather(
    val temperature: Double,
    val feelsLike: Double,
    val code: Int,
    val isDay: Boolean,
    val windKmh: Double,
    val humidity: Int,
)

data class HourWeather(val time: Long, val temperature: Double, val code: Int, val rainChance: Int)

data class DayWeather(
    /** Start of the day, local time. */
    val date: Long,
    val code: Int,
    val max: Double,
    val min: Double,
    val rainChance: Int,
    val sunrise: Long,
    val sunset: Long,
)

object Weather {
    /** Fetch again once the last forecast is this old. */
    const val REFRESH_MS = 30 * 60 * 1000L

    /** Older forecasts aren't shown on home: they'd be more wrong than helpful. */
    const val STALE_MS = 6 * 60 * 60 * 1000L

    private const val HINT_HOURS = 12
    private const val LIKELY = 60

    /** Plain words for a WMO weather code, as Open-Meteo sends them. */
    fun describe(code: Int): String = when (code) {
        0 -> "Clear"
        1 -> "Mostly clear"
        2 -> "Partly cloudy"
        3 -> "Cloudy"
        45, 48 -> "Fog"
        51, 53, 55 -> "Drizzle"
        56, 57 -> "Freezing drizzle"
        61 -> "Light rain"
        63 -> "Rain"
        65 -> "Heavy rain"
        66, 67 -> "Freezing rain"
        71 -> "Light snow"
        73 -> "Snow"
        75 -> "Heavy snow"
        77 -> "Snow grains"
        80 -> "Light showers"
        81 -> "Showers"
        82 -> "Heavy showers"
        85, 86 -> "Snow showers"
        95 -> "Thunderstorm"
        96, 99 -> "Thunderstorm with hail"
        else -> "Unknown"
    }

    fun isSnow(code: Int): Boolean = code in 71..77 || code == 85 || code == 86

    /** Rain, drizzle, snow, showers or storms. */
    fun isWet(code: Int): Boolean = code in 51..67 || isSnow(code) || code in 80..82 || code in 95..99

    /** "18°", rounded, in the chosen unit. */
    fun temperature(celsius: Double, fahrenheit: Boolean): String {
        val value = if (fahrenheit) celsius * 9 / 5 + 32 else celsius
        return "${value.roundToInt()}°"
    }

    fun isStale(report: WeatherReport, now: Long): Boolean = now - report.fetchedAt > STALE_MS

    /**
     * The line under the date: "18° Partly cloudy", plus a heads-up when rain or snow is likely
     * in the next 12 hours: "· rain around 15:00". [hourLabel] formats a time as the phone does.
     */
    fun headline(report: WeatherReport, now: Long, fahrenheit: Boolean, hourLabel: (Long) -> String): String {
        val current = report.current
        val base = "${temperature(current.temperature, fahrenheit)} ${describe(current.code)}"
        if (isWet(current.code)) return base
        val soon = report.hours.firstOrNull { hour ->
            hour.time > now && hour.time <= now + HINT_HOURS * 60 * 60 * 1000L &&
                isWet(hour.code) && hour.rainChance >= LIKELY
        } ?: return base
        val what = if (isSnow(soon.code)) "snow" else "rain"
        return "$base · $what around ${hourLabel(soon.time)}"
    }
}
