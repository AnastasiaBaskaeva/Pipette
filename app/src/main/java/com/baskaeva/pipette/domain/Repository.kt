package com.baskaeva.pipette.domain

import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow

interface PaletteRepository {
    suspend fun extractColors(bitmap: Bitmap, count: Int): List<ColorItem>
}

interface FavouritesRepository {
    fun getFavourites(): Flow<List<ColorItem>>
    suspend fun addFavourite(colorItem: ColorItem)
    suspend fun deleteFavourite(colorItem: ColorItem)
    suspend fun addFavouriteByHex(hex: String)
}
