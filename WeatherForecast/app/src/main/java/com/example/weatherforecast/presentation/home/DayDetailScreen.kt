package com.example.weatherforecast.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weatherforecast.R
import com.example.weatherforecast.data.model.WeatherItem
import com.example.weatherforecast.data.model.WeatherResponse
import com.example.weatherforecast.utils.formatLocal
import com.example.weatherforecast.utils.localizeDigits
import java.text.SimpleDateFormat
import java.util.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedDayDetailContent(
    date: String,
    weatherResponse: WeatherResponse,
    tempUnit: String,
    windUnit: String,
    onBack: () -> Unit
) {
    val dayItems = weatherResponse.list.filter { it.dtTxt.startsWith(date) }

    val tempSymbol = when (tempUnit) {
        "metric" -> stringResource(R.string.unit_celsius)
        "imperial" -> stringResource(R.string.unit_fahrenheit)
        else -> stringResource(R.string.unit_kelvin)
    }

    val formattedDate = try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault())
        val parsed = inputFormat.parse(date)
        parsed?.let { outputFormat.format(it).localizeDigits() } ?: date.localizeDigits()
    } catch (e: Exception) {
        date.localizeDigits()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(formattedDate) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (dayItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.error_title),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    DaySummaryCard(dayItems, tempSymbol, windUnit)
                }

                item {
                    Text(
                        text = stringResource(R.string.hourly_forecast),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(dayItems) { item ->
                    HourlyDetailCard(item, tempSymbol, windUnit)
                }
            }
        }
    }
}

@Composable
fun DayDetailScreen(
    date: String,
    viewModel: HomeViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    if (uiState is HomeUiState.Success) {
        val state = uiState as HomeUiState.Success
        SharedDayDetailContent(
            date = date,
            weatherResponse = state.weatherResponse,
            tempUnit = state.temperatureUnit,
            windUnit = state.windSpeedUnit,
            onBack = onBack
        )
    }
}

@Composable
private fun DaySummaryCard(
    items: List<WeatherItem>,
    tempSymbol: String,
    windUnit: String
) {
    val avgTemp = items.map { it.main.temp }.average()
    val minTemp = items.minOf { it.main.tempMin }
    val maxTemp = items.maxOf { it.main.tempMax }
    val avgHumidity = items.map { it.main.humidity }.average().toInt()
    val avgWind = items.map { it.wind.speed }.average()
    val avgPressure = items.map { it.main.pressure }.average().toInt()
    val mainWeather = items[items.size / 2].weather.firstOrNull()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WeatherIcon(
                iconCode = mainWeather?.icon,
                fontSize = 72.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${avgTemp.toInt()}$tempSymbol".localizeDigits(),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = mainWeather?.description?.replaceFirstChar { it.uppercase() } ?: "",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryItem(stringResource(R.string.min_max), "${minTemp.toInt()}° / ${maxTemp.toInt()}$tempSymbol".localizeDigits())
                SummaryItem(stringResource(R.string.humidity), "$avgHumidity%".localizeDigits())
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val windDisplay = if (windUnit == "mph") avgWind * 2.237 else avgWind
                val windUnitLabel = if (windUnit == "mph") stringResource(R.string.unit_miles_per_hour) else stringResource(R.string.unit_meters_per_second)
                SummaryItem(stringResource(R.string.wind), "${windDisplay.formatLocal(1)} $windUnitLabel")
                SummaryItem(stringResource(R.string.pressure), "${avgPressure.formatLocal()} ${stringResource(R.string.unit_hpa)}")
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun HourlyDetailCard(
    item: WeatherItem,
    tempSymbol: String,
    windUnit: String
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val time = timeFormat.format(Date(item.dt * 1000)).localizeDigits()
    val weatherDesc = item.weather.firstOrNull()
    val windDisplay = if (windUnit == "mph") item.wind.speed * 2.237 else item.wind.speed
    val windUnitLabel = if (windUnit == "mph") stringResource(R.string.unit_miles_per_hour) else stringResource(R.string.unit_meters_per_second)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = time,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                WeatherIcon(
                    iconCode = weatherDesc?.icon,
                    fontSize = 36.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${item.main.temp.toInt().formatLocal()}$tempSymbol",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = weatherDesc?.description?.replaceFirstChar { it.uppercase() } ?: "",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                DetailChip(stringResource(R.string.humidity), "${item.main.humidity.formatLocal()}%")
                DetailChip(stringResource(R.string.wind), "${windDisplay.formatLocal(1)} $windUnitLabel")
                DetailChip(stringResource(R.string.rain_chance), "${(item.pop * 100).toInt().formatLocal()}%")
            }
        }
    }
}

@Composable
private fun DetailChip(label: String, value: String) {
    Row {
        Text(
            text = "$label: ",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
