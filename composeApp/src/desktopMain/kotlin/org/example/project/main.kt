package org.example.project

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.lonelymeko.myemail.App
// MyEmail/composeApp/src/desktopMain/kotlin/com/lonelymeko/myemail/Main.kt (或 org.example.project.main.kt)
import androidx.compose.ui.window.application
import androidx.compose.ui.window.Window
import com.lonelymeko.myemail.App // commonMain 的 App Composable
import com.lonelymeko.myemail.di.desktopModule // Desktop 特定的 Koin 模块
import com.lonelymeko.myemail.di.initKoin // commonMain 的 Koin 初始化函数

fun main() { // <--- 注意这里
    initKoin( // <--- Koin 初始化应该在 application { ... } 块之外，或者在它内部的最开始
        platformSpecificModules = listOf(desktopModule)
    )
    // com.lonelymeko.myemail.common.logging.initNapier() // 如果使用日志

    application { // <--- Compose Desktop 的 application 构建器
        Window(
            onCloseRequest = ::exitApplication,
            title = "MyEmail Desktop Client"
        ) {
            App() // <--- 在这里 App() 及其内部的 KoinContext 被调用
        }
    }
}