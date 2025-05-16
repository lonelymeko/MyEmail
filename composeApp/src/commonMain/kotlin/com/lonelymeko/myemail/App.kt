package com.lonelymeko.myemail


import androidx.compose.material3.*
import androidx.compose.runtime.*
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import com.lonelymeko.myemail.presentation.feature.add_account.AddAccountScreen // 稍后创建
import com.lonelymeko.myemail.presentation.feature.bottom_nav.MainScreen // 稍后创建 (包含侧边栏和邮件列表)
import com.lonelymeko.myemail.presentation.navigation.AddAccountScreenRoute
import com.lonelymeko.myemail.presentation.navigation.MainContentScreenRoute
import com.lonelymeko.myemail.presentation.theme.AppTheme
import org.koin.compose.KoinContext // 如果需要在 App 级别获取 Koin 实例

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    AppTheme { // 应用你的 Material 3 主题
        KoinContext { // 确保 Koin 上下文在 Composable 树中可用
            // Navigator 是 Voyager 的核心，用于管理屏幕栈
            Navigator(
                screen = MainContentScreenRoute, // 初始屏幕是主内容界面
                // 可以定义 disposeBehavior 来控制 ScreenModel 的生命周期
            ) { navigator ->
                // CurrentScreen 会渲染当前导航栈顶的 Screen
                // 你可以在这里包装一个 Scaffold，或者让 MainScreen 自己处理 Scaffold
                CurrentScreen()
            }
        }
    }
}