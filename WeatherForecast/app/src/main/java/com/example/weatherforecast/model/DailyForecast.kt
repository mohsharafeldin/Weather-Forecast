package com.example.weatherforecast.model

data class DailyForecast(
    val date: String,
    val tempMin: Double,
    val tempMax: Double,
    val icon: String,
    val description: String
)
