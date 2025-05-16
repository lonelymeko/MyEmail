package com.lonelymeko.myemail.domain.usecase.email


import com.lonelymeko.myemail.data.model.AccountInfo
import com.lonelymeko.myemail.data.remote.api.EmailResult
import com.lonelymeko.myemail.data.repository.EmailRepository // Email 操作通过 EmailRepository

class DeleteEmailUseCase(private val emailRepository: EmailRepository) {
    suspend operator fun invoke(
        accountInfo: AccountInfo,
        folderName: String,
        messageServerIds: List<String>
    ): EmailResult<Unit> {
        return emailRepository.deleteEmails(accountInfo, folderName, messageServerIds)
    }
}