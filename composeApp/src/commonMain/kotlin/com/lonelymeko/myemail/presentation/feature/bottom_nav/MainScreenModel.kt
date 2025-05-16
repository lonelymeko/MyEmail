package com.lonelymeko.myemail.presentation.feature.bottom_nav

import androidx.compose.runtime.getValue // 这个导入在这个文件中似乎没有直接使用
import androidx.compose.runtime.mutableStateOf // 这个导入在这个文件中似乎没有直接使用
import androidx.compose.runtime.setValue // 这个导入在这个文件中似乎没有直接使用
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.lonelymeko.myemail.data.model.AccountInfo
import com.lonelymeko.myemail.domain.usecase.account.GetAccountsFlowUseCase
import com.lonelymeko.myemail.domain.usecase.account.GetActiveAccountFlowUseCase
import com.lonelymeko.myemail.domain.usecase.account.SetActiveAccountUseCase
// import kotlinx.coroutines.coroutineScope // 这个导入如果下面的 init 块被注释掉，则不需要
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onEach // <--- 添加这个导入用于 Flow 日志
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// data class MainScreenUiState(...) // 你没有使用这个合并的 UiState，所以可以暂时注释或删除

class MainScreenModel(
    private val getAccountsFlowUseCase: GetAccountsFlowUseCase,
    private val getActiveAccountFlowUseCase: GetActiveAccountFlowUseCase,
    private val setActiveAccountUseCase: SetActiveAccountUseCase
) : ScreenModel {

    init {
        println("MainScreenModel: INIT") // <--- 添加初始化日志
    }

    // 使用 stateIn 将 Flow 转换为 StateFlow，以便 Compose 可以观察
    val accountsState = getAccountsFlowUseCase()
        .onEach { accounts -> // <--- 添加日志来观察 accountsState 的变化
            println("MainScreenModel: accountsState emitting ${accounts.size} accounts. First: ${accounts.firstOrNull()?.emailAddress}")
        }
        .stateIn(
            scope = screenModelScope, // <--- 确保使用 screenModelScope
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeAccountState = getActiveAccountFlowUseCase()
        .onEach { activeAcc -> // <--- 添加日志来观察 activeAccountState 的变化
            println("MainScreenModel: activeAccountState emitting: ${activeAcc?.emailAddress}")
        }
        .stateIn(
            scope = screenModelScope, // <--- 确保使用 screenModelScope
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun onSwitchAccount(account: AccountInfo) {
        println("MainScreenModel: onSwitchAccount called for ${account.emailAddress}") // <--- 添加日志
        screenModelScope.launch {
            val result = setActiveAccountUseCase(account.emailAddress)
            println("MainScreenModel: setActiveAccountUseCase result for ${account.emailAddress}: $result") // <--- 添加日志
        }
    }

    override fun onDispose() { // <--- 添加 onDispose 日志
        super.onDispose()
        println("MainScreenModel: DISPOSED")
    }
}