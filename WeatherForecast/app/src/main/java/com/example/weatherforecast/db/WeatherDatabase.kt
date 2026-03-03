package com.example.weatherforecast.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.weatherforecast.model.FavoriteLocation
import com.example.weatherforecast.model.WeatherAlert
import com.example.weatherforecast.datasource.local.FavoriteLocationDao
import com.example.weatherforecast.datasource.local.WeatherAlertDao


@Database(
    entities = [FavoriteLocation::class, WeatherAlert::class],
    version = 1,
    exportSchema = false
)
abstract class WeatherDatabase : RoomDatabase() {

    abstract fun favoriteLocationDao(): FavoriteLocationDao
    abstract fun weatherAlertDao(): WeatherAlertDao

    companion object {
        @Volatile
        private var INSTANCE: WeatherDatabase? = null

        fun getInstance(context: Context): WeatherDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WeatherDatabase::class.java,
                    "weather_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
