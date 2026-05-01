package com.baskaeva.pipette.presentation.camera

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor() : ViewModel() {

    private val _colorCount = MutableStateFlow(6)
    val colorCount: StateFlow<Int> = _colorCount.asStateFlow()

    private val _pipetteColor = MutableStateFlow<Int?>(null)
    val pipetteColor: StateFlow<Int?> = _pipetteColor.asStateFlow()

    private val _currentZoomRatio = MutableStateFlow(1f)
    val currentZoomRatio: StateFlow<Float> = _currentZoomRatio.asStateFlow()

    val colorCountOptions = listOf(3, 6, 9, 12, 15)

    fun setColorCount(count: Int) {
        _colorCount.value = count
    }

    fun setPipetteColor(rgb: Int) {
        _pipetteColor.value = rgb
    }

    fun setZoomRatio(ratio: Float) {
        _currentZoomRatio.value = ratio
    }
}