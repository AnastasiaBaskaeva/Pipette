package com.baskaeva.pipette.data

import android.graphics.Bitmap
import androidx.core.graphics.scale
import androidx.core.graphics.toColorInt
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
import kotlin.math.abs
import kotlin.math.sqrt

class PaletteRepositoryImpl @Inject constructor() : PaletteRepository {

    /**
    *   Нюанс Palette API - все оттенки преобладающего цвета принимает за отдельные цвета,
    * из-за чего на результирующем экране будет отображаться только цвет, преобладающий на картине,
    * в разных своих оттенках, надо показывать также и другие, менее преобладающие цвета
    *   Для корректной работы генерации палитры результат дополнительно группирую по схожим цветам
    */

    override suspend fun extractColors(bitmap: Bitmap, count: Int): List<ColorItem> =
        suspendCancellableCoroutine { continuation ->
            val scaledBitmap = scaleBitmap(bitmap, 200)

            Palette.Builder(scaledBitmap)
                .maximumColorCount(128)  // теперь вместо count*4 (от 12 до 60) прошу больше найденных оттенков
                .generate { palette ->
                    val swatches = palette?.swatches ?: emptyList()
                    val result = if (swatches.isEmpty()) emptyList()
                    else clusterAndPick(swatches, count)
                    continuation.resume(result) // группирую
                }
        }

    /**
     * Кластеризует свотчи по перцептивному сходству в HSL,
     * затем выбирает по одному представителю от каждого кластера.
     *
     * 1. Сортируем свотчи по популяции (самые частые — вперёд).
     * 2. Greedy clustering: свотч попадает в ближайший кластер
     *    (если расстояние < threshold) или создаёт новый.
     * 3. От каждого кластера берём свотч с наибольшей популяцией.
     * 4. Сортируем кластеры по суммарной популяции и берём top-N.
     */
    private fun clusterAndPick(
        swatches: List<Palette.Swatch>,
        count: Int
    ): List<ColorItem> {
        val threshold = 0.12f
        val sorted = swatches.sortedByDescending { it.population }
        val clusters = mutableListOf<MutableList<Palette.Swatch>>()

        for (swatch in sorted) {
            val hsl = swatch.hsl
            val nearest = clusters.minByOrNull { cluster ->
                hslDistance(hsl, representativeHsl(cluster))
            }
            if (nearest != null && hslDistance(hsl, representativeHsl(nearest)) < threshold) {
                nearest.add(swatch)
            } else {
                clusters.add(mutableListOf(swatch))
            }
        }

        return clusters
            .sortedByDescending { cluster -> cluster.sumOf { it.population } }
            .take(count)
            .map { cluster ->
                val representative = cluster.maxByOrNull { it.population }!!
                ColorItem(
                    hex = rgbToHex(representative.rgb),
                    rgb = representative.rgb,
                    population = cluster.sumOf { it.population }
                )
            }
    }

    /**
     * Средневзвешенный HSL кластера.
     * Оттенок усредняется через sin/cos для корректной обработки перехода 360°→0°.
     */
    private fun representativeHsl(cluster: List<Palette.Swatch>): FloatArray {
        val totalPop = cluster.sumOf { it.population }.toFloat()
        if (totalPop == 0f) return cluster.first().hsl

        var sinSum = 0f
        var cosSum = 0f
        var sSum = 0f
        var lSum = 0f

        for (swatch in cluster) {
            val w = swatch.population / totalPop
            val hRad = Math.toRadians(swatch.hsl[0].toDouble())
            sinSum += Math.sin(hRad).toFloat() * w
            cosSum += Math.cos(hRad).toFloat() * w
            sSum += swatch.hsl[1] * w
            lSum += swatch.hsl[2] * w
        }

        val hAvg = (Math.toDegrees(Math.atan2(sinSum.toDouble(), cosSum.toDouble()))
            .toFloat() + 360f) % 360f

        return floatArrayOf(hAvg, sSum, lSum)
    }

    /**
     * Перцептивное расстояние в HSL.
     * Оттенок нормализован в [0,1] с учётом цикличности и взвешен вдвое сильнее.
     */
    private fun hslDistance(a: FloatArray, b: FloatArray): Float {
        val dh = minOf(abs(a[0] - b[0]), 360f - abs(a[0] - b[0])) / 360f
        val ds = abs(a[1] - b[1])
        val dl = abs(a[2] - b[2])
        return sqrt((dh * 2f) * (dh * 2f) + ds * ds + dl * dl)
    }

    private fun scaleBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val scale = minOf(maxSize.toFloat() / width, maxSize.toFloat() / height)
        return if (scale < 1f) {
            bitmap.scale((width * scale).toInt(), (height * scale).toInt())
        } else bitmap
    }

    private fun rgbToHex(rgb: Int): String = String.format("#%06X", 0xFFFFFF and rgb)
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