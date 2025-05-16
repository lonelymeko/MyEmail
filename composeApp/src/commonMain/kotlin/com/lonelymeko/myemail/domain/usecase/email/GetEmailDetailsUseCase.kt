package com.lonelymeko.myemail.domain.usecase.email


import com.lonelymeko.myemail.data.model.AccountInfo
import com.lonelymeko.myemail.data.model.EmailMessage
import com.lonelymeko.myemail.data.remote.api.EmailResult
import com.lonelymeko.myemail.data.repository.EmailRepository

class GetEmailDetailsUseCase(private val emailRepository: EmailRepository) {
    suspend operator fun invoke(
        accountInfo: AccountInfo,
        folderName: String,
        messageServerId: String
    ): EmailResult<EmailMessage> {
        return emailRepository.fetchEmailDetails(accountInfo, folderName, messageServerId)
    }
}