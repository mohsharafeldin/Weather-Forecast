package com.example.weatherforecast.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.weatherforecast.model.WeatherAlert
import com.example.weatherforecast.repository.IWeatherRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlertsViewModel(
    private val repository: IWeatherRepository
) : ViewModel() {

    val alerts: StateFlow<List<WeatherAlert>> = repository.getAllAlerts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addAlert(startTime: Long, endTime: Long, alertType: String) {
        viewModelScope.launch {
            repository.addAlert(
                WeatherAlert(
                    startTime = startTime,
                    endTime = endTime,
                    alertType = alertType,
                    isEnabled = true
                )
            )
        }
    }

    fun removeAlert(alert: WeatherAlert) {
        viewModelScope.launch {
            repository.removeAlert(alert)
        }
    }

    fun toggleAlert(alert: WeatherAlert) {
        viewModelScope.launch {
            repository.updateAlert(alert.copy(isEnabled = !alert.isEnabled))
        }
    }
}

class AlertsViewModelFactory(
    private val repository: IWeatherRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlertsViewModel::class.java)) {
            return AlertsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
