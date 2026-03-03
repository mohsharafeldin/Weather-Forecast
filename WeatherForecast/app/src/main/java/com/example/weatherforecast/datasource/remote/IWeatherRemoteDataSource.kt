package com.example.weatherforecast.datasource.remote

import com.example.weatherforecast.model.WeatherResponse


interface IWeatherRemoteDataSource {
    suspend fun getForecast(
        lat: Double,
        lon: Double,
        apiKey: String,
        units: String = "metric",
        lang: String = "en"
    ): WeatherResponse
}
