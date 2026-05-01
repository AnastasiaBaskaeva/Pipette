package com.baskaeva.pipette.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "favourites",
    indices = [Index(value = ["hex"], unique = true)]
)
data class FavouriteColorEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hex: String,
    val rgb: Int
)