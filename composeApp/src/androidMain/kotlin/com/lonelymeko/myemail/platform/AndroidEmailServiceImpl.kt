package com.lonelymeko.myemail.platform


// Android 实现通常使用 javax.mail.* 包名，因为 com.sun.mail:android-mail 库是这样打包的
import android.os.Build
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeUtility
// import javax.activation.* // 如果需要 DataHandler 等

import com.lonelymeko.myemail.common.exception.NotImplementedException
import com.lonelymeko.myemail.data.model.AccountInfo
import com.lonelymeko.myemail.data.model.EmailMessage
import com.lonelymeko.myemail.data.remote.api.EmailResult
import com.lonelymeko.myemail.data.remote.api.EmailService // 导入 commonMain 中的 interface
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import java.io.IOException // 用于附件流
import java.util.Date
import java.util.Properties
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toKotlinInstant
import kotlinx.datetime.toLocalDateTime
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMultipart


class AndroidEmailServiceImpl(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    // private val context: android.content.Context // 如果需要 Context，可以通过 Koin 注入
) : EmailService {

    // createMailProperties 方法与 Desktop 版本几乎完全相同
    private fun createMailProperties(host: String, port: Int, useSsl: Boolean, protocolPrefix: String): Properties {
        return Properties().apply {
            put("mail.$protocolPrefix.host", host)
            put("mail.$protocolPrefix.port", port.toString())
            put("mail.$protocolPrefix.auth", "true")

            if (useSsl) {
                if (protocolPrefix == "smtp" && (port == 587 || port == 25)) {
                    put("mail.smtp.starttls.enable", "true")
                    // 对于Android，信任所有主机通常通过自定义SSLSocketFactory或网络安全配置完成，
                    // 而不是直接设置 "mail.smtp.ssl.trust"。
                } else {
                    put("mail.$protocolPrefix.socketFactory.port", port.toString())
                    // android-mail 通常仍然使用 "javax.net.ssl.SSLSocketFactory"
                    put("mail.$protocolPrefix.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                    put("mail.$protocolPrefix.ssl.enable", "true")
                }
            } else {
                if (protocolPrefix == "smtp") {
                    put("mail.smtp.starttls.enable", "true")
                }
            }
            // put("mail.debug", "true")
        }
    }

    // testConnection 实现与 Desktop 版本相同
    override suspend fun testConnection(accountInfo: AccountInfo): EmailResult<Unit> = withContext(ioDispatcher) {
        // IMAP Test
        try {
            val imapProtocol = if (accountInfo.imapUseSsl && accountInfo.imapPort == 993) "imaps" else "imap"
            val imapProps = createMailProperties(accountInfo.imapHost, accountInfo.imapPort, accountInfo.imapUseSsl, "imap")
            val imapSession = Session.getInstance(imapProps)
            // imapSession.debug = true
            println("Android: Attempting IMAP ($imapProtocol) to ${accountInfo.imapHost}:${accountInfo.imapPort}")
            val store = imapSession.getStore(imapProtocol)
            store.connect(accountInfo.imapHost, accountInfo.imapPort, accountInfo.emailAddress, accountInfo.passwordOrAuthCode)
            store.close()
            println("Android: IMAP connection successful for ${accountInfo.emailAddress}")
        } catch (e: AuthenticationFailedException) { return@withContext EmailResult.Error(e, "IMAP认证失败: ${e.message}") }
        catch (e: MessagingException) { return@withContext EmailResult.Error(e, "IMAP连接失败: ${e.message}") }
        catch (e: Exception) { return@withContext EmailResult.Error(e, "IMAP连接时发生未知错误: ${e.message}") }

        // SMTP Test
        try {
            val smtpProtocol = if (accountInfo.smtpUseSsl && accountInfo.smtpPort == 465) "smtps" else "smtp"
            val smtpProps = createMailProperties(accountInfo.smtpHost, accountInfo.smtpPort, accountInfo.smtpUseSsl, "smtp")
            val smtpSession = Session.getInstance(smtpProps, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication { return PasswordAuthentication(accountInfo.emailAddress, accountInfo.passwordOrAuthCode) }
            })
            // smtpSession.debug = true
            println("Android: Attempting SMTP ($smtpProtocol) to ${accountInfo.smtpHost}:${accountInfo.smtpPort}")
            val transport = smtpSession.getTransport(smtpProtocol)
            transport.connect(accountInfo.smtpHost, accountInfo.smtpPort, accountInfo.emailAddress, accountInfo.passwordOrAuthCode)
            transport.close()
            println("Android: SMTP connection successful for ${accountInfo.emailAddress}")
        } catch (e: AuthenticationFailedException) { return@withContext EmailResult.Error(e, "SMTP认证失败: ${e.message}") }
        catch (e: MessagingException) { return@withContext EmailResult.Error(e, "SMTP连接失败: ${e.message}") }
        catch (e: Exception) { return@withContext EmailResult.Error(e, "SMTP连接时发生未知错误: ${e.message}") }
        return@withContext EmailResult.Success(Unit)
    }

    // sendEmail 实现与 Desktop 版本相同
    override suspend fun sendEmail(accountInfo: AccountInfo, emailMessage: EmailMessage): EmailResult<Unit> = withContext(ioDispatcher) {
        try {
            val smtpProtocol = if (accountInfo.smtpUseSsl && accountInfo.smtpPort == 465) "smtps" else "smtp"
            val props = createMailProperties(accountInfo.smtpHost, accountInfo.smtpPort, accountInfo.smtpUseSsl, "smtp")
            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication { return PasswordAuthentication(accountInfo.emailAddress, accountInfo.passwordOrAuthCode) }
            })
            // session.debug = true
            val message = MimeMessage(session)
            message.setFrom(InternetAddress(accountInfo.emailAddress, MimeUtility.encodeText(accountInfo.displayName ?: accountInfo.emailAddress, "UTF-8", "B")))
            emailMessage.toList?.forEach { if(it.isNotBlank()) message.addRecipient(Message.RecipientType.TO, InternetAddress(it.trim())) }
            emailMessage.ccList?.forEach { if(it.isNotBlank()) message.addRecipient(Message.RecipientType.CC, InternetAddress(it.trim())) }
            emailMessage.bccList?.forEach { if(it.isNotBlank()) message.addRecipient(Message.RecipientType.BCC, InternetAddress(it.trim())) }
            message.subject = MimeUtility.encodeText(emailMessage.subject ?: "", "UTF-8", "B")
            message.sentDate = Date()
            if (!emailMessage.bodyHtml.isNullOrBlank()) { message.setContent(emailMessage.bodyHtml, "text/html; charset=utf-8") }
            else { message.setText(emailMessage.bodyPlainText ?: "", "UTF-8") }
            println("Android: Sending email via ${accountInfo.smtpHost}:${accountInfo.smtpPort} as ${accountInfo.emailAddress}")
            val transport = session.getTransport(smtpProtocol)
            transport.connect(accountInfo.smtpHost, accountInfo.smtpPort, accountInfo.emailAddress, accountInfo.passwordOrAuthCode)
            transport.sendMessage(message, message.allRecipients)
            transport.close()
            println("Android: Email sent successfully!")
            EmailResult.Success(Unit)
        } catch (e: SendFailedException) { EmailResult.Error(e, "邮件发送失败 (部分地址无效): ${e.message}") }
        catch (e: MessagingException) { EmailResult.Error(e, "邮件发送时发生消息错误: ${e.message}") }
        catch (e: Exception) { EmailResult.Error(e, "邮件发送时发生未知错误: ${e.message}") }
    }

    // fetchEmails 实现与 Desktop 版本相同
    override fun fetchEmails(
        accountInfo: AccountInfo,
        folderName: String,
        page: Int,
        pageSize: Int
    ): Flow<EmailResult<List<EmailMessage>>> = flow {
        var store: Store? = null
        var folder: Folder? = null
        try {
            val imapProtocol = if (accountInfo.imapUseSsl && accountInfo.imapPort == 993) "imaps" else "imap"
            val imapProps = createMailProperties(accountInfo.imapHost, accountInfo.imapPort, accountInfo.imapUseSsl, "imap")
            val imapSession = Session.getInstance(imapProps)
            // imapSession.debug = true
            store = imapSession.getStore(imapProtocol)
            println("Android: Connecting to IMAP for fetchEmails: ${accountInfo.imapHost}")
            store.connect(accountInfo.imapHost, accountInfo.imapPort, accountInfo.emailAddress, accountInfo.passwordOrAuthCode)
            println("Android: IMAP connected for fetchEmails.")
            folder = store.getFolder(folderName)
            if (folder == null || !folder.exists()) { emit(EmailResult.Error(MessagingException("Folder '$folderName' not found."))); return@flow }
            folder.open(Folder.READ_ONLY)
            val totalMessages = folder.messageCount
            if (totalMessages == 0) { emit(EmailResult.Success(emptyList())); return@flow }
            val startIndex = Math.max(1, totalMessages - (page * pageSize) + 1)
            val endIndex = Math.max(1, totalMessages - ((page - 1) * pageSize))
            val actualStart = if (startIndex > endIndex || startIndex > totalMessages) { if (totalMessages > 0 && pageSize > 0 && page ==1) 1 else { emit(EmailResult.Success(emptyList())); return@flow } } else startIndex
            val actualEnd = Math.min(totalMessages, endIndex)
            if (actualStart > actualEnd) { emit(EmailResult.Success(emptyList())); return@flow }
            println("Android: Fetching messages from $actualStart to $actualEnd")
            val messages = folder.getMessages(actualStart, actualEnd)
            val fetchProfile = FetchProfile()
            fetchProfile.add(FetchProfile.Item.ENVELOPE)
            fetchProfile.add(FetchProfile.Item.FLAGS)
            if (folder is UIDFolder) fetchProfile.add(UIDFolder.FetchProfileItem.UID) // 检查 UIDFolder 支持
            folder.fetch(messages, fetchProfile)
            val emailList = mutableListOf<EmailMessage>()
            for (i in messages.indices.reversed()) {
                val msg = messages[i]
                val uid = if (folder is UIDFolder) folder.getUID(msg).toString() else msg.messageNumber.toString()
                val sentDateTime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    msg.sentDate?.toInstant()?.toKotlinInstant()?.toLocalDateTime(TimeZone.currentSystemDefault())
                } else {
                    TODO("VERSION.SDK_INT < O")
                    null
                }
                val receivedDateTime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    msg.receivedDate?.toInstant()?.toKotlinInstant()?.toLocalDateTime(TimeZone.currentSystemDefault())
                } else {
                    TODO("VERSION.SDK_INT < O")
                    null
                }
                emailList.add( EmailMessage( /* ... (字段填充与 Desktop 版本相同) ... */
                    messageServerId = uid, folderName = folderName, subject = msg.subject,
                    fromAddress = msg.from?.mapNotNull { (it as? InternetAddress)?.toString() }?.joinToString(),
                    toList = msg.getRecipients(Message.RecipientType.TO)?.mapNotNull { (it as? InternetAddress)?.toString() },
                    ccList = msg.getRecipients(Message.RecipientType.CC)?.mapNotNull { (it as? InternetAddress)?.toString() },
                    bccList = null, bodyPlainText = null, bodyHtml = null,
                    sentDate = sentDateTime, receivedDate = receivedDateTime,
                    isRead = msg.flags.contains(Flags.Flag.SEEN),
                    hasAttachments = msg.contentType?.contains("multipart", ignoreCase = true) ?: false,
                    attachments = null
                ))
            }
            emit(EmailResult.Success(emailList))
        } catch (e: AuthenticationFailedException) { emit(EmailResult.Error(e, "IMAP认证失败: ${e.message}")) }
        catch (e: FolderNotFoundException) { emit(EmailResult.Error(e, "文件夹 '$folderName' 未找到: ${e.message}")) }
        catch (e: MessagingException) { emit(EmailResult.Error(e, "获取邮件时发生消息错误: ${e.message}")) }
        catch (e: Exception) { emit(EmailResult.Error(e, "获取邮件时发生未知错误: ${e.message}")) }
        finally {
            try { folder?.close(false); store?.close(); println("Android: IMAP resources closed for fetchEmails.") }
            catch (e: MessagingException) { println("Android: Error closing IMAP resources: ${e.message}") }
        }
    }

    // fetchEmailDetails 实现与 Desktop 版本相同
    override suspend fun fetchEmailDetails(accountInfo: AccountInfo, folderName: String, messageServerId: String): EmailResult<EmailMessage> = withContext(ioDispatcher) {
        var store: Store? = null; var folder: Folder? = null
        try {
            val imapProtocol = if (accountInfo.imapUseSsl && accountInfo.imapPort == 993) "imaps" else "imap"
            val imapProps = createMailProperties(accountInfo.imapHost, accountInfo.imapPort, accountInfo.imapUseSsl, "imap")
            val imapSession = Session.getInstance(imapProps)
            // imapSession.debug = true
            store = imapSession.getStore(imapProtocol)
            store.connect(accountInfo.imapHost, accountInfo.imapPort, accountInfo.emailAddress, accountInfo.passwordOrAuthCode)
            folder = store.getFolder(folderName)
            if (folder == null || !folder.exists()) { return@withContext EmailResult.Error(MessagingException("Folder '$folderName' not found for details.")) }
            folder.open(Folder.READ_ONLY)
            val msg: Message? = if (folder is UIDFolder) { try { folder.getMessageByUID(messageServerId.toLong()) } catch (e: NumberFormatException) { null } } else { null }
            if (msg == null) { return@withContext EmailResult.Error(MessagingException("Email with server ID '$messageServerId' not found.")) }

            val fromAddresses = msg.from?.mapNotNull { (it as? InternetAddress)?.toString() }?.joinToString()
            val subject = msg.subject
            val sentDateTime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                msg.sentDate?.toInstant()?.toKotlinInstant()?.toLocalDateTime(TimeZone.currentSystemDefault())
            } else {
                TODO("VERSION.SDK_INT < O")
                null
            }
            val receivedDateTime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                msg.receivedDate?.toInstant()?.toKotlinInstant()?.toLocalDateTime(TimeZone.currentSystemDefault())
            } else {
                TODO("VERSION.SDK_INT < O")
                null
            }
            var bodyPlainText: String? = null; var bodyHtml: String? = null
            val attachmentsList = mutableListOf<com.lonelymeko.myemail.data.model.EmailAttachmentInfo>()
            var hasAttachmentsFlag = false // Renamed to avoid conflict with EmailMessage property

            val content = msg.content
            if (content is String) { bodyPlainText = content }
            else if (content is MimeMultipart) {
                val multipart = content
                hasAttachmentsFlag = true
                for (i in 0 until multipart.count) {
                    val bodyPart = multipart.getBodyPart(i) as MimeBodyPart
                    val disposition = bodyPart.disposition
                    if (disposition != null && (disposition.equals(Part.ATTACHMENT, true) || disposition.equals(Part.INLINE, true))) {
                        attachmentsList.add(com.lonelymeko.myemail.data.model.EmailAttachmentInfo(
                            fileName = MimeUtility.decodeText(bodyPart.fileName ?: "unknown"),
                            contentType = bodyPart.contentType.substringBefore(";"),
                            sizeBytes = bodyPart.size.toLong()
                        ))
                    } else if (bodyPart.isMimeType("text/plain") && bodyPlainText == null) { bodyPlainText = bodyPart.content as? String }
                    else if (bodyPart.isMimeType("text/html") && bodyHtml == null) { bodyHtml = bodyPart.content as? String }
                }
            }
            if (bodyPlainText == null && bodyHtml != null) { bodyPlainText = bodyHtml.replace(Regex("<[^>]*>"), "") }

            EmailResult.Success(EmailMessage( /* ... (字段填充与 Desktop 版本相同) ... */
                messageServerId = messageServerId, folderName = folderName, subject = subject, fromAddress = fromAddresses,
                toList = msg.getRecipients(Message.RecipientType.TO)?.mapNotNull { (it as? InternetAddress)?.toString() },
                ccList = msg.getRecipients(Message.RecipientType.CC)?.mapNotNull { (it as? InternetAddress)?.toString() },
                bccList = null, bodyPlainText = bodyPlainText, bodyHtml = bodyHtml,
                sentDate = sentDateTime, receivedDate = receivedDateTime,
                isRead = msg.flags.contains(Flags.Flag.SEEN),
                hasAttachments = attachmentsList.isNotEmpty() || hasAttachmentsFlag,
                attachments = if (attachmentsList.isNotEmpty()) attachmentsList else null,
            ))
        } catch (e: AuthenticationFailedException) { EmailResult.Error(e, "IMAP认证失败: ${e.message}") }
        catch (e: FolderNotFoundException) { EmailResult.Error(e, "文件夹 '$folderName' 未找到: ${e.message}") }
        catch (e: MessagingException) { EmailResult.Error(e, "获取邮件详情时消息错误: ${e.message}") }
        catch (e: IOException) { EmailResult.Error(e, "读取邮件内容IO错误: ${e.message}") }
        catch (e: Exception) { EmailResult.Error(e, "获取邮件详情时未知错误: ${e.message}") }
        finally {
            try { folder?.close(false); store?.close() }
            catch (e: MessagingException) { /* Log error */ }
        }
    }

    // markEmailFlags 实现与 Desktop 版本相同
    override suspend fun markEmailFlags(accountInfo: AccountInfo, folderName: String, messageServerIds: List<String>, markAsRead: Boolean?): EmailResult<Unit> = withContext(ioDispatcher) {
        if (messageServerIds.isEmpty() || markAsRead == null) return@withContext EmailResult.Success(Unit)
        var store: Store? = null; var folder: Folder? = null
        try {
            val imapProtocol = if (accountInfo.imapUseSsl && accountInfo.imapPort == 993) "imaps" else "imap"
            val imapProps = createMailProperties(accountInfo.imapHost, accountInfo.imapPort, accountInfo.imapUseSsl, "imap")
            val imapSession = Session.getInstance(imapProps)
            store = imapSession.getStore(imapProtocol)
            store.connect(accountInfo.imapHost, accountInfo.imapPort, accountInfo.emailAddress, accountInfo.passwordOrAuthCode)
            folder = store.getFolder(folderName)
            if (folder == null || !folder.exists()) { return@withContext EmailResult.Error(MessagingException("Folder '$folderName' not found.")) }
            folder.open(Folder.READ_WRITE)
            val uidsToProcess = messageServerIds.mapNotNull { it.toLongOrNull() }.toLongArray()
            if (uidsToProcess.isNotEmpty() && folder is UIDFolder) {
                val messages = folder.getMessagesByUID(uidsToProcess)
                if (messages.isNotEmpty()) {
                    val flagToSet = Flags(Flags.Flag.SEEN)
                    folder.setFlags(messages, flagToSet, markAsRead) // true to set SEEN, false to clear SEEN
                }
            } else if (uidsToProcess.isNotEmpty()) { return@withContext EmailResult.Error(MessagingException("Folder does not support UID ops.")) }
            EmailResult.Success(Unit)
        } catch (e: AuthenticationFailedException) { EmailResult.Error(e, "IMAP认证失败: ${e.message}") }
        catch (e: FolderNotFoundException) { EmailResult.Error(e, "文件夹 '$folderName' 未找到: ${e.message}") }
        catch (e: ReadOnlyFolderException) { EmailResult.Error(e, "文件夹 '$folderName' 只读: ${e.message}") }
        catch (e: MessagingException) { EmailResult.Error(e, "标记邮件时消息错误: ${e.message}") }
        catch (e: Exception) { EmailResult.Error(e, "标记邮件时未知错误: ${e.message}") }
        finally {
            try { folder?.close(false); store?.close() }
            catch (e: MessagingException) { /* Log error */ }
        }
    }

    // deleteEmails 实现与 Desktop 版本相同
    override suspend fun deleteEmails(accountInfo: AccountInfo, folderName: String, messageServerIds: List<String>): EmailResult<Unit> = withContext(ioDispatcher) {
        if (messageServerIds.isEmpty()) return@withContext EmailResult.Success(Unit)
        var store: Store? = null; var folder: Folder? = null; val expungeOnClose = true
        try {
            val imapProtocol = if (accountInfo.imapUseSsl && accountInfo.imapPort == 993) "imaps" else "imap"
            val imapProps = createMailProperties(accountInfo.imapHost, accountInfo.imapPort, accountInfo.imapUseSsl, "imap")
            val imapSession = Session.getInstance(imapProps)
            store = imapSession.getStore(imapProtocol)
            store.connect(accountInfo.imapHost, accountInfo.imapPort, accountInfo.emailAddress, accountInfo.passwordOrAuthCode)
            folder = store.getFolder(folderName)
            if (folder == null || !folder.exists()) { return@withContext EmailResult.Error(MessagingException("Folder '$folderName' not found.")) }
            folder.open(Folder.READ_WRITE)
            val uidsToProcess = messageServerIds.mapNotNull { it.toLongOrNull() }.toLongArray()
            if (uidsToProcess.isNotEmpty() && folder is UIDFolder) {
                val messages = folder.getMessagesByUID(uidsToProcess)
                if (messages.isNotEmpty()) {
                    folder.setFlags(messages, Flags(Flags.Flag.DELETED), true)
                }
            } else if (uidsToProcess.isNotEmpty()) { return@withContext EmailResult.Error(MessagingException("Folder does not support UID ops.")) }
            EmailResult.Success(Unit)
        } catch (e: AuthenticationFailedException) { EmailResult.Error(e, "IMAP认证失败: ${e.message}") }
        catch (e: FolderNotFoundException) { EmailResult.Error(e, "文件夹 '$folderName' 未找到: ${e.message}") }
        catch (e: ReadOnlyFolderException) { EmailResult.Error(e, "文件夹 '$folderName' 只读: ${e.message}") }
        catch (e: MessagingException) { EmailResult.Error(e, "删除邮件时消息错误: ${e.message}") }
        catch (e: Exception) { EmailResult.Error(e, "删除邮件时未知错误: ${e.message}") }
        finally {
            try { folder?.close(expungeOnClose); store?.close() }
            catch (e: MessagingException) { /* Log error */ }
        }
    }
}