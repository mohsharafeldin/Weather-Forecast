package com.example.weatherforecast.presentation.alerts

import com.example.weatherforecast.data.model.WeatherAlert

interface IAlertScheduler {
    fun schedule(alert: WeatherAlert, lat: Double, lon: Double)
    fun cancel(alert: WeatherAlert)
}
