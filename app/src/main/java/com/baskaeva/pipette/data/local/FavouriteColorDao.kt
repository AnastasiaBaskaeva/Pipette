package com.baskaeva.pipette.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouriteColorDao {

    @Query("SELECT * FROM favourites ORDER BY id DESC")
    fun getAll(): Flow<List<FavouriteColorEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: FavouriteColorEntity)

    @Delete
    suspend fun delete(entity: FavouriteColorEntity)

    @Query("DELETE FROM favourites WHERE hex = :hex")
    suspend fun deleteByHex(hex: String)

    @Query("SELECT * FROM favourites WHERE hex = :hex LIMIT 1")
    suspend fun getByHex(hex: String): FavouriteColorEntity?
}