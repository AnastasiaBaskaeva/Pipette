package com.baskaeva.pipette.presentation.navigation

sealed class Screen(val route: String) {
    object Camera : Screen("camera")
    object Result : Screen("result/{colorCount}") {
        fun createRoute(colorCount: Int) = "result/$colorCount"
    }
    object Favourites : Screen("favourites")
}
