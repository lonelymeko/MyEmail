package com.lonelymeko.myemail.domain.usecase.email


import com.lonelymeko.myemail.data.model.AccountInfo
import com.lonelymeko.myemail.data.remote.api.EmailResult
import com.lonelymeko.myemail.data.repository.EmailRepository

class MarkEmailFlagsUseCase(private val emailRepository: EmailRepository) {
    suspend operator fun invoke(
        accountInfo: AccountInfo,
        folderName: String,
        messageServerIds: List<String>,
        markAsRead: Boolean?
    ): EmailResult<Unit> {
        return emailRepository.markEmailFlags(accountInfo, folderName, messageServerIds, markAsRead)
    }
}