package com.lonelymeko.myemail.di


// com.russhwolf.settings.Settings 将由平台模块直接提供

import com.russhwolf.settings.Settings // 确保导入基础的 Settings 接口
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val localStorageModule = module {
    // Settings 实例将由平台模块 (AndroidModule, DesktopModule 等) 直接提供并绑定
    // commonMain 中的模块不再关心 Settings 是如何创建的，只关心它被提供了。
    // 因此，这里不需要 single<Settings> { ... } 的定义了，
    // 除非你想在这里定义一个 expect Settings，然后平台 actual 提供，但这又回到了 expect/actual。
    // 更简单的做法是，平台模块直接 single<Settings> { ... } 绑定自己的实现。

    // 我们仍然需要提供 Json 实例
    single<Json> {
        Json {
            isLenient = true
            ignoreUnknownKeys = true
            prettyPrint = false
            encodeDefaults = true
        }
    }
}