package com.lonelymeko.myemail.domain.usecase.account

import com.lonelymeko.myemail.data.repository.AccountRepository

class DeleteAccountUseCase(private val accountRepository: AccountRepository) {
    suspend operator fun invoke(emailAddress: String): Result<Unit> {
        return accountRepository.deleteAccount(emailAddress)
    }
}