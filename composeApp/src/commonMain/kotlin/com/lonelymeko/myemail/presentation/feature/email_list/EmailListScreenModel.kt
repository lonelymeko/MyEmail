// Path: MyEmail/composeApp/src/commonMain/kotlin/com/lonelymeko/myemail/presentation/feature/email_list/EmailListScreenModel.kt
package com.lonelymeko.myemail.presentation.feature.email_list

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.lonelymeko.myemail.data.model.AccountInfo
import com.lonelymeko.myemail.data.model.EmailMessage
import com.lonelymeko.myemail.data.remote.api.EmailResult
import com.lonelymeko.myemail.domain.usecase.email.DeleteEmailUseCase
import com.lonelymeko.myemail.domain.usecase.email.FetchEmailsUseCase
import com.lonelymeko.myemail.domain.usecase.email.MarkEmailFlagsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class EmailListUiState(
    val accountEmail: String = "",
    val folderName: String = "",
    val isLoading: Boolean = true, // 用于初始加载和加载更多
    val emails: List<EmailMessage> = emptyList(),
    val errorMessage: String? = null,
    val currentPage: Int = 1,
    val canLoadMore: Boolean = true, // 初始假设可以加载更多
    val isRefreshing: Boolean = false // 用于下拉刷新
)

class EmailListScreenModel(
    private val account: AccountInfo,
    private val initialFolderName: String,
    private val fetchEmailsUseCase: FetchEmailsUseCase,
    private val markEmailFlagsUseCase: MarkEmailFlagsUseCase,
    private val deleteEmailUseCase: DeleteEmailUseCase
) : ScreenModel {

    var uiState by mutableStateOf(
        EmailListUiState(
            accountEmail = account.emailAddress,
            folderName = initialFolderName,
            isLoading = true
        )
    )
        private set

    private var fetchEmailsJob: Job? = null

    init {
        println("EmailListScreenModel: INIT for account='${account.emailAddress}', folder='$initialFolderName'")
        loadEmails(isInitialLoad = true)
    }

    fun loadEmails(isRefresh: Boolean = false, isInitialLoad: Boolean = false) {
        // 防止在已经在加载或刷新时重复触发（除非是强制刷新）
        if (!isRefresh && !isInitialLoad && (uiState.isLoading || uiState.isRefreshing)) {
            println("EmailListScreenModel: loadEmails skipped, already loading/refreshing.")
            return
        }

        val pageToLoad = if (isRefresh || isInitialLoad) 1 else uiState.currentPage + 1

        println("EmailListScreenModel: loadEmails - Account: ${account.emailAddress}, Folder: ${uiState.folderName}, Page: $pageToLoad, Refresh: $isRefresh, Initial: $isInitialLoad")

        uiState = when {
            isRefresh -> uiState.copy(isRefreshing = true, errorMessage = null, currentPage = 1) // 刷新时重置页码
            isInitialLoad -> uiState.copy(isLoading = true, emails = emptyList(), errorMessage = null, currentPage = 1)
            else -> uiState.copy(isLoading = true, errorMessage = null) // 加载更多
        }

        fetchEmailsJob?.cancel()
        fetchEmailsJob = screenModelScope.launch {
            fetchEmailsUseCase(account, uiState.folderName, page = pageToLoad, pageSize = 20)
                .catch { e ->
                    println("EmailListScreenModel: fetchEmailsUseCase Flow CATCH: ${e.message}")
                    uiState = uiState.copy(isLoading = false, isRefreshing = false, errorMessage = "Error: ${e.message}")
                }
                .collect { result ->
                    println("EmailListScreenModel: fetchEmailsUseCase COLLECTED: $result (Page: $pageToLoad)")
                    when (result) {
                        is EmailResult.Success -> {
                            val newEmails = result.data
                            val allEmails = if (isRefresh || isInitialLoad || pageToLoad == 1) {
                                newEmails // 刷新或第一页，替换
                            } else {
                                uiState.emails + newEmails // 加载更多，追加
                            }
                            uiState = uiState.copy(
                                isLoading = false,
                                isRefreshing = false,
                                emails = allEmails,
                                errorMessage = null,
                                canLoadMore = newEmails.size == 20, // 如果返回满页，则认为还可以加载
                                currentPage = pageToLoad
                            )
                        }
                        is EmailResult.Error -> {
                            uiState = uiState.copy(
                                isLoading = false,
                                isRefreshing = false,
                                errorMessage = result.message ?: "Failed to load emails",
                                currentPage = if (pageToLoad > 1 && !isRefresh && !isInitialLoad) pageToLoad - 1 else pageToLoad // 加载更多失败回滚页码
                            )
                        }
                    }
                }
        }
    }

    fun refreshEmails() {
        println("EmailListScreenModel: refreshEmails called for ${account.emailAddress}/${uiState.folderName}")
        if (!uiState.isRefreshing && !uiState.isLoading) { // 避免在其他加载操作进行时刷新
            loadEmails(isRefresh = true)
        }
    }

    fun loadMoreEmails() {
        println("EmailListScreenModel: loadMoreEmails called. CanLoadMore: ${uiState.canLoadMore}, IsLoading: ${uiState.isLoading}, IsRefreshing: ${uiState.isRefreshing}")
        if (uiState.canLoadMore && !uiState.isLoading && !uiState.isRefreshing) {
            loadEmails(isRefresh = false) // isRefresh = false 表示加载下一页 (loadEmails内部会处理pageToLoad)
        }
    }

    fun consumeErrorMessage() {
        if (uiState.errorMessage != null) {
            uiState = uiState.copy(errorMessage = null)
        }
    }

    override fun onDispose() {
        fetchEmailsJob?.cancel()
        println("EmailListScreenModel: DISPOSED for account='${account.emailAddress}', folder='${initialFolderName}'")
        super.onDispose()
    }
}