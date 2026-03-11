package com.example.weatherforecast.model

data class GeocodingResult(
    val name: String,
    val lat: Double,
    val lon: Double,
    val country: String,
    val state: String? = null
)
