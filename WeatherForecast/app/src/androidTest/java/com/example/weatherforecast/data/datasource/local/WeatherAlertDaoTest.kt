package com.example.weatherforecast.data.datasource.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.weatherforecast.data.db.WeatherDatabase
import com.example.weatherforecast.data.model.WeatherAlert
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class WeatherAlertDaoTest {

    private lateinit var database: WeatherDatabase
    private lateinit var dao: WeatherAlertDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WeatherDatabase::class.java
        ).allowMainThreadQueries().build()

        dao = database.weatherAlertDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insert_validAlert_retrievesAllAlerts() = runTest {
        val alert = WeatherAlert(
            startTime = 1000L, endTime = 2000L,
            alertType = "NOTIFICATION", isEnabled = true, snoozeDuration = 5
        )
        
        dao.insert(alert)

        val alerts = dao.getAllAlerts().first()
        assertEquals(1, alerts.size)
        assertEquals("NOTIFICATION", alerts[0].alertType)
    }

    @Test
    fun insert_validAlert_returnsGeneratedId() = runTest {
        val alert = WeatherAlert(
            startTime = 1000L, endTime = 2000L,
            alertType = "NOTIFICATION", isEnabled = true, snoozeDuration = 5
        )
        
        val id = dao.insert(alert)

        assertTrue(id > 0)
    }

    @Test
    fun delete_existingAlert_removesFromDatabase() = runTest {
        val alert = WeatherAlert(
            startTime = 1000L, endTime = 2000L,
            alertType = "NOTIFICATION", isEnabled = true, snoozeDuration = 5
        )
        val id = dao.insert(alert)

        val inserted = dao.getAlertById(id.toInt())
        assertNotNull(inserted)

        dao.delete(inserted!!)

        val alerts = dao.getAllAlerts().first()
        assertTrue(alerts.isEmpty())
    }

    @Test
    fun update_updatedAlert_updatesInDatabase() = runTest {
        val alert = WeatherAlert(
            startTime = 1000L, endTime = 2000L,
            alertType = "NOTIFICATION", isEnabled = true, snoozeDuration = 5
        )
        val id = dao.insert(alert)

        val inserted = dao.getAlertById(id.toInt())!!
        val updated = inserted.copy(isEnabled = false)
        
        dao.update(updated)

        val result = dao.getAlertById(id.toInt())
        assertEquals(false, result!!.isEnabled)
    }

    @Test
    fun getActiveAlerts_mixedAlerts_returnsOnlyActiveAndNotExpired() = runTest {
        val activeAlert = WeatherAlert(
            startTime = 1000L, endTime = 3000L,
            alertType = "NOTIFICATION", isEnabled = true, snoozeDuration = 5
        )
        val expiredAlert = WeatherAlert(
            startTime = 500L, endTime = 1000L,
            alertType = "NOTIFICATION", isEnabled = true, snoozeDuration = 5
        )
        val disabledAlert = WeatherAlert(
            startTime = 1000L, endTime = 3000L,
            alertType = "NOTIFICATION", isEnabled = false, snoozeDuration = 5
        )

        dao.insert(activeAlert)
        dao.insert(expiredAlert)
        dao.insert(disabledAlert)

        val result = dao.getActiveAlerts(1500L)

        assertEquals(1, result.size)
        assertTrue(result[0].isEnabled)
        assertTrue(result[0].endTime > 1500L)
    }

    @Test
    fun getActiveAlerts_noActiveAlerts_returnsEmpty() = runTest {
        // Given
        val expiredAlert = WeatherAlert(
            startTime = 500L, endTime = 1000L,
            alertType = "NOTIFICATION", isEnabled = true, snoozeDuration = 5
        )
        dao.insert(expiredAlert)

        // When
        val result = dao.getActiveAlerts(1500L)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun insert_multipleAlerts_retrievesAll() = runTest {
        dao.insert(WeatherAlert(startTime = 1000L, endTime = 2000L, alertType = "NOTIFICATION"))
        
        dao.insert(WeatherAlert(startTime = 3000L, endTime = 4000L, alertType = "ALARM"))

        val alerts = dao.getAllAlerts().first()
        assertEquals(2, alerts.size)
    }
}
