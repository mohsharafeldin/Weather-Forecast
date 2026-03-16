package com.example.weatherforecast.presentation.home

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import com.example.weatherforecast.R
import com.example.weatherforecast.data.model.WeatherItem
import com.example.weatherforecast.data.model.DailyForecast
import com.example.weatherforecast.utils.formatLocal
import com.example.weatherforecast.utils.localizeDigits
import java.text.SimpleDateFormat
import java.util.*
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onDayClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { messageId ->
            snackbarHostState.showSnackbar(context.getString(messageId))
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        viewModel.fetchForecast(location.latitude, location.longitude)
                    } else {
                        viewModel.fetchForecast()
                    }
                }.addOnFailureListener {
                    viewModel.fetchForecast()
                }
            } catch (e: SecurityException) {
                viewModel.fetchForecast()
            }
        } else {
            viewModel.fetchForecast()
        }
    }

    LaunchedEffect(Unit) {
        if (viewModel.uiState.value is HomeUiState.Loading) {
            val permissionsToRequest = mutableListOf<String>()
            val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            
            if (!hasFine && !hasCoarse) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
                permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            if (permissionsToRequest.isNotEmpty()) {
                permissionLauncher.launch(permissionsToRequest.toTypedArray())
            } else {
                if (hasFine || hasCoarse) {
                    try {
                        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                            if (location != null) {
                                viewModel.fetchForecast(location.latitude, location.longitude)
                            } else {
                                viewModel.fetchForecast()
                            }
                        }.addOnFailureListener {
                            viewModel.fetchForecast()
                        }
                    } catch (e: SecurityException) {
                        viewModel.fetchForecast()
                    }
                } else {
                    viewModel.fetchForecast()
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { scaffoldPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(scaffoldPadding)) {
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
}

@Composable
fun SharedWeatherContent(state: HomeUiState.Success, onDayClick: (String) -> Unit = {}) {
    val tempSymbol = when (state.temperatureUnit) {
        "metric" -> stringResource(R.string.unit_celsius)
        "imperial" -> stringResource(R.string.unit_fahrenheit)
        else -> stringResource(R.string.unit_kelvin)
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
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
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
                    text = state.weatherResponse.city.name + ", " + java.util.Locale("", state.weatherResponse.city.country).getDisplayCountry(java.util.Locale.getDefault()),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateFormat.format(now).localizeDigits(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                )
                Text(
                    text = timeFormat.format(now).localizeDigits(),
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Spacer(modifier = Modifier.height(20.dp))
                WeatherIcon(
                    iconCode = weatherDesc?.icon,
                    fontSize = 110.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${current.main.temp.toInt().formatLocal()}$tempSymbol",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = weatherDesc?.description?.replaceFirstChar { it.uppercase() } ?: "",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.feels_like, "${current.main.feelsLike.toInt().formatLocal()}$tempSymbol"),
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.temp_min),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${current.main.tempMin.toInt().formatLocal()}$tempSymbol",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Divider(
                        modifier = Modifier
                            .height(30.dp)
                            .width(1.dp),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.temp_max),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${current.main.tempMax.toInt().formatLocal()}$tempSymbol",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
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
    val windUnitLocale = if (windUnit == "mph") stringResource(R.string.unit_miles_per_hour) else stringResource(R.string.unit_meters_per_second)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DetailCard(
                modifier = Modifier.weight(1f),
                emoji = "💧",
                label = stringResource(R.string.humidity),
                value = "${current.main.humidity.formatLocal()}%"
            )
            DetailCard(
                modifier = Modifier.weight(1f),
                emoji = "💨",
                label = stringResource(R.string.wind),
                value = "${windSpeed.formatLocal(1)} $windUnitLocale"
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
                value = "${current.main.pressure.formatLocal()} ${stringResource(R.string.unit_hpa)}"
            )
            DetailCard(
                modifier = Modifier.weight(1f),
                emoji = "☁️",
                label = stringResource(R.string.clouds),
                value = "${current.clouds.all.formatLocal()}%"
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
                value = "${visibilityKm.formatLocal(1)} ${stringResource(R.string.unit_km)}"
            )
            DetailCard(
                modifier = Modifier.weight(1f),
                emoji = "🌧️",
                label = stringResource(R.string.rain_probability),
                value = "${(current.pop * 100).toInt().formatLocal()}%"
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
                value = "${windGust.formatLocal(1)} $windUnitLocale"
            )
            DetailCard(
                modifier = Modifier.weight(1f),
                emoji = "🧭",
                label = stringResource(R.string.wind_direction),
                value = "${current.wind.deg.formatLocal()}°"
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
        modifier = modifier
            .padding(2.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "🌅", fontSize = 32.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = utcFormat.format(sunriseDate).localizeDigits(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.sunrise),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "🌇", fontSize = 32.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = utcFormat.format(sunsetDate).localizeDigits(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.sunset),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun AtmosphereCard(state: HomeUiState.Success) {
    val current = state.currentWeather

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "🌊", fontSize = 28.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${current.main.seaLevel.formatLocal()} ${stringResource(R.string.unit_hpa)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.sea_level_pressure),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "⛰️", fontSize = 28.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${current.main.grndLevel.formatLocal()} ${stringResource(R.string.unit_hpa)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.ground_level_pressure),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
    val time = timeFormat.format(Date(item.dt * 1000)).localizeDigits()
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
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            
            WeatherIcon(
                iconCode = weatherDesc?.icon,
                fontSize = 40.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            
            Text(
                text = "${item.main.temp.toInt().formatLocal()}$tempSymbol",
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
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
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            WeatherIcon(
                iconCode = daily.icon,
                fontSize = 46.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${daily.tempMax.toInt().formatLocal()}° / ${daily.tempMin.toInt().formatLocal()}$tempSymbol",
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
        date?.let { outputFormat.format(it).localizeDigits() } ?: dateStr.localizeDigits()
    } catch (e: Exception) {
        dateStr.localizeDigits()
    }
}

@Composable
fun WeatherIcon(iconCode: String?, fontSize: TextUnit, modifier: Modifier = Modifier) {
    val emoji = when (iconCode) {
        "01d" -> "☀️"
        "01n" -> "🌙"
        "02d" -> "⛅"
        "02n" -> "☁️"
        "03d", "03n" -> "☁️"
        "04d", "04n" -> "☁️"
        "09d", "09n" -> "🌧️"
        "10d" -> "🌦️"
        "10n" -> "🌧️"
        "11d", "11n" -> "⛈️"
        "13d", "13n" -> "❄️"
        "50d", "50n" -> "🌫️"
        else -> "☀️"
    }
    Text(
        text = emoji,
        fontSize = fontSize,
        modifier = modifier
    )
}
