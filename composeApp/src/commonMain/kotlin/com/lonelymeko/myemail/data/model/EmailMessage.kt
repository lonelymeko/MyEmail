package com.lonelymeko.myemail.data.model

import kotlinx.datetime.LocalDateTime // <--- 导入 LocalDateTime
import kotlinx.serialization.Serializable
// 稍后我们会创建并导入自定义的 LocalDateTimeSerializer
import com.lonelymeko.myemail.common.serialization.NullableLocalDateTimeSerializer // <--- 导入处理可空类型的序列化器

@Serializable
data class EmailMessage(
    val messageServerId: String,
    val folderName: String,

    val subject: String?,
    val fromAddress: String?,
    val toList: List<String>?,
    val ccList: List<String>?,
    val bccList: List<String>?,

    val bodyPlainText: String?,
    val bodyHtml: String?,

    // 修改类型为 LocalDateTime? 并应用自定义序列化器
    @Serializable(with = NullableLocalDateTimeSerializer::class)
    val sentDate: LocalDateTime?,

    @Serializable(with = NullableLocalDateTimeSerializer::class)
    val receivedDate: LocalDateTime?,

    var isRead: Boolean = false,
    var hasAttachments: Boolean = false,
    val attachments: List<EmailAttachmentInfo>? = null,
) {
    val displayFrom: String
        get() {
            if (fromAddress.isNullOrBlank()) return "未知发件人"
            val match = Regex("""(.*)<(.*)>""").find(fromAddress)
            return match?.groupValues?.get(1)?.trim()?.ifBlank { null }
                ?: fromAddress
        }

    val recipientsPreview: String
        get() {
            val allRecipients = mutableListOf<String>()
            toList?.let { allRecipients.addAll(it) }
            if (allRecipients.isEmpty()) return "无收件人"
            return allRecipients.joinToString(separator = "; ", limit = 3)
        }

    fun isSameMessageAs(other: EmailMessage?): Boolean {
        if (other == null) return false
        return this.messageServerId == other.messageServerId && this.folderName == other.folderName
    }
}