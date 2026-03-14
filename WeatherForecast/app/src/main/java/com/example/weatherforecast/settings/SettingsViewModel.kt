package com.example.weatherforecast.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.example.weatherforecast.R

sealed class SettingsUiState {
    object Loading : SettingsUiState()
    data class Success(
        val temperatureUnit: String = "metric",
        val windSpeedUnit: String = "m/s",
        val language: String = "en",
        val locationMode: String = "gps",
        val mapLat: Double = 30.0444,
        val mapLon: Double = 31.2357,
        val mapCityName: String = "",
        val themeMode: String = "system"
    ) : SettingsUiState()
    data class Error(val message: String) : SettingsUiState()
}

class SettingsViewModel(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<Int>()
    val events: SharedFlow<Int> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            try {
                combine(
                    settingsDataStore.temperatureUnit,
                    settingsDataStore.windSpeedUnit,
                    settingsDataStore.language,
                    settingsDataStore.locationMode,
                    settingsDataStore.mapLat,
                    settingsDataStore.mapLon,
                    settingsDataStore.mapCityName
                ) { values ->
                    SettingsUiState.Success(
                        temperatureUnit = values[0] as String,
                        windSpeedUnit = values[1] as String,
                        language = values[2] as String,
                        locationMode = values[3] as String,
                        mapLat = values[4] as Double,
                        mapLon = values[5] as Double,
                        mapCityName = values[6] as String,
                        themeMode = (_uiState.value as? SettingsUiState.Success)?.themeMode ?: "system"
                    )
                }.collect { state ->
                    _uiState.value = state
                }
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.message ?: "Failed to load settings")
            }
        }
        viewModelScope.launch {
            settingsDataStore.themeMode.collect { mode ->
                val current = _uiState.value
                if (current is SettingsUiState.Success) {
                    _uiState.value = current.copy(themeMode = mode)
                }
            }
        }
    }

    fun setTemperatureUnit(unit: String) {
        viewModelScope.launch {
            settingsDataStore.setTemperatureUnit(unit)
            _events.emit(R.string.msg_temp_updated)
        }
    }

    fun setWindSpeedUnit(unit: String) {
        viewModelScope.launch {
            settingsDataStore.setWindSpeedUnit(unit)
            _events.emit(R.string.msg_wind_updated)
        }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            settingsDataStore.setLanguage(lang)
            _events.emit(R.string.msg_lang_updated)
        }
    }

    fun setLocationMode(mode: String) {
        viewModelScope.launch {
            settingsDataStore.setLocationMode(mode)
            _events.emit(R.string.msg_location_updated)
        }
    }

    fun setMapCoordinates(lat: Double, lon: Double, cityName: String = "") {
        viewModelScope.launch {
            settingsDataStore.setMapCoordinates(lat, lon, cityName)
            _events.emit(R.string.msg_map_updated)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            settingsDataStore.setThemeMode(mode)
            _events.emit(R.string.msg_theme_updated)
        }
    }
}

class SettingsViewModelFactory(
    private val settingsDataStore: SettingsDataStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(settingsDataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
