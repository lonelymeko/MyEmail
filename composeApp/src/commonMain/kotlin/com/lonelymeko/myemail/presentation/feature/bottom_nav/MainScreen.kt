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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.lonelymeko.myemail.data.model.AccountInfo
import com.lonelymeko.myemail.presentation.feature.add_account.AddAccountScreen
import com.lonelymeko.myemail.presentation.feature.compose_email.ComposeEmailScreen
import com.lonelymeko.myemail.presentation.feature.email_list.EmailListScreen
import kotlinx.coroutines.launch

object MainScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = getScreenModel<MainScreenModel>() // MainScreenModel 在 MainScreen 级别获取一次

        val accounts by screenModel.accountsState.collectAsState()
        val activeAccount by screenModel.activeAccountState.collectAsState() // 这个 activeAccount 将用于 UI 和传递

        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        // 当没有账户时，自动导航到添加账户界面
        // 这个 LaunchedEffect 会在 accounts 或 navigator.lastItem 变化时执行
        LaunchedEffect(accounts, navigator.lastItemOrNull) {
            println("MainScreen LaunchedEffect: accounts.size=${accounts.size}, activeAccount=${activeAccount?.emailAddress}, lastItem=${navigator.lastItemOrNull}")
            if (accounts.isNotEmpty() && activeAccount == null && !drawerState.isOpen && accounts.size == 1) {
                // 如果只有一个账户，并且没有活动的，自动设为活动 (可选逻辑)
                println("MainScreen: Auto-setting first account as active.")
                screenModel.onSwitchAccount(accounts.first())
            } else if (accounts.isEmpty() && navigator.lastItemOrNull !is AddAccountScreen) {
                // 避免在 AddAccountScreen 已经显示时再次 push
                // 更健壮的判断是检查当前导航栈是否只包含 MainScreen 或为空
                if (navigator.size <= 1 || navigator.lastItem is MainScreen) { //  MainScreen is the root or current
                    println("MainScreen: No accounts, pushing AddAccountScreen.")
                    navigator.push(AddAccountScreen())
                }
            }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(modifier = Modifier.fillMaxHeight()) { // 让抽屉内容可滚动
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "邮箱账户",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    accounts.forEach { account ->
                        NavigationDrawerItem(
                            icon = { Icon(if (account.emailAddress == activeAccount?.emailAddress) Icons.Filled.Mail else Icons.Outlined.MailOutline, contentDescription = account.displayName) },
                            label = { Text(account.displayName ?: account.emailAddress) },
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
            // TabNavigator 的初始 Tab 现在是稳定的 object
            TabNavigator(InboxTab) { tabNavigator -> // 默认打开 InboxTab
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(activeAccount?.displayName ?: "邮箱") },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Filled.Menu, contentDescription = "打开侧边栏")
                                }
                            },
                            actions = {
                                if (activeAccount != null) {
                                    IconButton(onClick = {
                                        // TODO: 实现刷新逻辑。需要一种方式通知当前 Tab 的 ScreenModel 刷新
                                        println("Refresh button clicked - TODO: Implement refresh for current tab")
                                    }) {
                                        Icon(Icons.Filled.Refresh, contentDescription = "刷新")
                                    }
                                }
                            }
                        )
                    },
                    content = { paddingValues ->
                        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                            println("MainScreen Content Box: Rendering content area. activeAccount=${activeAccount?.emailAddress}")
                            if (accounts.isEmpty() && navigator.lastItemOrNull !is AddAccountScreen) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("请添加一个邮箱账户开始使用")
                                    Spacer(Modifier.height(16.dp))
                                    Button(onClick = { navigator.push(AddAccountScreen()) }) {
                                        Text("添加账户")
                                    }
                                }
                            } else if (activeAccount == null && accounts.isNotEmpty()) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("请在侧边栏选择一个活动账户")
                                }
                            } else if (activeAccount != null) {
                                // CurrentTab 会渲染 InboxTab.Content() 或 SentTab.Content()
                                // 这些 Content 方法内部会自己获取最新的 activeAccount
                                CurrentTab()
                            } else {
                                // 初始状态或 accounts 正在加载时，或者没有账户且已导航到 AddAccountScreen
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    // 可以放一个加载指示器，如果 MainScreenModel 有 isLoadingAccounts 状态
                                    // Text("正在准备...")
                                    CircularProgressIndicator() // 作为一个通用的加载状态
                                }
                            }
                        }
                    },
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = tabNavigator.current == InboxTab, // 直接比较 Tab object 实例
                                onClick = { tabNavigator.current = InboxTab },
                                icon = {
                                    val currentIcon = InboxTab.options.icon // 在 Composable lambda 内部安全调用
                                    if (currentIcon != null) {
                                        Icon(painter = currentIcon, contentDescription = InboxTab.options.title)
                                    } else {
                                        Spacer(Modifier.size(24.dp)) // 回退图标
                                    }
                                },
                                label = { Text(InboxTab.options.title) }
                            )
                            NavigationBarItem(
                                selected = tabNavigator.current == SentTab,
                                onClick = { tabNavigator.current = SentTab },
                                icon = {
                                    val currentIcon = SentTab.options.icon
                                    if (currentIcon != null) {
                                        Icon(painter = currentIcon, contentDescription = SentTab.options.title)
                                    } else {
                                        Spacer(Modifier.size(24.dp))
                                    }
                                },
                                label = { Text(SentTab.options.title) }
                            )
                            // TODO: Add other tabs like Drafts, Spam, Trash etc.
                        }
                    },
                    floatingActionButton = {
                        activeAccount?.let { // 只在有活动账户时显示 FAB
                            FloatingActionButton(
                                onClick = {
                                    navigator.push(
                                        ComposeEmailScreen(
                                            replyToEmail = null, // 新邮件
                                            forwardEmail = null
                                        )
                                    )
                                }
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = "撰写新邮件")
                            }
                        }
                    }
                )
            }
        }
    }
}

// 定义底部导航栏的 Tab (恢复为 object)
internal object InboxTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = "收件箱"
            val imageVector = Icons.Filled.Inbox
            val painter = rememberVectorPainter(image = imageVector)
            return remember { TabOptions(index = 0u, title = title, icon = painter) }
        }

    @Composable
    override fun Content() {
        // 在 Tab 的 Content 内部，从共享的 MainScreenModel 获取 activeAccount
        // 注意：这里 getScreenModel<MainScreenModel>() 会获取与 MainScreen 中相同的实例 (如果 Koin 正确配置为单例或作用域单例)
        // 或者，如果 MainScreenModel 不是单例且 Tab 的 Content 与 MainScreen 的 Content 在不同的 remember 作用域，
        // 可能会导致获取到不同的 MainScreenModel 实例。
        // 更安全的方式是将 activeAccount 作为参数直接传递给 EmailListScreen，
        // 而 activeAccount 由 MainScreen 的 Content 一次性获取并向下传递。
        val screenModel = getScreenModel<MainScreenModel>() // 获取与 MainScreen 共享的 ScreenModel
        val activeAccount by screenModel.activeAccountState.collectAsState()

        println("InboxTab Content: Composing. Active account: ${activeAccount?.emailAddress}")

        activeAccount?.let { acc ->
            // 当 activeAccount 变化时，这个 EmailListScreen 会因为 key 的变化而重组或重新创建 ScreenModel
            EmailListScreen(account = acc, folderName = "INBOX")
        } ?: Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("请在侧边栏选择一个活动账户 (InboxTab)")
        }
    }
}

internal object SentTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = "已发送"
            val imageVector = Icons.AutoMirrored.Filled.Send
            val painter = rememberVectorPainter(image = imageVector)
            return remember { TabOptions(index = 1u, title = title, icon = painter) }
        }

    @Composable
    override fun Content() {
        val screenModel = getScreenModel<MainScreenModel>()
        val activeAccount by screenModel.activeAccountState.collectAsState()

        println("SentTab Content: Composing. Active account: ${activeAccount?.emailAddress}")

        activeAccount?.let { acc ->
            EmailListScreen(account = acc, folderName = "Sent")
        } ?: Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("请在侧边栏选择一个活动账户 (SentTab)")
        }
    }
}