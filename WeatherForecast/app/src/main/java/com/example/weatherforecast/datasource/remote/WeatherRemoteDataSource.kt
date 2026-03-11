package com.example.weatherforecast.datasource.remote

import com.example.weatherforecast.model.GeocodingResult
import com.example.weatherforecast.model.WeatherResponse
import com.example.weatherforecast.network.WeatherApiService


class WeatherRemoteDataSource(
    private val apiService: WeatherApiService
) : IWeatherRemoteDataSource {

    override suspend fun getForecast(
        lat: Double,
        lon: Double,
        apiKey: String,
        units: String,
        lang: String
    ): WeatherResponse {
        return apiService.getForecast(lat, lon, apiKey, units, lang)
    }

    override suspend fun searchCity(
        query: String,
        apiKey: String,
        limit: Int
    ): List<GeocodingResult> {
        return apiService.searchCity(query, limit, apiKey)
    }
}

