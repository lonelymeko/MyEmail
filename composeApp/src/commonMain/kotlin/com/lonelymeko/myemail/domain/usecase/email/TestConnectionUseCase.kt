package com.lonelymeko.myemail.domain.usecase.email


import com.lonelymeko.myemail.data.model.AccountInfo
import com.lonelymeko.myemail.data.remote.api.EmailResult
import com.lonelymeko.myemail.data.remote.api.EmailService // 直接使用 EmailService 接口

class TestConnectionUseCase(private val emailService: EmailService) {
    suspend operator fun invoke(accountInfo: AccountInfo): EmailResult<Unit> {
        return emailService.testConnection(accountInfo)
    }
}