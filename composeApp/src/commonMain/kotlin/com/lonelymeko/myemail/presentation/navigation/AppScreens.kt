package com.lonelymeko.myemail.presentation.navigation


import androidx.compose.runtime.Composable // 确保导入 @Composable
import cafe.adriel.voyager.core.screen.Screen
import com.lonelymeko.myemail.data.model.AccountInfo // 如果 AddAccountScreenRoute 需要它
import com.lonelymeko.myemail.presentation.feature.add_account.AddAccountScreen // <--- 导入 AddAccountScreen
import com.lonelymeko.myemail.presentation.feature.bottom_nav.MainScreen // <--- 导入 MainScreen

// AddAccountScreenRoute 之前是 data class AddAccountScreenRoute(...): Screen
// 为了与 Voyager 的 Screen 定义一致，并且如果 AddAccountScreen 本身是 Screen 实现，
// 我们可以直接使用 AddAccountScreen 作为路由，或者保持 AddAccountScreenRoute 并实现 Content。
// 为了简单，我们假设 AddAccountScreen 本身就是 Screen 的实现，可以直接 push。
// 如果 AddAccountScreenRoute 是用来传递参数给 AddAccountScreen 的，那么 AddAccountScreenRoute 也需要实现 Content。

// 让我们先修正 MainContentScreenRoute
object MainContentScreenRoute : Screen { // 主内容屏幕，会包含邮件列表等
    @Composable // 必须用 @Composable 注解
    override fun Content() { // 必须用 override 实现接口方法
        MainScreen.Content() // 调用 MainScreen 的 Composable 内容
        // 或者，如果 MainScreen 本身就是 Screen 的实现：
        // MainScreen.Content() // 这样也可以，如果 MainScreen 是 object : Screen
        // 但通常我们让 Route 对象调用另一个顶层的 Composable 函数
        // 或者，更常见的做法是，如果 MainScreen 也是一个实现了 Screen 接口的 object:
        // com.example.myemail.presentation.feature.bottom_nav.MainScreen.Content()
        // 但既然 MainScreen 也是 object : Screen，可以直接调用它，
        // 或者直接在 App.kt 中使用 MainScreen 作为初始屏幕。

        // 为了清晰，我们让这个 Route 对象调用 MainScreen 这个 Composable
        // 假设 MainScreen 的 Content 是这样定义的：
        // package com.example.myemail.presentation.feature.bottom_nav
        // @Composable
        // fun MainScreenContent() { /* ... */ }
        // MainScreen.Content() // 调用 MainScreen 的 @Composable Content() 方法
        // 我们之前将 MainScreen 定义为 object MainScreen : Screen，所以它有自己的 Content()
        // 因此，这里调用 MainScreen.Content() 是正确的
//        com.lonelymeko.myemail.presentation.feature.bottom_nav.MainScreen.Content()
    }
}

// 对于 AddAccountScreenRoute，如果它只是一个简单的路由标识，并且 AddAccountScreen 本身是 Screen 实现，
// 那么在导航时可以直接 navigator.push(AddAccountScreen(initialEmail = ...))
// 如果 AddAccountScreenRoute 也需要作为 Screen 实例被导航器管理，它也需要实现 Content()

// 假设 AddAccountScreen 也是一个实现了 Screen 接口的 data class：
// (之前 AddAccountScreen 是 data class AddAccountScreen(...) : Screen)
// 那么 AddAccountScreenRoute 可能就不再需要了，或者它也需要实现 Content()
// 如果 AddAccountScreenRoute 是为了参数化 AddAccountScreen，可以这样做：
data class AddAccountScreenRoute(val initialEmail: String? = null) : Screen {
    @Composable
    override fun Content() {
        AddAccountScreen(initialEmail = initialEmail).Content() // 委托给 AddAccountScreen
    }
}