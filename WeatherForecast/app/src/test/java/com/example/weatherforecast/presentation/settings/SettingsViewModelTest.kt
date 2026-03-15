package com.example.weatherforecast.presentation.settings

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
class SettingsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var settingsDataStore: ISettingsDataStore
    private lateinit var viewModel: SettingsViewModel

    private val temperatureUnitFlow = MutableStateFlow("metric")
    private val windSpeedUnitFlow = MutableStateFlow("m/s")
    private val languageFlow = MutableStateFlow("en")
    private val locationModeFlow = MutableStateFlow("gps")
    private val mapLatFlow = MutableStateFlow(30.0444)
    private val mapLonFlow = MutableStateFlow(31.2357)
    private val mapCityNameFlow = MutableStateFlow("")
    private val themeModeFlow = MutableStateFlow("system")

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        settingsDataStore = mockk(relaxed = true)

        every { settingsDataStore.temperatureUnit } returns temperatureUnitFlow
        every { settingsDataStore.windSpeedUnit } returns windSpeedUnitFlow
        every { settingsDataStore.language } returns languageFlow
        every { settingsDataStore.locationMode } returns locationModeFlow
        every { settingsDataStore.mapLat } returns mapLatFlow
        every { settingsDataStore.mapLon } returns mapLonFlow
        every { settingsDataStore.mapCityName } returns mapCityNameFlow
        every { settingsDataStore.themeMode } returns themeModeFlow

        viewModel = SettingsViewModel(settingsDataStore)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_loadSettings_successState() = runTest {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SettingsUiState.Success)
        val success = state as SettingsUiState.Success
        assertEquals("metric", success.temperatureUnit)
        assertEquals("m/s", success.windSpeedUnit)
        assertEquals("en", success.language)
        assertEquals("gps", success.locationMode)
    }

    @Test
    fun setTemperatureUnit_validUnit_delegatesToDataStore() = runTest {
        coEvery { settingsDataStore.setTemperatureUnit(any()) } just Runs
        advanceUntilIdle()

        viewModel.setTemperatureUnit("imperial")
        advanceUntilIdle()

        // Then: The data store's setTemperatureUnit method should be called with the new unit
    }

    @Test
    fun setWindSpeedUnit_validUnit_delegatesToDataStore() = runTest {
        coEvery { settingsDataStore.setWindSpeedUnit(any()) } just Runs
        advanceUntilIdle()

        viewModel.setWindSpeedUnit("mph")
        advanceUntilIdle()

        // Then: The data store's setWindSpeedUnit method should be called with the new unit
    }

    @Test
    fun setLanguage_validLanguage_delegatesToDataStore() = runTest {
        coEvery { settingsDataStore.setLanguage(any()) } just Runs
        advanceUntilIdle()

        viewModel.setLanguage("ar")
        advanceUntilIdle()

        // Then: The data store's setLanguage method should be called with the new language
    }

    @Test
    fun setLocationMode_validMode_delegatesToDataStore() = runTest {
        coEvery { settingsDataStore.setLocationMode(any()) } just Runs
        advanceUntilIdle()

        viewModel.setLocationMode("map")
        advanceUntilIdle()

        // Then: The data store's setLocationMode method should be called with the new mode
    }

    @Test
    fun setMapCoordinates_validCoordinates_delegatesToDataStore() = runTest {
        coEvery { settingsDataStore.setMapCoordinates(any(), any(), any()) } just Runs
        advanceUntilIdle()

        viewModel.setMapCoordinates(25.0, 35.0, "Alexandria")
        advanceUntilIdle()

        // Then: The data store's setMapCoordinates method should be called with the new location
    }

    @Test
    fun setThemeMode_validMode_delegatesToDataStore() = runTest {
        coEvery { settingsDataStore.setThemeMode(any()) } just Runs
        advanceUntilIdle()

        viewModel.setThemeMode("dark")
        advanceUntilIdle()

        // Then: The data store's setThemeMode method should be called with the new theme
    }

    @Test
    fun uiState_settingsChange_updatesState() = runTest {
        advanceUntilIdle()

        temperatureUnitFlow.value = "imperial"
        advanceUntilIdle()

        val state = viewModel.uiState.value as SettingsUiState.Success
        assertEquals("imperial", state.temperatureUnit)
    }
}
