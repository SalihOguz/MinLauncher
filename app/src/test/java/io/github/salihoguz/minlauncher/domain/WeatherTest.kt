package io.github.salihoguz.minlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherTest {

    private val hour = 60 * 60 * 1000L
    private val now = 1_000_000 * hour

    private fun report(code: Int, hours: List<HourWeather> = emptyList(), fetchedAt: Long = now) = WeatherReport(
        place = "Istanbul",
        fetchedAt = fetchedAt,
        current = CurrentWeather(temperature = 18.4, feelsLike = 17.0, code = code, isDay = true, windKmh = 10.0, humidity = 60),
        hours = hours,
        days = emptyList(),
    )

    private val label: (Long) -> String = { "${(it - now) / hour}h" }

    @Test
    fun describesCodesInPlainWords() {
        assertEquals("Clear", Weather.describe(0))
        assertEquals("Partly cloudy", Weather.describe(2))
        assertEquals("Rain", Weather.describe(63))
        assertEquals("Thunderstorm with hail", Weather.describe(99))
        assertTrue(Weather.isWet(61))
        assertTrue(Weather.isSnow(73))
        assertFalse(Weather.isWet(3))
    }

    @Test
    fun roundsTemperaturesInEitherUnit() {
        assertEquals("18°", Weather.temperature(18.4, fahrenheit = false))
        assertEquals("65°", Weather.temperature(18.4, fahrenheit = true))
        assertEquals("-3°", Weather.temperature(-2.6, fahrenheit = false))
    }

    @Test
    fun headlineWarnsAboutLikelyRainInTheNext12Hours() {
        val hours = listOf(
            HourWeather(now + 1 * hour, 18.0, 3, 20),
            HourWeather(now + 3 * hour, 16.0, 61, 70),
        )
        assertEquals("18° Partly cloudy · rain around 3h", Weather.headline(report(2, hours), now, false, label))
        // Unlikely or far-off rain stays out.
        val unlikely = listOf(HourWeather(now + 3 * hour, 16.0, 61, 30), HourWeather(now + 14 * hour, 16.0, 63, 90))
        assertEquals("18° Partly cloudy", Weather.headline(report(2, unlikely), now, false, label))
        // Already raining: no heads-up.
        assertEquals("18° Rain", Weather.headline(report(63, hours), now, false, label))
    }

    @Test
    fun snowIsCalledSnow() {
        val hours = listOf(HourWeather(now + 2 * hour, -1.0, 73, 80))
        assertEquals("18° Cloudy · snow around 2h", Weather.headline(report(3, hours), now, false, label))
    }

    @Test
    fun oldForecastsAreStale() {
        assertFalse(Weather.isStale(report(0, fetchedAt = now - 2 * hour), now))
        assertTrue(Weather.isStale(report(0, fetchedAt = now - 7 * hour), now))
    }
}
