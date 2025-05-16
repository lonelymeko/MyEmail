package com.lonelymeko.myemail.data.remote.api // 确保包名与你的项目一致


import com.lonelymeko.myemail.data.model.AccountInfo
import com.lonelymeko.myemail.data.model.EmailMessage
import kotlinx.coroutines.flow.Flow

/**
 * Interface EmailService
 * 定义与邮件服务器交互的接口。
 * 平台将提供具体的实现类。
 */
interface EmailService { // <--- 从 expect class 改为 interface

    suspend fun testConnection(accountInfo: AccountInfo): EmailResult<Unit>

    fun fetchEmails(
        accountInfo: AccountInfo,
        folderName: String,
        page: Int = 1,
        pageSize: Int = 20
    ): Flow<EmailResult<List<EmailMessage>>>

    suspend fun fetchEmailDetails(
        accountInfo: AccountInfo,
        folderName: String,
        messageServerId: String
    ): EmailResult<EmailMessage>

    suspend fun sendEmail(
        accountInfo: AccountInfo,
        emailMessage: EmailMessage
    ): EmailResult<Unit>

    suspend fun markEmailFlags(
        accountInfo: AccountInfo,
        folderName: String,
        messageServerIds: List<String>,
        markAsRead: Boolean?
    ): EmailResult<Unit>

    suspend fun deleteEmails(
        accountInfo: AccountInfo,
        folderName: String,
        messageServerIds: List<String>
    ): EmailResult<Unit>
}