package com.lonelymeko.myemail.domain.usecase.email


import com.lonelymeko.myemail.data.model.AccountInfo
import com.lonelymeko.myemail.data.model.EmailMessage
import com.lonelymeko.myemail.data.remote.api.EmailResult
import com.lonelymeko.myemail.data.repository.EmailRepository

class SendEmailUseCase(private val emailRepository: EmailRepository) {
    suspend operator fun invoke(
        accountInfo: AccountInfo,
        emailMessage: EmailMessage
    ): EmailResult<Unit> {
        return emailRepository.sendEmail(accountInfo, emailMessage)
    }
}