package com.lonelymeko.myemail.presentation.feature.add_account


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope


import com.lonelymeko.myemail.data.model.AccountInfo
import com.lonelymeko.myemail.data.model.AccountInfo.Companion.createNetease163Account
import com.lonelymeko.myemail.data.model.AccountInfo.Companion.createQqAccount
import com.lonelymeko.myemail.data.model.AccountType
//import com.lonelymeko.myemail.data.model.createNetease163Account
//import com.lonelymeko.myemail.data.model.createQqAccount
import com.lonelymeko.myemail.data.remote.api.EmailResult
import com.lonelymeko.myemail.domain.usecase.account.AddAccountUseCase
import com.lonelymeko.myemail.domain.usecase.account.SetActiveAccountUseCase
import com.lonelymeko.myemail.domain.usecase.email.TestConnectionUseCase // 新增 UseCase
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

// 定义 UI 状态
data class AddAccountUiState(
    val emailAddress: String = "",
    val passwordOrAuthCode: String = "",
    val selectedAccountType: AccountType = AccountType.GENERIC_IMAP_SMTP,
    val displayName: String = "",
    val imapHost: String = "",
    val imapPort: String = "993", // 通常是 993 for IMAPS
    val imapUseSsl: Boolean = true,
    val smtpHost: String = "",
    val smtpPort: String = "465", // 通常是 465 for SMTPS or 587 for STARTTLS
    val smtpUseSsl: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val connectionTestSuccess: Boolean? = null, // null: not tested, true: success, false: failed
    val addAccountSuccess: Boolean = false
)

class AddAccountScreenModel(
    private val addAccountUseCase: AddAccountUseCase,
    private val setActiveAccountUseCase: SetActiveAccountUseCase,
    private val testConnectionUseCase: TestConnectionUseCase // 注入 TestConnectionUseCase
) : ScreenModel {

    var uiState by mutableStateOf(AddAccountUiState())
        private set

    fun onEmailChanged(email: String) {
        uiState = uiState.copy(emailAddress = email, displayName = if (uiState.displayName.isBlank() || uiState.displayName == uiState.emailAddress.substringBefore('@')) email.substringBefore('@') else uiState.displayName)
        // 尝试根据邮箱后缀自动填充类型和服务器信息
        autoFillBasedOnEmail(email)
    }

    fun onPasswordChanged(password: String) {
        uiState = uiState.copy(passwordOrAuthCode = password)
    }

    fun onAccountTypeChanged(type: AccountType) {
        uiState = uiState.copy(selectedAccountType = type)
        prefillServerInfo(type, uiState.emailAddress)
    }

    fun onDisplayNameChanged(name: String) {
        uiState = uiState.copy(displayName = name)
    }

    // ... 其他字段的 onXxxChanged 方法 ...
    fun onImapHostChanged(host: String) { uiState = uiState.copy(imapHost = host) }
    fun onImapPortChanged(port: String) { uiState = uiState.copy(imapPort = port) }
    fun onImapSslChanged(useSsl: Boolean) { uiState = uiState.copy(imapUseSsl = useSsl) }
    fun onSmtpHostChanged(host: String) { uiState = uiState.copy(smtpHost = host) }
    fun onSmtpPortChanged(port: String) { uiState = uiState.copy(smtpPort = port) }
    fun onSmtpSslChanged(useSsl: Boolean) { uiState = uiState.copy(smtpUseSsl = useSsl) }


    private fun autoFillBasedOnEmail(email: String) {
        when {
            email.endsWith("@qq.com", ignoreCase = true) -> onAccountTypeChanged(AccountType.QQ)
            email.endsWith("@163.com", ignoreCase = true) || email.endsWith("@126.com", ignoreCase = true) || email.endsWith("@yeah.net", ignoreCase = true) -> onAccountTypeChanged(AccountType.NETEASE_163)
            // 可以添加更多常用邮箱的自动填充规则
            else -> {
                if (uiState.selectedAccountType != AccountType.GENERIC_IMAP_SMTP) {
                    onAccountTypeChanged(AccountType.GENERIC_IMAP_SMTP) // 如果不匹配已知类型，则重置为通用类型
                }
            }
        }
    }
    private fun prefillServerInfo(type: AccountType, email: String) {
        val accountTemplate = when (type) {
            AccountType.QQ -> createQqAccount(email, "") // 密码暂时为空
            AccountType.NETEASE_163 -> createNetease163Account(email, "")
            AccountType.GENERIC_IMAP_SMTP -> AccountInfo(emailAddress = email, passwordOrAuthCode = "", imapHost = "", imapPort = 993, smtpHost = "", smtpPort = 465, accountType = type/*临时*/)
        }
        uiState = uiState.copy(
            imapHost = accountTemplate.imapHost,
            imapPort = accountTemplate.imapPort.toString(),
            imapUseSsl = accountTemplate.imapUseSsl,
            smtpHost = accountTemplate.smtpHost,
            smtpPort = accountTemplate.smtpPort.toString(),
            smtpUseSsl = accountTemplate.smtpUseSsl
        )
    }

    fun testConnection() {
        if (uiState.emailAddress.isBlank() || uiState.passwordOrAuthCode.isBlank()) {
            uiState = uiState.copy(errorMessage = "邮箱地址和授权码不能为空", connectionTestSuccess = false)
            return
        }
        val accountToTest = buildAccountInfoFromState() ?: return

        uiState = uiState.copy(isLoading = true, errorMessage = null, connectionTestSuccess = null)
        screenModelScope.launch {
            when (val result = testConnectionUseCase(accountToTest)) {
                is EmailResult.Success<*> -> {
                    uiState = uiState.copy(isLoading = false, connectionTestSuccess = true, errorMessage = "连接成功！")
                }
                is EmailResult.Error -> {
                    uiState = uiState.copy(isLoading = false, connectionTestSuccess = false, errorMessage = "连接失败: ${result.message ?: result.exception.message}")
                }
            }
        }
    }

    fun addAccount() {
        if (uiState.emailAddress.isBlank() || uiState.passwordOrAuthCode.isBlank()) {
            uiState = uiState.copy(errorMessage = "邮箱地址和授权码不能为空")
            return
        }
        val accountToAdd = buildAccountInfoFromState() ?: return

        uiState = uiState.copy(isLoading = true, errorMessage = null)
        screenModelScope.launch {
            // 最好在添加前再测试一次连接，或者依赖用户已测试
            val addResult = addAccountUseCase(accountToAdd)
            if (addResult.isSuccess) {
                // 添加成功后，将此账户设为活动账户
                setActiveAccountUseCase(accountToAdd.emailAddress)
                uiState = uiState.copy(isLoading = false, addAccountSuccess = true)
            } else {
                uiState = uiState.copy(isLoading = false, errorMessage = "添加账户失败: ${addResult.exceptionOrNull()?.message}")
            }
        }
    }

    private fun buildAccountInfoFromState(): AccountInfo? {
        val imapPortInt = uiState.imapPort.toIntOrNull() ?: run {
            uiState = uiState.copy(errorMessage = "IMAP 端口号无效")
            return null
        }
        val smtpPortInt = uiState.smtpPort.toIntOrNull() ?: run {
            uiState = uiState.copy(errorMessage = "SMTP 端口号无效")
            return null
        }
        return AccountInfo(
            emailAddress = uiState.emailAddress,
            passwordOrAuthCode = uiState.passwordOrAuthCode,
            displayName = uiState.displayName.ifBlank { uiState.emailAddress.substringBefore('@') },
            imapHost = uiState.imapHost,
            imapPort = imapPortInt,
            imapUseSsl = uiState.imapUseSsl,
            smtpHost = uiState.smtpHost,
            smtpPort = smtpPortInt,
            smtpUseSsl = uiState.smtpUseSsl,
            accountType = uiState.selectedAccountType
        )
    }

    fun consumeErrorMessage() {
        uiState = uiState.copy(errorMessage = null)
    }
    fun consumeConnectionTestResult() {
        uiState = uiState.copy(connectionTestSuccess = null)
    }
}