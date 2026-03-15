package com.example.weatherforecast.data.network

import com.example.weatherforecast.data.model.GeocodingResult
import com.example.weatherforecast.data.model.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query


interface WeatherApiService {

    @GET("data/2.5/forecast")
    suspend fun getForecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "en"
    ): WeatherResponse

    @GET("geo/1.0/direct")
    suspend fun searchCity(
        @Query("q") query: String,
        @Query("limit") limit: Int = 5,
        @Query("appid") apiKey: String
    ): List<GeocodingResult>

    companion object {
        const val BASE_URL = "https://api.openweathermap.org/"
    }
}

