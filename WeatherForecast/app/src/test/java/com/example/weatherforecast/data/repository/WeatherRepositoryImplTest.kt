package com.example.weatherforecast.data.repository

import com.example.weatherforecast.data.datasource.local.IWeatherLocalDataSource
import com.example.weatherforecast.data.datasource.remote.IWeatherRemoteDataSource
import com.example.weatherforecast.data.model.CachedForecast
import com.example.weatherforecast.data.model.CityData
import com.example.weatherforecast.data.model.CloudsData
import com.example.weatherforecast.data.model.Coord
import com.example.weatherforecast.data.model.FavoriteLocation
import com.example.weatherforecast.data.model.GeocodingResult
import com.example.weatherforecast.data.model.MainData
import com.example.weatherforecast.data.model.WeatherAlert
import com.example.weatherforecast.data.model.WeatherDescription
import com.example.weatherforecast.data.model.WeatherItem
import com.example.weatherforecast.data.model.WeatherResponse
import com.example.weatherforecast.data.model.WindData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherRepositoryImplTest {

    private lateinit var remoteDataSource: IWeatherRemoteDataSource
    private lateinit var localDataSource: IWeatherLocalDataSource
    private lateinit var repository: WeatherRepositoryImpl

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

    private val testLocation = FavoriteLocation(
        id = 1, name = "Cairo", latitude = 30.0, longitude = 31.0
    )

    private val testAlert = WeatherAlert(
        id = 1, startTime = 1000L, endTime = 2000L,
        alertType = "NOTIFICATION", isEnabled = true, snoozeDuration = 5
    )

    @Before
    fun setup() {
        remoteDataSource = mockk(relaxed = true)
        localDataSource = mockk(relaxed = true)

        every { localDataSource.getAllFavorites() } returns flowOf(emptyList())
        every { localDataSource.getAllAlerts() } returns flowOf(emptyList())
        every { localDataSource.getCachedForecast() } returns flowOf(null)

        repository = WeatherRepositoryImpl(remoteDataSource, localDataSource)
    }

    @Test
    fun `getForecast should delegate to remote data source with API key`() = runTest {
        every {
            remoteDataSource.getForecast(any(), any(), any(), any(), any())
        } returns flowOf(fakeResponse)

        val result = repository.getForecast(30.0, 31.0).first()

        assertEquals("200", result.cod)
        assertEquals("Cairo", result.city.name)
    }

    @Test
    fun `searchCity should delegate to remote data source`() = runTest {
        val results = listOf(
            GeocodingResult(name = "Cairo", lat = 30.0, lon = 31.0, country = "EG")
        )
        every { remoteDataSource.searchCity(any(), any(), any()) } returns flowOf(results)

        val result = repository.searchCity("Cairo").first()

        assertEquals(1, result.size)
        assertEquals("Cairo", result[0].name)
    }

    @Test
    fun `addFavorite should delegate to local data source`() = runTest {
        coEvery { localDataSource.addFavorite(any()) } just Runs

        repository.addFavorite(testLocation)

        coVerify { localDataSource.addFavorite(testLocation) }
    }

    @Test
    fun `updateFavorite should delegate to local data source`() = runTest {
        coEvery { localDataSource.updateFavorite(any()) } just Runs

        repository.updateFavorite(testLocation)

        coVerify { localDataSource.updateFavorite(testLocation) }
    }

    @Test
    fun `removeFavorite should delegate to local data source`() = runTest {
        coEvery { localDataSource.removeFavorite(any()) } just Runs

        repository.removeFavorite(testLocation)

        coVerify { localDataSource.removeFavorite(testLocation) }
    }

    @Test
    fun `getFavoriteById should delegate to local data source`() = runTest {
        coEvery { localDataSource.getFavoriteById(1) } returns testLocation

        val result = repository.getFavoriteById(1)

        assertEquals(testLocation, result)
    }

    @Test
    fun `addAlert should delegate to local data source`() = runTest {
        coEvery { localDataSource.addAlert(any()) } returns 1L

        val result = repository.addAlert(testAlert)

        assertEquals(1L, result)
        coVerify { localDataSource.addAlert(testAlert) }
    }

    @Test
    fun `removeAlert should delegate to local data source`() = runTest {
        coEvery { localDataSource.removeAlert(any()) } just Runs

        repository.removeAlert(testAlert)

        coVerify { localDataSource.removeAlert(testAlert) }
    }

    @Test
    fun `updateAlert should delegate to local data source`() = runTest {
        coEvery { localDataSource.updateAlert(any()) } just Runs

        repository.updateAlert(testAlert)

        coVerify { localDataSource.updateAlert(testAlert) }
    }

    @Test
    fun `getActiveAlerts should delegate to local data source`() = runTest {
        coEvery { localDataSource.getActiveAlerts(any()) } returns listOf(testAlert)

        val result = repository.getActiveAlerts(1500L)

        assertEquals(1, result.size)
        coVerify { localDataSource.getActiveAlerts(1500L) }
    }

    @Test
    fun `cacheForecast should serialize and delegate to local`() = runTest {
        val slot = slot<CachedForecast>()
        coEvery { localDataSource.cacheForecast(capture(slot)) } just Runs

        repository.cacheForecast(fakeResponse)

        coVerify { localDataSource.cacheForecast(any()) }
        assertNotNull(slot.captured.responseJson)
        assertEquals(1, slot.captured.id)
    }

    @Test
    fun `getCachedForecastSync should deserialize from local`() = runTest {
        val json = com.google.gson.Gson().toJson(fakeResponse)
        val cached = CachedForecast(id = 1, responseJson = json, lastUpdated = System.currentTimeMillis())
        coEvery { localDataSource.getCachedForecastSync() } returns cached

        val result = repository.getCachedForecastSync()

        assertNotNull(result)
        assertEquals("Cairo", result!!.city.name)
    }

    @Test
    fun `getCachedForecastSync should return null when no cache`() = runTest {
        coEvery { localDataSource.getCachedForecastSync() } returns null

        val result = repository.getCachedForecastSync()

        assertNull(result)
    }

    @Test
    fun `getCachedWeatherForFavorite should return null when no cached json`() = runTest {
        val locationNoCached = testLocation.copy(cachedResponseJson = null)

        val result = repository.getCachedWeatherForFavorite(locationNoCached)

        assertNull(result)
    }

    @Test
    fun `getCachedWeatherForFavorite should deserialize cached json`() = runTest {
        val json = com.google.gson.Gson().toJson(fakeResponse)
        val locationWithCache = testLocation.copy(cachedResponseJson = json)

        val result = repository.getCachedWeatherForFavorite(locationWithCache)

        assertNotNull(result)
        assertEquals("Cairo", result!!.city.name)
    }
}
