package com.example.weatherforecast.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.weatherforecast.model.WeatherItem
import com.example.weatherforecast.model.WeatherResponse
import com.example.weatherforecast.repository.IWeatherRepository
import com.example.weatherforecast.settings.SettingsDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val weatherResponse: WeatherResponse,
        val currentWeather: WeatherItem,
        val hourlyForecast: List<WeatherItem>,
        val dailyForecast: List<DailyForecast>,
        val temperatureUnit: String,
        val windSpeedUnit: String
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

data class DailyForecast(
    val date: String,
    val tempMin: Double,
    val tempMax: Double,
    val icon: String,
    val description: String
)

class HomeViewModel(
    private val repository: IWeatherRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var lastGpsLat: Double? = null
    private var lastGpsLon: Double? = null

    init {
        viewModelScope.launch {
            combine(
                settingsDataStore.temperatureUnit,
                settingsDataStore.language,
                settingsDataStore.windSpeedUnit
            ) { tempUnit, lang, windUnit ->
                Triple(tempUnit, lang, windUnit)
            }.collect { _ ->
                if (_uiState.value !is HomeUiState.Loading || lastGpsLat != null) {
                    fetchForecast(lastGpsLat, lastGpsLon)
                }
            }
        }
    }

    fun fetchForecast(gpsLat: Double? = null, gpsLon: Double? = null) {
        if (gpsLat != null && gpsLon != null) {
            lastGpsLat = gpsLat
            lastGpsLon = gpsLon
        }

        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val locationMode = settingsDataStore.locationMode.first()
                val tempUnit = settingsDataStore.temperatureUnit.first()
                val lang = settingsDataStore.language.first()
                val windUnit = settingsDataStore.windSpeedUnit.first()

                val lat: Double
                val lon: Double
                if (locationMode == "gps" && lastGpsLat != null && lastGpsLon != null) {
                    lat = lastGpsLat!!
                    lon = lastGpsLon!!
                } else {
                    lat = settingsDataStore.mapLat.first()
                    lon = settingsDataStore.mapLon.first()
                }

                val response = repository.getForecast(lat, lon, tempUnit, lang)
                val current = response.list.first()
                val hourly = response.list.take(8)

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

                _uiState.value = HomeUiState.Success(
                    weatherResponse = response,
                    currentWeather = current,
                    hourlyForecast = hourly,
                    dailyForecast = daily,
                    temperatureUnit = tempUnit,
                    windSpeedUnit = windUnit
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
}

class HomeViewModelFactory(
    private val repository: IWeatherRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(repository, settingsDataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
