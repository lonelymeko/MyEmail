package com.lonelymeko.myemail.domain.usecase.email


import com.lonelymeko.myemail.data.model.AccountInfo
import com.lonelymeko.myemail.data.model.EmailMessage
import com.lonelymeko.myemail.data.remote.api.EmailResult
import com.lonelymeko.myemail.data.repository.EmailRepository
import kotlinx.coroutines.flow.Flow

class FetchEmailsUseCase(private val emailRepository: EmailRepository) {
    operator fun invoke( // Flow 通常不是 suspend
        accountInfo: AccountInfo,
        folderName: String,
        page: Int = 1,
        pageSize: Int = 20
    ): Flow<EmailResult<List<EmailMessage>>> {
        return emailRepository.fetchEmails(accountInfo, folderName, page, pageSize)
    }
}