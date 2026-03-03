package com.example.weatherforecast.repository

import com.example.weatherforecast.datasource.local.IWeatherLocalDataSource
import com.example.weatherforecast.datasource.remote.IWeatherRemoteDataSource
import com.example.weatherforecast.model.FavoriteLocation
import com.example.weatherforecast.model.WeatherAlert
import com.example.weatherforecast.model.WeatherResponse
import kotlinx.coroutines.flow.Flow

class WeatherRepositoryImpl(
    private val remoteDataSource: IWeatherRemoteDataSource,
    private val localDataSource: IWeatherLocalDataSource
) : IWeatherRepository {

    companion object {
        const val API_KEY ="062593663b89ef55f2f612b4d06a86bc"
    }

    override suspend fun getForecast(
        lat: Double,
        lon: Double,
        units: String,
        lang: String
    ): WeatherResponse {
        return remoteDataSource.getForecast(lat, lon, API_KEY, units, lang)
    }

    override fun getAllFavorites(): Flow<List<FavoriteLocation>> =
        localDataSource.getAllFavorites()

    override suspend fun addFavorite(location: FavoriteLocation) =
        localDataSource.addFavorite(location)

    override suspend fun removeFavorite(location: FavoriteLocation) =
        localDataSource.removeFavorite(location)

    override suspend fun getFavoriteById(id: Int): FavoriteLocation? =
        localDataSource.getFavoriteById(id)

    override fun getAllAlerts(): Flow<List<WeatherAlert>> =
        localDataSource.getAllAlerts()

    override suspend fun addAlert(alert: WeatherAlert) =
        localDataSource.addAlert(alert)

    override suspend fun removeAlert(alert: WeatherAlert) =
        localDataSource.removeAlert(alert)

    override suspend fun updateAlert(alert: WeatherAlert) =
        localDataSource.updateAlert(alert)

    override suspend fun getActiveAlerts(currentTime: Long): List<WeatherAlert> =
        localDataSource.getActiveAlerts(currentTime)
}
