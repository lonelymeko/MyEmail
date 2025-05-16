package com.lonelymeko.myemail.di


import com.lonelymeko.myemail.data.model.AccountInfo
import com.lonelymeko.myemail.data.model.EmailMessage
import com.lonelymeko.myemail.data.remote.api.EmailService // commonMain interface
import com.lonelymeko.myemail.data.repository.AccountRepository
import com.lonelymeko.myemail.data.repository.AccountRepositoryImpl
import com.lonelymeko.myemail.data.repository.EmailRepository
import com.lonelymeko.myemail.data.repository.EmailRepositoryImpl
import com.lonelymeko.myemail.domain.usecase.account.*
import com.lonelymeko.myemail.domain.usecase.email.*
import com.lonelymeko.myemail.presentation.feature.add_account.AddAccountScreenModel
import com.lonelymeko.myemail.presentation.feature.bottom_nav.MainScreenModel
import com.lonelymeko.myemail.presentation.feature.compose_email.ComposeEmailScreenModel
import com.lonelymeko.myemail.presentation.feature.email_list.EmailListScreenModel
import com.lonelymeko.myemail.presentation.feature.email_list.formatDateForList
// 导入其他 ScreenModel (当你创建它们时)
// import com.example.myemail.presentation.feature.email_list.EmailListScreenModel
// import com.example.myemail.presentation.feature.email_detail.EmailDetailScreenModel
// import com.example.myemail.presentation.feature.compose_email.ComposeEmailScreenModel

import com.russhwolf.settings.Settings // multiplatform-settings 接口
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf // 用于简化 UseCase 和 ScreenModel 的工厂定义
import org.koin.core.parameter.ParametersHolder
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.reflect.KClass

// --- Koin 初始化函数 ---
fun initKoin(platformSpecificModules: List<Module> = emptyList()) {
    startKoin {
        // printLogger() // Koin 日志 (用于调试)
        modules(
            commonModule,
            localStorageModule,
            repositoryModule,
            useCaseModule,
            screenModelModule
            // 确保 platformSpecificModules 被添加到列表的末尾或合适的位置
            // 以便平台实现可以覆盖或提供 commonMain 声明的接口/expect类
        )
        // 在 Koin 3.2+ 中，可以直接传递模块列表给 modules()
        // 如果 platformSpecificModules 是一个 vararg Module，可以这样：
        // modules(listOf(commonModule, localStorageModule, ...) + platformSpecificModules)
        // 或者如果 modules() 接受 vararg:
        // modules(commonModule, localStorageModule, ..., *platformSpecificModules.toTypedArray())
        // 最简单的方式是确保 platformSpecificModules 包含在传递给 modules() 的总列表中：
        val allModules = mutableListOf(
            commonModule,
            localStorageModule,
            repositoryModule,
            // serviceModule, // EmailService 接口在 commonMain, 实现由平台提供并注入到 repositoryModule
            useCaseModule,
            screenModelModule
        )
        allModules.addAll(platformSpecificModules)
        modules(allModules)
    }
}


// --- Koin 模块定义 ---

/**
 * 通用工具和协程调度器
 */
val commonModule = module {
    single(named("IODispatcher")) { Dispatchers.IO }
    single(named("DefaultDispatcher")) { Dispatchers.Default }
    // Main dispatcher (UI) 通常由平台或 UI 框架 (Compose/Voyager) 处理，
    // 或者如果需要在 ScreenModel 中显式使用，可以 expect/actual 提供。

}

/**
 * 本地存储相关的依赖 (Settings, Json)
 */
val localStorageModule = module {
    // Settings 实例将由平台模块 (AndroidModule, DesktopModule 等) 直接创建并绑定为 single<Settings>
    // commonMain 的模块不再负责创建 Settings，只负责使用它。
    // 因此，这里不需要 single<SettingsFactory> 或 single<Settings> 的定义。
    // Koin 会在需要 Settings 时，从平台模块中找到已注册的 Settings 实例。

    // 提供 kotlinx.serialization.json.Json 实例
    single<Json> {
        Json {
            isLenient = true
            ignoreUnknownKeys = true
            prettyPrint = false // 生产环境设为 false
            encodeDefaults = true
        }
    }
}

/**
 * 数据仓库相关的依赖
 */
val repositoryModule = module {
    // AccountRepository
    single<AccountRepository> {
        AccountRepositoryImpl(
            settings = get(), // Koin 会从平台模块中找到 Settings
            json = get(),     // Koin 会从 localStorageModule 中找到 Json
            ioDispatcher = get(named("IODispatcher")) // Koin 会从 commonModule 中找到 IODispatcher
        )
    }

    // EmailRepository
    single<EmailRepository> {
        EmailRepositoryImpl(
            emailService = get(), // Koin 会从平台模块中找到 EmailService 的实现
            ioDispatcher = get(named("IODispatcher"))
        )
    }
}

/**
 * 服务层 (如果 EmailService 的 commonMain 部分有独立逻辑，不常用，因为我们用了接口)
 * 通常 EmailService 接口在 commonMain，实现类在平台模块并直接绑定到 EmailService 接口。
 * 所以这个 serviceModule 可能不是必需的，除非你有其他 common 服务。
 */
// val serviceModule = module {
//     // EmailService 接口的实现由平台模块提供
// }


/**
 * 领域层 UseCases (用例)
 */
val useCaseModule = module {
    // Account UseCases
    factoryOf(::AddAccountUseCase)
    factoryOf(::DeleteAccountUseCase)
    factoryOf(::GetAccountByEmailUseCase)
    factoryOf(::GetAccountsFlowUseCase)
    factoryOf(::GetActiveAccountFlowUseCase)
    factoryOf(::SetActiveAccountUseCase)
    // Email UseCases
    factoryOf(::DeleteEmailUseCase)
    factoryOf(::FetchEmailsUseCase)
    factoryOf(::GetEmailDetailsUseCase)
    factoryOf(::MarkEmailFlagsUseCase)
    factoryOf(::SendEmailUseCase)
    factoryOf(::TestConnectionUseCase) // 我们为账户添加时创建的
}

/**
 * 表示层 ScreenModels (UI 逻辑处理器)
 */
val screenModelModule = module {
    factory {
        AddAccountScreenModel(
            addAccountUseCase = get(),
            setActiveAccountUseCase = get(),
            testConnectionUseCase = get()
            // 如果 ScreenModel 需要 CoroutineScope 或 Dispatcher，通常由 Voyager 的
            // getScreenModel 或自定义的 ScreenModelFactory 处理，或者直接注入
            // ioDispatcher = get(named("IODispatcher")) // 示例：如果 ScreenModel 需要它
        )
    }
    factory {
        MainScreenModel(
            getAccountsFlowUseCase = get(),
            getActiveAccountFlowUseCase = get(),
            setActiveAccountUseCase = get()
        )
    }
    factory { params: ParametersHolder -> // 让 factory 显式接收 ParametersHolder
        EmailListScreenModel(
            account = params.get<AccountInfo>(0),      // 从参数中按索引0获取 AccountInfo
            initialFolderName = params.get<String>(1), // 从参数中按索引1获取 String (folderName)
            fetchEmailsUseCase = get(),                // Koin 会自动注入这些 UseCase
            markEmailFlagsUseCase = get(),
            deleteEmailUseCase = get()
        )
    }
    factory { params: ParametersHolder ->
        // 从 ParametersHolder 安全地获取可空参数
        // Koin 的 params.getOrNull<Type>() 或 params.getOrNull<Type>(index) 可以用
        // 但更通用的方式是检查参数数量
        val replyTo: EmailMessage? = if (params.size() > 0) params.get<EmailMessage>(0) else null
        val forward: EmailMessage? = if (params.size() > 1) params.get<EmailMessage>(1) else null
        // 或者，如果参数总是按顺序传递（即使是null），可以直接用 getOrNull(index)

        // 我们需要根据 replyTo/forward 来构造 initialTo, initialSubject, initialBody
        var initialTo: String? = null
        var initialSubject: String? = null
        var initialBody: String? = null

        if (replyTo != null) {
            initialTo = replyTo.fromAddress
            initialSubject = if (replyTo.subject?.startsWith("Re: ", ignoreCase = true) == true) {
                replyTo.subject
            } else {
                "Re: ${replyTo.subject ?: ""}"
            }
            // 构建引用正文
            initialBody = "\n\n--- Original Message on ${replyTo.sentDate?.formatDateForList() ?: replyTo.receivedDate?.formatDateForList()} ---\n" +
                    "From: ${replyTo.fromAddress ?: ""}\n" +
                    "To: ${replyTo.toList?.joinToString() ?: ""}\n" +
                    "Subject: ${replyTo.subject ?: ""}\n\n" +
                    (replyTo.bodyPlainText ?: replyTo.bodyHtml?.replace(Regex("<[^>]*>"), "") ?: "") // 简单的引用
        } else if (forward != null) {
            initialSubject = if (forward.subject?.startsWith("Fwd: ", ignoreCase = true) == true) {
                forward.subject
            } else {
                "Fwd: ${forward.subject ?: ""}"
            }
            initialBody = "\n\n--- Forwarded Message ---\n" +
                    "From: ${forward.fromAddress ?: ""}\n" +
                    "Sent: ${forward.sentDate?.formatDateForList() ?: forward.receivedDate?.formatDateForList()}\n" +
                    "To: ${forward.toList?.joinToString() ?: ""}\n" +
                    "Subject: ${forward.subject ?: ""}\n\n" +
                    (forward.bodyPlainText ?: forward.bodyHtml ?: "") // 转发通常包含完整内容
        }


        ComposeEmailScreenModel(
            sendEmailUseCase = get(),
            getActiveAccountFlowUseCase = get(),
            initialTo = initialTo,
            initialSubject = initialSubject,
            initialBody = initialBody
        )
    }
    // 当你创建 EmailListScreenModel 等时，在这里添加它们的工厂定义
    // factory { params -> EmailListScreenModel(get(), get(), params.getOrNull(), params.getOrNull()) }
    // factory { params -> EmailDetailScreenModel(get(), get(), params.get()) }
    // factory { params -> ComposeEmailScreenModel(get(), params.getOrNull()) }
}