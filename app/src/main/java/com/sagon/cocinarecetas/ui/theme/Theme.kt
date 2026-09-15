package com.sagon.cocinarecetas.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PoolBlue,
    secondary = NatureGreen,
    tertiary = FoodRed,
    background = FoodDarkBackground,
    surface = FoodDarkSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    primaryContainer = PoolBlueDark.copy(alpha = 0.3f),
    secondaryContainer = NatureGreen.copy(alpha = 0.3f)
)

private val LightColorScheme = lightColorScheme(
    primary = PoolBlueDark,
    secondary = NatureGreen,
    tertiary = FoodRed,
    background = SoftBlueBackground, // Fondo azulado muy claro y limpio
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = FoodTextNavy,
    onSurface = FoodTextNavy,
    primaryContainer = PoolBlue.copy(alpha = 0.15f),
    secondaryContainer = NatureGreen.copy(alpha = 0.12f),
    surfaceVariant = Color.White
)

@Composable
fun CocinaREcetasTheme(
    darkTheme: Boolean = false, // Modo oscuro desactivado por completo
    // Desactivamos dynamicColor para que se use nuestra paleta apetecible
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}