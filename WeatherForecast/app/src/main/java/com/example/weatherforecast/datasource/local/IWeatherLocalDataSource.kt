package com.example.weatherforecast.datasource.local

import com.example.weatherforecast.model.CachedForecast
import com.example.weatherforecast.model.FavoriteLocation
import com.example.weatherforecast.model.WeatherAlert
import kotlinx.coroutines.flow.Flow


interface IWeatherLocalDataSource {


    fun getAllFavorites(): Flow<List<FavoriteLocation>>
    suspend fun addFavorite(location: FavoriteLocation)
    suspend fun updateFavorite(location: FavoriteLocation)
    suspend fun removeFavorite(location: FavoriteLocation)
    suspend fun getFavoriteById(id: Int): FavoriteLocation?


    fun getAllAlerts(): Flow<List<WeatherAlert>>
    suspend fun addAlert(alert: WeatherAlert)
    suspend fun removeAlert(alert: WeatherAlert)
    suspend fun updateAlert(alert: WeatherAlert)
    suspend fun getActiveAlerts(currentTime: Long): List<WeatherAlert>

    suspend fun cacheForecast(forecast: CachedForecast)
    suspend fun getCachedForecast(): CachedForecast?
}
