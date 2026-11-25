package org.bharatai.ps5.metalinspection.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.luminance
import androidx.compose.runtime.SideEffect

private val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE7FF),
    onPrimaryContainer = Color(0xFF142E64),
    secondary = Teal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD8F1ED),
    onSecondaryContainer = Color(0xFF123F3A),
    tertiary = Amber,
    onTertiary = Color.White,
    background = LightCanvas,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline,
    error = Color(0xFFB42332),
)

private val DarkColors = darkColorScheme(
    primary = CoolBlue,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF1E3A5F),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = ElectricCyan,
    onSecondary = Color(0xFF003833),
    secondaryContainer = Color(0xFF004D47),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = HazardAmber,
    onTertiary = Color(0xFF451A03),
    background = GraphiteSlate,
    onBackground = DarkText,
    surface = SlateSurface,
    onSurface = DarkText,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = PrecisionBorder,
    error = LaserCrimson,
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun MetalInspectionAssistantTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val context = LocalContext.current
    SideEffect {
        val activity = context as? ComponentActivity ?: return@SideEffect
        activity.window.statusBarColor = colors.background.toArgb()
        activity.window.navigationBarColor = colors.background.toArgb()
        WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = colors.background.luminance() > 0.5f
            isAppearanceLightNavigationBars = colors.background.luminance() > 0.5f
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        shapes = AppShapes,
        content = content,
    )
}
