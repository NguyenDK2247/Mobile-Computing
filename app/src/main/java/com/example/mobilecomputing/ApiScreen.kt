// ApiScreen function
//
// Shows user weather updates for Oulu: temperature and wind contidions

package com.example.mobilecomputing

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URL

data class WeatherData(
    val temperature: Double,
    val windspeed: Double,
    val weatherCode: Int
)

fun weatherCodeToDescription(code: Int): String = when (code) {
    0 -> "Clear sky ☀️"
    1, 2, 3 -> "Partly cloudy ⛅"
    45, 48 -> "Foggy 🌫️"
    51, 53, 55 -> "Drizzle 🌦️"
    61, 63, 65 -> "Rain 🌧️"
    71, 73, 75 -> "Snow 🌨️"
    80, 81, 82 -> "Rain showers 🌩️"
    95 -> "Thunderstorm ⛈️"
    else -> "Unknown ($code)"
}

@Composable
fun ApiScreen(navController: NavHostController) {
    var weatherData by remember { mutableStateOf<WeatherData?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun fetchWeather() {
        isLoading = true
        error = null
        weatherData = null
        try {
            val result = withContext(Dispatchers.IO) {
                val url = "https://api.open-meteo.com/v1/forecast?" +
                        "latitude=65.0121&longitude=25.4651" +
                        "&current_weather=true"
                val json = URL(url).readText()
                val obj = JSONObject(json).getJSONObject("current_weather")
                WeatherData(
                    temperature = obj.getDouble("temperature"),
                    windspeed = obj.getDouble("windspeed"),
                    weatherCode = obj.getInt("weathercode")
                )
            }
            weatherData = result
        } catch (e: Exception) {
            error = "Failed to fetch: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    // Fetch on first composition
    LaunchedEffect(Unit) { fetchWeather() }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { navController.popBackStack() }) { Text("← Back") }
            Spacer(modifier = Modifier.weight(1f))
            Text("Weather API", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.weight(1f))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Current Weather — Oulu",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                "Source: Open-Meteo (open-meteo.com)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            when {
                isLoading -> CircularProgressIndicator()
                error != null -> {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { scope.launch { fetchWeather() } }) { Text("Retry") }
                }
                weatherData != null -> {
                    val w = weatherData!!
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                weatherCodeToDescription(w.weatherCode),
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "🌡 Temperature: ${w.temperature} °C",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "💨 Wind speed: ${w.windspeed} km/h",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { scope.launch { fetchWeather() } }) { Text("Refresh") }
                }
            }
        }
    }
}