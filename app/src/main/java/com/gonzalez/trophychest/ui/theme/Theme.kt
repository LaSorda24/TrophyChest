package com.gonzalez.trophychest.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = GoldTrophy,
    secondary = Color(0xFFF6C453),
    tertiary = Color(0xFFFFD166),
    background = Color(0xFF0F0F0F),
    surface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFF242424),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = ReadableSecondary,
    outline = VisibleOutline,
    outlineVariant = ReadableDisabled,
    inverseSurface = Color(0xFFE6E6E6),
    inverseOnSurface = Color(0xFF1A1A1A)
)

private val LightColorScheme = lightColorScheme(
    primary = GoldTrophy,
    secondary = Color(0xFFF6C453),
    tertiary = Color(0xFFFFD166),
    background = Color(0xFF0F0F0F),
    surface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFF242424),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = ReadableSecondary,
    outline = VisibleOutline,
    outlineVariant = ReadableDisabled
)

@Composable
fun TrophyChestTheme(
    darkTheme: Boolean = true,
    // DEJO EL COLOR DINAMICO APAGADO PARA QUE LA APP SIEMPRE TENGA EL MISMO ESTILO
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {//ELEGIMOS PALETA DE COLORES
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {//EN CASO DE COLOR DIAMMICO O ANDROID 12`+
            val context = LocalContext.current//CALCULA COLORES DINAMICOS
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(//APLICA MATERIAL 3 AL CONTENIDO
        colorScheme = colorScheme,
        typography = Typography,//APLICA TIPOGRAFIA
        content = content//MUESTRA CONTENIDO
    )
}
