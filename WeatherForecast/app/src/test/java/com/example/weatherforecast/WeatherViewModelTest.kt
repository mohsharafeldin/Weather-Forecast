package com.example.weatherforecast

import com.example.weatherforecast.model.*
import com.example.weatherforecast.alerts.AlertsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {

    private lateinit var fakeRepository: FakeWeatherRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeWeatherRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getForecast returns default data when no error`() = runTest {
        val response = fakeRepository.getForecast(30.0, 31.0)
        assertEquals("200", response.cod)
        assertEquals("Test City", response.city.name)
        assertEquals(1, response.list.size)
    }

    @Test
    fun `getForecast throws when shouldThrowError is true`() = runTest {
        fakeRepository.shouldThrowError = true
        try {
            fakeRepository.getForecast(30.0, 31.0)
            fail("Expected exception not thrown")
        } catch (e: Exception) {
            assertEquals("Test error", e.message)
        }
    }

    @Test
    fun `addFavorite and getAllFavorites works correctly`() = runTest {
        val location = FavoriteLocation(id = 1, name = "Cairo", latitude = 30.0, longitude = 31.0)
        fakeRepository.addFavorite(location)
        val favorites = fakeRepository.getAllFavorites().first()
        assertEquals(1, favorites.size)
        assertEquals("Cairo", favorites[0].name)
    }

    @Test
    fun `removeFavorite works correctly`() = runTest {
        val location = FavoriteLocation(id = 1, name = "Cairo", latitude = 30.0, longitude = 31.0)
        fakeRepository.addFavorite(location)
        fakeRepository.removeFavorite(location)
        val favorites = fakeRepository.getAllFavorites().first()
        assertTrue(favorites.isEmpty())
    }

    @Test
    fun `addAlert and remove alert works correctly`() = runTest {
        val alert = WeatherAlert(
            id = 1,
            startTime = System.currentTimeMillis(),
            endTime = System.currentTimeMillis() + 86400000,
            alertType = "NOTIFICATION",
            isEnabled = true
        )
        fakeRepository.addAlert(alert)
        val alerts = fakeRepository.getAllAlerts().first()
        assertEquals(1, alerts.size)

        fakeRepository.removeAlert(alert)
        val alertsAfter = fakeRepository.getAllAlerts().first()
        assertTrue(alertsAfter.isEmpty())
    }

    @Test
    fun `updateAlert toggles enabled state`() = runTest {
        val alert = WeatherAlert(
            id = 1,
            startTime = System.currentTimeMillis(),
            endTime = System.currentTimeMillis() + 86400000,
            alertType = "ALARM",
            isEnabled = true
        )
        fakeRepository.addAlert(alert)
        fakeRepository.updateAlert(alert.copy(isEnabled = false))

        val updated = fakeRepository.getAllAlerts().first()
        assertFalse(updated[0].isEnabled)
    }

    @Test
    fun `getActiveAlerts returns only enabled future alerts`() = runTest {
        val now = System.currentTimeMillis()
        val activeAlert = WeatherAlert(1, now - 1000, now + 86400000, "NOTIFICATION", true)
        val expiredAlert = WeatherAlert(2, now - 86400000, now - 1000, "ALARM", true)
        val disabledAlert = WeatherAlert(3, now - 1000, now + 86400000, "NOTIFICATION", false)

        fakeRepository.addAlert(activeAlert)
        fakeRepository.addAlert(expiredAlert)
        fakeRepository.addAlert(disabledAlert)

        val active = fakeRepository.getActiveAlerts(now)
        assertEquals(1, active.size)
        assertEquals(1, active[0].id)
    }

    @Test
    fun `AlertsViewModel addAlert adds to list`() = runTest {
        val viewModel = AlertsViewModel(fakeRepository)
        val now = System.currentTimeMillis()
        viewModel.addAlert(now, now + 86400000, "NOTIFICATION")

        advanceUntilIdle()

        val alerts = fakeRepository.getAllAlerts().first()
        assertEquals(1, alerts.size)
        assertEquals("NOTIFICATION", alerts[0].alertType)
    }

    @Test
    fun `AlertsViewModel removeAlert removes from list`() = runTest {
        val alert = WeatherAlert(1, System.currentTimeMillis(), System.currentTimeMillis() + 86400000, "ALARM", true)
        fakeRepository.addAlert(alert)

        val viewModel = AlertsViewModel(fakeRepository)
        viewModel.removeAlert(alert)
        advanceUntilIdle()

        val alerts = fakeRepository.getAllAlerts().first()
        assertTrue(alerts.isEmpty())
    }
}
