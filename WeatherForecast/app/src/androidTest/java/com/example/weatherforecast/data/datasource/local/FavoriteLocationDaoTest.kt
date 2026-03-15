package com.example.weatherforecast.data.datasource.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.weatherforecast.data.db.WeatherDatabase
import com.example.weatherforecast.data.model.FavoriteLocation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class FavoriteLocationDaoTest {

    private lateinit var database: WeatherDatabase
    private lateinit var dao: FavoriteLocationDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WeatherDatabase::class.java
        ).allowMainThreadQueries().build()

        dao = database.favoriteLocationDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetAllFavorites() = runTest {
        val location = FavoriteLocation(name = "Cairo", latitude = 30.0, longitude = 31.0)
        dao.insert(location)

        val favorites = dao.getAllFavorites().first()

        assertEquals(1, favorites.size)
        assertEquals("Cairo", favorites[0].name)
    }

    @Test
    fun insertMultipleAndGetAll() = runTest {
        dao.insert(FavoriteLocation(name = "Cairo", latitude = 30.0, longitude = 31.0))
        dao.insert(FavoriteLocation(name = "Alex", latitude = 31.2, longitude = 29.9))

        val favorites = dao.getAllFavorites().first()

        assertEquals(2, favorites.size)
    }

    @Test
    fun getFavoriteById_returnsCorrectItem() = runTest {
        val location = FavoriteLocation(name = "Cairo", latitude = 30.0, longitude = 31.0)
        dao.insert(location)

        val favorites = dao.getAllFavorites().first()
        val insertedId = favorites[0].id
        val result = dao.getFavoriteById(insertedId)

        assertNotNull(result)
        assertEquals("Cairo", result!!.name)
    }

    @Test
    fun getFavoriteById_returnsNullForNonExistentId() = runTest {
        val result = dao.getFavoriteById(999)

        assertNull(result)
    }

    @Test
    fun updateFavorite() = runTest {
        val location = FavoriteLocation(name = "Cairo", latitude = 30.0, longitude = 31.0)
        dao.insert(location)

        val inserted = dao.getAllFavorites().first()[0]
        val updated = inserted.copy(name = "New Cairo")
        dao.update(updated)

        val result = dao.getFavoriteById(inserted.id)
        assertEquals("New Cairo", result!!.name)
    }

    @Test
    fun deleteFavorite() = runTest {
        val location = FavoriteLocation(name = "Cairo", latitude = 30.0, longitude = 31.0)
        dao.insert(location)

        val inserted = dao.getAllFavorites().first()[0]
        dao.delete(inserted)

        val favorites = dao.getAllFavorites().first()
        assertTrue(favorites.isEmpty())
    }

    @Test
    fun insertWithReplaceStrategy() = runTest {
        val location = FavoriteLocation(id = 1, name = "Cairo", latitude = 30.0, longitude = 31.0)
        dao.insert(location)

        val updated = location.copy(name = "Cairo Updated")
        dao.insert(updated)

        val result = dao.getFavoriteById(1)
        assertEquals("Cairo Updated", result!!.name)
    }
}
