package com.lonelymeko.myemail.domain.usecase.account


import com.lonelymeko.myemail.data.model.AccountInfo
import com.lonelymeko.myemail.data.remote.api.EmailResult
import com.lonelymeko.myemail.data.repository.AccountRepository
import com.lonelymeko.myemail.domain.usecase.email.TestConnectionUseCase // 依赖 TestConnectionUseCase

class AddAccountUseCase(
    private val accountRepository: AccountRepository,
    private val testConnectionUseCase: TestConnectionUseCase // 添加账户前先测试连接
) {
    suspend operator fun invoke(accountInfo: AccountInfo): Result<Unit> {
        // 1. 先测试连接性 (可选，但推荐)
        // 如果 testConnectionUseCase 内部已经处理了 EmailResult，这里可以直接判断
        when (val connectionTest = testConnectionUseCase(accountInfo)) {
            is EmailResult.Error -> {
                // 将 EmailResult.Error 转换为 kotlin.Result.failure
                return Result.failure(Exception("连接测试失败: ${connectionTest.message ?: connectionTest.exception.message}"))
            }
            is EmailResult.Success<*> -> {
                // 连接成功，继续添加账户
                return accountRepository.addAccount(accountInfo)
            }
        }
        return Result.failure(Exception("未知错误"))
    }
}