package com.example.weatherforecast.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.weatherforecast.data.model.WeatherItem
import com.example.weatherforecast.data.model.WeatherResponse
import com.example.weatherforecast.data.model.DailyForecast
import com.example.weatherforecast.data.network.IConnectivityObserver
import com.example.weatherforecast.data.repository.IForecastRepository
import com.example.weatherforecast.presentation.settings.ISettingsDataStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.weatherforecast.R
import com.example.weatherforecast.utils.WeatherInterpolator

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

@Suppress("unused")
class HomeViewModel(
    private val repository: IForecastRepository,
    private val settingsDataStore: ISettingsDataStore,
    connectivityObserver: IConnectivityObserver
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<Int>()
    val snackbarEvents: SharedFlow<Int> = _snackbarEvents.asSharedFlow()

    val isOnline: StateFlow<Boolean> = connectivityObserver.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private var lastGpsLat: Double? = null
    private var lastGpsLon: Double? = null

    init {
        viewModelScope.launch {
            combine(
                settingsDataStore.temperatureUnit,
                settingsDataStore.language,
                settingsDataStore.windSpeedUnit,
                settingsDataStore.locationMode,
                settingsDataStore.mapLat,
                settingsDataStore.mapLon
            ) { values ->
                values
            }.collect { _ ->
                if (_uiState.value !is HomeUiState.Loading || lastGpsLat != null) {
                    fetchForecast(lastGpsLat, lastGpsLon)
                }
            }
        }
    }

    fun refreshForecast() {
        viewModelScope.launch {
            _isRefreshing.value = true
            fetchForecastInternal()
            _isRefreshing.value = false
        }
    }

    fun fetchForecast(gpsLat: Double? = null, gpsLon: Double? = null) {
        if (gpsLat != null && gpsLon != null) {
            lastGpsLat = gpsLat
            lastGpsLon = gpsLon
        }

        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            fetchForecastInternal()
        }
    }

    private suspend fun fetchForecastInternal() {
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

            repository.getForecast(lat, lon, tempUnit, lang)
                .catch { e ->
                    val cached = repository.getCachedForecastSync()
                    if (cached != null) {
                        applyResponse(cached, tempUnit, windUnit)
                        _snackbarEvents.emit(R.string.msg_offline_cached)
                    } else {
                        _uiState.value = HomeUiState.Error(e.message ?: "Unknown error occurred")
                    }
                }
                .collect { response ->
                    repository.cacheForecast(response)
                    applyResponse(response, tempUnit, windUnit)
                }
        } catch (e: Exception) {
            _uiState.value = HomeUiState.Error(e.message ?: "Unknown error occurred")
        }
    }

    private fun applyResponse(response: WeatherResponse, tempUnit: String, windUnit: String) {
        val interpolatedList = WeatherInterpolator.interpolateWeatherList(response.list)
        val interpolatedResponse = response.copy(list = interpolatedList)

        val current = interpolatedList.firstOrNull() ?: response.list.first()
        val hourly = interpolatedList.take(24)

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
            weatherResponse = interpolatedResponse,
            currentWeather = current,
            hourlyForecast = hourly,
            dailyForecast = daily,
            temperatureUnit = tempUnit,
            windSpeedUnit = windUnit
        )
    }
}

class HomeViewModelFactory(
    private val repository: IForecastRepository,
    private val settingsDataStore: ISettingsDataStore,
    private val connectivityObserver: IConnectivityObserver
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(repository, settingsDataStore, connectivityObserver) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

