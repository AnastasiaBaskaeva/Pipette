package com.baskaeva.pipette.domain

data class ColorItem(
    val id: Long = 0,
    val hex: String,
    val rgb: Int,
    val population: Int = 0
)
