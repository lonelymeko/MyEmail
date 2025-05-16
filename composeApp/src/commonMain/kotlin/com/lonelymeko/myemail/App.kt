package com.lonelymeko.myemail



import androidx.compose.material3.ExperimentalMaterial3Api // 如果使用了实验性的 M3 API
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import com.lonelymeko.myemail.presentation.navigation.MainContentScreenRoute // 我们之前定义的初始屏幕路由
import com.lonelymeko.myemail.presentation.theme.AppTheme // 你定义的主题
import org.koin.compose.KoinContext // Koin 的 Compose 集成

@OptIn(ExperimentalMaterial3Api::class) // 如果你的主题或内部使用了实验性API
@Composable
fun App() {
    // 1. 应用自定义的 Material 3 主题
    // AppTheme 通常在 presentation/theme/Theme.kt 中定义
    AppTheme {
        // 2. 提供 Koin 上下文，使得 @Composable 函数中可以使用 koinInject() 或 getScreenModel()
        // 对于 Voyager，getScreenModel() 通常会自动查找 Koin 实例，
        // 但显式提供 KoinContext 是一个好习惯，确保 Koin 在整个 Composable 树中可用。
        KoinContext {
            // 3. 设置 Voyager 的根导航器
            // Navigator 是所有屏幕导航的核心容器。
            Navigator(
                screen = MainContentScreenRoute, // 指定应用的初始屏幕
                // MainContentScreenRoute 是我们定义的 Screen 对象或类
                // 它会渲染 MainScreen

                // 可选：自定义 Navigator 的行为
                // key = "RootNavigator", // 可以给 Navigator 一个 key，但通常不需要
                // disposeBehavior = NavigatorDisposeBehavior(disposeNestedNavigators = false, disposeSteps = true), // 控制 Screen 和 ScreenModel 的销毁行为
                // onBackPressed = { currentScreen -> ... ; true } // 自定义返回按钮行为
            ) { navigator ->
                // CurrentScreen 是一个 Composable，它会渲染当前导航栈顶部的 Screen 的 Content() 方法。
                // navigator 对象可以在 Screen 内部通过 LocalNavigator.currentOrThrow 获取，
                // 用于执行 push, pop, replace 等导航操作。
                CurrentScreen()

                // 或者，你可以在这里包裹一个全局的 Scaffold (如果所有屏幕都需要相同的外部结构)
                // 但通常每个主要的 Screen (如 MainScreen) 会管理自己的 Scaffold。
                //例如：
                // Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                //    CurrentScreen()
                // }
            }
        }
    }
}