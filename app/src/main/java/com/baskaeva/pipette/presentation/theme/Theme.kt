package com.baskaeva.pipette.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BackgroundDark = Color(0xFF121212)
val SurfaceDark = Color(0xFF1E1E1E)
val CardDark = Color(0xFF2A2A2A)
val AccentColor = Color(0xFFBB86FC)
val OnBackground = Color(0xFFE0E0E0)
val OnSurface = Color(0xFFB0B0B0)

private val DarkColorScheme = darkColorScheme(
    primary = AccentColor,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = Color.Black,
    onBackground = OnBackground,
    onSurface = OnSurface,
)

@Composable
fun PipetteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
