package com.baskaeva.pipette.presentation.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.baskaeva.pipette.presentation.camera.CameraScreen
import com.baskaeva.pipette.presentation.favourites.FavouritesScreen
import com.baskaeva.pipette.presentation.result.ResultPhotoScreen

object SharedImageState {
    var capturedImageUri: Uri? = null
}

@Composable
fun PipetteNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Camera.route
    ) {
        composable(Screen.Camera.route) {
            CameraScreen(
                onPhotoTaken = { uri, colorCount ->
                    SharedImageState.capturedImageUri = uri
                    navController.navigate(Screen.Result.createRoute(colorCount))
                },
                onOpenFavourites = {
                    navController.navigate(Screen.Favourites.route)
                }
            )
        }

        composable(
            route = Screen.Result.route,
            arguments = listOf(
                navArgument("colorCount") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val colorCount = backStackEntry.arguments?.getInt("colorCount") ?: 6
            val imageUri = SharedImageState.capturedImageUri
            ResultPhotoScreen(
                imageUri = imageUri,
                colorCount = colorCount,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Favourites.route) {
            FavouritesScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
