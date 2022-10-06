package com.ojhdtapp.parabox.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.emptyPreferences
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

object Theme {
    const val DEFAULT = 0
    const val SAKURA = 1
    const val GARDENIA = 2
    const val WATER = 3
}

private val LightThemeColors = lightColorScheme(

    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    errorContainer = md_theme_light_errorContainer,
    onError = md_theme_light_onError,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inverseSurface = md_theme_light_inverseSurface,
    inversePrimary = md_theme_light_inversePrimary,
//	shadow = md_theme_light_shadow,
)
private val DarkThemeColors = darkColorScheme(

    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    errorContainer = md_theme_dark_errorContainer,
    onError = md_theme_dark_onError,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inverseSurface = md_theme_dark_inverseSurface,
    inversePrimary = md_theme_dark_inversePrimary,
//	shadow = md_theme_dark_shadow,
)

private val SakuraLightThemeColors = lightColorScheme(
    primary = sakura_theme_light_primary,
    onPrimary = sakura_theme_light_onPrimary,
    primaryContainer = sakura_theme_light_primaryContainer,
    onPrimaryContainer = sakura_theme_light_onPrimaryContainer,
    secondary = sakura_theme_light_secondary,
    onSecondary = sakura_theme_light_onSecondary,
    secondaryContainer = sakura_theme_light_secondaryContainer,
    onSecondaryContainer = sakura_theme_light_onSecondaryContainer,
    tertiary = sakura_theme_light_tertiary,
    onTertiary = sakura_theme_light_onTertiary,
    tertiaryContainer = sakura_theme_light_tertiaryContainer,
    onTertiaryContainer = sakura_theme_light_onTertiaryContainer,
    error = sakura_theme_light_error,
    errorContainer = sakura_theme_light_errorContainer,
    onError = sakura_theme_light_onError,
    onErrorContainer = sakura_theme_light_onErrorContainer,
    background = sakura_theme_light_background,
    onBackground = sakura_theme_light_onBackground,
    surface = sakura_theme_light_surface,
    onSurface = sakura_theme_light_onSurface,
    surfaceVariant = sakura_theme_light_surfaceVariant,
    onSurfaceVariant = sakura_theme_light_onSurfaceVariant,
    outline = sakura_theme_light_outline,
    inverseOnSurface = sakura_theme_light_inverseOnSurface,
    inverseSurface = sakura_theme_light_inverseSurface,
    inversePrimary = sakura_theme_light_inversePrimary,
)
private val SakuraDarkThemeColors = darkColorScheme(
    primary = sakura_theme_dark_primary,
    onPrimary = sakura_theme_dark_onPrimary,
    primaryContainer = sakura_theme_dark_primaryContainer,
    onPrimaryContainer = sakura_theme_dark_onPrimaryContainer,
    secondary = sakura_theme_dark_secondary,
    onSecondary = sakura_theme_dark_onSecondary,
    secondaryContainer = sakura_theme_dark_secondaryContainer,
    onSecondaryContainer = sakura_theme_dark_onSecondaryContainer,
    tertiary = sakura_theme_dark_tertiary,
    onTertiary = sakura_theme_dark_onTertiary,
    tertiaryContainer = sakura_theme_dark_tertiaryContainer,
    onTertiaryContainer = sakura_theme_dark_onTertiaryContainer,
    error = sakura_theme_dark_error,
    errorContainer = sakura_theme_dark_errorContainer,
    onError = sakura_theme_dark_onError,
    onErrorContainer = sakura_theme_dark_onErrorContainer,
    background = sakura_theme_dark_background,
    onBackground = sakura_theme_dark_onBackground,
    surface = sakura_theme_dark_surface,
    onSurface = sakura_theme_dark_onSurface,
    surfaceVariant = sakura_theme_dark_surfaceVariant,
    onSurfaceVariant = sakura_theme_dark_onSurfaceVariant,
    outline = sakura_theme_dark_outline,
    inverseOnSurface = sakura_theme_dark_inverseOnSurface,
    inverseSurface = sakura_theme_dark_inverseSurface,
    inversePrimary = sakura_theme_dark_inversePrimary,
)
private val GardeniaLightThemeColors = lightColorScheme(
    primary = gardenia_theme_light_primary,
    onPrimary = gardenia_theme_light_onPrimary,
    primaryContainer = gardenia_theme_light_primaryContainer,
    onPrimaryContainer = gardenia_theme_light_onPrimaryContainer,
    secondary = gardenia_theme_light_secondary,
    onSecondary = gardenia_theme_light_onSecondary,
    secondaryContainer = gardenia_theme_light_secondaryContainer,
    onSecondaryContainer = gardenia_theme_light_onSecondaryContainer,
    tertiary = gardenia_theme_light_tertiary,
    onTertiary = gardenia_theme_light_onTertiary,
    tertiaryContainer = gardenia_theme_light_tertiaryContainer,
    onTertiaryContainer = gardenia_theme_light_onTertiaryContainer,
    error = gardenia_theme_light_error,
    errorContainer = gardenia_theme_light_errorContainer,
    onError = gardenia_theme_light_onError,
    onErrorContainer = gardenia_theme_light_onErrorContainer,
    background = gardenia_theme_light_background,
    onBackground = gardenia_theme_light_onBackground,
    surface = gardenia_theme_light_surface,
    onSurface = gardenia_theme_light_onSurface,
    surfaceVariant = gardenia_theme_light_surfaceVariant,
    onSurfaceVariant = gardenia_theme_light_onSurfaceVariant,
    outline = gardenia_theme_light_outline,
    inverseOnSurface = gardenia_theme_light_inverseOnSurface,
    inverseSurface = gardenia_theme_light_inverseSurface,
    inversePrimary = gardenia_theme_light_inversePrimary,
)
private val GardeniaDarkThemeColors = darkColorScheme(
    primary = gardenia_theme_dark_primary,
    onPrimary = gardenia_theme_dark_onPrimary,
    primaryContainer = gardenia_theme_dark_primaryContainer,
    onPrimaryContainer = gardenia_theme_dark_onPrimaryContainer,
    secondary = gardenia_theme_dark_secondary,
    onSecondary = gardenia_theme_dark_onSecondary,
    secondaryContainer = gardenia_theme_dark_secondaryContainer,
    onSecondaryContainer = gardenia_theme_dark_onSecondaryContainer,
    tertiary = gardenia_theme_dark_tertiary,
    onTertiary = gardenia_theme_dark_onTertiary,
    tertiaryContainer = gardenia_theme_dark_tertiaryContainer,
    onTertiaryContainer = gardenia_theme_dark_onTertiaryContainer,
    error = gardenia_theme_dark_error,
    errorContainer = gardenia_theme_dark_errorContainer,
    onError = gardenia_theme_dark_onError,
    onErrorContainer = gardenia_theme_dark_onErrorContainer,
    background = gardenia_theme_dark_background,
    onBackground = gardenia_theme_dark_onBackground,
    surface = gardenia_theme_dark_surface,
    onSurface = gardenia_theme_dark_onSurface,
    surfaceVariant = gardenia_theme_dark_surfaceVariant,
    onSurfaceVariant = gardenia_theme_dark_onSurfaceVariant,
    outline = gardenia_theme_dark_outline,
    inverseOnSurface = gardenia_theme_dark_inverseOnSurface,
    inverseSurface = gardenia_theme_dark_inverseSurface,
    inversePrimary = gardenia_theme_dark_inversePrimary,
)
private val WaterLightThemeColors = lightColorScheme(
    primary = water_theme_light_primary,
    onPrimary = water_theme_light_onPrimary,
    primaryContainer = water_theme_light_primaryContainer,
    onPrimaryContainer = water_theme_light_onPrimaryContainer,
    secondary = water_theme_light_secondary,
    onSecondary = water_theme_light_onSecondary,
    secondaryContainer = water_theme_light_secondaryContainer,
    onSecondaryContainer = water_theme_light_onSecondaryContainer,
    tertiary = water_theme_light_tertiary,
    onTertiary = water_theme_light_onTertiary,
    tertiaryContainer = water_theme_light_tertiaryContainer,
    onTertiaryContainer = water_theme_light_onTertiaryContainer,
    error = water_theme_light_error,
    errorContainer = water_theme_light_errorContainer,
    onError = water_theme_light_onError,
    onErrorContainer = water_theme_light_onErrorContainer,
    background = water_theme_light_background,
    onBackground = water_theme_light_onBackground,
    surface = water_theme_light_surface,
    onSurface = water_theme_light_onSurface,
    surfaceVariant = water_theme_light_surfaceVariant,
    onSurfaceVariant = water_theme_light_onSurfaceVariant,
    outline = water_theme_light_outline,
    inverseOnSurface = water_theme_light_inverseOnSurface,
    inverseSurface = water_theme_light_inverseSurface,
    inversePrimary = water_theme_light_inversePrimary,
)
private val WaterDarkThemeColors = darkColorScheme(
    primary = water_theme_dark_primary,
    onPrimary = water_theme_dark_onPrimary,
    primaryContainer = water_theme_dark_primaryContainer,
    onPrimaryContainer = water_theme_dark_onPrimaryContainer,
    secondary = water_theme_dark_secondary,
    onSecondary = water_theme_dark_onSecondary,
    secondaryContainer = water_theme_dark_secondaryContainer,
    onSecondaryContainer = water_theme_dark_onSecondaryContainer,
    tertiary = water_theme_dark_tertiary,
    onTertiary = water_theme_dark_onTertiary,
    tertiaryContainer = water_theme_dark_tertiaryContainer,
    onTertiaryContainer = water_theme_dark_onTertiaryContainer,
    error = water_theme_dark_error,
    errorContainer = water_theme_dark_errorContainer,
    onError = water_theme_dark_onError,
    onErrorContainer = water_theme_dark_onErrorContainer,
    background = water_theme_dark_background,
    onBackground = water_theme_dark_onBackground,
    surface = water_theme_dark_surface,
    onSurface = water_theme_dark_onSurface,
    surfaceVariant = water_theme_dark_surfaceVariant,
    onSurfaceVariant = water_theme_dark_onSurfaceVariant,
    outline = water_theme_dark_outline,
    inverseOnSurface = water_theme_dark_inverseOnSurface,
    inverseSurface = water_theme_dark_inverseSurface,
    inversePrimary = water_theme_dark_inversePrimary,
)

@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable() () -> Unit
) {
    val context = LocalContext.current
    val enableDynamicColorFlow = remember {
        context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { settings ->
                settings[DataStoreKeys.SETTINGS_ENABLE_DYNAMIC_COLOR]
                    ?: (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            }
    }
    val enableDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            enableDynamicColorFlow.collectAsState(initial = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S).value
    val themeFlow = remember {
        context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { settings ->
                settings[DataStoreKeys.SETTINGS_THEME]
                    ?: Theme.DEFAULT
            }
    }
    val theme = themeFlow.collectAsState(initial = Theme.DEFAULT).value
    val colors = when {
        enableDynamicColor && !useDarkTheme -> dynamicLightColorScheme(LocalContext.current)
        enableDynamicColor && useDarkTheme -> dynamicDarkColorScheme(LocalContext.current)
        useDarkTheme -> when (theme) {
            Theme.SAKURA -> SakuraDarkThemeColors
            Theme.GARDENIA -> GardeniaDarkThemeColors
            Theme.WATER -> WaterDarkThemeColors
            else -> DarkThemeColors
        }

        else -> when (theme) {
            Theme.SAKURA -> SakuraLightThemeColors
            Theme.GARDENIA -> GardeniaLightThemeColors
            Theme.WATER -> WaterLightThemeColors
            else -> LightThemeColors
        }
    }

    CompositionLocalProvider(LocalFontSize provides FontSize()) {
        androidx.compose.material3.MaterialTheme(
            colorScheme = colors,
            typography = AppTypography,
            content = content
        )
    }
}