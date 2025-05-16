package com.lonelymeko.myemail.presentation.theme


import androidx.compose.ui.graphics.Color

// --- 为亮色主题定义颜色 ---
val SkyBlue_Primary_Light = Color(0xFF03A9F4)    // 一个不错的天蓝色 (Material Blue 500)
val SkyBlue_OnPrimary_Light = Color.White       // 在主色上显示的文本/图标颜色
val SkyBlue_PrimaryContainer_Light = Color(0xFFB3E5FC) // 主色的容器色，更浅更柔和
val SkyBlue_OnPrimaryContainer_Light = Color(0xFF014361) // 主色容器上的文本/图标颜色

val SkyBlue_Secondary_Light = Color(0xFF0288D1)  // 次要颜色，可选一个稍深或不同的蓝色
val SkyBlue_OnSecondary_Light = Color.White
val SkyBlue_SecondaryContainer_Light = Color(0xFF81D4FA)
val SkyBlue_OnSecondaryContainer_Light = Color(0xFF003549)

// 背景和表面颜色 (以白色为主)
val SkyBlue_Background_Light = Color.White
val SkyBlue_OnBackground_Light = Color(0xFF1A1C1E) // 深灰色文本，用于白色背景
val SkyBlue_Surface_Light = Color.White          // 卡片、对话框等的表面颜色
val SkyBlue_OnSurface_Light = Color(0xFF1A1C1E)   // 表面上的文本/图标颜色
val SkyBlue_SurfaceVariant_Light = Color(0xFFE0E2EC) // 表面的变体色，例如输入框边框
val SkyBlue_OnSurfaceVariant_Light = Color(0xFF44474F)

// 错误颜色
val SkyBlue_Error_Light = Color(0xFFBA1A1A)
val SkyBlue_OnError_Light = Color.White
val SkyBlue_ErrorContainer_Light = Color(0xFFFFDAD6)
val SkyBlue_OnErrorContainer_Light = Color(0xFF410002)

val SkyBlue_Outline_Light = Color(0xFF74777F) // 边框颜色

// --- 为暗色主题定义颜色 (简单示例，实际应更细致) ---
// 对于暗色主题，通常背景变暗，主色可能需要调整亮度以保持对比度
val SkyBlue_Primary_Dark = Color(0xFF81D4FA)      // 亮色背景上的天蓝色可能需要更亮
val SkyBlue_OnPrimary_Dark = Color(0xFF003549)
val SkyBlue_PrimaryContainer_Dark = Color(0xFF004D6E)
val SkyBlue_OnPrimaryContainer_Dark = Color(0xFFCDE5FF)

// 其他暗色系颜色... (为简洁起见，这里省略，实际应完整定义)
val SkyBlue_Background_Dark = Color(0xFF121212) // 常见的深色背景
val SkyBlue_OnBackground_Dark = Color(0xFFE0E0E0) // 浅色文本
val SkyBlue_Surface_Dark = Color(0xFF1E1E1E)      // 深色表面
val SkyBlue_OnSurface_Dark = Color(0xFFE0E0E0)
// ...等等...