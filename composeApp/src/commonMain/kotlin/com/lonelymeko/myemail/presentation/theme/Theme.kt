package com.lonelymeko.myemail.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme // 如果要支持暗色主题
import androidx.compose.runtime.Composable

// 定义亮色主题的颜色方案
private val LightColors = lightColorScheme(
    primary = SkyBlue_Primary_Light,
    onPrimary = SkyBlue_OnPrimary_Light,
    primaryContainer = SkyBlue_PrimaryContainer_Light,
    onPrimaryContainer = SkyBlue_OnPrimaryContainer_Light,
    secondary = SkyBlue_Secondary_Light,
    onSecondary = SkyBlue_OnSecondary_Light,
    secondaryContainer = SkyBlue_SecondaryContainer_Light,
    onSecondaryContainer = SkyBlue_OnSecondaryContainer_Light,
    // tertiary 和 onTertiary 可以根据需要定义，否则会使用默认值
    background = SkyBlue_Background_Light,
    onBackground = SkyBlue_OnBackground_Light,
    surface = SkyBlue_Surface_Light,
    onSurface = SkyBlue_OnSurface_Light,
    surfaceVariant = SkyBlue_SurfaceVariant_Light,
    onSurfaceVariant = SkyBlue_OnSurfaceVariant_Light,
    error = SkyBlue_Error_Light,
    onError = SkyBlue_OnError_Light,
    errorContainer = SkyBlue_ErrorContainer_Light,
    onErrorContainer = SkyBlue_OnErrorContainer_Light,
    outline = SkyBlue_Outline_Light,
    // 你可以继续定义其他颜色角色，如 tertiary, inversePrimary, surfaceTint 等
    surfaceTint = SkyBlue_Primary_Light // surfaceTint 通常与 primary 相同或相似
)

// 定义暗色主题的颜色方案 (简单示例)
private val DarkColors = darkColorScheme(
    primary = SkyBlue_Primary_Dark,
    onPrimary = SkyBlue_OnPrimary_Dark,
    primaryContainer = SkyBlue_PrimaryContainer_Dark,
    onPrimaryContainer = SkyBlue_OnPrimaryContainer_Dark,
    // ... 为其他暗色主题颜色角色赋值 ...
    background = SkyBlue_Background_Dark,
    onBackground = SkyBlue_OnBackground_Dark,
    surface = SkyBlue_Surface_Dark,
    onSurface = SkyBlue_OnSurface_Dark,
    error = SkyBlue_Error_Light, // 暗色主题的错误色可能也需要调整
    onError = SkyBlue_OnError_Light,
    surfaceTint = SkyBlue_Primary_Dark
)

@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(), // 跟随系统设置
    content: @Composable () -> Unit
) {
    val colors = if (useDarkTheme) {
        DarkColors // 使用暗色方案
    } else {
        LightColors // 使用亮色方案
    }

    MaterialTheme(
        colorScheme = colors,
        // typography = Typography, // 如果你定义了 Typography.kt
        // shapes = Shapes,       // 如果你定义了 Shape.kt
        content = content
    )
}