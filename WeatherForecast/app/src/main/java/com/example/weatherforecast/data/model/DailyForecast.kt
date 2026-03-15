package com.example.weatherforecast.data.model

data class DailyForecast(
    val date: String,
    val tempMin: Double,
    val tempMax: Double,
    val icon: String,
    val description: String
)
