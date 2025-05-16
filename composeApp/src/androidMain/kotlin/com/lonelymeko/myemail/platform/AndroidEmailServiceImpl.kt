package com.lonelymeko.myemail.platform


import com.lonelymeko.myemail.common.exception.NotImplementedException
import com.lonelymeko.myemail.data.model.AccountInfo
import com.lonelymeko.myemail.data.model.AccountType
import com.lonelymeko.myemail.data.model.EmailMessage
import com.lonelymeko.myemail.data.remote.api.EmailResult
import com.lonelymeko.myemail.data.remote.api.EmailService // <--- 导入 commonMain 中的 interface
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.Properties
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeUtility

// 这个类实现了 commonMain 中的 EmailService 接口
class AndroidEmailServiceImpl( // <--- 普通类，不再有 actual
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : EmailService { // <--- 实现 EmailService 接口

    private fun createMailProperties(host: String, port: Int, useSsl: Boolean, protocolPrefix: String): Properties {
        // ... (这个方法的实现保持不变) ...
        return Properties().apply {
            put("mail.$protocolPrefix.host", host)
            put("mail.$protocolPrefix.port", port.toString())
            put("mail.$protocolPrefix.auth", "true")
            if (useSsl) {
                if (protocolPrefix == "smtp" && (port == 587 || port == 25)) {
                    put("mail.smtp.starttls.enable", "true")
                } else {
                    put("mail.$protocolPrefix.socketFactory.port", port.toString())
                    put("mail.$protocolPrefix.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                    put("mail.$protocolPrefix.ssl.enable", "true")
                }
            } else {
                if (protocolPrefix == "smtp") { put("mail.smtp.starttls.enable", "true") }
            }
        }
    }

    // 所有接口方法都需要用 override 关键字标记
    override suspend fun testConnection(accountInfo: AccountInfo): EmailResult<Unit> = withContext(ioDispatcher) {
        // ... (实现保持不变，但方法前有 override) ...
        // IMAP Test
        try {
            val imapProtocol = if (accountInfo.imapUseSsl && accountInfo.imapPort == 993) "imaps" else "imap"
            val imapProps = createMailProperties(accountInfo.imapHost, accountInfo.imapPort, accountInfo.imapUseSsl, "imap")
            val imapSession = Session.getInstance(imapProps)
            val store = imapSession.getStore(imapProtocol)
            store.connect(accountInfo.emailAddress, accountInfo.passwordOrAuthCode)
            store.close()
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
            val transport = smtpSession.getTransport(smtpProtocol)
            transport.connect(accountInfo.emailAddress, accountInfo.passwordOrAuthCode)
            transport.close()
        } catch (e: AuthenticationFailedException) { return@withContext EmailResult.Error(e, "SMTP认证失败: ${e.message}") }
        catch (e: MessagingException) { return@withContext EmailResult.Error(e, "SMTP连接失败: ${e.message}") }
        catch (e: Exception) { return@withContext EmailResult.Error(e, "SMTP连接时发生未知错误: ${e.message}") }
        return@withContext EmailResult.Success(Unit)
    }

    override suspend fun sendEmail(accountInfo: AccountInfo, emailMessage: EmailMessage): EmailResult<Unit> = withContext(ioDispatcher) {
        // ... (实现保持不变，但方法前有 override) ...
        try {
            val smtpProtocol = if (accountInfo.smtpUseSsl && accountInfo.smtpPort == 465) "smtps" else "smtp"
            val props = createMailProperties(accountInfo.smtpHost, accountInfo.smtpPort, accountInfo.smtpUseSsl, "smtp")
            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication { return PasswordAuthentication(accountInfo.emailAddress, accountInfo.passwordOrAuthCode) }
            })
            val message = MimeMessage(session)
            message.setFrom(InternetAddress(accountInfo.emailAddress, MimeUtility.encodeText(accountInfo.displayName ?: accountInfo.emailAddress, "UTF-8", "B")))
            emailMessage.toList?.forEach { if(it.isNotBlank()) message.addRecipient(Message.RecipientType.TO, InternetAddress(it)) }
            emailMessage.ccList?.forEach { if(it.isNotBlank()) message.addRecipient(Message.RecipientType.CC, InternetAddress(it)) }
            emailMessage.bccList?.forEach { if(it.isNotBlank()) message.addRecipient(Message.RecipientType.BCC, InternetAddress(it)) }
            message.subject = MimeUtility.encodeText(emailMessage.subject ?: "", "UTF-8", "B")
            message.sentDate = Date()
            if (!emailMessage.bodyHtml.isNullOrBlank()) { message.setContent(emailMessage.bodyHtml, "text/html; charset=utf-8") }
            else { message.setText(emailMessage.bodyPlainText ?: "", "UTF-8") }
            val transport = session.getTransport(smtpProtocol)
            transport.connect(accountInfo.emailAddress, accountInfo.passwordOrAuthCode)
            transport.sendMessage(message, message.allRecipients)
            transport.close()
            EmailResult.Success(Unit)
        } catch (e: SendFailedException) { EmailResult.Error(e, "邮件发送失败 (部分地址无效): ${e.message}") }
        catch (e: MessagingException) { EmailResult.Error(e, "邮件发送时消息错误: ${e.message}") }
        catch (e: Exception) { EmailResult.Error(e, "邮件发送时未知错误: ${e.message}") }
    }

    override fun fetchEmails(accountInfo: AccountInfo, folderName: String, page: Int, pageSize: Int): Flow<EmailResult<List<EmailMessage>>> {
        println("AndroidEmailServiceImpl: fetchEmails for ${accountInfo.emailAddress}, folder $folderName - NOT IMPLEMENTED YET")
        return flowOf(EmailResult.Error(NotImplementedException("fetchEmails not implemented on Android")))
    }

    override suspend fun fetchEmailDetails(accountInfo: AccountInfo, folderName: String, messageServerId: String): EmailResult<EmailMessage> {
        println("AndroidEmailServiceImpl: fetchEmailDetails for ${accountInfo.emailAddress}, msgId $messageServerId - NOT IMPLEMENTED YET")
        return EmailResult.Error(NotImplementedException("fetchEmailDetails not implemented on Android"))
    }

    override suspend fun markEmailFlags(accountInfo: AccountInfo, folderName: String, messageServerIds: List<String>, markAsRead: Boolean?): EmailResult<Unit> {
        println("AndroidEmailServiceImpl: markEmailFlags for ${accountInfo.emailAddress} - NOT IMPLEMENTED YET")
        return EmailResult.Error(NotImplementedException("markEmailFlags not implemented on Android"))
    }

    override suspend fun deleteEmails(accountInfo: AccountInfo, folderName: String, messageServerIds: List<String>): EmailResult<Unit> {
        println("AndroidEmailServiceImpl: deleteEmails for ${accountInfo.emailAddress} - NOT IMPLEMENTED YET")
        return EmailResult.Error(NotImplementedException("deleteEmails not implemented on Android"))
    }


    // --- 其他方法的存根仍然保留 ---
//    actual fun fetchEmails( /* ... */ ): Flow<EmailResult<List<EmailMessage>>> { /* ... */ return flowOf(EmailResult.Error(NotImplementedError("..."))) }
//    actual suspend fun fetchEmailDetails( /* ... */ ): EmailResult<EmailMessage> { /* ... */ return EmailResult.Error(NotImplementedError("...")) }
//    actual suspend fun markEmailFlags( /* ... */ ): EmailResult<Unit> { /* ... */ return EmailResult.Error(NotImplementedError("...")) }
//    actual suspend fun deleteEmails( /* ... */ ): EmailResult<Unit> { /* ... */ return EmailResult.Error(NotImplementedError("...")) }

// --- 测试用的 main 函数 ---
// 你可以将这个 main 函数放在 AndroidEmailServiceImpl.kt 文件的末尾，或者一个单独的测试文件中
// 注意：直接运行这个 main 需要配置好 JVM 环境和依赖，并且它不会有 Android Context。
// 如果 AndroidEmailService 构造函数需要 Context，这个 main 函数将无法直接实例化它。
// 为了简单测试网络部分，我们可以临时移除构造函数中的 Context 或提供一个 mock。
// 或者，将测试逻辑移到单元测试中，使用 Robolectric 或 mock Context。

// 简单的测试 main (如果 AndroidEmailService 不需要 Context，或者我们创建一个临时的)
}
fun main() = runBlocking {
    // 重要: 替换为你的真实邮箱和授权码! 不要提交真实凭据到版本库!
    val myTestAccount = AccountInfo(
        emailAddress = "2477183238@qq.com", // 你的发件邮箱
        passwordOrAuthCode = "xpifijkejiirdiaf",     // 你的邮箱授权码
        imapHost = "imap.qq.com",                 // 你的IMAP服务器
        imapPort = 993,
        imapUseSsl = true,
        smtpHost = "smtp.qq.com",                 // 你的SMTP服务器
        smtpPort = 465, // 或 587
        smtpUseSsl = true, // 如果 smtpPort 是 587，这里通常是 true (配合 STARTTLS) 或 false (然后props里配置STARTTLS)
        accountType = AccountType.QQ,   // 或 QQ, NETEASE_163
        displayName = "测试发件人"
    )

    // 如果 AndroidEmailService 构造函数需要 Context，这个 main 无法直接运行。
    // 假设我们有一个不需要 Context 的版本或使用默认 Dispatcher 的构造函数
    val emailService = AndroidEmailServiceImpl() // 假设构造函数允许无参或有默认值

    println("--- Testing Connection ---")
    val connectionResult = emailService.testConnection(myTestAccount)
    when (connectionResult) {
        is EmailResult.Success -> println("Connection Test: SUCCESS")
        is EmailResult.Error -> println("Connection Test: FAILED - ${connectionResult.message} (Exception: ${connectionResult.exception})")
    }
    println("--------------------------\n")

    if (connectionResult is EmailResult.Success) {
        println("--- Testing Send Email ---")
        val emailToSend = EmailMessage(
            messageServerId = "", // 发送时不需要
            folderName = "",      // 发送时不需要
            subject = "KMP邮件客户端测试邮件 Subject with 中文",
            fromAddress = myTestAccount.emailAddress, // 会被 setFrom 覆盖
            toList = listOf("2477183238@qq.com"), // 替换为你的收件人邮箱
            ccList = null,
            bccList = null,
            bodyPlainText = "这是一封来自 Kotlin Multiplatform 邮件客户端的纯文本测试邮件。\nHello from KMP Mail Client!",
            bodyHtml = "<h1>KMP邮件客户端测试</h1><p>这是一封来自 <b>Kotlin Multiplatform</b> 邮件客户端的 <i>HTML</i> 测试邮件。</p><p>Hello from KMP Mail Client!</p>",
            sentDate = null, // 会被自动设置
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
        println("------------------------\n")
    }
}