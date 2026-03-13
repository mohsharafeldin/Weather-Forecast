package com.example.weatherforecast.datasource.remote

import com.example.weatherforecast.model.GeocodingResult
import com.example.weatherforecast.model.WeatherResponse
import com.example.weatherforecast.network.WeatherApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow


class WeatherRemoteDataSource(
    private val apiService: WeatherApiService
) : IWeatherRemoteDataSource {

    override fun getForecast(
        lat: Double,
        lon: Double,
        apiKey: String,
        units: String,
        lang: String
    ): Flow<WeatherResponse> = flow {
        emit(apiService.getForecast(lat, lon, apiKey, units, lang))
    }

    override fun searchCity(
        query: String,
        apiKey: String,
        limit: Int
    ): Flow<List<GeocodingResult>> = flow {
        emit(apiService.searchCity(query, limit, apiKey))
    }
}
