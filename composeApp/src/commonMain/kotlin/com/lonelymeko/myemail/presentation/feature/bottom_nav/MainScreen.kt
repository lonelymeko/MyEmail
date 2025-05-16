package com.lonelymeko.myemail.presentation.feature.bottom_nav


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.lonelymeko.myemail.data.model.AccountInfo
import com.lonelymeko.myemail.presentation.feature.add_account.AddAccountScreen
import com.lonelymeko.myemail.presentation.feature.email_list.EmailListScreen // 稍后创建
import com.lonelymeko.myemail.presentation.navigation.AddAccountScreenRoute
import kotlinx.coroutines.launch

object MainScreen : Screen { // 改为 object，因为它不带参数

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow // 获取外层 Navigator
        val screenModel = getScreenModel<MainScreenModel>()

        val accounts by screenModel.accountsState.collectAsState()
        val activeAccount by screenModel.activeAccountState.collectAsState()

        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        // 如果没有账户，自动导航到添加账户界面
        LaunchedEffect(accounts) {
            if (accounts.isEmpty()) { // 可以加一个判断，比如在 ScreenModel 初始化后 isLoadingAccounts 变为 false 时
                navigator.push(AddAccountScreen()) // 使用我们定义的 Screen
            }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Spacer(Modifier.height(12.dp))
                    Text("邮箱账户", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    accounts.forEach { account:  AccountInfo ->
                        NavigationDrawerItem(
                            icon = { Icon(if (account.emailAddress == activeAccount?.emailAddress) Icons.Filled.Mail else Icons.Outlined.MailOutline, contentDescription = account.displayName) },
                            label = { Text(account.displayName ?: account.emailAddress) },
                            selected = account.emailAddress == activeAccount?.emailAddress,
                            onClick = {
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
                    // Можно добавить пункт "Управление аккаунтами", ведущий на AccountManagementScreenRoute
                }
            }
        ) {
            // TabNavigator 用于底部导航栏
            TabNavigator(InboxTab) { tabNavigator ->
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
                                // 可以添加刷新按钮等
                                if (activeAccount != null) {
                                    IconButton(onClick = { /* TODO: Refresh emails */ }) {
                                        Icon(Icons.Filled.Refresh, contentDescription = "刷新")
                                    }
                                }
                            }
                        )
                    },
                    content = { paddingValues ->
                        Box(modifier = Modifier.padding(paddingValues)) {
                            if (activeAccount == null && accounts.isNotEmpty()) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("请在侧边栏选择一个活动账户")
                                }
                            } else if (activeAccount != null) {
                                // CurrentTab 会渲染当前选中的 Tab 的内容
                                CurrentTab()
                            } else if (accounts.isEmpty()){
                                // 显示加载或提示添加账户（虽然上面 LaunchedEffect 应该会导航走）
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
                            }
                        }
                    },
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = tabNavigator.current == InboxTab,
                                onClick = { tabNavigator.current = InboxTab },
                                icon = { Icon(InboxTab.options.icon!!, contentDescription = InboxTab.options.title) },
                                label = { Text(InboxTab.options.title) }
                            )
                            // 可以添加其他底部导航项，例如 "已发送", "草稿" 等，它们也需要是 Tab 对象
                            NavigationBarItem(
                                selected = tabNavigator.current == SentTab, // 假设有 SentTab
                                onClick = { tabNavigator.current = SentTab },
                                icon = { Icon(SentTab.options.icon!!, contentDescription = SentTab.options.title) },
                                label = { Text(SentTab.options.title) }
                            )
                        }
                    }
                )
            }
        }
    }
}

// 定义底部导航栏的 Tab
internal object InboxTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = "收件箱"
            val icon = Icons.Filled.Inbox
            return remember { TabOptions(index = 0u, title = title, icon = icon) }
        }

    @Composable
    override fun Content() {
        // 这里会显示收件箱的邮件列表
        // 我们需要获取当前活动账户，并将其传递给 EmailListScreen
        val screenModel = getScreenModel<MainScreenModel>()
        val activeAccount by screenModel.activeAccountState.collectAsState()

        activeAccount?.let { acc ->
            EmailListScreen(account = acc, folderName = "INBOX") // 传递账户和文件夹名
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("请先选择一个活动账户")
        }
    }
}
// 示例：已发送 Tab (你需要创建对应的 EmailListScreen 或其他 Screen)
internal object SentTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = "已发送"
            val icon = Icons.Filled.Send
            return remember { TabOptions(index = 1u, title = title, icon = icon) }
        }
    @Composable
    override fun Content() {
        val screenModel = getScreenModel<MainScreenModel>()
        val activeAccount by screenModel.activeAccountState.collectAsState()
        activeAccount?.let { acc ->
            EmailListScreen(account = acc, folderName = "Sent") // 假设已发送文件夹名为 "Sent"
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("请先选择一个活动账户")
        }
    }
}