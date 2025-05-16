package com.lonelymeko.myemail.presentationfeature.email_detail


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler // 用于打开链接
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.lonelymeko.myemail.data.model.AccountInfo
import com.lonelymeko.myemail.data.model.EmailAttachmentInfo
import com.lonelymeko.myemail.data.model.EmailMessage
import com.lonelymeko.myemail.data.remote.api.EmailResult
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import org.koin.core.parameter.parametersOf
// --- 预定义的日期时间格式化器 ---
private val FULL_DATE_TIME_FORMATTER = LocalDateTime.Format { // 创建并存储格式化器实例
    year()
    char('-')
    monthNumber(Padding.ZERO) // 使用 Padding.ZERO 确保月份是两位数 (例如 "05")
    char('-')
    dayOfMonth(Padding.ZERO)  // 使用 Padding.ZERO 确保天数是两位数 (例如 "08")
    char(' ')                 // 日期和时间之间的空格
    hour(Padding.ZERO)        // 使用 Padding.ZERO 确保小时是两位数
    char(':')
    minute(Padding.ZERO)      // 使用 Padding.ZERO 确保分钟是两位数
    // 如果需要秒：
    // char(':')
    // second(Padding.ZERO)
}

// Screen 接收 AccountInfo, folderName, 和 messageServerId
data class EmailDetailScreen(
    val account: AccountInfo,
    val folderName: String,
    val messageServerId: String
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = getScreenModel<EmailDetailScreenModel>(
//            tag = "${account.emailAddress}_${folderName}_$messageServerId", // 唯一 tag
            parameters = {
                parametersOf(account, folderName, messageServerId)
//                EmailDetailScreenModel(account, folderName, messageServerId, get(), get(), get())
            }
        )
        val uiState = screenModel.uiState // 使用 by screenModel.uiState
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val uriHandler = LocalUriHandler.current // 用于打开链接

        // 显示错误信息
        LaunchedEffect(uiState.errorMessage) {
            uiState.errorMessage?.let {
                snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
                screenModel.consumeErrorMessage()
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = uiState.emailMessage?.subject ?: "加载中...",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "返回邮件列表")
                        }
                    },
                    actions = {
                        uiState.emailMessage?.let { email ->
                            // 标记已读/未读按钮
                            IconButton(onClick = { screenModel.markAsRead(!email.isRead) }) {
                                Icon(
                                    if (email.isRead) Icons.Filled.MarkEmailUnread else Icons.Filled.MarkEmailRead,
                                    contentDescription = if (email.isRead) "标记为未读" else "标记为已读"
                                )
                            }
                            // 删除按钮
                            IconButton(onClick = {
                                scope.launch {
                                    screenModel.deleteEmail()?.collectLatest { result ->
                                        if (result is EmailResult.Success) {
                                            snackbarHostState.showSnackbar("邮件已删除")
                                            navigator.pop() // 删除成功后返回列表
                                        } else if (result is EmailResult.Error) {
                                            snackbarHostState.showSnackbar("删除失败: ${result.message}")
                                        }
                                    }
                                }
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "删除邮件")
                            }
                            // TODO: 实现全部回复和转发按钮的导航，传递相应的参数
                            IconButton(onClick = {
                                // navigator.push(ComposeEmailScreen(replyToEmail = emailToReply, replyAll = true)) // 可能需要调整ComposeEmailScreen的参数
//                                snackbarHostState.showSnackbar("全部回复功能待实现")
                            }) {
                                Icon(Icons.Filled.ReplyAll, "全部回复")
                            }
                            IconButton(onClick = {
                                // navigator.push(ComposeEmailScreen(forwardEmail = emailToReply))
//                                snackbarHostState.showSnackbar("转发功能待实现")
                            }) {
                                Icon(Icons.Filled.Forward, "转发")
                            }
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.emailMessage != null) {
                val email = uiState.emailMessage!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // 邮件头部信息
                    Text("主题: ${email.subject ?: "(无主题)"}", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("发件人: ${email.fromAddress ?: "未知"}", style = MaterialTheme.typography.bodyMedium)
                    email.toList?.let { Text("收件人: ${it.joinToString()}", style = MaterialTheme.typography.bodyMedium) }
                    email.ccList?.let { Text("抄送: ${it.joinToString()}", style = MaterialTheme.typography.bodyMedium) }
                    Text("日期: ${email.sentDate?.formatFull() ?: email.receivedDate?.formatFull() ?: "未知"}", style = MaterialTheme.typography.bodySmall)

                    Divider(modifier = Modifier.padding(vertical = 16.dp))

                    // 附件列表 (如果存在)
                    email.attachments?.takeIf { it.isNotEmpty() }?.let { attachments ->
                        Text("附件 (${attachments.size}):", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(attachments) { attachment ->
                                AttachmentChip(attachment = attachment, onClick = {
                                    // TODO: 实现附件下载/打开逻辑
                                    scope.launch { snackbarHostState.showSnackbar("打开/下载 '${attachment.fileName}' 功能待实现") }
                                })
                            }
                        }
                        Divider(modifier = Modifier.padding(vertical = 16.dp))
                    }

                    // 邮件正文 (优先HTML，否则纯文本)
                    // TODO: 实现一个更好的 HTML 查看器 (例如使用 WebView on Android)
                    // 目前简单地用 Text 显示，HTML 会带标签，纯文本则正常显示
                    val bodyContent = email.bodyHtml ?: email.bodyPlainText ?: "(无正文内容)"
                    // 如果是HTML，我们可能想用不同的方式渲染
                    if (email.bodyHtml != null) {
                        // 在 Android 上可以使用 WebView 来渲染 HTML
                        // 在 Desktop 上可能需要 JCEF 或其他库
                        // 作为一个简单的回退，我们仍然用 Text，但用户会看到 HTML 标签
                        Text("HTML 正文 (简单显示):", style = MaterialTheme.typography.labelSmall)
                        HtmlTextView(html = email.bodyHtml!!, modifier = Modifier.fillMaxWidth()) // 假设有 HtmlTextView
                    } else {
                        Text(bodyContent, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text("无法加载邮件详情。")
                }
            }
        }
    }
}

@Composable
fun AttachmentChip(attachment: EmailAttachmentInfo, onClick: () -> Unit) {
    SuggestionChip(
        onClick = onClick,
        label = { Text("${attachment.fileName} (${formatSize(attachment.sizeBytes)})", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        icon = { Icon(Icons.Filled.AttachFile, contentDescription = "附件") }
    )
}

fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "${"%.1f".format(kb)} KB"
    val mb = kb / 1024.0
    return "${"%.1f".format(mb)} MB"
}

// 辅助函数格式化完整日期显示
// 辅助函数格式化完整日期显示
fun LocalDateTime.formatFull(): String {
    // 使用预定义的格式化器实例来格式化 'this' (即 LocalDateTime 对象)
    return FULL_DATE_TIME_FORMATTER.format(this)
}

// 简易的HTML渲染 (非常基础，不能处理复杂HTML或CSS)
// 在实际应用中，Android 使用 WebView, Desktop 使用 JCEF 或类似方案
@Composable
fun HtmlTextView(html: String, modifier: Modifier = Modifier) {
    // 1. 在 @Composable 函数的直接作用域内获取颜色值
    val linkColor = MaterialTheme.colorScheme.primary // 通常链接使用 primary color
        // 或者如果颜色是固定的，则不需要作为 key
    // 这是一个非常简化的版本，仅用于演示。
    // 它不能正确处理所有HTML，并且没有链接点击等交互。
    // 你可能需要查找特定于 Compose Multiplatform 的 HTML 渲染库，
    // 或者为每个平台提供特定的 expect/actual Composable。
    val annotatedString = remember(html,linkColor) {
        // 尝试非常基础的标签去除和链接识别 (非常不完美)
        // 更好的方法是使用一个 Markdown->AnnotatedString 或 HTML->AnnotatedString 的库
        buildAnnotatedString {
            var currentIndex = 0
            val linkRegex = Regex("""<a\s+href\s*=\s*"(https?://[^"]+)"[^>]*>(.*?)</a>""", RegexOption.IGNORE_CASE)
            linkRegex.findAll(html).forEach { matchResult ->
                val (url, linkText) = matchResult.destructured
                val startIndex = matchResult.range.first
                val endIndex = matchResult.range.last + 1

                // 添加链接前的文本
                if (startIndex > currentIndex) {
                    append(html.substring(currentIndex, startIndex).replace(Regex("<[^>]*>"), ""))
                }
                // 添加链接文本
                pushStringAnnotation(tag = "URL", annotation = url)
                withStyle(style = SpanStyle(color = linkColor, fontWeight = FontWeight.Bold)) {
                    append(linkText.replace(Regex("<[^>]*>"), ""))
                }
                pop()
                currentIndex = endIndex
            }
            // 添加剩余文本
            if (currentIndex < html.length) {
                append(html.substring(currentIndex).replace(Regex("<[^>]*>"), ""))
            }
        }
    }
    val uriHandler = LocalUriHandler.current
    ClickableText(
        text = annotatedString,
        style = MaterialTheme.typography.bodyLarge,
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    try {
                        uriHandler.openUri(annotation.item)
                    } catch (e: Exception) {
                        // 处理无法打开链接的异常
                        println("Could not open URL: ${annotation.item}")
                    }
                }
        },
        modifier = modifier
    )
}