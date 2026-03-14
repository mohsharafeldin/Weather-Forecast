package com.example.weatherforecast.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.weatherforecast.model.FavoriteLocation
import com.example.weatherforecast.model.WeatherResponse
import com.example.weatherforecast.repository.IWeatherRepository
import com.example.weatherforecast.settings.SettingsDataStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.weatherforecast.R

sealed class FavoriteDetailState {
    object Idle : FavoriteDetailState()
    object Loading : FavoriteDetailState()
    data class Success(val weatherResponse: WeatherResponse, val tempUnit: String, val windUnit: String, val cityName: String = "") : FavoriteDetailState()
    data class Error(val message: String) : FavoriteDetailState()
}

class FavoritesViewModel(
    private val repository: IWeatherRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val favorites: StateFlow<List<FavoriteLocation>> = repository.allFavorites

    private val _detailState = MutableStateFlow<FavoriteDetailState>(FavoriteDetailState.Idle)
    val detailState: StateFlow<FavoriteDetailState> = _detailState.asStateFlow()

    private val _events = MutableSharedFlow<Int>()
    val events: SharedFlow<Int> = _events.asSharedFlow()

    private var refreshedForSettings: Pair<String, String>? = null

    init {
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                repository.allFavorites,
                settingsDataStore.temperatureUnit,
                settingsDataStore.language
            ) { favs, tempUnit, lang ->
                Triple(favs, tempUnit, lang)
            }.collect { (favs, tempUnit, lang) ->
                if (favs.isNotEmpty()) {
                    val currentSettings = Pair(tempUnit, lang)
                    if (refreshedForSettings != currentSettings) {
                        refreshedForSettings = currentSettings
                        for (location in favs) {
                            try {
                                val response = repository.getForecast(location.latitude, location.longitude, tempUnit, lang).first()
                                val gson = com.google.gson.Gson()
                                val updatedLocation = location.copy(cachedResponseJson = gson.toJson(response))
                                repository.updateFavorite(updatedLocation)
                            } catch (_: Exception) { }
                        }
                    }
                }
            }
        }
    }

    fun refreshAllFavorites() {
    }

    fun addFavorite(name: String, lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                val tempUnit = settingsDataStore.temperatureUnit.first()
                val lang = settingsDataStore.language.first()
                
                repository.getForecast(lat, lon, tempUnit, lang)
                    .catch {
                        repository.addFavorite(
                            FavoriteLocation(
                                name = name,
                                latitude = lat,
                                longitude = lon
                            )
                        )
                        _events.emit(R.string.msg_fav_added_offline)
                    }
                    .collect { response ->
                        val gson = com.google.gson.Gson()
                        val cachedResponseJson = gson.toJson(response)
                        
                        repository.addFavorite(
                            FavoriteLocation(
                                name = name,
                                latitude = lat,
                                longitude = lon,
                                cachedResponseJson = cachedResponseJson
                            )
                        )
                        _events.emit(R.string.msg_fav_added)
                    }
            } catch (e: Exception) {
                repository.addFavorite(
                    FavoriteLocation(
                        name = name,
                        latitude = lat,
                        longitude = lon
                    )
                )
                _events.emit(R.string.msg_fav_added_no_weather)
            }
        }
    }

    fun removeFavorite(location: FavoriteLocation) {
        viewModelScope.launch {
            repository.removeFavorite(location)
            _events.emit(R.string.msg_fav_removed)
        }
    }

    fun loadFavoriteDetail(id: Int) {
        viewModelScope.launch {
            _detailState.value = FavoriteDetailState.Loading
            try {
                val location = repository.getFavoriteById(id)
                if (location == null) {
                    _detailState.value = FavoriteDetailState.Error("Favorite location not found")
                    return@launch
                }

                val tempUnit = settingsDataStore.temperatureUnit.first()
                val lang = settingsDataStore.language.first()
                val windUnit = settingsDataStore.windSpeedUnit.first()

                repository.getForecast(location.latitude, location.longitude, tempUnit, lang)
                    .catch { e ->
                        if (location.cachedResponseJson != null) {
                            try {
                                val gson = com.google.gson.Gson()
                                val cachedResponse = gson.fromJson(location.cachedResponseJson, WeatherResponse::class.java)
                                val interpolatedList = com.example.weatherforecast.utils.WeatherInterpolator.interpolateWeatherList(cachedResponse.list)
                                val interpolatedResponse = cachedResponse.copy(list = interpolatedList)
                                _detailState.value = FavoriteDetailState.Success(interpolatedResponse, tempUnit, windUnit, cachedResponse.city.name)
                            } catch (parseEx: Exception) {
                                _detailState.value = FavoriteDetailState.Error("Failed to parse cached data")
                            }
                        } else {
                            _detailState.value = FavoriteDetailState.Error(e.message ?: "You are offline and no cached data is available")
                        }
                    }
                    .collect { response ->
                        val gson = com.google.gson.Gson()
                        val updatedLocation = location.copy(cachedResponseJson = gson.toJson(response))
                        repository.updateFavorite(updatedLocation)
                        
                        val interpolatedList = com.example.weatherforecast.utils.WeatherInterpolator.interpolateWeatherList(response.list)
                        val interpolatedResponse = response.copy(list = interpolatedList)
                        _detailState.value = FavoriteDetailState.Success(interpolatedResponse, tempUnit, windUnit, response.city.name)
                    }
            } catch (e: Exception) {
                _detailState.value = FavoriteDetailState.Error(e.message ?: "Error loading forecast")
            }
        }
    }

    fun resetDetailState() {
        _detailState.value = FavoriteDetailState.Idle
    }
}

class FavoritesViewModelFactory(
    private val repository: IWeatherRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FavoritesViewModel::class.java)) {
            return FavoritesViewModel(repository, settingsDataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

