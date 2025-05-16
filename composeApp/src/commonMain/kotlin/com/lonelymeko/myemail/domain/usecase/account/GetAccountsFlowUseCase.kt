package com.lonelymeko.myemail.domain.usecase.account


import com.lonelymeko.myemail.data.model.AccountInfo
import com.lonelymeko.myemail.data.repository.AccountRepository
import kotlinx.coroutines.flow.Flow

class GetAccountsFlowUseCase(private val accountRepository: AccountRepository) {
    operator fun invoke(): Flow<List<AccountInfo>> { // Flow 通常不是 suspend
        return accountRepository.getAccountsFlow()
    }
}