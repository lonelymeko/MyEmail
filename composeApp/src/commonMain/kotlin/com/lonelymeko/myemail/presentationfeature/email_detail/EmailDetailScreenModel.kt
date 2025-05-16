package com.lonelymeko.myemail.presentationfeature.email_detail


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope // 使用 screenModelScope
import com.lonelymeko.myemail.data.model.AccountInfo
import com.lonelymeko.myemail.data.model.EmailMessage
import com.lonelymeko.myemail.data.remote.api.EmailResult
import com.lonelymeko.myemail.domain.usecase.email.DeleteEmailUseCase
import com.lonelymeko.myemail.domain.usecase.email.GetEmailDetailsUseCase
import com.lonelymeko.myemail.domain.usecase.email.MarkEmailFlagsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

data class EmailDetailUiState(
    val emailMessage: EmailMessage? = null, // 当前显示的邮件详情
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    // 可以添加更多与邮件详情相关的状态，如附件下载进度等
)

class EmailDetailScreenModel(
    private val account: AccountInfo,       // 当前账户
    private val folderName: String,         // 邮件所在文件夹
    private val messageServerId: String,    // 邮件的服务器ID (UID)
    private val getEmailDetailsUseCase: GetEmailDetailsUseCase,
    private val markEmailFlagsUseCase: MarkEmailFlagsUseCase,
    private val deleteEmailUseCase: DeleteEmailUseCase
    // 如果需要下载附件，将来可以注入 DownloadAttachmentUseCase
) : ScreenModel {

    var uiState by mutableStateOf(EmailDetailUiState())
        private set

    private var loadDetailsJob: Job? = null

    init {
        fetchDetails()
        // 如果邮件是未读的，在进入详情时自动标记为已读
        // (这需要在获取到邮件后判断其 isRead 状态，或者由调用者先判断)
    }

    fun fetchDetails() {
        uiState = uiState.copy(isLoading = true, errorMessage = null)
        loadDetailsJob?.cancel()
        loadDetailsJob = screenModelScope.launch {
            when (val result = getEmailDetailsUseCase(account, folderName, messageServerId)) {
                is EmailResult.Success -> {
                    uiState = uiState.copy(isLoading = false, emailMessage = result.data)
                    // 如果邮件是未读的，现在标记它为已读
                    if (result.data.isRead == false) { // 注意：kotlin的Boolean?比较，这里假设isRead是非空的
                        markAsRead(true) // 调用标记为已读的方法
                    }
                }
                is EmailResult.Error -> {
                    uiState = uiState.copy(isLoading = false, errorMessage = "加载邮件详情失败: ${result.message ?: result.exception.message}")
                }
            }
        }
    }

    fun markAsRead(read: Boolean) {
        val currentEmail = uiState.emailMessage ?: return // 如果没有邮件，则不操作
        // 只有当期望的状态与当前状态不同时才操作
        if (currentEmail.isRead == read && read) return // 已经是已读，且要标记为已读，则不重复操作
        if (currentEmail.isRead == !read && !read) return // 已经是未读，且要标记为未读，则不重复操作


        screenModelScope.launch {
            // 实际调用API标记
            val apiResult = markEmailFlagsUseCase(account, folderName, listOf(messageServerId), markAsRead = read)
            if (apiResult is EmailResult.Success) {
                // API调用成功后，更新本地UI状态
                uiState = uiState.copy(
                    emailMessage = currentEmail.copy(isRead = read)
                )
                // TODO: 可能还需要通知邮件列表界面刷新该邮件的状态
            } else if (apiResult is EmailResult.Error) {
                uiState = uiState.copy(errorMessage = "标记已读/未读失败: ${apiResult.message}")
            }
        }
    }

    fun deleteEmail(): Flow<EmailResult<Unit>>? { // 返回 Flow 以便 UI 可以观察删除过程或结果
        val currentEmail = uiState.emailMessage ?: return null
        // 这里直接返回Flow，让UI层去收集并处理导航等。
        // 或者，也可以在这里处理结果并更新一个删除状态到uiState
        return kotlinx.coroutines.flow.flow {
            emit(deleteEmailUseCase(account, folderName, listOf(currentEmail.messageServerId)))
        }
    }

    fun consumeErrorMessage() {
        uiState = uiState.copy(errorMessage = null)
    }

    override fun onDispose() {
        loadDetailsJob?.cancel()
        super.onDispose()
    }
}