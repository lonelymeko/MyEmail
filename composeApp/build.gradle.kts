
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinxSerialization) // <--- 确保这一行存在且正确！
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    
    jvm("desktop"){
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
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
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.multiplatform.settings.no.arg) // 基础库
            implementation(libs.multiplatform.settings.serialization) // Kotlinx Serialization 支持 (用于存取对象)
            implementation(libs.kotlinx.serialization.json)
            // Voyager (导航库, 可选但推荐)
            implementation("cafe.adriel.voyager:voyager-navigator:1.1.0-beta03") // 核心导航
            implementation("cafe.adriel.voyager:voyager-transitions:1.1.0-beta03") // 页面切换动画
            implementation("cafe.adriel.voyager:voyager-koin:1.1.0-beta03")      // Voyager 与 Koin 集成
            implementation("cafe.adriel.voyager:voyager-bottom-sheet-navigator:1.1.0-beta03") // 底部导航栏可能用到
            implementation("cafe.adriel.voyager:voyager-tab-navigator:1.1.0-beta03") // Tab导航
            implementation(libs.kotlinx.datetime)
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
    namespace = "com.lonelymeko.myemail"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.lonelymeko.myemail"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            // 排除重复的 LICENSE.md 文件
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
            excludes += "META-INF/NOTICE.md"
            // 如果需要排除其他文件，可以继续添加
            excludes += "META-INF/*.kotlin_module"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }


dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "com.lonelymeko.myemail.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.lonelymeko.myemail"
            packageVersion = "1.0.0"
        }
    }
}}
