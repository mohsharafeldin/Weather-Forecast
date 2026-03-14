package com.example.weatherforecast.repository

import com.example.weatherforecast.model.FavoriteLocation
import com.example.weatherforecast.model.GeocodingResult
import com.example.weatherforecast.model.WeatherAlert
import com.example.weatherforecast.model.WeatherResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface IWeatherRepository {

    fun getForecast(
        lat: Double,
        lon: Double,
        units: String = "metric",
        lang: String = "en"
    ): Flow<WeatherResponse>

    fun searchCity(query: String): Flow<List<GeocodingResult>>

    val allFavorites: StateFlow<List<FavoriteLocation>>
    suspend fun addFavorite(location: FavoriteLocation)
    suspend fun updateFavorite(location: FavoriteLocation)
    suspend fun removeFavorite(location: FavoriteLocation)
    suspend fun getFavoriteById(id: Int): FavoriteLocation?

    val allAlerts: StateFlow<List<WeatherAlert>>
    suspend fun addAlert(alert: WeatherAlert): Long
    suspend fun removeAlert(alert: WeatherAlert)
    suspend fun updateAlert(alert: WeatherAlert)
    suspend fun getActiveAlerts(currentTime: Long): List<WeatherAlert>

    suspend fun cacheForecast(response: WeatherResponse)
    val cachedForecast: StateFlow<WeatherResponse?>
    suspend fun getCachedForecastSync(): WeatherResponse?
}
