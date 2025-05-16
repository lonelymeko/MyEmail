package com.lonelymeko.myemail.data.repository

import com.lonelymeko.myemail.data.model.AccountInfo
import com.russhwolf.settings.Settings // 直接使用基础的 Settings 接口
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json // 我们仍然需要 Json 来手动序列化/反序列化

private const val ACCOUNTS_LIST_KEY = "app_accounts_list_v1_manual_json" // 键名，区分一下
private const val ACTIVE_ACCOUNT_EMAIL_KEY = "app_active_account_email_v1_manual_json"

@OptIn(ExperimentalCoroutinesApi::class) // For StateFlow/Flow operators
class AccountRepositoryImpl(
    private val settings: Settings, // 通过 Koin 注入 (来自你的 SettingsWrapper)
    private val json: Json,         // 通过 Koin 注入
    private val ioDispatcher: CoroutineDispatcher
) : AccountRepository {

    // 使用 MutableStateFlow 来驱动响应式更新
    private val _accountsFlow = MutableStateFlow(loadAccountsFromSettings())
    private val _activeAccountEmailFlow = MutableStateFlow(settings.getStringOrNull(ACTIVE_ACCOUNT_EMAIL_KEY))

    override suspend fun addAccount(accountInfo: AccountInfo): Result<Unit> = withContext(ioDispatcher) {
        try {
            val currentAccounts = _accountsFlow.value.toMutableList()
            if (currentAccounts.any { it.emailAddress == accountInfo.emailAddress }) {
                Result.failure(IllegalArgumentException("账户 '${accountInfo.emailAddress}' 已存在。"))
            } else {
                currentAccounts.add(accountInfo)
                saveAccountsToSettings(currentAccounts) // 手动序列化并保存
                _accountsFlow.value = currentAccounts.toList()
                if (currentAccounts.size == 1) {
                    setActiveAccount(accountInfo.emailAddress)
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getAccountsFlow(): Flow<List<AccountInfo>> = _accountsFlow.asStateFlow()

    override suspend fun getAccountByEmail(emailAddress: String): Result<AccountInfo?> = withContext(ioDispatcher) {
        try {
            Result.success(_accountsFlow.value.find { it.emailAddress == emailAddress })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAccount(emailAddress: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val currentAccounts = _accountsFlow.value.toMutableList()
            val removed = currentAccounts.removeAll { it.emailAddress == emailAddress }
            if (removed) {
                saveAccountsToSettings(currentAccounts)
                _accountsFlow.value = currentAccounts.toList()
                if (_activeAccountEmailFlow.value == emailAddress) {
                    setActiveAccount(currentAccounts.firstOrNull()?.emailAddress)
                }
                Result.success(Unit)
            } else {
                Result.failure(NoSuchElementException("未找到账户: $emailAddress"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setActiveAccount(emailAddress: String?): Result<Unit> = withContext(ioDispatcher) {
        try {
            if (emailAddress == null) {
                settings.remove(ACTIVE_ACCOUNT_EMAIL_KEY)
            } else {
                if (!_accountsFlow.value.any { it.emailAddress == emailAddress }) {
                    return@withContext Result.failure(NoSuchElementException("无法设为活动账户，账户 '$emailAddress' 不存在。"))
                }
                settings.putString(ACTIVE_ACCOUNT_EMAIL_KEY, emailAddress)
            }
            _activeAccountEmailFlow.value = emailAddress
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getActiveAccountFlow(): Flow<AccountInfo?> {
        return combine(_activeAccountEmailFlow, _accountsFlow) { activeEmail, accounts ->
            accounts.find { it.emailAddress == activeEmail }
        }.distinctUntilChanged()
    }

    // --- 手动序列化/反序列化辅助函数 ---
    private fun loadAccountsFromSettings(): List<AccountInfo> {
        return try {
            val jsonString = settings.getStringOrNull(ACCOUNTS_LIST_KEY)
            if (jsonString.isNullOrBlank()) {
                emptyList()
            } else {
                // 使用 Json 实例手动解码
                json.decodeFromString(ListSerializer(AccountInfo.serializer()), jsonString)
            }
        } catch (e: Exception) {
            println("加载账户列表失败 (手动JSON): ${e.message}")
            emptyList()
        }
    }

    private fun saveAccountsToSettings(accounts: List<AccountInfo>) {
        try {
            // 使用 Json 实例手动编码
            val jsonString = json.encodeToString(ListSerializer(AccountInfo.serializer()), accounts)
            settings.putString(ACCOUNTS_LIST_KEY, jsonString)
        } catch (e: Exception) {
            println("保存账户列表失败 (手动JSON): ${e.message}")
        }
    }

    init {
        _accountsFlow.value = loadAccountsFromSettings()
        _activeAccountEmailFlow.value = settings.getStringOrNull(ACTIVE_ACCOUNT_EMAIL_KEY)
    }
}