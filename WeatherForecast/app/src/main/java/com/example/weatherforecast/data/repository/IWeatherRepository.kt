package com.example.weatherforecast.data.repository

import com.example.weatherforecast.data.model.FavoriteLocation
import com.example.weatherforecast.data.model.GeocodingResult
import com.example.weatherforecast.data.model.WeatherAlert
import com.example.weatherforecast.data.model.WeatherResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface IForecastRepository {
    fun getForecast(
        lat: Double,
        lon: Double,
        units: String = "metric",
        lang: String = "en"
    ): Flow<WeatherResponse>

    fun searchCity(query: String): Flow<List<GeocodingResult>>

    suspend fun cacheForecast(response: WeatherResponse)
    val cachedForecast: StateFlow<WeatherResponse?>
    suspend fun getCachedForecastSync(): WeatherResponse?
}

interface IFavoritesRepository {
    val allFavorites: StateFlow<List<FavoriteLocation>>
    suspend fun addFavorite(location: FavoriteLocation)
    suspend fun updateFavorite(location: FavoriteLocation)
    suspend fun removeFavorite(location: FavoriteLocation)
    suspend fun getFavoriteById(id: Int): FavoriteLocation?
    suspend fun cacheWeatherForFavorite(location: FavoriteLocation, response: WeatherResponse)
    suspend fun getCachedWeatherForFavorite(location: FavoriteLocation): WeatherResponse?
}

interface IAlertsRepository {
    val allAlerts: StateFlow<List<WeatherAlert>>
    suspend fun addAlert(alert: WeatherAlert): Long
    suspend fun removeAlert(alert: WeatherAlert)
    suspend fun updateAlert(alert: WeatherAlert)
    suspend fun getActiveAlerts(currentTime: Long): List<WeatherAlert>
}

interface IWeatherRepository : IForecastRepository, IFavoritesRepository, IAlertsRepository
