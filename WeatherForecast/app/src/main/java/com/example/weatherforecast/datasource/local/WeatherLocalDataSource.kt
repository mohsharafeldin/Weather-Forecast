package com.example.weatherforecast.datasource.local

import com.example.weatherforecast.model.CachedForecast
import com.example.weatherforecast.model.FavoriteLocation
import com.example.weatherforecast.model.WeatherAlert
import kotlinx.coroutines.flow.Flow


class WeatherLocalDataSource(
    private val favoriteLocationDao: FavoriteLocationDao,
    private val weatherAlertDao: WeatherAlertDao,
    private val cachedForecastDao: CachedForecastDao
) : IWeatherLocalDataSource {


    override fun getAllFavorites(): Flow<List<FavoriteLocation>> =
        favoriteLocationDao.getAllFavorites()

    override suspend fun addFavorite(location: FavoriteLocation) =
        favoriteLocationDao.insert(location)

    override suspend fun updateFavorite(location: FavoriteLocation) =
        favoriteLocationDao.update(location)

    override suspend fun removeFavorite(location: FavoriteLocation) =
        favoriteLocationDao.delete(location)

    override suspend fun getFavoriteById(id: Int): FavoriteLocation? =
        favoriteLocationDao.getFavoriteById(id)


    override fun getAllAlerts(): Flow<List<WeatherAlert>> =
        weatherAlertDao.getAllAlerts()

    override suspend fun addAlert(alert: WeatherAlert) =
        weatherAlertDao.insert(alert)

    override suspend fun removeAlert(alert: WeatherAlert) =
        weatherAlertDao.delete(alert)

    override suspend fun updateAlert(alert: WeatherAlert) =
        weatherAlertDao.update(alert)

    override suspend fun getActiveAlerts(currentTime: Long): List<WeatherAlert> =
        weatherAlertDao.getActiveAlerts(currentTime)

    override suspend fun cacheForecast(forecast: CachedForecast) =
        cachedForecastDao.insertForecast(forecast)

    override suspend fun getCachedForecast(): CachedForecast? =
        cachedForecastDao.getCachedForecast()
}
