package com.example.weatherforecast.presentation.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.weatherforecast.data.model.WeatherAlert
import com.example.weatherforecast.data.repository.IAlertsRepository
import com.example.weatherforecast.presentation.settings.ISettingsDataStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.example.weatherforecast.R

sealed class AlertsUiState {
    object Loading : AlertsUiState()
    data class Success(val alerts: List<WeatherAlert>) : AlertsUiState()
    data class Error(val message: String) : AlertsUiState()
}

class AlertsViewModel(
    private val repository: IAlertsRepository,
    private val alertScheduler: IAlertScheduler,
    private val settingsDataStore: ISettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow<AlertsUiState>(AlertsUiState.Loading)
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<Int>()
    val events: SharedFlow<Int> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.allAlerts
                .catch { e ->
                    _uiState.value = AlertsUiState.Error(e.message ?: "Failed to load alerts")
                }
                .collect { alerts ->
                    _uiState.value = AlertsUiState.Success(alerts)
                }
        }
    }

    fun addAlert(startTime: Long, endTime: Long, alertType: String, snoozeDuration: Int) {
        viewModelScope.launch {
            val alert = WeatherAlert(
                startTime = startTime,
                endTime = endTime,
                alertType = alertType,
                isEnabled = true,
                snoozeDuration = snoozeDuration
            )
            val idList = repository.addAlert(alert)
            val generatedId = idList.toInt()
            
            val lat = settingsDataStore.mapLat.first()
            val lon = settingsDataStore.mapLon.first()
            val savedAlert = alert.copy(id = generatedId)
            alertScheduler.schedule(savedAlert, lat, lon)
            _events.emit(R.string.msg_alert_scheduled)
        }
    }

    fun removeAlert(alert: WeatherAlert) {
        viewModelScope.launch {
            repository.removeAlert(alert)
            alertScheduler.cancel(alert)
            _events.emit(R.string.msg_alert_removed)
        }
    }

    fun toggleAlert(alert: WeatherAlert) {
        viewModelScope.launch {
            val updated = alert.copy(isEnabled = !alert.isEnabled)
            repository.updateAlert(updated)
            if (updated.isEnabled) {
                val lat = settingsDataStore.mapLat.first()
                val lon = settingsDataStore.mapLon.first()
                alertScheduler.schedule(updated, lat, lon)
            } else {
                alertScheduler.cancel(updated)
            }
        }
    }
}

class AlertsViewModelFactory(
    private val repository: IAlertsRepository,
    private val alertScheduler: IAlertScheduler,
    private val settingsDataStore: ISettingsDataStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlertsViewModel::class.java)) {
            return AlertsViewModel(repository, alertScheduler, settingsDataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
