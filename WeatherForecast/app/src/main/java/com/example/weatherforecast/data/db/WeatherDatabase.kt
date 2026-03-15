package com.example.weatherforecast.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.weatherforecast.data.model.CachedForecast
import com.example.weatherforecast.data.model.FavoriteLocation
import com.example.weatherforecast.data.model.WeatherAlert
import com.example.weatherforecast.data.datasource.local.CachedForecastDao
import com.example.weatherforecast.data.datasource.local.FavoriteLocationDao
import com.example.weatherforecast.data.datasource.local.WeatherAlertDao

@Database(
    entities = [FavoriteLocation::class, WeatherAlert::class, CachedForecast::class],
    version = 6,
    exportSchema = false
)
abstract class WeatherDatabase : RoomDatabase() {

    abstract fun favoriteLocationDao(): FavoriteLocationDao
    abstract fun weatherAlertDao(): WeatherAlertDao
    abstract fun cachedForecastDao(): CachedForecastDao

    companion object {
        @Volatile
        private var INSTANCE: WeatherDatabase? = null

        fun getInstance(context: Context): WeatherDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WeatherDatabase::class.java,
                    "weather_database"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
