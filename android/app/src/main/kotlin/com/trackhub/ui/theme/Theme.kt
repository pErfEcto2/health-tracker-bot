package com.trackhub.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Material3 mapping mirrors web:
//   background    = body bg     (--secondary-bg)
//   surface       = card bg     (--bg / --section-bg)
//   surfaceVariant= chip bg     (--secondary-bg, blends with body, pops on cards)
private val Light = lightColorScheme(
    primary = Brand,
    onPrimary = BgLight,
    background = SecondaryBgLight,
    onBackground = TextLight,
    surface = BgLight,
    onSurface = TextLight,
    surfaceVariant = SecondaryBgLight,
    onSurfaceVariant = TextLight,
    error = Destructive,
    onError = BgLight,
)

private val Dark = darkColorScheme(
    primary = BrandDark,
    onPrimary = BgLight,
    background = SecondaryBgDark,
    onBackground = TextDark,
    surface = BgDark,
    onSurface = TextDark,
    surfaceVariant = SecondaryBgDark,
    onSurfaceVariant = TextDark,
    error = DestructiveDark,
    onError = BgDark,
)

@Composable
fun TrackHubTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
