package com.mal5odha.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.mal5odha.core.data.preferences.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryCyanBlue,
    onPrimary = PureWhite,
    primaryContainer = Color(0xFF004D61),
    onPrimaryContainer = Color(0xFFBCE9F5),
    secondary = Color(0xFF4FC3F7),
    onSecondary = Color(0xFF003544),
    secondaryContainer = Color(0xFF004D61),
    onSecondaryContainer = Color(0xFFBCE9F5),
    tertiary = SecondaryGray,
    onTertiary = PureWhite,
    background = Color(0xFF121212),
    onBackground = PureWhite,
    surface = Color(0xFF1E1E1E),
    onSurface = PureWhite,
    surfaceVariant = Color(0xFF2C2D30),
    onSurfaceVariant = Color(0xFFCCCCCC),
    outline = Color(0xFF44474E),
    outlineVariant = Color(0xFF2E3035),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryCyanBlue,
    onPrimary = PureWhite,
    primaryContainer = Color(0xFFBCE9F5),
    onPrimaryContainer = Color(0xFF001F28),
    secondary = ActionDarkBlue,
    onSecondary = PureWhite,
    secondaryContainer = Color(0xFFD6F0F8),
    onSecondaryContainer = Color(0xFF001F28),
    tertiary = SecondaryGray,
    onTertiary = PureWhite,
    background = PureWhite,
    onBackground = ActionDarkBlue,
    surface = PureWhite,
    onSurface = ActionDarkBlue,
    surfaceVariant = Color(0xFFF0F2F5),
    onSurfaceVariant = Color(0xFF4D4D4D),
    outline = Color(0xFFD0D0D0),
    outlineVariant = Color(0xFFE5E5E5),
    error = Color(0xFFBA1A1A),
    onError = PureWhite
)

@Composable
fun Mal5odhaTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    darkTheme: Boolean = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    },
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
