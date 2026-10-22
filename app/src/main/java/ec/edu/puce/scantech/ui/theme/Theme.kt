package ec.edu.puce.scantech.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Static Dark Color Scheme for ScanTech LTIC.
 * High contrast with dark surfaces for battery efficiency in warehouse environments.
 */
private val DarkColorScheme = darkColorScheme(
    primary = ScanTechDarkPrimary,
    onPrimary = ScanTechDarkOnPrimary,
    primaryContainer = ScanTechDarkPrimaryContainer,
    onPrimaryContainer = ScanTechDarkOnPrimaryContainer,
    secondary = ScanTechDarkSecondary,
    onSecondary = ScanTechDarkOnSecondary,
    secondaryContainer = ScanTechDarkSecondaryContainer,
    onSecondaryContainer = ScanTechDarkOnSecondaryContainer,
    tertiary = ScanTechDarkTertiary,
    onTertiary = ScanTechDarkOnTertiary,
    tertiaryContainer = ScanTechDarkTertiaryContainer,
    onTertiaryContainer = ScanTechDarkOnTertiaryContainer,
    background = ScanTechDarkBackground,
    onBackground = ScanTechDarkOnBackground,
    surface = ScanTechDarkSurface,
    onSurface = ScanTechDarkOnSurface,
    surfaceVariant = ScanTechDarkSurfaceVariant,
    onSurfaceVariant = ScanTechDarkOnSurfaceVariant,
    error = ScanTechDarkError,
    onError = ScanTechDarkOnError,
    errorContainer = ScanTechDarkErrorContainer,
    onErrorContainer = ScanTechDarkOnErrorContainer
)

/**
 * Static Light Color Scheme for ScanTech LTIC.
 */
private val LightColorScheme = lightColorScheme(
    primary = ScanTechPrimary,
    onPrimary = ScanTechOnPrimary,
    primaryContainer = ScanTechPrimaryContainer,
    onPrimaryContainer = ScanTechOnPrimaryContainer,
    secondary = ScanTechSecondary,
    onSecondary = ScanTechOnSecondary,
    secondaryContainer = ScanTechSecondaryContainer,
    onSecondaryContainer = ScanTechOnSecondaryContainer,
    tertiary = ScanTechTertiary,
    onTertiary = ScanTechOnTertiary,
    tertiaryContainer = ScanTechTertiaryContainer,
    onTertiaryContainer = ScanTechOnTertiaryContainer,
    background = ScanTechLightBackground,
    onBackground = ScanTechLightOnBackground,
    surface = ScanTechLightSurface,
    onSurface = ScanTechLightOnSurface,
    surfaceVariant = ScanTechLightSurfaceVariant,
    onSurfaceVariant = ScanTechLightOnSurfaceVariant,
    error = ScanTechError,
    onError = ScanTechOnError,
    errorContainer = ScanTechErrorContainer,
    onErrorContainer = ScanTechOnErrorContainer
)

/**
 * ScanTechTheme Composable.
 * Implements Material You dynamic colors on supported devices (API 31+)
 * with automatic fallback to ScanTech brand palettes.
 */
@Composable
fun ScanTechTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
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
