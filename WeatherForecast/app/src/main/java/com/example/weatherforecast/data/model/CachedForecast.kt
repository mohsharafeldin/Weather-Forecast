package com.example.weatherforecast.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_forecast")
data class CachedForecast(
    @PrimaryKey val id: Int = 1,
    val responseJson: String,
    val lastUpdated: Long
)
