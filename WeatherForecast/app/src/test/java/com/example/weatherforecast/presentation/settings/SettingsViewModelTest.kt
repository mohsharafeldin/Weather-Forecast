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
    fun `init should load settings into Success state`() = runTest {
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
    fun `setTemperatureUnit should delegate to dataStore`() = runTest {
        coEvery { settingsDataStore.setTemperatureUnit(any()) } just Runs
        advanceUntilIdle()

        viewModel.setTemperatureUnit("imperial")
        advanceUntilIdle()

        coVerify { settingsDataStore.setTemperatureUnit("imperial") }
    }

    @Test
    fun `setWindSpeedUnit should delegate to dataStore`() = runTest {
        coEvery { settingsDataStore.setWindSpeedUnit(any()) } just Runs
        advanceUntilIdle()

        viewModel.setWindSpeedUnit("mph")
        advanceUntilIdle()

        coVerify { settingsDataStore.setWindSpeedUnit("mph") }
    }

    @Test
    fun `setLanguage should delegate to dataStore`() = runTest {
        coEvery { settingsDataStore.setLanguage(any()) } just Runs
        advanceUntilIdle()

        viewModel.setLanguage("ar")
        advanceUntilIdle()

        coVerify { settingsDataStore.setLanguage("ar") }
    }

    @Test
    fun `setLocationMode should delegate to dataStore`() = runTest {
        coEvery { settingsDataStore.setLocationMode(any()) } just Runs
        advanceUntilIdle()

        viewModel.setLocationMode("map")
        advanceUntilIdle()

        coVerify { settingsDataStore.setLocationMode("map") }
    }

    @Test
    fun `setMapCoordinates should delegate to dataStore`() = runTest {
        coEvery { settingsDataStore.setMapCoordinates(any(), any(), any()) } just Runs
        advanceUntilIdle()

        viewModel.setMapCoordinates(25.0, 35.0, "Alexandria")
        advanceUntilIdle()

        coVerify { settingsDataStore.setMapCoordinates(25.0, 35.0, "Alexandria") }
    }

    @Test
    fun `setThemeMode should delegate to dataStore`() = runTest {
        coEvery { settingsDataStore.setThemeMode(any()) } just Runs
        advanceUntilIdle()

        viewModel.setThemeMode("dark")
        advanceUntilIdle()

        coVerify { settingsDataStore.setThemeMode("dark") }
    }

    @Test
    fun `uiState should update when settings change`() = runTest {
        advanceUntilIdle()

        temperatureUnitFlow.value = "imperial"
        advanceUntilIdle()

        val state = viewModel.uiState.value as SettingsUiState.Success
        assertEquals("imperial", state.temperatureUnit)
    }
}
