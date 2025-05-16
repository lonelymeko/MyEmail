package com.lonelymeko.myemail.presentation.feature.add_account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel // 用于获取 ScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.lonelymeko.myemail.data.model.AccountType

// 使用之前定义的 AddAccountScreenRoute
data class AddAccountScreen(val initialEmail: String? = null) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = getScreenModel<AddAccountScreenModel>()
         val uiState = screenModel.uiState

        var passwordVisible by remember { mutableStateOf(false) }

        // 当添加账户成功时，自动返回上一页
        LaunchedEffect(uiState.addAccountSuccess) {
            if (uiState.addAccountSuccess) {
                navigator.pop() // 或导航到主界面 navigator.replaceAll(MainContentScreenRoute)
            }
        }
        // 当有错误消息时，显示 Snackbar
        val snackbarHostState = remember { SnackbarHostState() }
        LaunchedEffect(uiState.errorMessage) {
            uiState.errorMessage?.let {
                snackbarHostState.showSnackbar(it)
                screenModel.consumeErrorMessage() // 消费错误消息，避免重复显示
            }
        }
        LaunchedEffect(uiState.connectionTestSuccess) {
            when (uiState.connectionTestSuccess) {
                true -> snackbarHostState.showSnackbar("连接成功！")
                false -> snackbarHostState.showSnackbar("连接测试失败。") // 具体错误已在 errorMessage 显示
                null -> {} // Do nothing
            }
            screenModel.consumeConnectionTestResult()
        }


        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("添加邮箱账户") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.emailAddress,
                    onValueChange = screenModel::onEmailChanged,
                    label = { Text("邮箱地址") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.passwordOrAuthCode,
                    onValueChange = screenModel::onPasswordChanged,
                    label = { Text("密码/授权码") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(image, if (passwordVisible) "隐藏密码" else "显示密码")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.displayName,
                    onValueChange = screenModel::onDisplayNameChanged,
                    label = { Text("显示名称 (可选)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 账户类型选择 (DropdownMenu)
                ExposedDropdownMenuBox(
                    expanded = false, // 你需要状态来控制展开
                    onExpandedChange = { /* ... */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = uiState.selectedAccountType.name, // 显示选择的类型
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("账户类型") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = false /* pass expanded state */) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = false, // pass expanded state
                        onDismissRequest = { /* ... */ }
                    ) {
                        AccountType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    screenModel.onAccountTypeChanged(type)
                                    // close dropdown
                                }
                            )
                        }
                    }
                }
                // 如果是 GENERIC_IMAP_SMTP，显示服务器配置字段
                if (uiState.selectedAccountType == AccountType.GENERIC_IMAP_SMTP ||
                    uiState.imapHost.isBlank() || uiState.smtpHost.isBlank() // 或者当预填信息不完整时也显示
                ) {
                    Text("高级服务器设置", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
                    OutlinedTextField(value = uiState.imapHost, onValueChange = screenModel::onImapHostChanged, label = { Text("IMAP 主机") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = uiState.imapPort, onValueChange = screenModel::onImapPortChanged, label = { Text("IMAP 端口") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = uiState.imapUseSsl, onCheckedChange = screenModel::onImapSslChanged)
                        Text("IMAP 使用 SSL/TLS")
                    }
                    OutlinedTextField(value = uiState.smtpHost, onValueChange = screenModel::onSmtpHostChanged, label = { Text("SMTP 主机") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = uiState.smtpPort, onValueChange = screenModel::onSmtpPortChanged, label = { Text("SMTP 端口") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = uiState.smtpUseSsl, onCheckedChange = screenModel::onSmtpSslChanged)
                        Text("SMTP 使用 SSL/TLS (或 STARTTLS)")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = screenModel::testConnection,
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isLoading && uiState.connectionTestSuccess == null) Text("测试中...") else Text("测试连接")
                }

                Button(
                    onClick = screenModel::addAccount,
                    enabled = !uiState.isLoading && (uiState.connectionTestSuccess == true || uiState.selectedAccountType != AccountType.GENERIC_IMAP_SMTP), // 如果是通用类型，最好先测试连接
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isLoading && uiState.connectionTestSuccess != null) Text("添加中...") else Text("添加账户")
                }

                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}