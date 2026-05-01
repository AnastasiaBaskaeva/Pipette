package com.baskaeva.pipette.presentation.result

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baskaeva.pipette.domain.ColorItem
import com.baskaeva.pipette.domain.AddFavouriteUseCase
import com.baskaeva.pipette.domain.DeleteFavouriteUseCase
import com.baskaeva.pipette.domain.ExtractColorsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed class ResultUiState {
    object Loading : ResultUiState()
    data class Success(val colors: List<ColorItem>) : ResultUiState()
    data class Error(val message: String) : ResultUiState()
}

@HiltViewModel
class ResultPhotoViewModel @Inject constructor(
    private val extractColorsUseCase: ExtractColorsUseCase,
    private val addFavouriteUseCase: AddFavouriteUseCase,
    private val deleteFavouriteUseCase: DeleteFavouriteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ResultUiState>(ResultUiState.Loading)
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    // Хранит HEX-коды цветов, добавленных в избранное на этом экране
    private val _favouritedHexes = MutableStateFlow<Set<String>>(emptySet())
    val favouritedHexes: StateFlow<Set<String>> = _favouritedHexes.asStateFlow()

    fun processImage(context: Context, uri: Uri, colorCount: Int) {
        viewModelScope.launch {
            _uiState.value = ResultUiState.Loading
            _favouritedHexes.value = emptySet()
            try {
                val bitmap = withContext(Dispatchers.IO) { loadBitmap(context, uri) }
                if (bitmap == null) {
                    _uiState.value = ResultUiState.Error("Не удалось загрузить изображение")
                    return@launch
                }
                val colors = withContext(Dispatchers.Default) {
                    extractColorsUseCase(bitmap, colorCount)
                }
                _uiState.value = ResultUiState.Success(colors)
            } catch (e: Exception) {
                _uiState.value = ResultUiState.Error("Ошибка обработки: ${e.localizedMessage}")
            }
        }
    }

    fun toggleFavourite(colorItem: ColorItem) {
        viewModelScope.launch {
            val hex = colorItem.hex
            if (hex in _favouritedHexes.value) {
                deleteFavouriteUseCase(colorItem)
                _favouritedHexes.value = _favouritedHexes.value - hex
            } else {
                addFavouriteUseCase(colorItem)
                _favouritedHexes.value = _favouritedHexes.value + hex
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun loadBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
            } else {
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (_: Exception) {
            null
        }
    }
}