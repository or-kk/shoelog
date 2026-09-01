package ai.orkk.shoelog.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = ShoeLime,
    onPrimary = ShoeDark,
    background = ShoeDark,
    surface = ShoeDarkSurface,
)

private val ColorPrimaryLight = androidx.compose.ui.graphics.Color(0xFF426800)

private val LightColors = lightColorScheme(
    primary = ColorPrimaryLight,
    onPrimary = ShoeLight,
    background = ShoeLight,
    surface = ShoeLightSurface,
)

@Composable
fun ShoeLogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = ShoeLogTypography,
        content = content,
    )
}
