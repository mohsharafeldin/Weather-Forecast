package com.example.weatherforecast.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteDetailScreen(
    locationName: String,
    viewModel: FavoritesViewModel,
    onBack: () -> Unit
) {
    val detailState by viewModel.detailState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(locationName) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.resetDetailState()
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = detailState) {
            is FavoriteDetailState.Idle, is FavoriteDetailState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is FavoriteDetailState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is FavoriteDetailState.Success -> {
                val response = state.weatherResponse
                val tempSymbol = when (state.tempUnit) {
                    "metric" -> "°C"
                    "imperial" -> "°F"
                    else -> "K"
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    val current = response.list.firstOrNull()
                    if (current != null) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${response.city.name}, ${response.city.country}",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    AsyncImage(
                                        model = "https://openweathermap.org/img/wn/${current.weather.firstOrNull()?.icon ?: "01d"}@4x.png",
                                        contentDescription = null,
                                        modifier = Modifier.size(80.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                    Text(
                                        text = "${current.main.temp.toInt()}$tempSymbol",
                                        fontSize = 48.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = current.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "",
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        DetailItem("Humidity", "${current.main.humidity}%")
                                        DetailItem("Wind", "${"%.1f".format(current.wind.speed)} ${state.windUnit}")
                                        DetailItem("Pressure", "${current.main.pressure} hPa")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            "Hourly Forecast",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(response.list.take(8)) { item ->
                                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Text(timeFormat.format(Date(item.dt * 1000)), fontSize = 12.sp)
                                        AsyncImage(
                                            model = "https://openweathermap.org/img/wn/${item.weather.firstOrNull()?.icon ?: "01d"}@2x.png",
                                            contentDescription = null,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Text("${item.main.temp.toInt()}$tempSymbol", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            "5-Day Forecast",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    val dailyGrouped = response.list.groupBy { it.dtTxt.substring(0, 10) }
                    items(dailyGrouped.entries.toList()) { (date, dayItems) ->
                        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val outputFormat = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
                        val displayDate = try {
                            inputFormat.parse(date)?.let { outputFormat.format(it) } ?: date
                        } catch (e: Exception) { date }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(displayDate, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                AsyncImage(
                                    model = "https://openweathermap.org/img/wn/${dayItems[dayItems.size / 2].weather.firstOrNull()?.icon ?: "01d"}@2x.png",
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    "${dayItems.maxOf { it.main.tempMax }.toInt()}° / ${dayItems.minOf { it.main.tempMin }.toInt()}$tempSymbol",
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
    }
}
