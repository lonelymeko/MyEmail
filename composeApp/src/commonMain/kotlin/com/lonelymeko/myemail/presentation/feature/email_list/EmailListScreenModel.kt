package com.lonelymeko.myemail.presentation.feature.email_list

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.lonelymeko.myemail.data.model.AccountInfo
import com.lonelymeko.myemail.data.model.EmailMessage
import com.lonelymeko.myemail.data.remote.api.EmailResult
// import com.lonelymeko.myemail.domain.usecase.account.GetActiveAccountFlowUseCase // 这个 ScreenModel 当前不直接使用它
import com.lonelymeko.myemail.domain.usecase.email.DeleteEmailUseCase
import com.lonelymeko.myemail.domain.usecase.email.FetchEmailsUseCase
import com.lonelymeko.myemail.domain.usecase.email.MarkEmailFlagsUseCase
import kotlinx.coroutines.Job // 你之前有 fetchJob，但在这个简化版中移除了，所以 Job 可能不需要导入了
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
// import org.koin.core.Koin // 这个导入通常不需要在 ScreenModel 中

data class EmailListUiState(
    val emails: List<EmailMessage> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val folderName: String = "INBOX",
    val currentPage: Int = 1,
    val canLoadMore: Boolean = true, // 虽然简化版设为false，但保留字段
    val isRefreshing: Boolean = false,
    val selectedEmails: Set<String> = emptySet()
)

class EmailListScreenModel(
    private val account: AccountInfo, // 通过构造函数传入
    initialFolderName: String,      // 通过构造函数传入
    private val fetchEmailsUseCase: FetchEmailsUseCase,
    private val markEmailFlagsUseCase: MarkEmailFlagsUseCase,
    private val deleteEmailUseCase: DeleteEmailUseCase
) : ScreenModel {

    var uiState by mutableStateOf(EmailListUiState(folderName = initialFolderName, isLoading = true))
        private set

    init {
        println("EmailListScreenModel: INIT for account='${account.emailAddress}', folder='$initialFolderName'")
        loadInitialEmails()
    }

    private fun loadInitialEmails() {
        // 确保 account 和 folderName 是期望的值
        println("EmailListScreenModel: loadInitialEmails() called. Account: ${account.emailAddress}, Folder: ${uiState.folderName}, Current isLoading: ${uiState.isLoading}")

        // 避免在已加载或正在加载时重复触发（虽然 init 中通常只会调用一次）
         if (!uiState.isLoading) { // 如果初始 isLoading 就是 true，这个判断可能不适用
             uiState = uiState.copy(isLoading = true, errorMessage = null)
         } else if(uiState.isLoading && uiState.emails.isEmpty()){
//         已经是 isLoading = true, 可能是 init 调用的
         } else {
             println("EmailListScreenModel: loadInitialEmails() skipped, already loading or loaded and not refresh.")
             return
         }
//         简化：直接设置为加载中，因为 init 时 isLoading 已经是 true
//        uiState = uiState.copy(isLoading = true, errorMessage = null) // 确保每次加载都重置错误信息


        println("EmailListScreenModel: Launching coroutine to fetch emails...")
        screenModelScope.launch {
            fetchEmailsUseCase(account, uiState.folderName, page = 1, pageSize = 50)
                .catch { e ->
                    println("EmailListScreenModel: fetchEmailsUseCase Flow CATCH block: ${e.message}")
                    uiState = uiState.copy(isLoading = false, isRefreshing = false, errorMessage = "加载邮件错误: ${e.message}")
                }
                .collect { result ->
                    println("EmailListScreenModel: fetchEmailsUseCase COLLECT block, result is: $result")
                    when (result) {
                        is EmailResult.Success -> {
                            println("EmailListScreenModel: fetchEmailsUseCase SUCCESS, emails count: ${result.data.size}")
                            uiState = uiState.copy(
                                isLoading = false,
                                isRefreshing = false, // 如果是从刷新调用的，这里也应该重置
                                emails = result.data,
                                errorMessage = null,
                                canLoadMore = false, // 简化，不实现加载更多
                                currentPage = 1 // 重置页码
                            )
                        }
                        is EmailResult.Error -> {
                            println("EmailListScreenModel: fetchEmailsUseCase ERROR: ${result.message ?: result.exception.message}")
                            uiState = uiState.copy(
                                isLoading = false,
                                isRefreshing = false,
                                errorMessage = "加载邮件失败: ${result.message ?: result.exception.message}"
                            )
                        }
                    }
                    println("EmailListScreenModel: uiState after collect: $uiState")
                }
        }
    }

    fun consumeErrorMessage() {
        println("EmailListScreenModel: consumeErrorMessage() called.")
        uiState = uiState.copy(errorMessage = null)
    }

    // 如果你需要刷新功能，可以添加这个方法，并在 EmailListScreen 中调用
    fun refreshEmails() {
        println("EmailListScreenModel: refreshEmails() called.")
        // 重置状态并重新加载第一页
        uiState = uiState.copy(isRefreshing = true, emails = emptyList(), currentPage = 1, canLoadMore = true /* 刷新时可以重新尝试加载更多 */)
        loadInitialEmails() // 或者一个专门的 loadEmails(isRefresh = true)
    }

    // 如果你需要加载更多功能
    fun loadMoreEmails() {
        if (uiState.isLoading || !uiState.canLoadMore || uiState.isRefreshing) {
            println("EmailListScreenModel: loadMoreEmails() skipped. isLoading=${uiState.isLoading}, canLoadMore=${uiState.canLoadMore}, isRefreshing=${uiState.isRefreshing}")
            return
        }
        val nextPage = uiState.currentPage + 1
        println("EmailListScreenModel: loadMoreEmails() called for page $nextPage.")
        uiState = uiState.copy(isLoading = true, currentPage = nextPage) // 设置加载中，更新页码

        screenModelScope.launch {
            fetchEmailsUseCase(account, uiState.folderName, page = nextPage, pageSize = 50)
                .catch { e ->
                    println("EmailListScreenModel: loadMoreEmails Flow CATCH block: ${e.message}")
                    uiState = uiState.copy(isLoading = false, currentPage = nextPage - 1 /* 恢复到上一页 */, errorMessage = "加载更多邮件错误: ${e.message}")
                }
                .collect { result ->
                    println("EmailListScreenModel: loadMoreEmails COLLECT block, result is: $result")
                    when (result) {
                        is EmailResult.Success -> {
                            val newEmails = result.data
                            println("EmailListScreenModel: loadMoreEmails SUCCESS, new emails count: ${newEmails.size}")
                            uiState = uiState.copy(
                                isLoading = false,
                                emails = uiState.emails + newEmails, // 追加到现有列表
                                errorMessage = null,
                                canLoadMore = newEmails.size == 50 // 如果返回满页，则认为还可以加载
                            )
                        }
                        is EmailResult.Error -> {
                            println("EmailListScreenModel: loadMoreEmails ERROR: ${result.message ?: result.exception.message}")
                            uiState = uiState.copy(
                                isLoading = false,
                                currentPage = nextPage - 1, // 加载失败，恢复到上一页码
                                errorMessage = "加载更多邮件失败: ${result.message ?: result.exception.message}"
                            )
                        }
                    }
                    println("EmailListScreenModel: uiState after loadMoreEmails collect: $uiState")
                }
        }
    }
}