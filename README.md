# MyEmail KMP - 一个简单的跨平台邮件客户端

MyEmail KMP 是一个使用 Kotlin Multiplatform (KMP) 构建的简单邮件客户端示例项目。它旨在演示如何在 KMP 架构下实现邮件收发功能，支持 Android 和 Desktop (JVM) 平台，并使用 Jetpack Compose for UI。

## 特性 (目标)

*   **跨平台**: 基于 Kotlin Multiplatform，核心逻辑和 UI (部分) 在各平台共享。
    *   :heavy_check_mark: Android
    *   :heavy_check_mark: Desktop (JVM - Windows, macOS, Linux)
    *   (:heavy_multiplication_x: iOS - 当前版本未包含完整支持)
    *   (WasmJs - 实验性或有限功能)
*   **多账户支持**:
    *   添加和管理多个邮件账户。
    *   支持通用 IMAP/SMTP 服务器配置。
    *   预设 QQ 邮箱、网易163邮箱等常见邮箱的服务器模板。
*   **邮件核心功能**:
    *   连接测试：验证账户配置的有效性。
    *   邮件发送：撰写并发送邮件。
    *   邮件接收：获取邮件列表 (TODO)。
    *   邮件详情查看 (TODO)。
    *   标记邮件已读/未读 (TODO)。
    *   删除邮件 (TODO)。
*   **用户界面**:
    *   使用 Jetpack Compose for UI (Compose Multiplatform)。
    *   Material 3 设计风格。
    *   底部导航栏进行账户切换 (TODO)。
    *   侧边栏进行账户管理和设置 (TODO)。
*   **本地存储**:
    *   使用 `multiplatform-settings` 库以键值对形式在本地存储账户配置信息 (JSON 序列化)。
*   **依赖注入**:
    *   使用 Koin 进行依赖管理。
*   **导航**:
    *   使用 Voyager进行页面导航。

## 技术栈

*   **Kotlin Multiplatform (KMP)**
*   **Jetpack Compose / Compose Multiplatform** for UI
*   **Kotlin Coroutines** for asynchronous operations
*   **Kotlinx Serialization** for JSON processing
*   **Koin** for Dependency Injection
*   **Voyager** for Navigation
*   **Multiplatform Settings** for local key-value storage
*   **javax.mail (Jakarta Mail)** for email protocol implementation on Android and Desktop (JVM)
*   **Kotlinx DateTime** for date and time handling

## 项目结构

项目遵循典型的 KMP 架构，主要代码位于 `composeApp` 共享模块中：

*   `composeApp/src/commonMain`: 平台无关的核心逻辑、数据模型、仓库、UseCases、共享 UI (Compose)、Koin 模块定义、`expect` 声明。
*   `composeApp/src/androidMain`: Android 平台的 `actual` 实现 (如 `EmailService` using `javax.mail`, `Settings` 实现)、Android 特定的 Koin 模块、`MainActivity` 和 `MainApplication`。
*   `composeApp/src/desktopMain`: Desktop (JVM) 平台的 `actual` 实现、Desktop 特定的 Koin 模块、`main.kt` 入口。
*   `composeApp/src/wasmJsMain`: (如果保留) WebAssembly JS 平台的 `actual` 实现和入口。

## 如何构建和运行

### 前提条件

*   Android Studio (最新稳定版推荐，如 Hedgehog 或 Iguana) 或 IntelliJ IDEA (with Kotlin Multiplatform Mobile plugin)。
*   JDK 11 或更高版本 (推荐 JDK 17)。
*   配置好的 Android SDK (如果需要构建和运行 Android 应用)。
    *   确保已接受 Android SDK 许可证 (可通过 Android Studio SDK Manager 或 `sdkmanager --licenses` 命令)。
    *   在项目根目录的 `local.properties` 文件中正确设置 `sdk.dir`。

### 构建步骤

1.  克隆仓库:
    ```bash
    git clone https://github.com/your-username/MyEmailKMP.git # 替换为你的仓库地址
    cd MyEmailKMP
    ```
2.  使用 Android Studio 或 IntelliJ IDEA 打开项目。
3.  等待 Gradle 同步完成。
4.  选择目标平台并运行：
    *   **Android**: 选择 `composeApp` (如果它配置为应用) 或你的 `androidApp` 模块，然后选择一个模拟器或连接的设备，点击 "Run"。
    *   **Desktop**: 在 Gradle 任务中找到 `composeApp` -> `Tasks` -> `compose desktop` -> `run` (或者它可能被命名为 `desktopRun` 或类似)，双击运行。或者，如果配置了运行配置，直接运行。
    *   **WasmJs**: (如果配置) 通常通过 Gradle 任务 `wasmJsBrowserRun` 或类似任务在浏览器中运行。

### 测试 (本地 `main` 方法)

在 `composeApp/src/androidMain/kotlin/com/example/myemail/platform/AndroidEmailService.kt` (或 `DesktopEmailService.kt`) 文件底部有一个用于快速测试连接和发送邮件的 `main` 函数。
**重要**: 在运行此 `main` 函数前，你需要：
1.  将其中的占位符（如 `YOUR_SENDER_EMAIL@example.com`, `YOUR_EMAIL_AUTH_CODE`, 服务器地址, `RECEIVER_EMAIL_1@example.com`）替换为**你自己的真实有效的邮箱账户信息和授权码**。
2.  **切勿将你的真实凭据提交到版本控制系统中！**
3.  此 `main` 函数主要用于测试 `javax.mail` 的核心网络逻辑，它在纯 JVM 环境下运行，可能无法访问 Android `Context` (如果 `EmailService` 的构造函数需要它)。

## 注意事项和待办事项 (TODO)

*   **错误处理**: 需要更完善的错误处理和用户反馈机制。
*   **邮件内容解析与显示**:
    *   完整支持 HTML 邮件的渲染。
    *   处理邮件中的附件（下载、显示、附加到新邮件）。
    *   处理内嵌图片和复杂的 MIME 结构。
*   **IMAP 功能完善**:
    *   获取邮件列表 (分页、惰性加载)。
    *   文件夹管理 (获取文件夹列表、创建/删除/重命名文件夹)。
    *   邮件搜索。
    *   邮件同步策略。
*   **安全性**:
    *   **授权码存储**: 目前授权码直接存储，生产环境应考虑更安全的加密存储方案。
    *   **SSL/TLS 证书处理**: `ssl.trust = "*"` 是不安全的，生产环境需要正确的证书验证。
    *   考虑 OAuth2 支持（如果邮件服务商提供）。
*   **UI/UX 优化**:
    *   完善账户切换和管理界面。
    *   邮件列表的加载状态、空状态、错误状态显示。
    *   更丰富的邮件撰写编辑器。
*   **后台操作**: 对于邮件同步、发送等耗时操作，应考虑使用后台服务或 WorkManager (Android)。
*   **本地化/国际化 (i18n)**。
*   **单元测试和集成测试**。

## 贡献

欢迎提交 Pull Requests 或 Issues 来改进这个项目！

## 许可证

本项目采用 [MIT 许可证](LICENSE) (如果决定使用的话，添加一个 LICENSE 文件)。

---

**请根据你的实际情况进行修改：**

*   替换 `your-username/MyEmailKMP.git` 为你的实际 GitHub 仓库地址。
*   如果你使用了特定的库版本或有独特的配置，可以在 "技术栈" 或 "构建步骤" 中说明。
*   随着项目的进展，更新 "特性" 和 "待办事项" 列表。
*   如果你决定了项目的许可证，添加一个 `LICENSE` 文件并在 README 中链接它。
*   添加项目截图会更吸引人。

这个 README 提供了一个不错的起点，希望能帮助你向他人介绍你的项目！
