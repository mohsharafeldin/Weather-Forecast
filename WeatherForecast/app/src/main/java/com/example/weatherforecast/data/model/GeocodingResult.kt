package com.example.weatherforecast.data.model

data class GeocodingResult(
    val name: String,
    val lat: Double,
    val lon: Double,
    val country: String,
    val state: String? = null
)
