package com.baskaeva.pipette.presentation.favourites

import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baskaeva.pipette.R
import com.baskaeva.pipette.domain.AddFavouriteByHexUseCase
import com.baskaeva.pipette.domain.ColorItem
import com.baskaeva.pipette.domain.DeleteFavouriteUseCase
import com.baskaeva.pipette.domain.GetFavouritesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavouritesViewModel @Inject constructor(
    private val getFavouritesUseCase: GetFavouritesUseCase,
    private val deleteFavouriteUseCase: DeleteFavouriteUseCase,
    private val addFavouriteByHexUseCase: AddFavouriteByHexUseCase
) : ViewModel() {

    val favourites: StateFlow<List<ColorItem>> = getFavouritesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _hexInput = MutableStateFlow("")
    val hexInput: StateFlow<String> = _hexInput.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun onHexInputChange(value: String) {
        _hexInput.value = value
        _errorMessage.value = null
    }

    fun addColorByHex() {
        val input = _hexInput.value.trim()
        if (input.isEmpty()) {
            _errorMessage.value = R.string.no_favs.toString()
            return
        }
        viewModelScope.launch {
            try {
                addFavouriteByHexUseCase(input)
                _hexInput.value = ""
                _errorMessage.value = null
            } catch (_: Exception) {
                _errorMessage.value = R.string.uncorrect_hex.toString()
            }
        }
    }

    fun deleteColor(colorItem: ColorItem) {
        viewModelScope.launch {
            deleteFavouriteUseCase(colorItem)
        }
    }
}
