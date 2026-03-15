package com.example.weatherforecast.data.datasource.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.weatherforecast.data.model.FavoriteLocation
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteLocationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(location: FavoriteLocation)

    @Update
    suspend fun update(location: FavoriteLocation)

    @Delete
    suspend fun delete(location: FavoriteLocation)

    @Query("SELECT * FROM favorite_locations")
    fun getAllFavorites(): Flow<List<FavoriteLocation>>

    @Query("SELECT * FROM favorite_locations WHERE id = :id")
    suspend fun getFavoriteById(id: Int): FavoriteLocation?
}
