import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    jvm("desktop")
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "composeApp"
        browser {
            val rootDirPath = project.rootDir.path
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                outputFileName = "composeApp.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static = (static ?: mutableListOf()).apply {
                        // Serve sources to debug inside browser
                        add(rootDirPath)
                        add(projectDirPath)
                    }
                }
            }
        }
        binaries.executable()
    }
    
    sourceSets {
        val desktopMain by getting
        
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            // javax.mail for Android (注意：com.sun.mail 官方的 Android 版本)
            implementation("com.sun.mail:android-mail:1.6.7")
            implementation("com.sun.mail:android-activation:1.6.7") // JAF (JavaBeans Activation Framework)

            // SQLDelight Android 驱动
            implementation("app.cash.sqldelight:android-driver:2.0.1")

            // Koin for Android (包含 AndroidViewModel 支持等)
            implementation("io.insert-koin:koin-android:3.5.3")

            // AndroidX core & activity for compose integration
            implementation("androidx.core:core-ktx:1.12.0")
            implementation("androidx.activity:activity-compose:1.8.2")
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(libs.material.icons.core)
            implementation(libs.material.icons.extended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            // Koin (依赖注入)
            implementation(libs.koin.core) // Koin 核心库
            implementation(libs.koin.compose) // Koin 与 Jetpack Compose 集成 (如果使用 Compose Multiplatform)
            // Kotlin Coroutines (协程，用于异步操作)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
            implementation("com.russhwolf:multiplatform-settings-no-arg:1.1.1") // 基础库
            implementation("com.russhwolf:multiplatform-settings-serialization:1.3.0") // Kotlinx Serialization 支持 (用于存取对象)
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1") // JSON 序列化

            // SQLDelight (数据库)
//            implementation("app.cash.sqldelight:runtime:2.0.2") // SQLDelight 运行时
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            // javax.mail for JVM (使用 Jakarta Mail)
            implementation("com.sun.mail:jakarta.mail:2.0.1") // 或者使用 org.eclipse.angus:jakarta.mail

            // SQLDelight JVM (Desktop) 驱动
//            implementation("app.cash.sqldelight:sqlite-driver:2.0.1") // 这个驱动用于通用的JVM环境，包括桌面
        }
    }
}

android {
    namespace = "org.example.project"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.example.project"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "org.example.project.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "org.example.project"
            packageVersion = "1.0.0"
        }
    }
}
