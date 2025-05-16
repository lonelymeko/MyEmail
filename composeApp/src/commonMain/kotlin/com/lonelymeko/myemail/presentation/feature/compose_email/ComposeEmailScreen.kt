package com.lonelymeko.myemail.presentation.feature.compose_email

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.lonelymeko.myemail.data.model.EmailMessage
import org.koin.core.parameter.parametersOf
import java.lang.reflect.Modifier

// ... imports ...

data class ComposeEmailScreen(
    val replyToEmail: EmailMessage? = null, // 用于回复/转发时预填数据
    val forwardEmail: EmailMessage? = null  // TODO: 处理转发
) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = getScreenModel<ComposeEmailScreenModel>(
            // 传递 replyToEmail 和 forwardEmail 作为参数
            // Koin factory 会根据这两个参数是否为 null 来决定如何初始化
            parameters = { parametersOf(replyToEmail, forwardEmail) }
        )
        val uiState = screenModel.uiState
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(uiState.errorMessage) { /* ... */ }
        LaunchedEffect(uiState.sendSuccess) { if (uiState.sendSuccess) navigator.pop() }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("撰写邮件") },
                    navigationIcon = { IconButton(onClick = { navigator.pop() }) { Icon(Icons.Filled.Close, "关闭") } },
                    actions = {
                        IconButton(onClick = screenModel::sendEmail, enabled = !uiState.isLoading && uiState.fromAccount != null) {
                            Icon(Icons.Filled.Send, "发送")
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Column(modifier = androidx.compose.ui.Modifier.fillMaxSize().padding(paddingValues).padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("发件人: ${uiState.fromAccount?.emailAddress ?: "加载中..."}")
                OutlinedTextField(value = uiState.to, onValueChange = screenModel::onToChanged, label = { Text("收件人") }, modifier = androidx.compose.ui.Modifier.fillMaxWidth())
                OutlinedTextField(value = uiState.subject, onValueChange = screenModel::onSubjectChanged, label = { Text("主题") }, modifier = androidx.compose.ui.Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = uiState.body,
                    onValueChange = screenModel::onBodyChanged,
                    label = { Text("邮件正文") },
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth().weight(1f),
                    minLines = 10
                )
                if (uiState.isLoading) CircularProgressIndicator()
            }
        }
    }
}