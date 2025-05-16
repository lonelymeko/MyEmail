package com.lonelymeko.myemail.data.remote.api
/**
 * 用于封装 EmailService 操作结果的密封类。
 * @param T 成功时的数据类型。
 */
sealed class EmailResult<out T> {
    /**
     * 表示操作成功。
     * @param data 操作成功返回的数据。
     */
    data class Success<T>(val data: T) : EmailResult<T>()

    /**
     * 表示操作失败。
     * @param exception 发生的异常。
     * @param message 可选的错误描述信息。
     */
    data class Error(val exception: Exception, val message: String? = null) : EmailResult<Nothing>()
}