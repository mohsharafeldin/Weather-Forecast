package com.example.weatherforecast.presentation.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.weatherforecast.data.model.DailyForecast
import com.example.weatherforecast.presentation.home.HomeUiState
import com.example.weatherforecast.presentation.home.SharedDayDetailContent
import com.example.weatherforecast.presentation.home.SharedWeatherContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteDetailScreen(
    locationName: String,
    viewModel: FavoritesViewModel,
    onBack: () -> Unit,
    onDayClick: (String) -> Unit = {}
) {
    val detailState by viewModel.detailState.collectAsState()

    val displayName = when (val state = detailState) {
        is FavoriteDetailState.Success -> state.cityName.ifBlank { locationName }
        else -> locationName
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(displayName) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.resetDetailState()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                val current = response.list.firstOrNull() ?: return@Scaffold
                val hourly = response.list.take(24)
                val daily = response.list.groupBy { it.dtTxt.substring(0, 10) }
                    .map { (date, items) ->
                        DailyForecast(
                            date = date,
                            tempMin = items.minOf { it.main.tempMin },
                            tempMax = items.maxOf { it.main.tempMax },
                            icon = items[items.size / 2].weather.firstOrNull()?.icon ?: "01d",
                            description = items[items.size / 2].weather.firstOrNull()?.description ?: ""
                        )
                    }

                val homeUiState = HomeUiState.Success(
                    weatherResponse = response,
                    currentWeather = current,
                    hourlyForecast = hourly,
                    dailyForecast = daily,
                    temperatureUnit = state.tempUnit,
                    windSpeedUnit = state.windUnit
                )

                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    SharedWeatherContent(
                        state = homeUiState,
                        onDayClick = onDayClick
                    )
                }
            }
        }
    }
}

@Composable
fun FavoriteDayDetailScreen(
    date: String,
    viewModel: FavoritesViewModel,
    onBack: () -> Unit
) {
    val detailState by viewModel.detailState.collectAsState()
    if (detailState is FavoriteDetailState.Success) {
        val state = detailState as FavoriteDetailState.Success
        SharedDayDetailContent(
            date = date,
            weatherResponse = state.weatherResponse,
            tempUnit = state.tempUnit,
            windUnit = state.windUnit,
            onBack = onBack
        )
    }
}
