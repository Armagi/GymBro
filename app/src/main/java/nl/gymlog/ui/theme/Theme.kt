package nl.gymlog.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    background = Background,
    surface = Surface,
    primary = AccentCalories,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = Border
)

@Composable
fun GymLogTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
