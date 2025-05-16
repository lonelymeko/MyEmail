package com.lonelymeko.myemail.presentation.feature.bottom_nav

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.lonelymeko.myemail.data.model.AccountInfo
import com.lonelymeko.myemail.domain.usecase.account.GetAccountsFlowUseCase
import com.lonelymeko.myemail.domain.usecase.account.GetActiveAccountFlowUseCase
import com.lonelymeko.myemail.domain.usecase.account.SetActiveAccountUseCase
import com.lonelymeko.myemail.data.model.AccountInfo
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainScreenUiState(
    val accounts: List<AccountInfo> = emptyList(),
    val activeAccount: AccountInfo? = null,
    val isLoadingAccounts: Boolean = true
)

class MainScreenModel(
    private val getAccountsFlowUseCase: GetAccountsFlowUseCase,
    private val getActiveAccountFlowUseCase: GetActiveAccountFlowUseCase,
    private val setActiveAccountUseCase: SetActiveAccountUseCase
) : ScreenModel {

    // 使用 stateIn 将 Flow 转换为 StateFlow，以便 Compose 可以观察
    val accountsState = getAccountsFlowUseCase()
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeAccountState = getActiveAccountFlowUseCase()
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), null)

    // 如果需要一个合并的 UI State，可以这样做 (但通常直接观察上面的 StateFlows 更简单)
    // var uiState by mutableStateOf(MainScreenUiState())
    //     private set
    //
    // init {
    //     coroutineScope.launch {
    //         getAccountsFlowUseCase().collect { accounts ->
    //             uiState = uiState.copy(accounts = accounts, isLoadingAccounts = false)
    //         }
    //     }
    //     coroutineScope.launch {
    //         getActiveAccountFlowUseCase().collect { activeAccount ->
    //             uiState = uiState.copy(activeAccount = activeAccount)
    //         }
    //     }
    // }

    fun onSwitchAccount(account: AccountInfo) {
        screenModelScope.launch {
            setActiveAccountUseCase(account.emailAddress)
        }
    }
}