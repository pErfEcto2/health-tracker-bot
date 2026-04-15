package com.trackhub.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val Light = lightColorScheme(
    primary = Brand,
    background = BgLight,
    surface = BgLight,
    surfaceVariant = SecondaryBgLight,
    onBackground = TextLight,
    onSurface = TextLight,
    error = Destructive,
)

private val Dark = darkColorScheme(
    primary = BrandDark,
    background = BgDark,
    surface = BgDark,
    surfaceVariant = SecondaryBgDark,
    onBackground = TextDark,
    onSurface = TextDark,
    error = Destructive,
)

@Composable
fun TrackHubTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
