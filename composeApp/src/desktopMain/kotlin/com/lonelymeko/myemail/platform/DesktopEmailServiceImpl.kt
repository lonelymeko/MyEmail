package com.lonelymeko.myemail.platform

// 导入 Jakarta Mail 相关的类，注意包名是 jakarta.* 而不是 javax.*
import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeUtility
// import jakarta.activation.* // 如果需要显式使用 DataHandler 等，可能需要导入

import com.lonelymeko.myemail.common.exception.NotImplementedException // 使用我们之前定义的自定义异常
import com.lonelymeko.myemail.data.model.AccountInfo
import com.lonelymeko.myemail.data.model.AccountType // 确保 AccountType 在 commonMain 中定义
import com.lonelymeko.myemail.data.model.EmailMessage
import com.lonelymeko.myemail.data.remote.api.EmailResult
import com.lonelymeko.myemail.data.remote.api.EmailService // 导入 commonMain 中的 interface
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking // 用于 main 函数测试
import kotlinx.coroutines.withContext
import java.util.Date // Jakarta Mail 仍然使用 java.util.Date
import java.util.Properties
import kotlinx.coroutines.flow.flow // 用于创建 Flow
import kotlinx.datetime.Instant as KotlinxInstant // 使用别名
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant // 用于 java.util.Date -> kotlinx.datetime.Instant
import kotlinx.datetime.toLocalDateTime // 用于 kotlinx.datetime.Instant -> kotlinx.datetime.LocalDateTime
import jakarta.mail.Flags
import jakarta.mail.Folder
import jakarta.mail.UIDFolder // 用于通过 UID 获取邮件
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMultipart
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toKotlinInstant

class DesktopEmailServiceImpl(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : EmailService {

    private fun createMailProperties(host: String, port: Int, useSsl: Boolean, protocolPrefix: String): Properties {
        return Properties().apply {
            put("mail.$protocolPrefix.host", host)
            put("mail.$protocolPrefix.port", port.toString())
            put("mail.$protocolPrefix.auth", "true")

            if (useSsl) {
                // SMTP STARTTLS (e.g., port 587) vs SMTPS (e.g., port 465)
                if (protocolPrefix == "smtp" && (port == 587 || port == 25)) { // 25 也很少用SSL了
                    put("mail.smtp.starttls.enable", "true")
                    // 对于STARTTLS，有时也需要信任主机（开发阶段）或配置特定协议
                    // put("mail.smtp.ssl.trust", host)
                    // put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3")
                } else { // Direct SSL (IMAPS on 993, SMTPS on 465)
                    put("mail.$protocolPrefix.socketFactory.port", port.toString())
                    put("mail.$protocolPrefix.socketFactory.class", "javax.net.ssl.SSLSocketFactory") // 对于 Jakarta Mail，这个可能还是 javax
                    // 或者，如果使用的是更新的 SSLSocketFactory 实现，可能是 "jakarta.net.ssl.SSLSocketFactory"
                    // 需要根据实际使用的 Jakarta Mail 实现来确定。com.sun.mail 通常仍用 javax.net.ssl
                    put("mail.$protocolPrefix.ssl.enable", "true") // 确保 SSL 被启用
                    // put("mail.$protocolPrefix.ssl.trust", host) // 信任特定主机或 "*" (不安全)
                }
            } else { // 非 SSL 端口
                if (protocolPrefix == "smtp") {
                    // 如果是非SSL端口，但服务器支持STARTTLS，则启用它
                    put("mail.smtp.starttls.enable", "true")
                }
            }
            // put("mail.debug", "true") // 开启 Jakarta Mail 调试日志
        }
    }

    override suspend fun testConnection(accountInfo: AccountInfo): EmailResult<Unit> = withContext(ioDispatcher) {
        // IMAP Test
        try {
            val imapProtocol = if (accountInfo.imapUseSsl == true && accountInfo.imapPort == 993) "imaps" else "imap"
            val imapProps =
                accountInfo.imapHost?.let { accountInfo.imapPort?.let { port -> accountInfo.imapUseSsl?.let { useSsl -> createMailProperties(it, port, useSsl, "imap") } } }
            val imapSession = Session.getInstance(imapProps)
             imapSession.setDebug(true) // 开启调试
            println("Desktop: Attempting IMAP ($imapProtocol) to ${accountInfo.imapHost}:${accountInfo.imapPort}")
            val store = imapSession.getStore(imapProtocol)
//            store.connect(accountInfo.emailAddress, accountInfo.passwordOrAuthCode)
            accountInfo.imapPort?.let {
                store.connect(
                    accountInfo.imapHost, // <--- 显式传递主机名
                    it, // <--- 显式传递端口号
                    accountInfo.emailAddress,
                    accountInfo.passwordOrAuthCode
                )
            }
//            store.close()
            println("Desktop: IMAP connection successful for ${accountInfo.emailAddress}")
        } catch (e: AuthenticationFailedException) {
            return@withContext EmailResult.Error(e, "IMAP认证失败: ${e.message}")
        } catch (e: MessagingException) {
            return@withContext EmailResult.Error(e, "IMAP连接失败: ${e.message}")
        } catch (e: Exception) {
            return@withContext EmailResult.Error(e, "IMAP连接时发生未知错误: ${e.message}")
        }

        // SMTP Test
        try {
            val smtpProtocol = if (accountInfo.smtpUseSsl == true && accountInfo.smtpPort == 465) "smtps" else "smtp"
            val smtpProps =
                accountInfo.smtpHost?.let { accountInfo.smtpPort?.let { port -> accountInfo.smtpUseSsl?.let { useSsl -> createMailProperties(it, port, useSsl, "smtp") } } }
            val smtpSession = Session.getInstance(smtpProps, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(accountInfo.emailAddress, accountInfo.passwordOrAuthCode)
                }
            })
             smtpSession.setDebug(true) // 开启调试
            println("Desktop: Attempting SMTP ($smtpProtocol) to ${accountInfo.smtpHost}:${accountInfo.smtpPort}")
            val transport = smtpSession.getTransport(smtpProtocol)

            accountInfo.smtpPort?.let {
                transport.connect(
                    accountInfo.smtpHost, // <--- 显式传递主机名
                    it, // <--- 显式传递端口号
                    accountInfo.emailAddress,
                    accountInfo.passwordOrAuthCode
                )
            }
            transport.close()

            println("Desktop: SMTP connection successful for ${accountInfo.emailAddress}")
        } catch (e: AuthenticationFailedException) {
            return@withContext EmailResult.Error(e, "SMTP认证失败: ${e.message}")
        } catch (e: MessagingException) {
            return@withContext EmailResult.Error(e, "SMTP连接失败: ${e.message}")
        } catch (e: Exception) {
            return@withContext EmailResult.Error(e, "SMTP连接时发生未知错误: ${e.message}")
        }
        return@withContext EmailResult.Success(Unit)
    }

    override suspend fun sendEmail(accountInfo: AccountInfo, emailMessage: EmailMessage): EmailResult<Unit> = withContext(ioDispatcher) {
        try {
            val smtpProtocol = if (accountInfo.smtpUseSsl == true && accountInfo.smtpPort == 465) "smtps" else "smtp"
            val props =
                accountInfo.smtpHost?.let { accountInfo.smtpPort?.let { port -> accountInfo.smtpUseSsl?.let { useSsl -> createMailProperties(it, port, useSsl, "smtp") } } }

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(accountInfo.emailAddress, accountInfo.passwordOrAuthCode)
                }
            })
             session.setDebug(true)

            val message = MimeMessage(session)
            // From: (处理发件人显示名称的编码)
            message.setFrom(InternetAddress(accountInfo.emailAddress, MimeUtility.encodeText(accountInfo.displayName ?: accountInfo.emailAddress, "UTF-8", "B")))

            // To Recipients:
            emailMessage.toList?.forEach { recipient ->
                if (recipient.isNotBlank()) {
                    message.addRecipient(Message.RecipientType.TO, InternetAddress(recipient.trim()))
                }
            }
            // CC Recipients:
            emailMessage.ccList?.forEach { recipient ->
                if (recipient.isNotBlank()) {
                    message.addRecipient(Message.RecipientType.CC, InternetAddress(recipient.trim()))
                }
            }
            // BCC Recipients:
            emailMessage.bccList?.forEach { recipient ->
                if (recipient.isNotBlank()) {
                    message.addRecipient(Message.RecipientType.BCC, InternetAddress(recipient.trim()))
                }
            }

            message.subject = MimeUtility.encodeText(emailMessage.subject ?: "", "UTF-8", "B")
            message.sentDate = Date() // 当前时间

            // Body: (简化处理，优先HTML，否则纯文本)
            // 真实应用中处理 multipart (text/plain, text/html, attachments) 会更复杂
            if (!emailMessage.bodyHtml.isNullOrBlank()) {
                message.setContent(emailMessage.bodyHtml, "text/html; charset=utf-8")
            } else {
                message.setText(emailMessage.bodyPlainText ?: "", "UTF-8")
            }

            println("Desktop: Sending email via ${accountInfo.smtpHost}:${accountInfo.smtpPort} as ${accountInfo.emailAddress}")
            val transport = session.getTransport(smtpProtocol)
            accountInfo.smtpPort?.let {
                transport.connect(
                    accountInfo.smtpHost, // <--- 显式传递主机名
                    it, // <--- 显式传递端口号
                    accountInfo.emailAddress,
                    accountInfo.passwordOrAuthCode
                )
            }
            transport.sendMessage(message, message.allRecipients)
            transport.close()
            println("Desktop: Email sent successfully!")
            EmailResult.Success(Unit)

        } catch (e: SendFailedException) {
            // 可以获取更详细的发送失败信息
            // e.invalidAddresses, e.validSentAddresses, e.validUnsentAddresses
            EmailResult.Error(e, "邮件发送失败 (部分地址无效): ${e.message}")
        } catch (e: MessagingException) {
            EmailResult.Error(e, "邮件发送时发生消息错误: ${e.message}")
        } catch (e: Exception) {
            EmailResult.Error(e, "邮件发送时发生未知错误: ${e.message}")
        }
    }

    // 其他方法的存根

    override fun fetchEmails(
        accountInfo: AccountInfo,
        folderName: String,
        page: Int, // 1-based page number
        pageSize: Int
    ): Flow<EmailResult<List<EmailMessage>>> = flow { // 使用 flow builder
        var store: Store? = null
        var folder: Folder? = null
        try {
            println("Desktop: Listing all top-level folders:")
            val defaultFolder = store?.defaultFolder
            if (defaultFolder != null) {
                defaultFolder.list("*").forEach { f ->
                    println("  Folder: ${f.fullName}, Type: ${f.type}")
                    // 特别注意看哪个文件夹的 Attributes 包含了 \Sent (如果服务器支持 SPECIAL-USE)
                    // 或者哪个看起来像你的“已发送邮件”文件夹
                }
            }
            val imapProtocol = if (accountInfo.imapUseSsl && accountInfo.imapPort == 993) "imaps" else "imap"
            val imapProps = createMailProperties(accountInfo.imapHost, accountInfo.imapPort, accountInfo.imapUseSsl, "imap")
            val imapSession = Session.getInstance(imapProps)
            // imapSession.debug = true // 开启调试

            store = imapSession.getStore(imapProtocol)
            println("Desktop: Connecting to IMAP for fetchEmails: ${accountInfo.imapHost}")
            store.connect(
                accountInfo.imapHost,
                accountInfo.imapPort,
                accountInfo.emailAddress,
                accountInfo.passwordOrAuthCode
            )
            println("Desktop: IMAP connected for fetchEmails.")

            folder = store.getFolder(folderName)
            if (folder == null || !folder.exists()) {
                emit(EmailResult.Error(MessagingException("Folder '$folderName' not found.")))
                return@flow
            }

            // 打开文件夹为只读模式
            folder.open(Folder.READ_ONLY)
            println("Desktop: Opened folder '$folderName'. Message count: ${folder.messageCount}")

            val totalMessages = folder.messageCount
            if (totalMessages == 0) {
                emit(EmailResult.Success(emptyList()))
                return@flow
            }

            // --- 分页逻辑 (简化版：获取最近的 pageSize * page 封邮件，然后取最后一页) ---
            // IMAP 邮件是按消息号索引的，通常1是最旧的，messageCount 是最新的。
            // 我们想要获取最新的邮件。
            val startIndex = Math.max(1, totalMessages - (page * pageSize) + 1)
            val endIndex = Math.max(1, totalMessages - ((page - 1) * pageSize))

            // 确保 startIndex 不超过 endIndex，并且都在有效范围内
            val actualStart = if (startIndex > endIndex || startIndex > totalMessages) {
                if (totalMessages > 0 && pageSize > 0 && page ==1) 1 // 至少尝试获取第一页
                else { // 没有更多邮件或页码无效
                    emit(EmailResult.Success(emptyList()))
                    return@flow
                }
            } else startIndex
            val actualEnd = Math.min(totalMessages, endIndex)


            if (actualStart > actualEnd) { // 如果计算后起始大于结束（通常意味着页码超出）
                emit(EmailResult.Success(emptyList())) // 返回空列表
                return@flow
            }

            println("Desktop: Fetching messages from $actualStart to $actualEnd (reversed for latest first)")
            // 获取指定范围的邮件，通常服务器返回的是按消息号升序
            // 我们需要从后往前取最新的
            val messages = folder.getMessages(actualStart, actualEnd)

            // --- 预取邮件头信息 (优化性能) ---
            val fetchProfile = FetchProfile()
            fetchProfile.add(FetchProfile.Item.ENVELOPE) // 包含 From, To, Subject, Date 等
            fetchProfile.add(FetchProfile.Item.FLAGS)     // 包含已读/未读等标记
            fetchProfile.add(UIDFolder.FetchProfileItem.UID) // 获取 UID，非常重要
            folder.fetch(messages, fetchProfile)

            val emailList = mutableListOf<EmailMessage>()
            // 遍历时从后往前，这样最新的邮件在列表前面
            for (i in messages.indices.reversed()) {
                val msg = messages[i]
                val fromAddresses = msg.from?.mapNotNull { (it as? InternetAddress)?.toString() }?.joinToString()
                val subject = msg.subject
                val sentDateJava = msg.sentDate
                val receivedDateJava = msg.receivedDate // 有些服务器可能不完全支持或返回null

                // UIDFolder 接口用于获取 UID
                val uid = if (folder is UIDFolder) {
                    folder.getUID(msg).toString()
                } else {
                    msg.messageNumber.toString() // 备选，但 UID 更可靠
                }
                val sentDateTime: LocalDateTime? = sentDateJava?.toInstant()?.toKotlinInstant()?.toLocalDateTime(TimeZone.currentSystemDefault())
                val receivedDateTime = receivedDateJava?.toInstant()?.toKotlinInstant()?.toLocalDateTime(TimeZone.currentSystemDefault())

                emailList.add(
                    EmailMessage(
                        messageServerId = uid,
                        folderName = folderName,
                        subject = subject,
                        fromAddress = fromAddresses,
                        toList = msg.getRecipients(Message.RecipientType.TO)?.mapNotNull { (it as? InternetAddress)?.toString() },
                        ccList = msg.getRecipients(Message.RecipientType.CC)?.mapNotNull { (it as? InternetAddress)?.toString() },
                        bccList = null, // 通常不获取 BCC 用于显示
                        bodyPlainText = null, // 列表视图不加载正文
                        bodyHtml = null,      // 列表视图不加载正文
                        sentDate = sentDateTime,
                        receivedDate = receivedDateTime,
                        isRead = msg.flags.contains(Flags.Flag.SEEN),
                        hasAttachments = msg.contentType?.contains("multipart", ignoreCase = true) ?: false, // 简单判断
                        attachments = null, // 列表视图不加载附件详情
                    )
                )
            }
            emit(EmailResult.Success(emailList))

        } catch (e: AuthenticationFailedException) {
            emit(EmailResult.Error(e, "IMAP认证失败: ${e.message}"))
        } catch (e: FolderNotFoundException) {
            emit(EmailResult.Error(e, "文件夹 '$folderName' 未找到: ${e.message}"))
        } catch (e: MessagingException) {
            emit(EmailResult.Error(e, "获取邮件时发生消息错误: ${e.message}"))
        } catch (e: Exception) {
            emit(EmailResult.Error(e, "获取邮件时发生未知错误: ${e.message}"))
        } finally {
            try {
                folder?.close(false) // false 表示不 expunge (永久删除标记为 \Deleted 的邮件)
                store?.close()
                println("Desktop: IMAP resources closed for fetchEmails.")
            } catch (e: MessagingException) {
                // Log or handle closing error
                println("Desktop: Error closing IMAP resources: ${e.message}")
            }
        }
    }

    override suspend fun fetchEmailDetails(
        accountInfo: AccountInfo,
        folderName: String,
        messageServerId: String // 通常是 UID
    ): EmailResult<EmailMessage> = withContext(ioDispatcher) {
        var store: Store? = null
        var folder: Folder? = null
        try {
            val imapProtocol = if (accountInfo.imapUseSsl && accountInfo.imapPort == 993) "imaps" else "imap"
            val imapProps = createMailProperties(accountInfo.imapHost, accountInfo.imapPort, accountInfo.imapUseSsl, "imap")
            val imapSession = Session.getInstance(imapProps)
            // imapSession.debug = true

            store = imapSession.getStore(imapProtocol)
            println("Desktop: Connecting to IMAP for fetchEmailDetails: ${accountInfo.imapHost}")
            store.connect(
                accountInfo.imapHost,
                accountInfo.imapPort,
                accountInfo.emailAddress,
                accountInfo.passwordOrAuthCode
            )
            println("Desktop: IMAP connected for fetchEmailDetails.")

            folder = store.getFolder(folderName)
            if (folder == null || !folder.exists()) {
                return@withContext EmailResult.Error(MessagingException("Folder '$folderName' not found for details."))
            }

            folder.open(Folder.READ_ONLY)
            println("Desktop: Opened folder '$folderName' for details. Searching for UID: $messageServerId")

            val msg: Message? = if (folder is UIDFolder) {
                try {
                    val uid = messageServerId.toLong() // UID 通常是 Long
                    folder.getMessageByUID(uid)
                } catch (e: NumberFormatException) {
                    println("Desktop: Invalid UID format: $messageServerId")
                    null // 如果 UID 格式不正确
                }
            } else {
                // 如果不支持 UIDFolder，尝试通过消息号查找（不太可靠，且 messageServerId 可能不是消息号）
                // 为了简单，如果不是UIDFolder，我们这里可能直接返回找不到
                println("Desktop: Folder does not support UIDFolder. Cannot reliably fetch by server ID.")
                null
            }

            if (msg == null) {
                return@withContext EmailResult.Error(MessagingException("Email with server ID '$messageServerId' not found in folder '$folderName'."))
            }

            // --- 获取基本信息 (很多可能在 fetchEmails 时已经获取，但这里可以重新获取以确保最新) ---
            val fromAddresses = msg.from?.mapNotNull { (it as? InternetAddress)?.toString() }?.joinToString()
            val subject = msg.subject
            val sentDateJava = msg.sentDate
            val receivedDateJava = msg.receivedDate

            val sentDateTime = sentDateJava?.toInstant()?.toKotlinInstant()?.toLocalDateTime(TimeZone.currentSystemDefault())
            val receivedDateTime = receivedDateJava?.toInstant()?.toKotlinInstant()?.toLocalDateTime(TimeZone.currentSystemDefault())

            var bodyPlainText: String? = null
            var bodyHtml: String? = null
            val attachmentsList = mutableListOf<com.lonelymeko.myemail.data.model.EmailAttachmentInfo>() // 使用你的数据模型
            var hasAttachments = false

            // --- 解析邮件内容 (正文和附件) ---
            val content = msg.content
            if (content is String) { // 最简单的情况，纯文本邮件
                bodyPlainText = content
            } else if (content is MimeMultipart) {
                val multipart = content
                hasAttachments = true // 假设 multipart 通常意味着有附件或多种格式
                for (i in 0 until multipart.count) {
                    val bodyPart = multipart.getBodyPart(i) as MimeBodyPart // 类型转换为 MimeBodyPart 以访问更多方法

                    // 判断是正文部分还是附件部分
                    val disposition = bodyPart.disposition
                    if (disposition != null && (disposition.equals(Part.ATTACHMENT, ignoreCase = true) || disposition.equals(Part.INLINE, ignoreCase = true))) {
                        // 这是附件或内嵌资源
                        attachmentsList.add(
                            com.lonelymeko.myemail.data.model.EmailAttachmentInfo(
                                fileName = MimeUtility.decodeText(bodyPart.fileName ?: "unknown_attachment"), // 解码文件名
                                contentType = bodyPart.contentType.substringBefore(";"), // 获取主 MIME 类型
                                sizeBytes = bodyPart.size.toLong() // 注意：size 可能返回 -1
                            )
                        )
                    } else if (bodyPart.isMimeType("text/plain") && bodyPlainText == null) { // 优先取第一个 text/plain
                        bodyPlainText = bodyPart.content as? String
                    } else if (bodyPart.isMimeType("text/html") && bodyHtml == null) { // 优先取第一个 text/html
                        bodyHtml = bodyPart.content as? String
                    }
                    // TODO: 处理更复杂的 multipart/related (内嵌图片) 和 multipart/mixed
                }
            }
            // 如果只有 HTML 而没有 plain text，可以考虑从 HTML 生成一个粗略的 plain text 版本
            if (bodyPlainText == null && bodyHtml != null) {
                // 简单的HTML转Text (可以使用库或者基础的正则替换)
                // bodyPlainText = HtmlCompat.fromHtml(bodyHtml, HtmlCompat.FROM_HTML_MODE_LEGACY).toString() // Android 示例
                // 对于桌面，可能需要一个简单的HTML解析库或手动去除标签
                bodyPlainText = bodyHtml.replace(Regex("<[^>]*>"), "") // 非常粗略的去标签
            }


            val emailMessage = EmailMessage(
                messageServerId = messageServerId, // 使用传入的 ID
                folderName = folderName,
                subject = subject,
                fromAddress = fromAddresses,
                toList = msg.getRecipients(Message.RecipientType.TO)?.mapNotNull { (it as? InternetAddress)?.toString() },
                ccList = msg.getRecipients(Message.RecipientType.CC)?.mapNotNull { (it as? InternetAddress)?.toString() },
                bccList = null,
                bodyPlainText = bodyPlainText,
                bodyHtml = bodyHtml,
                sentDate = sentDateTime,
                receivedDate = receivedDateTime,
                isRead = msg.flags.contains(Flags.Flag.SEEN),
                hasAttachments = attachmentsList.isNotEmpty() || hasAttachments, // 更新 hasAttachments 标志
                attachments = if (attachmentsList.isNotEmpty()) attachmentsList else null,
            )
            EmailResult.Success(emailMessage)

        } catch (e: AuthenticationFailedException) {
            EmailResult.Error(e, "IMAP认证失败: ${e.message}")
        } catch (e: FolderNotFoundException) {
            EmailResult.Error(e, "文件夹 '$folderName' 未找到: ${e.message}")
        } catch (e: MessagingException) {
            EmailResult.Error(e, "获取邮件详情时发生消息错误: ${e.message}")
        } catch (e: Exception) {
            EmailResult.Error(e, "获取邮件详情时发生未知错误: ${e.message}")
        } finally {
            try {
                folder?.close(false)
                store?.close()
                println("Desktop: IMAP resources closed for fetchEmailDetails.")
            } catch (e: MessagingException) {
                println("Desktop: Error closing IMAP resources for details: ${e.message}")
            }
        }
    }
    override suspend fun markEmailFlags(
        accountInfo: AccountInfo,
        folderName: String,
        messageServerIds: List<String>, // 通常是 UID 列表
        markAsRead: Boolean? // true for read, false for unread, null to not change
        // markAsStarred: Boolean? // 示例：将来可扩展
    ): EmailResult<Unit> = withContext(ioDispatcher) {
        if (messageServerIds.isEmpty()) {
            return@withContext EmailResult.Success(Unit) // 没有要操作的邮件
        }
        if (markAsRead == null /* && markAsStarred == null */) { // 如果所有标记参数都为null，则无操作
            return@withContext EmailResult.Success(Unit)
        }

        var store: Store? = null
        var folder: Folder? = null
        try {
            val imapProtocol = if (accountInfo.imapUseSsl && accountInfo.imapPort == 993) "imaps" else "imap"
            val imapProps = createMailProperties(accountInfo.imapHost, accountInfo.imapPort, accountInfo.imapUseSsl, "imap")
            val imapSession = Session.getInstance(imapProps)
            // imapSession.debug = true

            store = imapSession.getStore(imapProtocol)
            println("Desktop: Connecting to IMAP for markEmailFlags: ${accountInfo.imapHost}")
            store.connect(
                accountInfo.imapHost,
                accountInfo.imapPort,
                accountInfo.emailAddress,
                accountInfo.passwordOrAuthCode
            )
            println("Desktop: IMAP connected for markEmailFlags.")

            folder = store.getFolder(folderName)
            if (folder == null || !folder.exists()) {
                return@withContext EmailResult.Error(MessagingException("Folder '$folderName' not found for marking flags."))
            }

            // 打开文件夹为读写模式，因为我们要修改标记
            folder.open(Folder.READ_WRITE)
            println("Desktop: Opened folder '$folderName' in READ_WRITE mode for marking flags.")

            val uidsToProcess = messageServerIds.mapNotNull { it.toLongOrNull() }.toLongArray()
            if (uidsToProcess.isEmpty() && messageServerIds.isNotEmpty()) {
                // 如果传入的 IDs 都不是有效的 Long (UID)，则报错或忽略
                println("Desktop: No valid UIDs provided for marking flags.")
                // 可以选择返回错误或成功（因为没有有效操作）
                // return@withContext EmailResult.Error(IllegalArgumentException("Invalid UIDs provided."))
            }

            if (uidsToProcess.isNotEmpty() && folder is UIDFolder) {
                val messages = folder.getMessagesByUID(uidsToProcess)
                if (messages.isEmpty()) {
                    println("Desktop: No messages found for UIDs: ${uidsToProcess.joinToString()}")
                    // 可能部分 UID 无效，或邮件已被删除
                }

                val flagsToSet = Flags()
                val flagsToClear = Flags()

                markAsRead?.let { read ->
                    if (read) {
                        flagsToSet.add(Flags.Flag.SEEN)
                    } else {
                        flagsToClear.add(Flags.Flag.SEEN)
                    }
                }
                // markAsStarred?.let { starred ->
                //     if (starred) {
                //         flagsToSet.add(Flags.Flag.FLAGGED)
                //     } else {
                //         flagsToClear.add(Flags.Flag.FLAGGED)
                //     }
                // }

                if (messages.isNotEmpty()) {
                    // 设置标记 (true 表示设置，false 表示清除)
                    // folder.setFlags(messages, flagsToSet, true) // 会添加标记，不清除已有的其他标记
                    // folder.setFlags(messages, flagsToClear, false) // 会清除标记

//                    // 更精细的控制：合并 flags
//                    for (msg in messages) {
//                        val currentFlags = msg.flags
//                        val newFlags = Flags(currentFlags) // 复制当前标记
//                        if (flagsToSet.contains(Flags.Flag.SEEN)) newFlags.add(Flags.Flag.SEEN)
//                        if (flagsToClear.contains(Flags.Flag.SEEN)) newFlags.remove(Flags.Flag.SEEN)
//                        // ... 其他标记类似处理 ...
//                        if (!newFlags.equals(currentFlags)) { // 只有当标记实际改变时才设置
//                            msg.flags = newFlags
//                        }
//                    }
                    // 或者，如果只想简单地设置/清除特定标记而不关心其他标记：
                     if (flagsToSet.systemFlags.isNotEmpty()) { // 检查是否有要设置的系统标记
                        folder.setFlags(messages, flagsToSet, true)
                     }
                     if (flagsToClear.systemFlags.isNotEmpty()) {
                        folder.setFlags(messages, flagsToClear, false)
                     }
                    // **注意**: setFlags(messages, Flags, boolean) 会替换所有匹配的标记，
                    // 如果只想添加/移除特定标记而不影响其他标记，需要更小心。
                    // 通常的做法是获取现有标记，修改，然后设置回去，如上面的 for 循环。
                    // 或者，对于 SEEN 标记，更简单的方式可能是：
                    // folder.setFlags(messages, Flags(Flags.Flag.SEEN), markAsRead /* true to set, false to clear */)
                    // 但这只适用于单个标记的简单设置/清除。

                    println("Desktop: Flags updated for ${messages.size} messages.")
                }

            } else if (uidsToProcess.isNotEmpty()) {
                // 如果文件夹不支持 UIDFolder，则无法通过 UID 操作，这是一个问题
                return@withContext EmailResult.Error(MessagingException("Folder does not support UID operations."))
            }

            EmailResult.Success(Unit)

        } catch (e: AuthenticationFailedException) {
            EmailResult.Error(e, "IMAP认证失败: ${e.message}")
        } catch (e: FolderNotFoundException) {
            EmailResult.Error(e, "文件夹 '$folderName' 未找到: ${e.message}")
        } catch (e: ReadOnlyFolderException) {
            EmailResult.Error(e, "文件夹 '$folderName' 是只读的，无法修改标记: ${e.message}")
        } catch (e: MessagingException) {
            EmailResult.Error(e, "标记邮件时发生消息错误: ${e.message}")
        } catch (e: Exception) {
            EmailResult.Error(e, "标记邮件时发生未知错误: ${e.message}")
        } finally {
            try {
                // 关闭文件夹时，如果以 READ_WRITE 打开，第二个参数 true 会 expunge 已删除邮件
                // 这里我们只是修改标记，所以用 false
                folder?.close(false)
                store?.close()
                println("Desktop: IMAP resources closed for markEmailFlags.")
            } catch (e: MessagingException) {
                println("Desktop: Error closing IMAP resources for flags: ${e.message}")
            }
        }
    }
    override suspend fun deleteEmails(
        accountInfo: AccountInfo,
        folderName: String,
        messageServerIds: List<String> // UID 列表
    ): EmailResult<Unit> = withContext(ioDispatcher) {
        if (messageServerIds.isEmpty()) {
            return@withContext EmailResult.Success(Unit) // 没有要操作的邮件
        }

        var store: Store? = null
        var folder: Folder? = null
        // 控制是否在关闭文件夹时执行 expunge (永久删除)
        // 对于“删除”操作，通常我们希望立即 expunge 或让服务器处理（如移到垃圾箱）
        val expungeOnClose = true

        try {
            val imapProtocol = if (accountInfo.imapUseSsl && accountInfo.imapPort == 993) "imaps" else "imap"
            val imapProps = createMailProperties(accountInfo.imapHost, accountInfo.imapPort, accountInfo.imapUseSsl, "imap")
            val imapSession = Session.getInstance(imapProps)
            // imapSession.setDebug(true)

            store = imapSession.getStore(imapProtocol)
            println("Desktop: Connecting to IMAP for deleteEmails: ${accountInfo.imapHost}")
            store.connect(
                accountInfo.imapHost,
                accountInfo.imapPort,
                accountInfo.emailAddress,
                accountInfo.passwordOrAuthCode
            )
            println("Desktop: IMAP connected for deleteEmails.")

            folder = store.getFolder(folderName)
            if (folder == null || !folder.exists()) {
                return@withContext EmailResult.Error(FolderNotFoundException(folder,"Folder '$folderName' not found for deleting emails."))
            }

            // 需要读写权限来设置 DELETED 标记并可能执行 expunge
            folder.open(Folder.READ_WRITE)
            println("Desktop: Opened folder '$folderName' in READ_WRITE mode for deleting emails.")

            val uidsToProcess = messageServerIds.mapNotNull { it.toLongOrNull() }.toLongArray()
            if (uidsToProcess.isEmpty() && messageServerIds.isNotEmpty()) {
                println("Desktop: No valid UIDs provided for deleting emails.")
                // 可以选择返回错误或成功（因为没有有效操作）
            }

            if (uidsToProcess.isNotEmpty() && folder is UIDFolder) {
                val messages = folder.getMessagesByUID(uidsToProcess)
                if (messages.isNotEmpty()) {
                    // 将邮件标记为 \Deleted
                    folder.setFlags(messages, Flags(Flags.Flag.DELETED), true)
                    println("Desktop: Marked ${messages.size} messages as DELETED in folder '$folderName'.")

                    // 如果不希望依赖 close(true) 来 expunge，可以在这里显式调用
                    // folder.expunge()
                    // println("Desktop: Expunged messages from folder '$folderName'.")
                    // 注意: expunge() 会移除所有标记为 DELETED 的邮件，不仅仅是我们刚标记的这些

                } else {
                    println("Desktop: No messages found for UIDs to delete in folder '$folderName': ${uidsToProcess.joinToString()}")
                }
            } else if (uidsToProcess.isNotEmpty()) {
                // 文件夹不支持 UIDFolder，这是一个问题，因为 messageServerIds 预期是 UIDs
                return@withContext EmailResult.Error(MessagingException("Folder '$folderName' does not support UID operations for deletion."))
            }

            EmailResult.Success(Unit)

        } catch (e: AuthenticationFailedException) {
            EmailResult.Error(e, "IMAP认证失败: ${e.message}")
        } catch (e: FolderNotFoundException) {
            EmailResult.Error(e, "文件夹 '$folderName' 未找到: ${e.message}")
        } catch (e: ReadOnlyFolderException) {
            EmailResult.Error(e, "文件夹 '$folderName' 是只读的，无法删除邮件: ${e.message}")
        } catch (e: MessagingException) {
            EmailResult.Error(e, "删除邮件时发生消息错误: ${e.message}")
        } catch (e: Exception) {
            EmailResult.Error(e, "删除邮件时发生未知错误: ${e.message}")
        } finally {
            try {
                // 当关闭文件夹时，如果 expungeOnClose 为 true,
                // 服务器会永久删除所有在该会话中被标记为 \Deleted 且在该文件夹中的邮件。
                // 某些服务器可能会将这些邮件移动到“已删除邮件”或“垃圾箱”文件夹。
                folder?.close(expungeOnClose)
                store?.close()
                println("Desktop: IMAP resources closed for deleteEmails. Expunged on close: $expungeOnClose for folder '$folderName'")
            } catch (e: MessagingException) {
                println("Desktop: Error closing IMAP resources for delete: ${e.message}")
            }
        }
    }
}

// --- 测试用的 main 函数 ---
// 将其放在 DesktopEmailServiceImpl.kt 文件的末尾，或者一个单独的测试文件中
fun main() = runBlocking {
    // !!! 替换为你的真实邮箱和授权码进行测试 !!!
    // !!! 不要将真实凭据提交到版本控制系统 !!!
    val myTestAccount = AccountInfo(
        emailAddress = "2477183238@qq.com",       // 替换
        passwordOrAuthCode = "xpifijkejiirdiaf",           // 替换
        imapHost = "imap.qq.com",                   // 替换
        imapPort = 993,
        imapUseSsl = true,
        smtpHost = "smtp.qq.com",                   // 替换
        smtpPort = 465, // or 587 for STARTTLS
        smtpUseSsl = true, // if port is 587 and server uses STARTTLS, this might be false, and props configure STARTTLS
        accountType = AccountType.QQ,     // 或你的邮箱类型
        displayName = "Desktop Test Sender"
    )

    val emailService = DesktopEmailServiceImpl() // 直接实例化进行测试

    println("--- Testing Connection (Desktop) ---")
    val connectionResult = emailService.testConnection(myTestAccount)
    when (connectionResult) {
        is EmailResult.Success -> println("Connection Test: SUCCESS")
        is EmailResult.Error -> println("Connection Test: FAILED - ${connectionResult.message} (Exception: ${connectionResult.exception})")
    }
    println("----------------------------------\n")
//
//    if (connectionResult is EmailResult.Success) {
//        println("--- Testing Send Email (Desktop) ---")
//        val emailToSend = EmailMessage(
//            messageServerId = "", // 发送时不需要
//            folderName = "",      // 发送时不需要
//            subject = "Desktop KMP邮件客户端测试 (中文主题)",
//            fromAddress = myTestAccount.emailAddress, // 会被 setFrom 覆盖
//            toList = listOf("2477183238@qq.com"), // 替换为真实收件人
//            ccList = null,
//            bccList = null,
//            bodyPlainText = "这是一封来自 Desktop Kotlin Multiplatform 邮件客户端的纯文本测试邮件。",
//            bodyHtml = "<h1>Desktop KMP邮件客户端测试</h1><p>这是一封来自 <b>Desktop Kotlin Multiplatform</b> 邮件客户端的 <i>HTML</i> 测试邮件。</p>",
//            sentDate = null, // 会自动设置
//            receivedDate = null,
//            isRead = false,
//            hasAttachments = false,
//            attachments = null,
//        )
//
//        val sendResult = emailService.sendEmail(myTestAccount, emailToSend)
//        when (sendResult) {
//            is EmailResult.Success -> println("Send Email Test: SUCCESS")
//            is EmailResult.Error -> println("Send Email Test: FAILED - ${sendResult.message} (Exception: ${sendResult.exception})")
//        }
//        println("--------------------------------\n")
//        println("--- Testing Fetch Emails (Desktop) INBOX ---")
//        if (connectionResult is EmailResult.Success) { // 确保连接是成功的
//            emailService.fetchEmails(myTestAccount, "INBOX", page = 1, pageSize = 5) // 获取收件箱第一页，最多5封
//                .collect { result ->
//                    when (result) {
//                        is EmailResult.Success -> {
//                            println("Fetch Emails (INBOX): SUCCESS, Got ${result.data.size} emails.")
//                            result.data.forEachIndexed { index, email ->
//                                println("  Email ${index + 1}: UID=${email.messageServerId}, From='${email.fromAddress}', Subject='${email.subject}', Sent='${email.sentDate}', Read=${email.isRead}")
//                            }
//                        }
//                        is EmailResult.Error -> {
//                            println("Fetch Emails (INBOX): FAILED - ${result.message}")
//                            result.exception.printStackTrace()
//                        }
//                    }
//                }
//        }
        println("------------------------------------------\n")
        println("--- Testing Fetch Emails (Desktop) INBOX for Details ---")
        var firstEmailUid: String? = null
        if (connectionResult is EmailResult.Success) { // 假设 connectionResult 是之前 testConnection 的结果
            emailService.fetchEmails(myTestAccount, "INBOX", page = 1, pageSize = 1) // 获取第一封邮件的摘要
                .collect { result ->
                    when (result) {
                        is EmailResult.Success -> {
                            if (result.data.isNotEmpty()) {
                                firstEmailUid = result.data.first().messageServerId
                                println("Fetch Emails for Details: SUCCESS, First email UID: $firstEmailUid")
                            } else {
                                println("No emails found in INBOX to fetch details for.")
                            }
                        }
                        is EmailResult.Error -> println("Fetch Emails for Details: FAILED - ${result.message}")
                    }
                }
        }

        if (firstEmailUid != null) {
            println("\n--- Testing Fetch Email Details (Desktop) ---")
            val detailsResult = emailService.fetchEmailDetails(myTestAccount, "INBOX", firstEmailUid!!)
            when (detailsResult) {
                is EmailResult.Success -> {
                    val email = detailsResult.data
                    println("Fetch Email Details: SUCCESS")
                    println("  UID: ${email.messageServerId}")
                    println("  Subject: ${email.subject}")
                    println("  From: ${email.fromAddress}")
                    println("  Sent Date: ${email.sentDate}")
                    println("  Is Read: ${email.isRead}")
                    println("  Has Attachments: ${email.hasAttachments}")
                    email.attachments?.let {
                        println("  Attachments (${it.size}):")
                        it.forEach { att -> println("    - ${att.fileName} (${att.contentType}, ${att.sizeBytes} bytes)") }
                    }
                    println("  Body HTML (first 100 chars): ${email.bodyHtml?.take(100)}...")
                    println("  Body Plain Text (first 100 chars): ${email.bodyPlainText?.take(100)}...")
                }
                is EmailResult.Error -> {
                    println("Fetch Email Details: FAILED - ${detailsResult.message} (Exception: ${detailsResult.exception})")
                    detailsResult.exception.printStackTrace()
                }
            }
            println("-----------------------------------------\n")
            var uidsToMark: List<String>? = null
            if (connectionResult is EmailResult.Success) {
                println("--- Testing Fetch Emails to get UIDs for Marking ---")
                emailService.fetchEmails(myTestAccount, "INBOX", page = 1, pageSize = 2) // 获取前2封
                    .collect { result ->
                        when (result) {
                            is EmailResult.Success -> {
                                if (result.data.isNotEmpty()) {
                                    uidsToMark = result.data.map { it.messageServerId }
                                    println("Fetch for Marking: SUCCESS, UIDs to mark: $uidsToMark")
                                } else {
                                    println("No emails found in INBOX to mark.")
                                }
                            }
                            is EmailResult.Error -> println("Fetch for Marking: FAILED - ${result.message}")
                        }
                    }
                println("------------------------------------------------\n")
            }

            if (uidsToMark != null && uidsToMark!!.isNotEmpty()) {
                println("--- Testing Mark Email As Read (Desktop) ---")
                // 假设我们将第一封标记为已读，第二封标记为未读 (如果存在第二封)
                val markReadResult = emailService.markEmailFlags(myTestAccount, "INBOX", listOf(uidsToMark!!.first()), markAsRead = true)
                when (markReadResult) {
                    is EmailResult.Success -> println("Mark as Read Test: SUCCESS for ${uidsToMark!!.first()}")
                    is EmailResult.Error -> println("Mark as Read Test: FAILED - ${markReadResult.message}")
                }

                if (uidsToMark!!.size > 1) {
                    println("\n--- Testing Mark Email As Unread (Desktop) ---")
                    val markUnreadResult = emailService.markEmailFlags(myTestAccount, "INBOX", listOf(uidsToMark!![1]), markAsRead = false)
                    when (markUnreadResult) {
                        is EmailResult.Success -> println("Mark as Unread Test: SUCCESS for ${uidsToMark!![1]}")
                        is EmailResult.Error -> println("Mark as Unread Test: FAILED - ${markUnreadResult.message}")
                    }
                }
                println("------------------------------------------\n")
                println("!!! PLEASE CHECK YOUR EMAIL CLIENT TO VERIFY THE READ/UNREAD STATUS !!!")
            }
        }
    println("----------------删除测试------------------\n")

    // --- 获取一些邮件的UID用于删除测试 ---
    var uidsToDelete: List<String>? = null
    println("--- Testing Fetch Emails to get UIDs for Deletion (Desktop) ---")
    emailService.fetchEmails(myTestAccount, "INBOX", page = 1, pageSize = 2) // 获取最新的两封
        .collect { result ->
            when (result) {
                is EmailResult.Success -> {
                    if (result.data.isNotEmpty()) {
                        uidsToDelete = result.data.map { it.messageServerId }
                        println("Fetch for Deletion: SUCCESS, UIDs found: $uidsToDelete")
                    } else {
                        println("No emails found in INBOX to select for deletion.")
                    }
                }
                is EmailResult.Error -> println("Fetch for Deletion: FAILED - ${result.message}")
            }
        }
    println("----------------------------------------------------------\n")


    if (uidsToDelete != null && uidsToDelete!!.isNotEmpty()) {
        val uidsToActuallyDelete = listOf(uidsToDelete!!.first()) // 只删除获取到的第一封，以减少风险

        println("--- Testing Delete Emails (Desktop) ---")
        println("WARNING: About to mark emails for deletion (UIDs: $uidsToActuallyDelete) from INBOX.")
        println("         These emails might be permanently deleted or moved to Trash depending on server and expunge settings.")
        println("         Press Enter to continue, or Ctrl+C to abort...")
        // readlnOrNull() // 在实际测试中，可以取消注释这一行以进行手动确认

        val deleteResult = emailService.deleteEmails(myTestAccount, "INBOX", uidsToActuallyDelete)
        when (deleteResult) {
            is EmailResult.Success -> println("Delete Emails Test: SUCCESS for UIDs: $uidsToActuallyDelete")
            is EmailResult.Error -> {
                println("Delete Emails Test: FAILED - ${deleteResult.message} (Exception: ${deleteResult.exception})")
                deleteResult.exception.printStackTrace()
            }
        }
        println("-------------------------------------\n")
        println("!!! CHECK YOUR EMAIL CLIENT OR WEBPAGE (INBOX and TRASH/DELETED ITEMS) TO VERIFY THE DELETION !!!")
    } else {
        println("No UIDs selected for deletion test.")
    }
}




