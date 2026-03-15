package com.example.weatherforecast.data.datasource.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.weatherforecast.data.db.WeatherDatabase
import com.example.weatherforecast.data.model.CachedForecast
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class CachedForecastDaoTest {

    private lateinit var database: WeatherDatabase
    private lateinit var dao: CachedForecastDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WeatherDatabase::class.java
        ).allowMainThreadQueries().build()

        dao = database.cachedForecastDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertForecast_validForecast_retrievesForecast() = runTest {
        val forecast = CachedForecast(
            id = 1,
            responseJson = """{"cod":"200"}""",
            lastUpdated = System.currentTimeMillis()
        )
        
        dao.insertForecast(forecast)

        val result = dao.getCachedForecast().first()
        assertNotNull(result)
        assertEquals("""{"cod":"200"}""", result!!.responseJson)
    }

    @Test
    fun getCachedForecastSync_existingData_returnsCachedData() = runTest {
        val forecast = CachedForecast(
            id = 1,
            responseJson = """{"cod":"200"}""",
            lastUpdated = 1000L
        )
        dao.insertForecast(forecast)

        val result = dao.getCachedForecastSync()

        assertNotNull(result)
        assertEquals(1000L, result!!.lastUpdated)
    }

    @Test
    fun getCachedForecastSync_emptyDatabase_returnsNull() = runTest {
        // Given (Implicit via Setup)
        
        // When
        val result = dao.getCachedForecastSync()

        // Then
        assertNull(result)
    }

    @Test
    fun insertForecast_existingId_replacesExisting() = runTest {
        val first = CachedForecast(id = 1, responseJson = """{"v":1}""", lastUpdated = 1000L)
        val second = CachedForecast(id = 1, responseJson = """{"v":2}""", lastUpdated = 2000L)

        dao.insertForecast(first)
        
        dao.insertForecast(second)

        val result = dao.getCachedForecastSync()
        assertNotNull(result)
        assertEquals("""{"v":2}""", result!!.responseJson)
        assertEquals(2000L, result.lastUpdated)
    }

    @Test
    fun clearCache_existingData_removesAllData() = runTest {
        val forecast = CachedForecast(
            id = 1,
            responseJson = """{"cod":"200"}""",
            lastUpdated = System.currentTimeMillis()
        )
        dao.insertForecast(forecast)

        dao.clearCache()

        val result = dao.getCachedForecastSync()
        assertNull(result)
    }

    @Test
    fun getCachedForecast_emptyDatabase_flowReturnsNull() = runTest {
        // Given (Implicit via Setup)
        
        // When
        val result = dao.getCachedForecast().first()

        // Then
        assertNull(result)
    }
}
