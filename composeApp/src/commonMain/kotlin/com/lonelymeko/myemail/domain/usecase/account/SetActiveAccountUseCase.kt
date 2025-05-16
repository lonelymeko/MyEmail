package com.lonelymeko.myemail.domain.usecase.account


import com.lonelymeko.myemail.data.repository.AccountRepository

class SetActiveAccountUseCase(private val accountRepository: AccountRepository) {
    suspend operator fun invoke(emailAddress: String?): Result<Unit> {
        return accountRepository.setActiveAccount(emailAddress)
    }
}