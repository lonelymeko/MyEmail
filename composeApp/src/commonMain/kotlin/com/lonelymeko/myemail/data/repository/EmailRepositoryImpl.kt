package com.lonelymeko.myemail.data.repository


import com.lonelymeko.myemail.data.model.AccountInfo
import com.lonelymeko.myemail.data.model.EmailMessage
import com.lonelymeko.myemail.data.remote.api.EmailResult
import com.lonelymeko.myemail.data.remote.api.EmailService // commonMain expect
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class EmailRepositoryImpl(
    private val emailService: EmailService, // 将由 Koin 注入平台提供的 actual EmailService
    private val ioDispatcher: CoroutineDispatcher
) : EmailRepository {

    override fun fetchEmails(
        accountInfo: AccountInfo,
        folderName: String,
        page: Int,
        pageSize: Int
    ): Flow<EmailResult<List<EmailMessage>>> {
        return emailService.fetchEmails(accountInfo, folderName, page, pageSize)
            .flowOn(ioDispatcher)
    }

    override suspend fun fetchEmailDetails(
        accountInfo: AccountInfo,
        folderName: String,
        messageServerId: String
    ): EmailResult<EmailMessage> = withContext(ioDispatcher) {
        emailService.fetchEmailDetails(accountInfo, folderName, messageServerId)
    }

    override suspend fun sendEmail(
        accountInfo: AccountInfo,
        emailMessage: EmailMessage
    ): EmailResult<Unit> = withContext(ioDispatcher) {
        emailService.sendEmail(accountInfo, emailMessage)
    }

    override suspend fun markEmailFlags(
        accountInfo: AccountInfo,
        folderName: String,
        messageServerIds: List<String>,
        markAsRead: Boolean?
    ): EmailResult<Unit> = withContext(ioDispatcher) {
        emailService.markEmailFlags(accountInfo, folderName, messageServerIds, markAsRead)
    }

    override suspend fun deleteEmails(
        accountInfo: AccountInfo,
        folderName: String,
        messageServerIds: List<String>
    ): EmailResult<Unit> = withContext(ioDispatcher) {
        emailService.deleteEmails(accountInfo, folderName, messageServerIds)
    }
}