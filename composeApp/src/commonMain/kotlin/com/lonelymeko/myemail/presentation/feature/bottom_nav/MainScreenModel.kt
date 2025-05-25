package com.lonelymeko.myemail.presentation.feature.bottom_nav // 你的包名

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope // 正确的导入
import com.lonelymeko.myemail.data.model.AccountInfo
import com.lonelymeko.myemail.domain.usecase.account.GetAccountsFlowUseCase
import com.lonelymeko.myemail.domain.usecase.account.GetActiveAccountFlowUseCase
import com.lonelymeko.myemail.domain.usecase.account.SetActiveAccountUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainScreenModel(
    private val getAccountsFlowUseCase: GetAccountsFlowUseCase,
    private val getActiveAccountFlowUseCase: GetActiveAccountFlowUseCase,
    private val setActiveAccountUseCase: SetActiveAccountUseCase
) : ScreenModel {

    init {
        println("MainScreenModel: INIT - ScreenModel instance created.")
        // 我们将依赖 AccountRepositoryImpl 在添加第一个账户时设置活动账户的逻辑，
        // 或者依赖用户手动选择。
        // activeAccountState 会通过 getActiveAccountFlowUseCase() 自动更新。
        // accountsState 也会通过 getAccountsFlowUseCase() 自动更新。
        // 移除之前在 init 中尝试设置活动账户的 launch 块，因为它可能在 stateIn 的 Flow 准备好之前执行。
    }

    val accountsState: StateFlow<List<AccountInfo>> = getAccountsFlowUseCase()
        .onEach { accounts ->
            println("MainScreenModel: accountsState received ${accounts.size} accounts. First: ${accounts.firstOrNull()?.emailAddress}")
        }
        .stateIn(
            scope = screenModelScope,
            started = SharingStarted.WhileSubscribed(5000L), // 推荐加上 L 表示 Long
            initialValue = emptyList()
        )

    val activeAccountState: StateFlow<AccountInfo?> = getActiveAccountFlowUseCase()
        .onEach { activeAcc ->
            println("MainScreenModel: activeAccountState received: ${activeAcc?.emailAddress}")
        }
        .stateIn(
            scope = screenModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )

    fun onSwitchAccount(account: AccountInfo) {
        println("MainScreenModel: onSwitchAccount called for ${account.emailAddress}")
        // 避免重复设置同一个活动账户
        if (activeAccountState.value?.emailAddress == account.emailAddress) {
            println("MainScreenModel: Account ${account.emailAddress} is already active. Skipping setActiveAccountUseCase.")
            return
        }
        screenModelScope.launch {
            val result = setActiveAccountUseCase(account.emailAddress) // UseCase 期望 String?
            result.fold(
                onSuccess = { println("MainScreenModel: setActiveAccountUseCase SUCCESS for ${account.emailAddress}") },
                onFailure = { error -> println("MainScreenModel: setActiveAccountUseCase FAILED for ${account.emailAddress}: ${error.message}") }
            )
        }
    }

    override fun onDispose() {
        super.onDispose()
        println("MainScreenModel: DISPOSED")
    }
}