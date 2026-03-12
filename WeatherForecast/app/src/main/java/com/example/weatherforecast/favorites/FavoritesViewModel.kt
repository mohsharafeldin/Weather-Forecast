package com.example.weatherforecast.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.weatherforecast.model.FavoriteLocation
import com.example.weatherforecast.model.WeatherResponse
import com.example.weatherforecast.repository.IWeatherRepository
import com.example.weatherforecast.settings.SettingsDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class FavoriteDetailState {
    object Idle : FavoriteDetailState()
    object Loading : FavoriteDetailState()
    data class Success(val weatherResponse: WeatherResponse, val tempUnit: String, val windUnit: String) : FavoriteDetailState()
    data class Error(val message: String) : FavoriteDetailState()
}

class FavoritesViewModel(
    private val repository: IWeatherRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val favorites: StateFlow<List<FavoriteLocation>> = repository.getAllFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _detailState = MutableStateFlow<FavoriteDetailState>(FavoriteDetailState.Idle)
    val detailState: StateFlow<FavoriteDetailState> = _detailState.asStateFlow()

    fun addFavorite(name: String, lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                val tempUnit = settingsDataStore.temperatureUnit.first()
                val lang = settingsDataStore.language.first()
                
                val response = repository.getForecast(lat, lon, tempUnit, lang)
                
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
            } catch (e: Exception) {
                repository.addFavorite(
                    FavoriteLocation(
                        name = name,
                        latitude = lat,
                        longitude = lon
                    )
                )
            }
        }
    }

    fun removeFavorite(location: FavoriteLocation) {
        viewModelScope.launch {
            repository.removeFavorite(location)
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

                try {
                    val response = repository.getForecast(location.latitude, location.longitude, tempUnit, lang)
                    
                    val gson = com.google.gson.Gson()
                    val updatedLocation = location.copy(cachedResponseJson = gson.toJson(response))
                    repository.updateFavorite(updatedLocation)
                    
                    _detailState.value = FavoriteDetailState.Success(response, tempUnit, windUnit)
                } catch (e: Exception) {
                    if (location.cachedResponseJson != null) {
                        try {
                            val gson = com.google.gson.Gson()
                            val cachedResponse = gson.fromJson(location.cachedResponseJson, WeatherResponse::class.java)
                            _detailState.value = FavoriteDetailState.Success(cachedResponse, tempUnit, windUnit)
                        } catch (parseEx: Exception) {
                            _detailState.value = FavoriteDetailState.Error("Failed to parse cached data")
                        }
                    } else {
                        _detailState.value = FavoriteDetailState.Error(e.message ?: "You are offline and no cached data is available")
                    }
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
