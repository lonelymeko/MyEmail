package com.lonelymeko.myemail.data.model

import kotlinx.serialization.Serializable

@Serializable   //  序列化
enum class AccountType {
    QQ, // QQ
    NETEASE_163,    //  网易邮箱
    GENERIC_IMAP_SMIO   // 自定义

}