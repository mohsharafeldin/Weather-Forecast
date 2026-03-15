package com.example.weatherforecast.data.datasource.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.weatherforecast.data.model.WeatherAlert
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherAlertDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: WeatherAlert): Long

    @Delete
    suspend fun delete(alert: WeatherAlert)

    @Update
    suspend fun update(alert: WeatherAlert)

    @Query("SELECT * FROM weather_alerts")
    fun getAllAlerts(): Flow<List<WeatherAlert>>

    @Query("SELECT * FROM weather_alerts WHERE id = :alertId LIMIT 1")
    suspend fun getAlertById(alertId: Int): WeatherAlert?

    @Query("SELECT * FROM weather_alerts WHERE isEnabled = 1 AND endTime > :currentTime")
    suspend fun getActiveAlerts(currentTime: Long): List<WeatherAlert>
}
