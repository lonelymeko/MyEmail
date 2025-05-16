package com.lonelymeko.myemail.domain.usecase.account

import com.lonelymeko.myemail.data.model.AccountInfo
import com.lonelymeko.myemail.data.repository.AccountRepository

class GetAccountByEmailUseCase(private val accountRepository: AccountRepository) {
    suspend operator fun invoke(emailAddress: String): Result<AccountInfo?> {
        return accountRepository.getAccountByEmail(emailAddress)
    }
}