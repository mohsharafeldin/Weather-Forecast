package com.example.weatherforecast.datasource.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.weatherforecast.model.CachedForecast

@Dao
interface CachedForecastDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForecast(forecast: CachedForecast)

    @Query("SELECT * FROM cached_forecast WHERE id = 1")
    suspend fun getCachedForecast(): CachedForecast?

    @Query("DELETE FROM cached_forecast")
    suspend fun clearCache()
}
