package com.example.weatherforecast.datasource.remote

import com.example.weatherforecast.model.GeocodingResult
import com.example.weatherforecast.model.WeatherResponse


interface IWeatherRemoteDataSource {
    suspend fun getForecast(
        lat: Double,
        lon: Double,
        apiKey: String,
        units: String = "metric",
        lang: String = "en"
    ): WeatherResponse

    suspend fun searchCity(
        query: String,
        apiKey: String,
        limit: Int = 5
    ): List<GeocodingResult>
}

