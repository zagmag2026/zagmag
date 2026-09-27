package com.nimsdeveloper.zhagmagdresses.admin.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ZhagmagColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = AppSurface,
    primaryContainer = BrandSoft,
    onPrimaryContainer = BrandPrimary,
    secondary = BrandSecondary,
    onSecondary = AppSurface,
    background = AppBackground,
    onBackground = AppText,
    surface = AppSurface,
    onSurface = AppText,
    surfaceVariant = BrandSoft,
    onSurfaceVariant = AppTextMuted,
    outline = AppBorder,
    error = StatusError,
    onError = AppSurface,
    errorContainer = StatusErrorSoft,
    onErrorContainer = StatusError
)

@Composable
fun ZhagmagAdminTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ZhagmagColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
