package com.lonelymeko.myemail.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AccountInfo(
    val emailAddress: String,
    val passwordOrAuthCode: String, //  密码或授权码
    val imapHost: String,
    val imapPort: Int,
    val imapUseSsl: Boolean = true, // IMAP 是否使用SSL，默认为true
    val smtpHost: String,
    val smtpPort: Int,
    val smtpUseSsl: Boolean = true, // SMTP 是否使用SSL，默认为true
    val accountType: AccountType,   // 账户类型，引用上面的枚举
    val displayName: String = emailAddress // 显示名称，默认为邮箱地址
){
    companion object{
        // 辅助函数，用于快速创建特定类型的账户配置 (请根据实际服务器信息调整)
        fun createQqAccount(email: String, authCode: String, displayName: String? = null): AccountInfo {
            return AccountInfo(
                emailAddress = email,
                passwordOrAuthCode = authCode,
                imapHost = "imap.qq.com",
                imapPort = 993, // QQ邮箱 IMAP SSL端口
                imapUseSsl = true,
                smtpHost = "smtp.qq.com",
                smtpPort = 465, // QQ邮箱 SMTP SSL端口 (或者 587 使用 STARTTLS)
                smtpUseSsl = true,
                accountType = AccountType.QQ,
                displayName = displayName ?: email
            )
        }

        fun createNetease163Account(email: String, authCode: String, displayName: String? = null): AccountInfo {
            return AccountInfo(
                emailAddress = email,
                passwordOrAuthCode = authCode,
                imapHost = "imap.163.com",
                imapPort = 993, // 网易163 IMAP SSL端口
                imapUseSsl = true,
                smtpHost = "smtp.163.com",
                smtpPort = 465, // 网易163 SMTP SSL端口 (或994, 或25非SSL，465/587 for STARTTLS)
                smtpUseSsl = true,
                accountType = AccountType.NETEASE_163,
                displayName = displayName ?: email
            )
        }

        // 为其他常用邮箱添加更多模板
        fun createGenericImapSmtpAccount(email: String, password: String, imapHost: String, imapPort: Int, smtpHost: String, smtpPort: Int, displayName: String? = null): AccountInfo {
            return AccountInfo(
                emailAddress = email,
                passwordOrAuthCode = password,
                imapHost = imapHost,
                imapPort = imapPort,
                imapUseSsl = true,
                smtpHost = smtpHost,
                smtpPort = smtpPort,
                smtpUseSsl = true,
                accountType = AccountType.GENERIC_IMAP_SMTP,
                displayName = displayName ?: email
            )
        }
    }
}
