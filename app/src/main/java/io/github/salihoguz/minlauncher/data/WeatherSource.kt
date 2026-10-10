package io.github.salihoguz.minlauncher.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import io.github.salihoguz.minlauncher.domain.CurrentWeather
import io.github.salihoguz.minlauncher.domain.DayWeather
import io.github.salihoguz.minlauncher.domain.HourWeather
import io.github.salihoguz.minlauncher.domain.WeatherReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.roundToInt

/** A place to show the weather for. */
data class Place(val name: String, val detail: String, val latitude: Double, val longitude: Double)

/**
 * Weather from Open-Meteo (free, no account). Only a rounded position (about 10 km) is sent.
 * The last forecast is kept in a file, so home shows it straight away after a restart.
 */
class WeatherSource(private val context: Context) {

    private val cacheFile = File(context.filesDir, "weather.json")

    suspend fun cached(): WeatherReport? = withContext(Dispatchers.IO) {
        runCatching {
            val saved = JSONObject(cacheFile.readText())
            parse(JSONObject(saved.getString("response")), saved.getString("place"), saved.getLong("fetchedAt"))
        }.getOrNull()
    }

    /** Fetches a fresh forecast; null when offline or the service fails. */
    suspend fun fetch(latitude: Double, longitude: Double, place: String): WeatherReport? = withContext(Dispatchers.IO) {
        runCatching {
            val url = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=${round(latitude)}&longitude=${round(longitude)}" +
                "&current=temperature_2m,apparent_temperature,weather_code,is_day,wind_speed_10m,relative_humidity_2m" +
                "&hourly=temperature_2m,weather_code,precipitation_probability" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset" +
                "&timezone=auto&timeformat=unixtime&forecast_days=7"
            val text = get(url)
            val now = System.currentTimeMillis()
            val report = parse(JSONObject(text), place, now)
            cacheFile.writeText(JSONObject().put("place", place).put("fetchedAt", now).put("response", text).toString())
            report
        }.getOrNull()
    }

    /** Places matching a typed city name. */
    suspend fun search(name: String): List<Place> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "https://geocoding-api.open-meteo.com/v1/search?count=8&format=json" +
                "&language=${Locale.getDefault().language}&name=${Uri.encode(name.trim())}"
            val results = JSONObject(get(url)).optJSONArray("results") ?: JSONArray()
            (0 until results.length()).map { i ->
                val r = results.getJSONObject(i)
                Place(
                    name = r.getString("name"),
                    detail = listOf(r.optString("admin1"), r.optString("country")).filter { it.isNotBlank() }.joinToString(", "),
                    latitude = r.getDouble("latitude"),
                    longitude = r.getDouble("longitude"),
                )
            }
        }.getOrDefault(emptyList())
    }

    fun hasLocationPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** The phone's approximate position, from the network. Null without permission or a fix. */
    suspend fun approximateLocation(): Place? {
        if (!hasLocationPermission()) return null
        val manager = context.getSystemService(LocationManager::class.java)
        val recent = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
        val fresh = recent != null && System.currentTimeMillis() - recent.time < 60 * 60 * 1000L
        val location: Location = (if (fresh) recent else currentLocation(manager) ?: recent) ?: return null
        // Not named with Android's Geocoder: on most phones that sends the position to Google.
        return Place("Your location", "Approximate location", location.latitude, location.longitude)
    }

    private suspend fun currentLocation(manager: LocationManager): Location? {
        if (Build.VERSION.SDK_INT < 30 || !manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) return null
        return withTimeoutOrNull(10_000L) {
            suspendCancellableCoroutine<Location?> { continuation ->
                try {
                    manager.getCurrentLocation(LocationManager.NETWORK_PROVIDER, null, context.mainExecutor) { location ->
                        if (continuation.isActive) continuation.resume(location)
                    }
                } catch (e: SecurityException) {
                    if (continuation.isActive) continuation.resume(null)
                }
            }
        }
    }

    private fun get(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        try {
            check(connection.responseCode == 200) { "HTTP ${connection.responseCode}" }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    /** Two decimals would pin a street; one is enough for weather. */
    private fun round(value: Double): String = String.format(Locale.US, "%.1f", (value * 10).roundToInt() / 10.0)

    private fun parse(json: JSONObject, place: String, fetchedAt: Long): WeatherReport {
        val current = json.getJSONObject("current")
        val hourly = json.getJSONObject("hourly")
        val daily = json.getJSONObject("daily")
        val hourTimes = hourly.getJSONArray("time")
        val dayTimes = daily.getJSONArray("time")
        return WeatherReport(
            place = place,
            fetchedAt = fetchedAt,
            current = CurrentWeather(
                temperature = current.getDouble("temperature_2m"),
                feelsLike = current.optDouble("apparent_temperature", current.getDouble("temperature_2m")),
                code = current.optInt("weather_code"),
                isDay = current.optInt("is_day", 1) == 1,
                windKmh = current.optDouble("wind_speed_10m", 0.0),
                humidity = current.optInt("relative_humidity_2m"),
            ),
            hours = (0 until hourTimes.length()).map { i ->
                HourWeather(
                    time = hourTimes.getLong(i) * 1000,
                    temperature = hourly.getJSONArray("temperature_2m").optDouble(i, 0.0),
                    code = hourly.getJSONArray("weather_code").optInt(i),
                    rainChance = hourly.getJSONArray("precipitation_probability").optInt(i),
                )
            },
            days = (0 until dayTimes.length()).map { i ->
                DayWeather(
                    date = dayTimes.getLong(i) * 1000,
                    code = daily.getJSONArray("weather_code").optInt(i),
                    max = daily.getJSONArray("temperature_2m_max").optDouble(i, 0.0),
                    min = daily.getJSONArray("temperature_2m_min").optDouble(i, 0.0),
                    rainChance = daily.getJSONArray("precipitation_probability_max").optInt(i),
                    sunrise = daily.getJSONArray("sunrise").optLong(i) * 1000,
                    sunset = daily.getJSONArray("sunset").optLong(i) * 1000,
                )
            },
        )
    }
}
