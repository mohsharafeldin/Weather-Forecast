package com.example.weatherforecast.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.weatherforecast.model.WeatherItem
import java.text.SimpleDateFormat
import java.util.*


@Composable
fun HomeScreen(
    viewModel: HomeViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is HomeUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is HomeUiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Error",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = state.message, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.fetchForecast() }) {
                        Text("Retry")
                    }
                }
            }
        }
        is HomeUiState.Success -> {
            HomeContent(state)
        }
    }
}

@Composable
private fun HomeContent(state: HomeUiState.Success) {
    val tempSymbol = when (state.temperatureUnit) {
        "metric" -> "°C"
        "imperial" -> "°F"
        else -> "K"
    }
    val windUnitLabel = state.windSpeedUnit

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {

        item {
            CurrentWeatherCard(state, tempSymbol)
        }


        item {
            WeatherDetailsRow(state, windUnitLabel)
        }


        item {
            Text(
                text = "Hourly Forecast",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        item {
            HourlyForecastRow(state.hourlyForecast, tempSymbol)
        }


        item {
            Text(
                text = "5-Day Forecast",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        items(state.dailyForecast) { daily ->
            DailyForecastItem(daily, tempSymbol)
        }
    }
}

@Composable
private fun CurrentWeatherCard(state: HomeUiState.Success, tempSymbol: String) {
    val current = state.currentWeather
    val weatherDesc = current.weather.firstOrNull()
    val dateFormat = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val now = Date()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = state.weatherResponse.city.name + ", " + state.weatherResponse.city.country,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateFormat.format(now),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = timeFormat.format(now),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                AsyncImage(
                    model = "https://openweathermap.org/img/wn/${weatherDesc?.icon ?: "01d"}@4x.png",
                    contentDescription = weatherDesc?.description,
                    modifier = Modifier.size(100.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${current.main.temp.toInt()}$tempSymbol",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = weatherDesc?.description?.replaceFirstChar { it.uppercase() } ?: "",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Feels like ${current.main.feelsLike.toInt()}$tempSymbol",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun WeatherDetailsRow(state: HomeUiState.Success, windUnitLabel: String) {
    val current = state.currentWeather
    val windSpeed = if (windUnitLabel == "mph") {
        current.wind.speed * 2.237 
    } else {
        current.wind.speed
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        WeatherDetailItem("Humidity", "${current.main.humidity}%")
        WeatherDetailItem("Wind", "${"%.1f".format(windSpeed)} $windUnitLabel")
        WeatherDetailItem("Pressure", "${current.main.pressure} hPa")
        WeatherDetailItem("Clouds", "${current.clouds.all}%")
    }
}

@Composable
private fun WeatherDetailItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun HourlyForecastRow(hourlyList: List<WeatherItem>, tempSymbol: String) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(hourlyList) { item ->
            HourlyForecastItem(item, tempSymbol)
        }
    }
}

@Composable
private fun HourlyForecastItem(item: WeatherItem, tempSymbol: String) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val time = timeFormat.format(Date(item.dt * 1000))
    val weatherDesc = item.weather.firstOrNull()

    Card(
        modifier = Modifier
            .width(80.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
        ) {
            Text(text = time, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AsyncImage(
                model = "https://openweathermap.org/img/wn/${weatherDesc?.icon ?: "01d"}@2x.png",
                contentDescription = weatherDesc?.description,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = "${item.main.temp.toInt()}$tempSymbol",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DailyForecastItem(daily: DailyForecast, tempSymbol: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatDailyDate(daily.date),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = daily.description.replaceFirstChar { it.uppercase() },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            AsyncImage(
                model = "https://openweathermap.org/img/wn/${daily.icon}@2x.png",
                contentDescription = daily.description,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${daily.tempMax.toInt()}° / ${daily.tempMin.toInt()}$tempSymbol",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDailyDate(dateStr: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
        val date = inputFormat.parse(dateStr)
        date?.let { outputFormat.format(it) } ?: dateStr
    } catch (e: Exception) {
        dateStr
    }
}
