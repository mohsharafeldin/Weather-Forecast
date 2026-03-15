package com.example.weatherforecast.presentation.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map



private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) : ISettingsDataStore {

    companion object {
        val TEMPERATURE_UNIT = stringPreferencesKey("temperature_unit") 
        val WIND_SPEED_UNIT = stringPreferencesKey("wind_speed_unit")   
        val LANGUAGE = stringPreferencesKey("language")                   
        val LOCATION_MODE = stringPreferencesKey("location_mode")       
        val MAP_LAT = doublePreferencesKey("map_lat")
        val MAP_LON = doublePreferencesKey("map_lon")
        val MAP_CITY_NAME = stringPreferencesKey("map_city_name")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    override val temperatureUnit: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[TEMPERATURE_UNIT] ?: "metric"
    }

    override val windSpeedUnit: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[WIND_SPEED_UNIT] ?: "m/s"
    }

    override val language: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[LANGUAGE] ?: "en"
    }

    override val locationMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[LOCATION_MODE] ?: "gps"
    }

    override val mapLat: Flow<Double> = context.dataStore.data.map { prefs ->
        prefs[MAP_LAT] ?: 30.0444  
    }

    override val mapLon: Flow<Double> = context.dataStore.data.map { prefs ->
        prefs[MAP_LON] ?: 31.2357
    }

    override val mapCityName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[MAP_CITY_NAME] ?: ""
    }

    override val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[THEME_MODE] ?: "system"
    }

    override suspend fun setTemperatureUnit(unit: String) {
        context.dataStore.edit { prefs -> prefs[TEMPERATURE_UNIT] = unit }
    }

    override suspend fun setWindSpeedUnit(unit: String) {
        context.dataStore.edit { prefs -> prefs[WIND_SPEED_UNIT] = unit }
    }

    override suspend fun setLanguage(lang: String) {
        context.dataStore.edit { prefs -> prefs[LANGUAGE] = lang }
    }

    override suspend fun setLocationMode(mode: String) {
        context.dataStore.edit { prefs -> prefs[LOCATION_MODE] = mode }
    }

    override suspend fun setMapCoordinates(lat: Double, lon: Double, cityName: String) {
        context.dataStore.edit { prefs ->
            prefs[MAP_LAT] = lat
            prefs[MAP_LON] = lon
            prefs[MAP_CITY_NAME] = cityName
        }
    }

    override suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs -> prefs[THEME_MODE] = mode }
    }
}
