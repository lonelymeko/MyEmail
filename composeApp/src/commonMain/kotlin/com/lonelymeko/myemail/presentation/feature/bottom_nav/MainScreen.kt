// Path: MyEmail/composeApp/src/commonMain/kotlin/com/lonelymeko/myemail/presentation/feature/bottom_nav/MainScreen.kt
package com.lonelymeko.myemail.presentation.feature.bottom_nav

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel // MainScreen 用这个
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator // 用于获取当前 Tab
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.lonelymeko.myemail.presentation.feature.add_account.AddAccountScreen
import com.lonelymeko.myemail.presentation.feature.compose_email.ComposeEmailScreen
import com.lonelymeko.myemail.presentation.feature.email_list.EmailListScreen
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf

// 保存对 EmailListScreenModel 实例的引用，以便刷新
// 注意：这种方式比较 hacky，更好的方式是通过事件总线或共享的刷新状态
private val currentEmailListScreenModel = mutableStateOf<Any?>(null)


object MainScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = getScreenModel<MainScreenModel>()

        val accounts by screenModel.accountsState.collectAsState()
        val activeAccount by screenModel.activeAccountState.collectAsState()

        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        LaunchedEffect(accounts, activeAccount, navigator.lastItemOrNull, drawerState.isOpen) {
            println("MainScreen LaunchedEffect: accounts.size=${accounts.size}, activeAccount=${activeAccount?.emailAddress}, lastItem=${navigator.lastItemOrNull}, drawerOpen=${drawerState.isOpen}")
            if (accounts.isEmpty() && navigator.lastItemOrNull !is AddAccountScreen) {
                if (navigator.size <= 1 || navigator.lastItem is MainScreen) {
                    println("MainScreen: No accounts, pushing AddAccountScreen.")
                    navigator.push(AddAccountScreen())
                }
            }
            // 自动选择账户的逻辑已移至 MainScreenModel 的 init 块
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(modifier = Modifier.fillMaxHeight().width(IntrinsicSize.Max)) { // 抽屉宽度自适应
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "邮箱账户",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    accounts.forEach { account ->
                        NavigationDrawerItem(
                            icon = { Icon(if (account.emailAddress == activeAccount?.emailAddress) Icons.Filled.Mail else Icons.Outlined.MailOutline, contentDescription = account.displayName) },
                            label = { Text(account.displayName ?: account.emailAddress, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            selected = account.emailAddress == activeAccount?.emailAddress,
                            onClick = {
                                println("MainScreen: Switching account to ${account.emailAddress}")
                                screenModel.onSwitchAccount(account)
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.AddCircleOutline, contentDescription = "添加账户") },
                        label = { Text("添加账户") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navigator.push(AddAccountScreen())
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        ) {
            TabNavigator(InboxTab) { tabNavigator ->
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(activeAccount?.displayName ?: "邮箱", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Filled.Menu, contentDescription = "打开侧边栏")
                                }
                            },
                            actions = {
                                if (activeAccount != null) {
                                    IconButton(onClick = {
                                        println("Refresh button clicked in MainScreen")
                                        // 尝试获取当前 Tab 的 ScreenModel 并调用刷新
                                        // 这是一个简化的示例，实际中可能需要更健壮的方式
                                        val currentTab = tabNavigator.current
                                        if (currentEmailListScreenModel.value is com.lonelymeko.myemail.presentation.feature.email_list.EmailListScreenModel) {
                                            val model = currentEmailListScreenModel.value as com.lonelymeko.myemail.presentation.feature.email_list.EmailListScreenModel
                                            // 检查 model 的 folderName 是否与当前 tab 匹配 (可选)
                                            // 例如: if ( (currentTab == InboxTab && model.uiState.folderName == "INBOX") || ... )
                                            println("MainScreen: Requesting refresh for folder: ${model.uiState.folderName}")
                                            model.refreshEmails()
                                        } else {
                                            println("MainScreen: Could not get current EmailListScreenModel to refresh.")
                                        }
                                    }) {
                                        Icon(Icons.Filled.Refresh, contentDescription = "刷新")
                                    }
                                }
                            }
                        )
                    },
                    content = { paddingValues ->
                        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                            if (accounts.isEmpty() && navigator.lastItemOrNull !is AddAccountScreen) {
                                Column( /* ... Placeholder for no accounts ... */ ) { Text("请添加账户")}
                            } else if (activeAccount == null && accounts.isNotEmpty()) {
                                Column( /* ... Placeholder for no active account ... */ ) { Text("请选择账户") }
                            } else if (activeAccount != null) {
                                CurrentTab() // Voyager 会渲染当前选中 Tab 的 Content
                            } else {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    },
                    bottomBar = {
                        NavigationBar {
                            BottomNavItem(tabNavigator, InboxTab, Icons.Filled.Inbox)
                            BottomNavItem(tabNavigator, SentTab, Icons.AutoMirrored.Filled.Send)
                            // TODO: Add other tabs
                        }
                    },
                    floatingActionButton = {
                        activeAccount?.let {
                            FloatingActionButton(onClick = { navigator.push(ComposeEmailScreen()) }) {
                                Icon(Icons.Filled.Edit, contentDescription = "撰写新邮件")
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun RowScope.BottomNavItem(tabNavigator: TabNavigator, tab: Tab, iconVector: ImageVector) {
    val painter = rememberVectorPainter(image = iconVector)
    NavigationBarItem(
        selected = tabNavigator.current == tab,
        onClick = { tabNavigator.current = tab },
        icon = { Icon(painter = painter, contentDescription = tab.options.title) },
        label = { Text(tab.options.title) }
    )
}


// 定义 Tab (保持 object 定义以确保实例唯一性，方便比较)
internal object InboxTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = "收件箱"
            // val icon = rememberVectorPainter(Icons.Filled.Inbox) // icon 在 BottomNavItem 中处理
            return remember { TabOptions(index = 0u, title = title, icon = null) } // icon 设为 null，在 BottomNavItem 中提供
        }

    @Composable
    override fun Content() {
        val mainScreenModel = getScreenModel<MainScreenModel>() // 获取 MainScreenModel 以访问 activeAccount
        val activeAccount by mainScreenModel.activeAccountState.collectAsState()

        println("InboxTab Content: Composing. Active account: ${activeAccount?.emailAddress}")
        activeAccount?.let { acc ->
            // EmailListScreen 是一个独立的 Screen，Voyager TabNavigator 会处理它的 Content 调用
            // 我们需要确保当 acc 变化时，EmailListScreen 能获取到最新的 acc
            // 通过 remember(acc) { ... } 可以帮助在 acc 变化时重新创建 EmailListScreen 实例 (如果需要)
            // 或者，更好的方式是让 EmailListScreenModel 观察一个 activeAccount 的 Flow
            val emailListScreen = remember(acc) { // 当账户变化时，key 变化，会重新创建 EmailListScreen
                EmailListScreen(account = acc, folderName = "INBOX")
            }
            // 将 ScreenModel 实例传递给 MainScreen，以便刷新按钮可以调用它
            // 这是一种方式，但可能不是最优雅的
            val listScreenModel = emailListScreen.getScreenModel<com.lonelymeko.myemail.presentation.feature.email_list.EmailListScreenModel>(

                parameters = {parametersOf(acc, "INBOX")}
            )
            LaunchedEffect(listScreenModel){ // 当 ScreenModel 实例变化时更新
                currentEmailListScreenModel.value = listScreenModel
            }

            emailListScreen.Content() // 调用 EmailListScreen 的 Composable 内容
        } ?: CenteredText("请在侧边栏选择一个活动账户 (InboxTab)")
    }
}

internal object SentTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = "已发送"
            return remember { TabOptions(index = 1u, title = title, icon = null) }
        }

    @Composable
    override fun Content() {
        val mainScreenModel = getScreenModel<MainScreenModel>()
        val activeAccount by mainScreenModel.activeAccountState.collectAsState()
        println("SentTab Content: Composing. Active account: ${activeAccount?.emailAddress}")
        activeAccount?.let { acc ->
            val emailListScreen = remember(acc) {
                EmailListScreen(account = acc, folderName = "Sent")
            }
            val listScreenModel = emailListScreen.getScreenModel<com.lonelymeko.myemail.presentation.feature.email_list.EmailListScreenModel>(

                parameters = {parametersOf(acc, "Sent")}
            )
            LaunchedEffect(listScreenModel){
                currentEmailListScreenModel.value = listScreenModel
            }
            emailListScreen.Content()
        } ?: CenteredText("请在侧边栏选择一个活动账户 (SentTab)")
    }
}

@Composable
fun CenteredText(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text)
    }
}