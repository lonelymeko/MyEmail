package com.lonelymeko.myemail.di

import android.content.Context
import com.lonelymeko.myemail.data.remote.api.EmailService
import com.lonelymeko.myemail.platform.AndroidEmailServiceImpl


import com.russhwolf.settings.Settings // 确保导入
import com.russhwolf.settings.SharedPreferencesSettings // 确保导入
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val androidModule = module {
    // 提供 Settings 的 Android 实现 (之前我们简化方案时是这样做的)
    single<Settings> {
        val context: Context = androidContext()
        val prefsName = "my_email_app_settings_v1_direct"
        SharedPreferencesSettings(context.getSharedPreferences(prefsName, Context.MODE_PRIVATE))
    }
    single<EmailService> {
        AndroidEmailServiceImpl(ioDispatcher = get(named("IODispatcher")))
    }
    // 如果你还保留了 SettingsFactory 的 expect/actual 机制，那么这里应该是：
//     single<SettingsFactory> { AndroidSettingsFactory(androidContext()) }

}