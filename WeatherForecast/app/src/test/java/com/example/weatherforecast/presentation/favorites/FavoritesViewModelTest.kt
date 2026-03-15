package com.example.weatherforecast.presentation.favorites

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.weatherforecast.data.model.FavoriteLocation
import com.example.weatherforecast.data.model.GeocodingResult
import com.example.weatherforecast.data.repository.IFavoritesRepository
import com.example.weatherforecast.data.repository.IForecastRepository
import com.example.weatherforecast.presentation.settings.ISettingsDataStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var forecastRepository: IForecastRepository
    private lateinit var favoritesRepository: IFavoritesRepository
    private lateinit var settingsDataStore: ISettingsDataStore
    private lateinit var viewModel: FavoritesViewModel

    private val favoritesFlow = MutableStateFlow<List<FavoriteLocation>>(emptyList())

    private val testLocation = FavoriteLocation(
        id = 1,
        name = "Cairo",
        latitude = 30.0,
        longitude = 31.0
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        forecastRepository = mockk(relaxed = true)
        favoritesRepository = mockk(relaxed = true)
        settingsDataStore = mockk(relaxed = true)

        every { favoritesRepository.allFavorites } returns favoritesFlow
        every { settingsDataStore.temperatureUnit } returns MutableStateFlow("metric")
        every { settingsDataStore.language } returns MutableStateFlow("en")
        every { settingsDataStore.windSpeedUnit } returns MutableStateFlow("m/s")

        viewModel = FavoritesViewModel(forecastRepository, favoritesRepository, settingsDataStore)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `favorites should reflect repository allFavorites`() = runTest {
        advanceUntilIdle()
        assertEquals(emptyList<FavoriteLocation>(), viewModel.favorites.value)
    }

    @Test
    fun `removeFavorite should call repository`() = runTest {
        coEvery { favoritesRepository.removeFavorite(any()) } just Runs
        advanceUntilIdle()

        viewModel.removeFavorite(testLocation)
        advanceUntilIdle()

        coVerify { favoritesRepository.removeFavorite(testLocation) }
    }

    @Test
    fun `searchCity should populate searchResults`() = runTest {
        val results = listOf(
            GeocodingResult(name = "Cairo", lat = 30.0, lon = 31.0, country = "EG")
        )
        every { forecastRepository.searchCity(any()) } returns flowOf(results)
        advanceUntilIdle()

        viewModel.searchCity("Cairo")
        advanceUntilIdle()

        assertEquals(1, viewModel.searchResults.value.size)
        assertEquals("Cairo", viewModel.searchResults.value[0].name)
    }

    @Test
    fun `searchCity with short query should clear results`() = runTest {
        advanceUntilIdle()

        viewModel.searchCity("C")
        advanceUntilIdle()

        assertEquals(emptyList<GeocodingResult>(), viewModel.searchResults.value)
    }

    @Test
    fun `clearSearchResults should set empty list`() = runTest {
        val results = listOf(
            GeocodingResult(name = "Cairo", lat = 30.0, lon = 31.0, country = "EG")
        )
        every { forecastRepository.searchCity(any()) } returns flowOf(results)
        advanceUntilIdle()

        viewModel.searchCity("Cairo")
        advanceUntilIdle()

        viewModel.clearSearchResults()
        assertEquals(emptyList<GeocodingResult>(), viewModel.searchResults.value)
    }

    @Test
    fun `addFavorite should call favoritesRepository addFavorite`() = runTest {
        coEvery { favoritesRepository.addFavorite(any()) } just Runs
        coEvery { favoritesRepository.cacheWeatherForFavorite(any(), any()) } just Runs
        every { forecastRepository.getForecast(any(), any(), any(), any()) } returns flowOf(mockk(relaxed = true))
        advanceUntilIdle()

        viewModel.addFavorite("Alex", 31.2, 29.9)
        advanceUntilIdle()

        coVerify { favoritesRepository.addFavorite(any()) }
    }

    @Test
    fun `resetDetailState should set Idle`() = runTest {
        advanceUntilIdle()

        viewModel.resetDetailState()

        assertTrue(viewModel.detailState.value is FavoriteDetailState.Idle)
    }
}
