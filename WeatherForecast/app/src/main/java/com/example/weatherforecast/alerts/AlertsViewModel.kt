package com.example.weatherforecast.alerts

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.weatherforecast.model.WeatherAlert
import com.example.weatherforecast.repository.IWeatherRepository
import com.example.weatherforecast.settings.SettingsDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlertsViewModel(
    private val repository: IWeatherRepository,
    private val context: Context,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val alerts: StateFlow<List<WeatherAlert>> = repository.getAllAlerts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
            AlertScheduler.schedule(context, savedAlert, lat, lon)
        }
    }

    fun removeAlert(alert: WeatherAlert) {
        viewModelScope.launch {
            AlertScheduler.cancel(context, alert)
            repository.removeAlert(alert)
        }
    }

    fun toggleAlert(alert: WeatherAlert) {
        viewModelScope.launch {
            val updated = alert.copy(isEnabled = !alert.isEnabled)
            repository.updateAlert(updated)
            if (updated.isEnabled) {
                val lat = settingsDataStore.mapLat.first()
                val lon = settingsDataStore.mapLon.first()
                AlertScheduler.schedule(context, updated, lat, lon)
            } else {
                AlertScheduler.cancel(context, updated)
            }
        }
    }
}

class AlertsViewModelFactory(
    private val repository: IWeatherRepository,
    private val context: Context,
    private val settingsDataStore: SettingsDataStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlertsViewModel::class.java)) {
            return AlertsViewModel(repository, context, settingsDataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
