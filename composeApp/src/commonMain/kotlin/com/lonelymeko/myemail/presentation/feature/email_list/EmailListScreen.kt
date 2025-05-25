package com.lonelymeko.myemail.presentation.feature.email_list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material.icons.filled.MailOutline
// 导入 Material 3 的下拉刷新相关组件
// 注意：这些导入路径是基于常见的 M3 实验性 API 模式，如果官方 API 不同，请调整
import androidx.compose.material3.pulltorefresh.PullToRefreshBox // 假设的 M3 下拉刷新容器
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState // 假设的 M3 状态 hoist
import androidx.compose.material3.CircularProgressIndicator // M3 加载指示器
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api // M3 实验性 API 注解
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.Button // M3 Button
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.lonelymeko.myemail.data.model.AccountInfo
import com.lonelymeko.myemail.data.model.EmailMessage
import com.lonelymeko.myemail.presentation.feature.email_detail.EmailDetailScreen
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull // 使用 mapNotNull 避免 nullable lastVisibleItem
import kotlinx.coroutines.flow.collect // 手动 collect
import kotlinx.coroutines.flow.filter // 用于过滤
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import org.koin.core.parameter.parametersOf
import kotlin.time.Duration.Companion.days

// 在文件顶部或一个伴生对象中定义格式化器，以便复用
private val TIME_FORMATTER_FOR_LIST = LocalDateTime.Format {
    hour()
    char(':')
    minute()
}

private val DATE_FORMATTER_FOR_LIST = LocalDateTime.Format {
    monthNumber(Padding.NONE)
    char('/')
    dayOfMonth(Padding.NONE)
}


data class EmailListScreen(
    val account: AccountInfo,
    val folderName: String
) : Screen {
    override val key: String = "EmailListScreen_${account.emailAddress}_${folderName}"

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class) // 添加 ExperimentalMaterial3Api
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow.parent
        val screenModel = getScreenModel<EmailListScreenModel>(
            parameters = { parametersOf(account, folderName) }
        )

        val uiState = screenModel.uiState
        val snackbarHostState = remember { SnackbarHostState() }

        // Material 3 下拉刷新状态
        // isRefreshing 应该直接来自 uiState.isRefreshing
        // onRefresh 调用 screenModel.refreshEmails()
        val pullToRefreshState = rememberPullToRefreshState() // M3 的 state hoist

        LaunchedEffect(uiState.errorMessage) {
            uiState.errorMessage?.let {
                snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Short)
                screenModel.consumeErrorMessage()
            }
        }

        val listState = rememberLazyListState()
        LaunchedEffect(listState, uiState.canLoadMore, uiState.isLoading, uiState.isRefreshing, uiState.emails.size) {
            snapshotFlow { listState.layoutInfo.visibleItemsInfo }
                .mapNotNull { it.lastOrNull() }
                .distinctUntilChanged { old, new -> old.index == new.index }
                .filter { lastVisibleItem -> // 将判断条件移到 filter 中
                    lastVisibleItem.index >= uiState.emails.size - 1 - 5 && // 阈值
                            uiState.canLoadMore &&
                            !uiState.isLoading &&
                            !uiState.isRefreshing &&
                            uiState.emails.isNotEmpty() // 确保列表不为空时才尝试加载更多
                }
                .collect {
                    println("EmailListScreen: Reached near end of list, attempting to load more.")
                    screenModel.loadMoreEmails()
                }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { paddingValues ->
            // 使用 Material 3 的 PullToRefreshBox
            PullToRefreshBox(
                state = pullToRefreshState, // 传递 M3 的 state
                isRefreshing = uiState.isRefreshing, // 控制指示器的显示
                onRefresh = {
                    println("EmailListScreen: M3 PullToRefreshBox triggered for ${account.emailAddress}/$folderName")
                    screenModel.refreshEmails()
                },
                modifier = Modifier.padding(paddingValues).fillMaxSize()
                // indicator = { PullToRefreshDefaults.Indicator(state = pullToRefreshState) } // M3 指示器，通常 PullToRefreshBox 默认会处理
            ) {
                // PullToRefreshBox 的内容槽位
                if (uiState.isLoading && uiState.emails.isEmpty() && !uiState.isRefreshing) { // 初始加载
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                        Text("正在加载邮件...", modifier = Modifier.padding(top = 60.dp))
                    }
                } else if (uiState.emails.isEmpty() && !uiState.isLoading && !uiState.isRefreshing) { // 无邮件
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("文件夹 '${uiState.folderName}' 中没有邮件")
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { screenModel.refreshEmails() }) { Text("尝试刷新") }
                        }
                    }
                } else { // 显示邮件列表
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize() // LazyColumn 应该填充 PullToRefreshBox 的内容区域
                    ) {
                        itemsIndexed(uiState.emails, key = { _, email -> email.messageServerId }) { _, email ->
                            EmailListItem(
                                email = email,
                                isSelected = false,
                                onEmailClick = {
                                    navigator?.push(
                                        EmailDetailScreen(account, folderName, email.messageServerId)
                                    )
                                },
                                onEmailLongClick = { /* TODO */ }
                            )
                            Divider()
                        }

                        // 加载更多指示器
                        if (uiState.isLoading && uiState.emails.isNotEmpty() && !uiState.isRefreshing) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("正在加载更多...")
                                }
                            }
                        } else if (uiState.canLoadMore && uiState.emails.isNotEmpty() && !uiState.isLoading && !uiState.isRefreshing) {
                            // 可以考虑放一个“点击加载更多”的按钮，如果不想完全依赖自动滚动加载
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    // Text("已加载全部") // 或者什么都不显示，或者一个小的分隔符
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EmailListItem(
    email: EmailMessage,
    isSelected: Boolean,
    onEmailClick: () -> Unit,
    onEmailLongClick: () -> Unit
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val fontWeight = if (!email.isRead) FontWeight.Bold else FontWeight.Normal

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable( // 支持单击和长按
                onClick = onEmailClick,
                onLongClick = onEmailLongClick
            )
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isSelected) Icons.Filled.CheckCircle else if (!email.isRead) Icons.Filled.Drafts else Icons.Filled.MailOutline,
            contentDescription = if (!email.isRead) "未读" else "已读",
            tint = if (!email.isRead && !isSelected) MaterialTheme.colorScheme.primary else LocalContentColor.current
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = email.displayFrom,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = fontWeight),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = email.subject ?: "(无主题)",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = fontWeight),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // 可以再加一行显示邮件摘要或部分正文
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = email.sentDate?.formatDateForList() ?: email.receivedDate?.formatDateForList() ?: "",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = fontWeight
        )
    }
}

// 辅助函数格式化日期显示
fun LocalDateTime.formatDateForList(): String {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
//    val yesterday = today.minus(1, DateTimeUnit.DAY) // kotlinx-datetime 1.5.0+
      val yesterday = Clock.System.now().minus(1.days).toLocalDateTime(TimeZone.currentSystemDefault()).date

    return when (this.date) {
        today -> TIME_FORMATTER_FOR_LIST.format(this)
        yesterday -> "昨天"
        else -> DATE_FORMATTER_FOR_LIST.format(this)
    }
}