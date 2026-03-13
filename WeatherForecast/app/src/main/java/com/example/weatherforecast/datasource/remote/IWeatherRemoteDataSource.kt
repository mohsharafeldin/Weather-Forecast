package com.example.weatherforecast.datasource.remote

import com.example.weatherforecast.model.GeocodingResult
import com.example.weatherforecast.model.WeatherResponse
import kotlinx.coroutines.flow.Flow


interface IWeatherRemoteDataSource {
    fun getForecast(
        lat: Double,
        lon: Double,
        apiKey: String,
        units: String = "metric",
        lang: String = "en"
    ): Flow<WeatherResponse>

    fun searchCity(
        query: String,
        apiKey: String,
        limit: Int = 5
    ): Flow<List<GeocodingResult>>
}
