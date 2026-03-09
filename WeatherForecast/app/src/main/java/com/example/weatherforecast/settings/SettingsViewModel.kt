package com.example.weatherforecast.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val temperatureUnit: String = "metric",
    val windSpeedUnit: String = "m/s",
    val language: String = "en",
    val locationMode: String = "gps",
    val mapLat: Double = 30.0444,
    val mapLon: Double = 31.2357,
    val themeMode: String = "system"
)

class SettingsViewModel(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsDataStore.temperatureUnit.collect { unit ->
                _uiState.value = _uiState.value.copy(temperatureUnit = unit)
            }
        }
        viewModelScope.launch {
            settingsDataStore.windSpeedUnit.collect { unit ->
                _uiState.value = _uiState.value.copy(windSpeedUnit = unit)
            }
        }
        viewModelScope.launch {
            settingsDataStore.language.collect { lang ->
                _uiState.value = _uiState.value.copy(language = lang)
            }
        }
        viewModelScope.launch {
            settingsDataStore.locationMode.collect { mode ->
                _uiState.value = _uiState.value.copy(locationMode = mode)
            }
        }
        viewModelScope.launch {
            settingsDataStore.mapLat.collect { lat ->
                _uiState.value = _uiState.value.copy(mapLat = lat)
            }
        }
        viewModelScope.launch {
            settingsDataStore.mapLon.collect { lon ->
                _uiState.value = _uiState.value.copy(mapLon = lon)
            }
        }
        viewModelScope.launch {
            settingsDataStore.themeMode.collect { mode ->
                _uiState.value = _uiState.value.copy(themeMode = mode)
            }
        }
    }

    fun setTemperatureUnit(unit: String) {
        viewModelScope.launch { settingsDataStore.setTemperatureUnit(unit) }
    }

    fun setWindSpeedUnit(unit: String) {
        viewModelScope.launch { settingsDataStore.setWindSpeedUnit(unit) }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch { settingsDataStore.setLanguage(lang) }
    }

    fun setLocationMode(mode: String) {
        viewModelScope.launch { settingsDataStore.setLocationMode(mode) }
    }

    fun setMapCoordinates(lat: Double, lon: Double) {
        viewModelScope.launch { settingsDataStore.setMapCoordinates(lat, lon) }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { settingsDataStore.setThemeMode(mode) }
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
