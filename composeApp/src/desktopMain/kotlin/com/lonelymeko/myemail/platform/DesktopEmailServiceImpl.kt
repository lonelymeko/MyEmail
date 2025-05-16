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

    override suspend fun fetchEmailDetails(accountInfo: AccountInfo, folderName: String, messageServerId: String): EmailResult<EmailMessage> {
        val errorMessage = "fetchEmailDetails not implemented on Desktop"
        println("DesktopEmailServiceImpl: $errorMessage")
        return EmailResult.Error(NotImplementedException(errorMessage), errorMessage)
    }
    override suspend fun markEmailFlags(accountInfo: AccountInfo, folderName: String, messageServerIds: List<String>, markAsRead: Boolean?): EmailResult<Unit> {
        val errorMessage = "markEmailFlags not implemented on Desktop"
        println("DesktopEmailServiceImpl: $errorMessage")
        return EmailResult.Error(NotImplementedException(errorMessage), errorMessage)
    }
    override suspend fun deleteEmails(accountInfo: AccountInfo, folderName: String, messageServerIds: List<String>): EmailResult<Unit> {
        val errorMessage = "deleteEmails not implemented on Desktop"
        println("DesktopEmailServiceImpl: $errorMessage")
        return EmailResult.Error(NotImplementedException(errorMessage), errorMessage)
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

    if (connectionResult is EmailResult.Success) {
        println("--- Testing Send Email (Desktop) ---")
        val emailToSend = EmailMessage(
            messageServerId = "", // 发送时不需要
            folderName = "",      // 发送时不需要
            subject = "Desktop KMP邮件客户端测试 (中文主题)",
            fromAddress = myTestAccount.emailAddress, // 会被 setFrom 覆盖
            toList = listOf("2477183238@qq.com"), // 替换为真实收件人
            ccList = null,
            bccList = null,
            bodyPlainText = "这是一封来自 Desktop Kotlin Multiplatform 邮件客户端的纯文本测试邮件。",
            bodyHtml = "<h1>Desktop KMP邮件客户端测试</h1><p>这是一封来自 <b>Desktop Kotlin Multiplatform</b> 邮件客户端的 <i>HTML</i> 测试邮件。</p>",
            sentDate = null, // 会自动设置
            receivedDate = null,
            isRead = false,
            hasAttachments = false,
            attachments = null,
        )

        val sendResult = emailService.sendEmail(myTestAccount, emailToSend)
        when (sendResult) {
            is EmailResult.Success -> println("Send Email Test: SUCCESS")
            is EmailResult.Error -> println("Send Email Test: FAILED - ${sendResult.message} (Exception: ${sendResult.exception})")
        }
        println("--------------------------------\n")
        println("--- Testing Fetch Emails (Desktop) INBOX ---")
        if (connectionResult is EmailResult.Success) { // 确保连接是成功的
            emailService.fetchEmails(myTestAccount, "INBOX", page = 1, pageSize = 5) // 获取收件箱第一页，最多5封
                .collect { result ->
                    when (result) {
                        is EmailResult.Success -> {
                            println("Fetch Emails (INBOX): SUCCESS, Got ${result.data.size} emails.")
                            result.data.forEachIndexed { index, email ->
                                println("  Email ${index + 1}: UID=${email.messageServerId}, From='${email.fromAddress}', Subject='${email.subject}', Sent='${email.sentDate}', Read=${email.isRead}")
                            }
                        }
                        is EmailResult.Error -> {
                            println("Fetch Emails (INBOX): FAILED - ${result.message}")
                            result.exception.printStackTrace()
                        }
                    }
                }
        }
        println("------------------------------------------\n")

    }
}