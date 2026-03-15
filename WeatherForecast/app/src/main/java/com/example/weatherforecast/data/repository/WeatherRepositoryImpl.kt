package com.example.weatherforecast.data.repository

import com.example.weatherforecast.data.datasource.local.IWeatherLocalDataSource
import com.example.weatherforecast.data.datasource.remote.IWeatherRemoteDataSource
import com.example.weatherforecast.data.model.CachedForecast
import com.example.weatherforecast.data.model.GeocodingResult
import com.example.weatherforecast.data.model.FavoriteLocation
import com.example.weatherforecast.data.model.WeatherAlert
import com.example.weatherforecast.data.model.WeatherResponse
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class WeatherRepositoryImpl(
    private val remoteDataSource: IWeatherRemoteDataSource,
    private val localDataSource: IWeatherLocalDataSource
) : IWeatherRepository {

    companion object {
        const val API_KEY ="062593663b89ef55f2f612b4d06a86bc"
    }

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun getForecast(
        lat: Double,
        lon: Double,
        units: String,
        lang: String
    ): Flow<WeatherResponse> {
        return remoteDataSource.getForecast(lat, lon, API_KEY, units, lang)
    }

    override fun searchCity(query: String): Flow<List<GeocodingResult>> {
        return remoteDataSource.searchCity(query, API_KEY)
    }

    override val allFavorites: StateFlow<List<FavoriteLocation>> =
        localDataSource.getAllFavorites().stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    override suspend fun addFavorite(location: FavoriteLocation) =
        localDataSource.addFavorite(location)

    override suspend fun updateFavorite(location: FavoriteLocation) =
        localDataSource.updateFavorite(location)

    override suspend fun removeFavorite(location: FavoriteLocation) =
        localDataSource.removeFavorite(location)

    override suspend fun getFavoriteById(id: Int): FavoriteLocation? =
        localDataSource.getFavoriteById(id)

    override val allAlerts: StateFlow<List<WeatherAlert>> =
        localDataSource.getAllAlerts().stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    override suspend fun addAlert(alert: WeatherAlert): Long =
        localDataSource.addAlert(alert)

    override suspend fun removeAlert(alert: WeatherAlert) =
        localDataSource.removeAlert(alert)

    override suspend fun updateAlert(alert: WeatherAlert) =
        localDataSource.updateAlert(alert)

    override suspend fun getActiveAlerts(currentTime: Long): List<WeatherAlert> =
        localDataSource.getActiveAlerts(currentTime)

    override suspend fun cacheForecast(response: WeatherResponse) {
        val json = gson.toJson(response)
        val cached = CachedForecast(
            responseJson = json,
            lastUpdated = System.currentTimeMillis()
        )
        localDataSource.cacheForecast(cached)
    }

    override val cachedForecast: StateFlow<WeatherResponse?> =
        localDataSource.getCachedForecast().map { cached ->
            if (cached != null) {
                try {
                    gson.fromJson(cached.responseJson, WeatherResponse::class.java)
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    override suspend fun getCachedForecastSync(): WeatherResponse? {
        val cached = localDataSource.getCachedForecastSync() ?: return null
        return try {
            gson.fromJson(cached.responseJson, WeatherResponse::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun cacheWeatherForFavorite(location: FavoriteLocation, response: WeatherResponse) {
        val updatedLocation = location.copy(cachedResponseJson = gson.toJson(response))
        localDataSource.updateFavorite(updatedLocation)
    }

    override suspend fun getCachedWeatherForFavorite(location: FavoriteLocation): WeatherResponse? {
        val json = location.cachedResponseJson ?: return null
        return try {
            gson.fromJson(json, WeatherResponse::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
