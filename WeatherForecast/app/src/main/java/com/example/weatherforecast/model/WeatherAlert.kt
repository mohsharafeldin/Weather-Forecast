package com.example.weatherforecast.model

import androidx.room.Entity
import androidx.room.PrimaryKey


enum class AlertType {
    NOTIFICATION,
    ALARM
}


@Entity(tableName = "weather_alerts")
data class WeatherAlert(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startTime: Long,   
    val endTime: Long,     
    val alertType: String, 
    val isEnabled: Boolean = true,
    val snoozeDuration: Int = 5
)
