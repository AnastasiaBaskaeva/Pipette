package com.baskaeva.pipette.domain

import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ExtractColorsUseCase @Inject constructor(
    private val paletteRepository: PaletteRepository
) {
    suspend operator fun invoke(bitmap: Bitmap, count: Int): List<ColorItem> =
        paletteRepository.extractColors(bitmap, count)
}

class GetFavouritesUseCase @Inject constructor(
    private val favouritesRepository: FavouritesRepository
) {
    operator fun invoke(): Flow<List<ColorItem>> =
        favouritesRepository.getFavourites()
}

class AddFavouriteUseCase @Inject constructor(
    private val favouritesRepository: FavouritesRepository
) {
    suspend operator fun invoke(colorItem: ColorItem) =
        favouritesRepository.addFavourite(colorItem)
}

class DeleteFavouriteUseCase @Inject constructor(
    private val favouritesRepository: FavouritesRepository
) {
    suspend operator fun invoke(colorItem: ColorItem) =
        favouritesRepository.deleteFavourite(colorItem)
}

class AddFavouriteByHexUseCase @Inject constructor(
    private val favouritesRepository: FavouritesRepository
) {
    suspend operator fun invoke(hex: String) =
        favouritesRepository.addFavouriteByHex(hex)
}
