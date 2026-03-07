package com.example.weatherforecast.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.weatherforecast.R
import com.example.weatherforecast.model.WeatherItem
import java.text.SimpleDateFormat
import java.util.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onDayClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = !isOnline,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE53935))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.you_are_offline),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

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
                            text = stringResource(R.string.error_title),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = state.message, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.fetchForecast() }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }
            is HomeUiState.Success -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refreshForecast() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    SharedWeatherContent(state, onDayClick)
                }
            }
        }
    }
}

@Composable
fun SharedWeatherContent(state: HomeUiState.Success, onDayClick: (String) -> Unit = {}) {
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
            SectionTitle(stringResource(R.string.weather_details))
        }
        item {
            WeatherDetailsGrid(state, windUnitLabel)
        }

        item {
            SectionTitle(stringResource(R.string.sun_moon))
        }
        item {
            SunriseSunsetCard(state)
        }

        item {
            SectionTitle(stringResource(R.string.atmosphere))
        }
        item {
            AtmosphereCard(state)
        }

        item {
            SectionTitle(stringResource(R.string.hourly_forecast))
        }
        item {
            HourlyForecastRow(state.hourlyForecast, tempSymbol)
        }

        item {
            SectionTitle(stringResource(R.string.five_day_forecast))
        }
        items(state.dailyForecast) { daily ->
            DailyForecastItem(daily, tempSymbol, onClick = { onDayClick(daily.date) })
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
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
                    text = stringResource(R.string.feels_like, "${current.main.feelsLike.toInt()}$tempSymbol"),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "↓ ${current.main.tempMin.toInt()}$tempSymbol",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(24.dp))
                    Text(
                        text = "↑ ${current.main.tempMax.toInt()}$tempSymbol",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun WeatherDetailsGrid(state: HomeUiState.Success, windUnit: String) {
    val current = state.currentWeather
    val windSpeed = if (windUnit == "mph") {
        current.wind.speed * 2.237
    } else {
        current.wind.speed
    }
    val windGust = if (windUnit == "mph") {
        current.wind.gust * 2.237
    } else {
        current.wind.gust
    }
    val visibilityKm = current.visibility / 1000.0

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DetailCard(
                modifier = Modifier.weight(1f),
                emoji = "💧",
                label = stringResource(R.string.humidity),
                value = "${current.main.humidity}%"
            )
            DetailCard(
                modifier = Modifier.weight(1f),
                emoji = "💨",
                label = stringResource(R.string.wind),
                value = "${"%.1f".format(windSpeed)} $windUnit"
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DetailCard(
                modifier = Modifier.weight(1f),
                emoji = "🌡️",
                label = stringResource(R.string.pressure),
                value = "${current.main.pressure} hPa"
            )
            DetailCard(
                modifier = Modifier.weight(1f),
                emoji = "☁️",
                label = stringResource(R.string.clouds),
                value = "${current.clouds.all}%"
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DetailCard(
                modifier = Modifier.weight(1f),
                emoji = "👁️",
                label = stringResource(R.string.visibility),
                value = "${"%.1f".format(visibilityKm)} km"
            )
            DetailCard(
                modifier = Modifier.weight(1f),
                emoji = "🌧️",
                label = stringResource(R.string.rain_probability),
                value = "${(current.pop * 100).toInt()}%"
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DetailCard(
                modifier = Modifier.weight(1f),
                emoji = "🌀",
                label = stringResource(R.string.wind_gust),
                value = "${"%.1f".format(windGust)} $windUnit"
            )
            DetailCard(
                modifier = Modifier.weight(1f),
                emoji = "🧭",
                label = stringResource(R.string.wind_direction),
                value = "${current.wind.deg}°"
            )
        }
    }
}

@Composable
private fun DetailCard(
    modifier: Modifier = Modifier,
    emoji: String,
    label: String,
    value: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SunriseSunsetCard(state: HomeUiState.Success) {
    val city = state.weatherResponse.city
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    timeFormat.timeZone = TimeZone.getTimeZone("UTC").also {
        val offsetMs = city.timezone * 1000L
        timeFormat.timeZone = TimeZone.getDefault()
    }

    val sunriseDate = Date((city.sunrise + city.timezone) * 1000L)
    val sunsetDate = Date((city.sunset + city.timezone) * 1000L)
    val utcFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    utcFormat.timeZone = TimeZone.getTimeZone("UTC")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "🌅", fontSize = 28.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = utcFormat.format(sunriseDate),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.sunrise),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "🌇", fontSize = 28.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = utcFormat.format(sunsetDate),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.sunset),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun AtmosphereCard(state: HomeUiState.Success) {
    val current = state.currentWeather

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "🌊", fontSize = 24.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${current.main.seaLevel} hPa",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.sea_level_pressure),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "⛰️", fontSize = 24.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${current.main.grndLevel} hPa",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.ground_level_pressure),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun HourlyForecastRow(hourlyForecast: List<WeatherItem>, tempSymbol: String) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(hourlyForecast) { item ->
            HourlyForecastItem(item, tempSymbol)
        }
    }
}

@Composable
private fun HourlyForecastItem(item: WeatherItem, tempSymbol: String) {
    val timeFormat = SimpleDateFormat("hh a", Locale.getDefault())
    val time = timeFormat.format(Date(item.dt * 1000))
    val weatherDesc = item.weather.firstOrNull()

    Card(
        modifier = Modifier
            .width(85.dp)
            .height(130.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp, horizontal = 4.dp)
        ) {
            Text(
                text = time,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            
            AsyncImage(
                model = "https://openweathermap.org/img/wn/${weatherDesc?.icon ?: "01d"}@2x.png",
                contentDescription = weatherDesc?.description,
                modifier = Modifier.size(46.dp)
            )
            
            Text(
                text = "${item.main.temp.toInt()}$tempSymbol",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DailyForecastItem(daily: DailyForecast, tempSymbol: String, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        onClick = onClick
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
