package com.baskaeva.pipette.data

import android.graphics.Bitmap
import androidx.palette.graphics.Palette
import com.baskaeva.pipette.data.local.FavouriteColorDao
import com.baskaeva.pipette.data.local.FavouriteColorEntity
import com.baskaeva.pipette.domain.ColorItem
import com.baskaeva.pipette.domain.FavouritesRepository
import com.baskaeva.pipette.domain.PaletteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import androidx.core.graphics.scale
import androidx.core.graphics.toColorInt

class PaletteRepositoryImpl @Inject constructor() : PaletteRepository {

    override suspend fun extractColors(bitmap: Bitmap, count: Int): List<ColorItem> =
        suspendCancellableCoroutine { continuation ->
            // Scale down bitmap for performance
            val scaledBitmap = scaleBitmap(bitmap, 200)

            Palette.Builder(scaledBitmap)
                .maximumColorCount(count * 4) // Generate more, then pick top N by population
                .generate { palette ->
                    val swatches = palette?.swatches
                        ?.sortedByDescending { it.population }
                        ?.take(count)
                        ?: emptyList()

                    val colors = swatches.map { swatch ->
                        ColorItem(
                            hex = rgbToHex(swatch.rgb),
                            rgb = swatch.rgb,
                            population = swatch.population
                        )
                    }

                    // If we got fewer colors than requested, fill with distinct hues
                    val result = if (colors.size < count) {
                        val targets = getTargetSwatches(palette, count)
                        targets.map { rgb ->
                            ColorItem(hex = rgbToHex(rgb), rgb = rgb)
                        }
                    } else colors

                    continuation.resume(result)
                }
        }

    private fun getTargetSwatches(palette: Palette?, count: Int): List<Int> {
        if (palette == null) return emptyList()
        val all = palette.swatches.sortedByDescending { it.population }.map { it.rgb }
        return all.take(count)
    }

    private fun scaleBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val scale = minOf(maxSize.toFloat() / width, maxSize.toFloat() / height)
        return if (scale < 1f) {
            bitmap.scale((width * scale).toInt(), (height * scale).toInt())
        } else bitmap
    }

    private fun rgbToHex(rgb: Int): String {
        return String.format("#%06X", 0xFFFFFF and rgb)
    }
}

class FavouritesRepositoryImpl @Inject constructor(
    private val dao: FavouriteColorDao
) : FavouritesRepository {

    override fun getFavourites(): Flow<List<ColorItem>> =
        dao.getAll().map { entities ->
            entities.map { it.toColorItem() }
        }

    override suspend fun addFavourite(colorItem: ColorItem) {
        dao.insert(colorItem.toEntity())
    }

    override suspend fun deleteFavourite(colorItem: ColorItem) {
        dao.deleteByHex(colorItem.hex)
    }

    override suspend fun addFavouriteByHex(hex: String) {
        val normalizedHex = normalizeHex(hex) ?: return
        val rgb = hexToRgb(normalizedHex)
        dao.insert(FavouriteColorEntity(hex = normalizedHex, rgb = rgb))
    }

    private fun FavouriteColorEntity.toColorItem() = ColorItem(
        id = id,
        hex = hex,
        rgb = rgb
    )

    private fun ColorItem.toEntity() = FavouriteColorEntity(
        id = id,
        hex = hex,
        rgb = rgb
    )

    private fun normalizeHex(input: String): String? {
        val cleaned = input.trim().removePrefix("#").uppercase()
        return when (cleaned.length) {
            6 -> if (cleaned.all { it.isDigit() || it in 'A'..'F' }) "#$cleaned" else null
            3 -> {
                val expanded = cleaned.map { "$it$it" }.joinToString("")
                "#$expanded"
            }
            else -> null
        }
    }

    private fun hexToRgb(hex: String): Int {
        return try {
            hex.toColorInt()
        } catch (e: Exception) {
            0xFF000000.toInt()
        }
    }
}