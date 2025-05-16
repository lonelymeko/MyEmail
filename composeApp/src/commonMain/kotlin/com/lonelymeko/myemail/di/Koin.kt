package com.lonelymeko.myemail.di

import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

import org.koin.core.context.startKoin
import org.koin.dsl.module // 需要这个导入来使用 module { ... } DSL

/**
 * 初始化 Koin。
 * @param platformSpecificModules 一个包含平台特定模块的列表。
 */
fun initKoin(platformSpecificModules: List<Module> = emptyList()) {
    startKoin {
        // printLogger() // Koin 内置的日志记录器 (用于调试 Koin 本身)
        modules(
            commonModule + // 使用 '+' 来合并模块列表
                    localStorageModule +
                    repositoryModule + // 新增：仓库模块
//                    useCaseModule +    // 新增：用例模块
                    screenModelModule +// 新增：ScreenModel 模块 (UI逻辑)
                    platformSpecificModules
        )
    }
}


/**
 * 通用工具和协程调度器
 */
val commonModule = module {
    // Coroutine Dispatchers (用于指定协程在哪个线程池运行)
    // 使用 Koin 的 named qualifier 来区分不同的 Dispatcher
    single(org.koin.core.qualifier.named("IODispatcher")) { kotlinx.coroutines.Dispatchers.IO }
    single(org.koin.core.qualifier.named("DefaultDispatcher")) { kotlinx.coroutines.Dispatchers.Default }
    // Main dispatcher 通常由 UI 框架 (如 Compose) 或 Voyager ScreenModel 提供，或平台自己管理
}

/**
 * 数据仓库相关的依赖
 */
val repositoryModule = module {
    // AccountRepository
    // get() 会自动解析 localStorageModule 中定义的 Settings 和 Json，以及 commonModule 中的 IODispatcher
    single<com.lonelymeko.myemail.data.repository.AccountRepository> {
        com.lonelymeko.myemail.data.repository.AccountRepositoryImpl(
            settings = get(),
            json = get(),
            ioDispatcher = get(org.koin.core.qualifier.named("IODispatcher"))
        )
    }

    // EmailRepository
    // EmailService 的 actual 实现将由平台模块提供并注入到这里
    single<com.lonelymeko.myemail.data.repository.EmailRepository> {
        com.lonelymeko.myemail.data.repository.EmailRepositoryImpl(
            emailService = get(), // get() 会寻找平台提供的 EmailService actual 实现
            ioDispatcher = get(org.koin.core.qualifier.named("IODispatcher"))
        )
    }
}



/**
 * 表示层 ScreenModels (UI 逻辑处理器)
 * 注意：ScreenModel 的构造通常在 UI 层通过 koinInject() 或 getScreenModel() (来自 Voyager) 完成，
 * Koin 模块在这里定义了如何创建它们。
 * 如果 ScreenModel 有导航参数，通常在 UI 层实例化时传递。
 */
val screenModelModule = module {
    // 示例 (需要先创建这些 ScreenModel 类)
    // factory { params -> com.example.myemail.presentation.feature.add_account.AddAccountScreenModel(get(), get()) }
    // factory { com.example.myemail.presentation.feature.bottom_nav.MainScreenModel(get(), get()) }
    // ... 其他 ScreenModel 定义 ...
}