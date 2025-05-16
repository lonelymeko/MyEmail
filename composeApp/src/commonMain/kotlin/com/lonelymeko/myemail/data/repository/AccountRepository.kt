package com.lonelymeko.myemail.data.repository

import com.lonelymeko.myemail.data.model.AccountInfo
import kotlinx.coroutines.flow.Flow


/**
 * 账户数据仓库接口。
 * 负责账户信息的持久化存储和检索。
 */
interface AccountRepository {
    /**
     * 添加一个新的邮件账户。
     * 如果账户已存在（基于 emailAddress），则操作可能失败或更新现有账户（取决于实现策略）。
     * @param accountInfo 要添加的账户信息。
     * @return Result<Unit> 表示操作成功或失败（及错误信息）。
     */
    suspend fun addAccount(accountInfo: AccountInfo): Result<Unit>

    /**
     * 获取所有已存储的邮件账户列表。
     * 使用 Flow 以便在账户列表发生变化时 UI 可以自动更新。
     * @return 一个 Flow，它会发出当前的账户列表。
     */
    fun getAccountsFlow(): Flow<List<AccountInfo>>

    /**
     * 根据邮箱地址获取特定的账户信息。
     * @param emailAddress 要查找的账户的邮箱地址。
     * @return Result 包含找到的 AccountInfo (如果存在) 或 null，或者错误信息。
     */
    suspend fun getAccountByEmail(emailAddress: String): Result<AccountInfo?>

    /**
     * 删除指定邮箱地址的账户。
     * @param emailAddress 要删除的账户的邮箱地址。
     * @return Result<Unit> 表示操作成功或失败。
     */
    suspend fun deleteAccount(emailAddress: String): Result<Unit>

    /**
     * 设置当前活动的邮件账户。
     * @param emailAddress 要设为活动账户的邮箱地址。如果为 null，则表示没有活动账户。
     * @return Result<Unit> 表示操作成功或失败。
     */
    suspend fun setActiveAccount(emailAddress: String?): Result<Unit>

    /**
     * 获取当前活动的邮件账户。
     * 使用 Flow 以便在活动账户发生变化时 UI 可以自动更新。
     * @return 一个 Flow，它会发出当前的活动账户信息，如果没有活动账户则发出 null。
     */
    fun getActiveAccountFlow(): Flow<AccountInfo?>
}