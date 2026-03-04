package com.example.weatherforecast.model

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "favorite_locations")
data class FavoriteLocation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double
)
