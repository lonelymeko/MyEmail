package com.lonelymeko.myemail.data.repository

import com.lonelymeko.myemail.data.model.AccountInfo
import com.lonelymeko.myemail.data.model.EmailMessage
import com.lonelymeko.myemail.data.remote.api.EmailResult
import kotlinx.coroutines.flow.Flow

interface EmailRepository {
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