package com.lonelymeko.myemail.di

import com.lonelymeko.myemail.data.remote.api.EmailService // commonMain expect
import com.lonelymeko.myemail.platform.DesktopEmailServiceImpl
import com.russhwolf.settings.PreferencesSettings // 确保导入
import com.russhwolf.settings.Settings // 确保导入
import org.koin.dsl.module
import java.util.prefs.Preferences // 确保导入

val desktopModule = module {
    // 提供 Settings 的 Desktop 实现
    single<Settings> {
        val prefsNodeName = "com.example.myemail.desktop.settings_v1_direct"
        PreferencesSettings(Preferences.userRoot().node(prefsNodeName))
    }
    // 如果你还保留了 SettingsFactory 的 expect/actual 机制，那么这里应该是：
    // single<com.example.myemail.data.local.SettingsFactory> { com.example.myemail.platform.DesktopSettingsFactory() }


    // 提供 EmailService 的 actual 实现
    single<EmailService> { DesktopEmailServiceImpl(/* 如果需要 Desktop 特定的依赖 */) }
}