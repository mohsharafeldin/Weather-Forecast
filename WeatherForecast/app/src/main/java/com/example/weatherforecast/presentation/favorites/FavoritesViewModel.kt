package com.example.weatherforecast.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.weatherforecast.data.model.FavoriteLocation
import com.example.weatherforecast.data.model.GeocodingResult
import com.example.weatherforecast.data.model.WeatherResponse
import com.example.weatherforecast.data.repository.IFavoritesRepository
import com.example.weatherforecast.data.repository.IForecastRepository
import com.example.weatherforecast.presentation.settings.ISettingsDataStore
import com.example.weatherforecast.utils.WeatherInterpolator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.example.weatherforecast.R

sealed class FavoriteDetailState {
    object Idle : FavoriteDetailState()
    object Loading : FavoriteDetailState()
    data class Success(val weatherResponse: WeatherResponse, val tempUnit: String, val windUnit: String, val cityName: String = "") : FavoriteDetailState()
    data class Error(val message: String) : FavoriteDetailState()
}

class FavoritesViewModel(
    private val forecastRepository: IForecastRepository,
    private val favoritesRepository: IFavoritesRepository,
    private val settingsDataStore: ISettingsDataStore
) : ViewModel() {

    val favorites: StateFlow<List<FavoriteLocation>> = favoritesRepository.allFavorites

    private val _detailState = MutableStateFlow<FavoriteDetailState>(FavoriteDetailState.Idle)
    val detailState: StateFlow<FavoriteDetailState> = _detailState.asStateFlow()

    private val _events = MutableSharedFlow<Int>()
    val events: SharedFlow<Int> = _events.asSharedFlow()

    private val _searchResults = MutableStateFlow<List<GeocodingResult>>(emptyList())
    val searchResults: StateFlow<List<GeocodingResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var refreshedForSettings: Pair<String, String>? = null

    init {
        viewModelScope.launch {
            combine(
                favoritesRepository.allFavorites,
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
                                val response = forecastRepository.getForecast(location.latitude, location.longitude, tempUnit, lang).first()
                                favoritesRepository.cacheWeatherForFavorite(location, response)
                            } catch (_: Exception) { }
                        }
                    }
                }
            }
        }
    }

    fun searchCity(query: String) {
        viewModelScope.launch {
            if (query.length < 2) {
                _searchResults.value = emptyList()
                return@launch
            }
            _isSearching.value = true
            try {
                val results = forecastRepository.searchCity(query).first()
                _searchResults.value = results
            } catch (_: Exception) {
                _searchResults.value = emptyList()
            }
            _isSearching.value = false
        }
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }

    fun refreshAllFavorites() {
    }

    fun addFavorite(name: String, lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                val tempUnit = settingsDataStore.temperatureUnit.first()
                val lang = settingsDataStore.language.first()

                forecastRepository.getForecast(lat, lon, tempUnit, lang)
                    .catch {
                        favoritesRepository.addFavorite(
                            FavoriteLocation(
                                name = name,
                                latitude = lat,
                                longitude = lon
                            )
                        )
                        _events.emit(R.string.msg_fav_added_offline)
                    }
                    .collect { response ->
                        val location = FavoriteLocation(
                            name = name,
                            latitude = lat,
                            longitude = lon
                        )
                        favoritesRepository.addFavorite(location)
                        favoritesRepository.cacheWeatherForFavorite(location, response)
                        _events.emit(R.string.msg_fav_added)
                    }
            } catch (e: Exception) {
                favoritesRepository.addFavorite(
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
            favoritesRepository.removeFavorite(location)
            _events.emit(R.string.msg_fav_removed)
        }
    }

    fun loadFavoriteDetail(id: Int) {
        viewModelScope.launch {
            _detailState.value = FavoriteDetailState.Loading
            try {
                val location = favoritesRepository.getFavoriteById(id)
                if (location == null) {
                    _detailState.value = FavoriteDetailState.Error("Favorite location not found")
                    return@launch
                }

                val tempUnit = settingsDataStore.temperatureUnit.first()
                val lang = settingsDataStore.language.first()
                val windUnit = settingsDataStore.windSpeedUnit.first()

                forecastRepository.getForecast(location.latitude, location.longitude, tempUnit, lang)
                    .catch { e ->
                        val cachedResponse = favoritesRepository.getCachedWeatherForFavorite(location)
                        if (cachedResponse != null) {
                            val interpolatedList = WeatherInterpolator.interpolateWeatherList(cachedResponse.list)
                            val interpolatedResponse = cachedResponse.copy(list = interpolatedList)
                            _detailState.value = FavoriteDetailState.Success(interpolatedResponse, tempUnit, windUnit, cachedResponse.city.name)
                        } else {
                            _detailState.value = FavoriteDetailState.Error(e.message ?: "You are offline and no cached data is available")
                        }
                    }
                    .collect { response ->
                        favoritesRepository.cacheWeatherForFavorite(location, response)

                        val interpolatedList = WeatherInterpolator.interpolateWeatherList(response.list)
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
    private val forecastRepository: IForecastRepository,
    private val favoritesRepository: IFavoritesRepository,
    private val settingsDataStore: ISettingsDataStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FavoritesViewModel::class.java)) {
            return FavoritesViewModel(forecastRepository, favoritesRepository, settingsDataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
