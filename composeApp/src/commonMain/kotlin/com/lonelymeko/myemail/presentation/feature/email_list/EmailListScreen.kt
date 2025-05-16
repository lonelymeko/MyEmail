package com.lonelymeko.myemail.presentation.feature.email_list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Drafts // 未读图标
import androidx.compose.material.icons.filled.MailOutline // 已读图标
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel // 用于获取 ScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.lonelymeko.myemail.data.model.AccountInfo
import com.lonelymeko.myemail.data.model.EmailMessage
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.format.*
import kotlinx.datetime.format.char
import org.koin.core.context.GlobalContext.get
import org.koin.core.parameter.parametersOf
import kotlin.time.Duration.Companion.days
import androidx.compose.material3.SnackbarDuration
import com.lonelymeko.myemail.presentationfeature.email_detail.EmailDetailScreen
import org.koin.core.parameter.ParametersHolder

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


// Screen 接收 AccountInfo 和 folderName
data class EmailListScreen(val account: AccountInfo, val folderName: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        // 为 ScreenModel 提供构造函数参数
        val screenModel = getScreenModel<EmailListScreenModel>(
            parameters = { parametersOf(account, folderName) }, // account 是第0个, folderName 是第1个
//            tag = "${account.emailAddress}_${folderName}",

        )
        val uiState = screenModel.uiState
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(uiState.errorMessage) {
            uiState.errorMessage?.let {
                snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
                screenModel.consumeErrorMessage()
            }
        }

        Scaffold( // EmailListScreen 可能不需要自己的 Scaffold，如果它是 Tab 的内容
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.emails.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("文件夹 '${uiState.folderName}' 中没有邮件")
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(uiState.emails, key = { it.messageServerId }) { email ->
                            EmailListItem( // 使用之前定义的 EmailListItem
                                email = email,
                                isSelected = false, // 简化，暂无多选
                                onEmailClick = {
                                    navigator.push(
                                        EmailDetailScreen(
                                            account,
                                            folderName,
                                            email.messageServerId
                                        )
                                    )
                                },
                                onEmailLongClick = { /* 暂无长按操作 */ }
                            )
                            Divider()
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