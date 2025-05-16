package com.lonelymeko.myemail.presentation.feature.compose_email


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.lonelymeko.myemail.data.model.AccountInfo
import com.lonelymeko.myemail.data.model.EmailMessage
import com.lonelymeko.myemail.data.remote.api.EmailResult
import com.lonelymeko.myemail.domain.usecase.account.GetActiveAccountFlowUseCase // 获取当前发件账户
import com.lonelymeko.myemail.domain.usecase.email.SendEmailUseCase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class ComposeEmailUiState(
    val fromAccount: AccountInfo? = null,
    val to: String = "",
    val cc: String = "",
    val bcc: String = "",
    val subject: String = "",
    val body: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val sendSuccess: Boolean = false
)

class ComposeEmailScreenModel(
    private val sendEmailUseCase: SendEmailUseCase,
    private val getActiveAccountFlowUseCase: GetActiveAccountFlowUseCase,
    // 可以接收回复/转发的初始数据
    private val initialTo: String? = null,
    private val initialSubject: String? = null,
    private val initialBody: String? = null // (引用正文)
) : ScreenModel {
    var uiState by mutableStateOf(ComposeEmailUiState())
        private set

    init {
        screenModelScope.launch {
            val activeAccount = getActiveAccountFlowUseCase().firstOrNull() // 获取一次当前活动账户
            uiState = uiState.copy(
                fromAccount = activeAccount,
                to = initialTo ?: "",
                subject = initialSubject ?: "",
                body = initialBody ?: ""
            )
        }
    }

    fun onToChanged(value: String) { uiState = uiState.copy(to = value) }
    fun onCcChanged(value: String) { uiState = uiState.copy(cc = value) }
    fun onBccChanged(value: String) { uiState = uiState.copy(bcc = value) }
    fun onSubjectChanged(value: String) { uiState = uiState.copy(subject = value) }
    fun onBodyChanged(value: String) { uiState = uiState.copy(body = value) }

    fun sendEmail() {
        val fromAcc = uiState.fromAccount ?: run {
            uiState = uiState.copy(errorMessage = "未选择发件账户")
            return
        }
        if (uiState.to.isBlank()) {
            uiState = uiState.copy(errorMessage = "收件人不能为空")
            return
        }

        uiState = uiState.copy(isLoading = true, errorMessage = null)
        val emailToSend = EmailMessage(
            messageServerId = "", folderName = "", // 发送时不需要
            subject = uiState.subject,
            fromAddress = fromAcc.emailAddress, // 将被 EmailService 中的 setFrom 覆盖
            toList = uiState.to.split(',', ';').map { it.trim() }.filter { it.isNotBlank() },
            ccList = uiState.cc.split(',', ';').map { it.trim() }.filter { it.isNotBlank() }.takeIf { it.isNotEmpty() },
            bccList = uiState.bcc.split(',', ';').map { it.trim() }.filter { it.isNotBlank() }.takeIf { it.isNotEmpty() },
            bodyPlainText = uiState.body, // 简化，只发送纯文本
            bodyHtml = null,
            sentDate = null, receivedDate = null, isRead = false, hasAttachments = false, attachments = null,
        )

        screenModelScope.launch {
            when (val result = sendEmailUseCase(fromAcc, emailToSend)) {
                is EmailResult.Success -> uiState = uiState.copy(isLoading = false, sendSuccess = true)
                is EmailResult.Error -> uiState = uiState.copy(isLoading = false, errorMessage = "发送失败: ${result.message}")
            }
        }
    }
    fun consumeErrorMessage() { uiState = uiState.copy(errorMessage = null) }
}