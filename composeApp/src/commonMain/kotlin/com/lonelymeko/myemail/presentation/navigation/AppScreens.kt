package com.lonelymeko.myemail.presentation.navigation

import cafe.adriel.voyager.core.screen.Screen
import com.lonelymeko.myemail.data.model.AccountInfo // 如果需要传递 AccountInfo

// 为了简单起见，我们先不让 Screen 实现 Parcelable，但对于复杂参数传递，这是个好选择
// 如果 Screen 需要参数，可以定义为 data object 或 data class

data class AddAccountScreenRoute(val initialEmail: String? = null) : Screen
object MainContentScreenRoute : Screen // 主内容屏幕，会包含邮件列表等


// 如果有单独的账户管理列表屏幕
// object AccountManagementScreenRoute : Screen