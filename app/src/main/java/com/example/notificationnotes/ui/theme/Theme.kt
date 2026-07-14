package com.example.notificationnotes.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = White,
    primaryContainer = PrimaryBlueLight,
    onPrimaryContainer = PrimaryBlueDark,
    secondary = DarkGray,
    onSecondary = White,
    secondaryContainer = MediumGray,
    onSecondaryContainer = NearBlack,
    background = White,
    onBackground = NearBlack,
    surface = OffWhite,
    onSurface = NearBlack,
    surfaceVariant = LightGray,
    onSurfaceVariant = DarkGray,
    error = ErrorRed,
    onError = White,
    outline = MediumGray
)

@Composable
fun NotificationNotesTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = AppTypography,
        content = content
    )
}
