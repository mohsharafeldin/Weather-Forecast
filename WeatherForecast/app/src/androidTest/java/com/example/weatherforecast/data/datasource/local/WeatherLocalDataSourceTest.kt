package com.example.weatherforecast.data.datasource.local

import com.example.weatherforecast.data.model.CachedForecast
import com.example.weatherforecast.data.model.FavoriteLocation
import com.example.weatherforecast.data.model.WeatherAlert
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class WeatherLocalDataSourceTest {

    private lateinit var favoriteLocationDao: FavoriteLocationDao
    private lateinit var weatherAlertDao: WeatherAlertDao
    private lateinit var cachedForecastDao: CachedForecastDao
    private lateinit var localDataSource: WeatherLocalDataSource

    private val testLocation = FavoriteLocation(
        id = 1, name = "Cairo", latitude = 30.0, longitude = 31.0
    )

    private val testAlert = WeatherAlert(
        id = 1, startTime = 1000L, endTime = 2000L,
        alertType = "NOTIFICATION", isEnabled = true, snoozeDuration = 5
    )

    private val testCachedForecast = CachedForecast(
        id = 1, responseJson = "{}", lastUpdated = System.currentTimeMillis()
    )

    @Before
    fun setup() {
        favoriteLocationDao = mockk(relaxed = true)
        weatherAlertDao = mockk(relaxed = true)
        cachedForecastDao = mockk(relaxed = true)

        localDataSource = WeatherLocalDataSource(
            favoriteLocationDao, weatherAlertDao, cachedForecastDao
        )
    }

    @Test
    fun getAllFavorites_delegatesToDao() = runTest {
        every { favoriteLocationDao.getAllFavorites() } returns flowOf(listOf(testLocation))

        val result = localDataSource.getAllFavorites().first()

        assertEquals(1, result.size)
        assertEquals("Cairo", result[0].name)
    }

    @Test
    fun addFavorite_delegatesToDao() = runTest {
        coEvery { favoriteLocationDao.insert(any()) } just Runs

        localDataSource.addFavorite(testLocation)

        coVerify { favoriteLocationDao.insert(testLocation) }
    }

    @Test
    fun updateFavorite_delegatesToDao() = runTest {
        coEvery { favoriteLocationDao.update(any()) } just Runs

        localDataSource.updateFavorite(testLocation)

        coVerify { favoriteLocationDao.update(testLocation) }
    }

    @Test
    fun removeFavorite_delegatesToDao() = runTest {
        coEvery { favoriteLocationDao.delete(any()) } just Runs

        localDataSource.removeFavorite(testLocation)

        coVerify { favoriteLocationDao.delete(testLocation) }
    }

    @Test
    fun getFavoriteById_delegatesToDao() = runTest {
        coEvery { favoriteLocationDao.getFavoriteById(1) } returns testLocation

        val result = localDataSource.getFavoriteById(1)

        assertEquals(testLocation, result)
    }

    @Test
    fun getFavoriteById_returnsNullWhenNotFound() = runTest {
        coEvery { favoriteLocationDao.getFavoriteById(99) } returns null

        val result = localDataSource.getFavoriteById(99)

        assertNull(result)
    }

    @Test
    fun getAllAlerts_delegatesToDao() = runTest {
        every { weatherAlertDao.getAllAlerts() } returns flowOf(listOf(testAlert))

        val result = localDataSource.getAllAlerts().first()

        assertEquals(1, result.size)
    }

    @Test
    fun addAlert_delegatesToDao() = runTest {
        coEvery { weatherAlertDao.insert(any()) } returns 1L

        val result = localDataSource.addAlert(testAlert)

        assertEquals(1L, result)
        coVerify { weatherAlertDao.insert(testAlert) }
    }

    @Test
    fun removeAlert_delegatesToDao() = runTest {
        coEvery { weatherAlertDao.delete(any()) } just Runs

        localDataSource.removeAlert(testAlert)

        coVerify { weatherAlertDao.delete(testAlert) }
    }

    @Test
    fun updateAlert_delegatesToDao() = runTest {
        coEvery { weatherAlertDao.update(any()) } just Runs

        localDataSource.updateAlert(testAlert)

        coVerify { weatherAlertDao.update(testAlert) }
    }

    @Test
    fun getActiveAlerts_delegatesToDao() = runTest {
        coEvery { weatherAlertDao.getActiveAlerts(any()) } returns listOf(testAlert)

        val result = localDataSource.getActiveAlerts(1500L)

        assertEquals(1, result.size)
        coVerify { weatherAlertDao.getActiveAlerts(1500L) }
    }

    @Test
    fun cacheForecast_delegatesToDao() = runTest {
        coEvery { cachedForecastDao.insertForecast(any()) } just Runs

        localDataSource.cacheForecast(testCachedForecast)

        coVerify { cachedForecastDao.insertForecast(testCachedForecast) }
    }

    @Test
    fun getCachedForecast_delegatesToDao() = runTest {
        every { cachedForecastDao.getCachedForecast() } returns flowOf(testCachedForecast)

        val result = localDataSource.getCachedForecast().first()

        assertEquals(testCachedForecast, result)
    }

    @Test
    fun getCachedForecastSync_delegatesToDao() = runTest {
        coEvery { cachedForecastDao.getCachedForecastSync() } returns testCachedForecast

        val result = localDataSource.getCachedForecastSync()

        assertEquals(testCachedForecast, result)
    }
}
