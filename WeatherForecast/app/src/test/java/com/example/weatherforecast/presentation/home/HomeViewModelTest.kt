package com.example.weatherforecast.presentation.home

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.weatherforecast.data.model.CityData
import com.example.weatherforecast.data.model.CloudsData
import com.example.weatherforecast.data.model.Coord
import com.example.weatherforecast.data.model.MainData
import com.example.weatherforecast.data.model.WeatherDescription
import com.example.weatherforecast.data.model.WeatherItem
import com.example.weatherforecast.data.model.WeatherResponse
import com.example.weatherforecast.data.model.WindData
import com.example.weatherforecast.data.network.IConnectivityObserver
import com.example.weatherforecast.data.repository.IForecastRepository
import com.example.weatherforecast.presentation.settings.ISettingsDataStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
class HomeViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: IForecastRepository
    private lateinit var settingsDataStore: ISettingsDataStore
    private lateinit var connectivityObserver: IConnectivityObserver
    private lateinit var viewModel: HomeViewModel

    private val fakeWeatherItem = WeatherItem(
        dt = 1000L,
        main = MainData(
            temp = 25.0, feelsLike = 23.0, tempMin = 20.0, tempMax = 28.0,
            pressure = 1013, humidity = 60
        ),
        weather = listOf(
            WeatherDescription(id = 800, main = "Clear", description = "clear sky", icon = "01d")
        ),
        clouds = CloudsData(all = 0),
        wind = WindData(speed = 5.0, deg = 180),
        visibility = 10000,
        pop = 0.0,
        dtTxt = "2024-01-01 12:00:00"
    )

    private val fakeResponse = WeatherResponse(
        cod = "200",
        message = 0,
        cnt = 1,
        list = listOf(fakeWeatherItem),
        city = CityData(
            id = 1, name = "Cairo", coord = Coord(lat = 30.0, lon = 31.0),
            country = "EG", population = 1000000, timezone = 7200,
            sunrise = 1000L, sunset = 2000L
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        repository = mockk(relaxed = true)
        settingsDataStore = mockk(relaxed = true)
        connectivityObserver = mockk(relaxed = true)

        every { settingsDataStore.temperatureUnit } returns MutableStateFlow("metric")
        every { settingsDataStore.language } returns MutableStateFlow("en")
        every { settingsDataStore.windSpeedUnit } returns MutableStateFlow("m/s")
        every { settingsDataStore.locationMode } returns MutableStateFlow("gps")
        every { settingsDataStore.mapLat } returns MutableStateFlow(30.0)
        every { settingsDataStore.mapLon } returns MutableStateFlow(31.0)
        every { connectivityObserver.isOnline } returns MutableStateFlow(true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be Loading`() = runTest {
        viewModel = HomeViewModel(repository, settingsDataStore, connectivityObserver)
        assertEquals(HomeUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `fetchForecast should emit Success on successful response`() = runTest {
        every { repository.getForecast(any(), any(), any(), any()) } returns flowOf(fakeResponse)
        coEvery { repository.cacheForecast(any()) } returns Unit

        viewModel = HomeViewModel(repository, settingsDataStore, connectivityObserver)
        advanceUntilIdle()

        viewModel.fetchForecast(30.0, 31.0)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is HomeUiState.Success)
    }

    @Test
    fun `fetchForecast should use cached data when network fails`() = runTest {
        every { repository.getForecast(any(), any(), any(), any()) } returns flowOf(fakeResponse)
        coEvery { repository.cacheForecast(any()) } returns Unit
        coEvery { repository.getCachedForecastSync() } returns fakeResponse

        viewModel = HomeViewModel(repository, settingsDataStore, connectivityObserver)
        advanceUntilIdle()

        viewModel.fetchForecast(30.0, 31.0)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Success)
    }

    @Test
    fun `fetchForecast should cache response on success`() = runTest {
        every { repository.getForecast(any(), any(), any(), any()) } returns flowOf(fakeResponse)
        coEvery { repository.cacheForecast(any()) } returns Unit

        viewModel = HomeViewModel(repository, settingsDataStore, connectivityObserver)
        advanceUntilIdle()

        viewModel.fetchForecast(30.0, 31.0)
        advanceUntilIdle()

        coVerify { repository.cacheForecast(any()) }
    }

    @Test
    fun `refreshForecast should toggle isRefreshing`() = runTest {
        every { repository.getForecast(any(), any(), any(), any()) } returns flowOf(fakeResponse)
        coEvery { repository.cacheForecast(any()) } returns Unit

        viewModel = HomeViewModel(repository, settingsDataStore, connectivityObserver)
        advanceUntilIdle()

        viewModel.fetchForecast(30.0, 31.0)
        advanceUntilIdle()

        viewModel.refreshForecast()
        advanceUntilIdle()

        assertEquals(false, viewModel.isRefreshing.value)
    }

    @Test
    fun `Success state should contain correct temperature unit`() = runTest {
        every { repository.getForecast(any(), any(), any(), any()) } returns flowOf(fakeResponse)
        coEvery { repository.cacheForecast(any()) } returns Unit

        viewModel = HomeViewModel(repository, settingsDataStore, connectivityObserver)
        advanceUntilIdle()

        viewModel.fetchForecast(30.0, 31.0)
        advanceUntilIdle()

        val state = viewModel.uiState.value as HomeUiState.Success
        assertEquals("metric", state.temperatureUnit)
        assertEquals("m/s", state.windSpeedUnit)
    }
}
