package com.example.weatherforecast.presentation.settings

import kotlinx.coroutines.flow.Flow

interface ISettingsDataStore {
    val temperatureUnit: Flow<String>
    val windSpeedUnit: Flow<String>
    val language: Flow<String>
    val locationMode: Flow<String>
    val mapLat: Flow<Double>
    val mapLon: Flow<Double>
    val mapCityName: Flow<String>
    val themeMode: Flow<String>

    suspend fun setTemperatureUnit(unit: String)
    suspend fun setWindSpeedUnit(unit: String)
    suspend fun setLanguage(lang: String)
    suspend fun setLocationMode(mode: String)
    suspend fun setMapCoordinates(lat: Double, lon: Double, cityName: String = "")
    suspend fun setThemeMode(mode: String)
}
