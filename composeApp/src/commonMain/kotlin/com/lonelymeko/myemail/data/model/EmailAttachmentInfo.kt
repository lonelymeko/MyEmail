package com.lonelymeko.myemail.data.model

import kotlinx.serialization.Serializable

@Serializable // 虽然主要从网络获取，但如果 ViewModels 或 UseCases 之间传递，Serializable 有好处
data class EmailAttachmentInfo(
    val fileName: String,      // 附件文件名
    val contentType: String,   // MIME类型, e.g., "image/jpeg", "application/pdf"
    val sizeBytes: Long        // 附件大小 (字节)
    // 实际下载附件可能还需要其他信息，如在邮件中的 content ID (用于内嵌图片)
)