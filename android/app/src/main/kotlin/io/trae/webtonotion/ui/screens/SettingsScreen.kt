package io.trae.webtonotion.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.trae.webtonotion.data.prefs.AiMode
import io.trae.webtonotion.data.prefs.SettingsStore
import io.trae.webtonotion.data.repository.NoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: NoteRepository,
    private val settings: SettingsStore
) : ViewModel() {
    val notionToken: StateFlow<String> =
        settings.notionToken.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val databaseId: StateFlow<String> =
        settings.databaseId.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val groqKey: StateFlow<String> =
        settings.groqKey.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val groqEnabled: StateFlow<Boolean> =
        settings.groqEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val aiMode: StateFlow<String> =
        settings.aiMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AiMode.CLEAN)

    fun setNotionToken(v: String) { viewModelScope.launch { settings.setNotionToken(v) } }
    fun setDatabaseId(v: String) { viewModelScope.launch { settings.setDatabaseId(v) } }
    fun setGroqKey(v: String) { viewModelScope.launch { settings.setGroqKey(v) } }
    fun setGroqEnabled(v: Boolean) { viewModelScope.launch { settings.setGroqEnabled(v) } }

    fun testConnection(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.testConnection()
            onResult(result.success, result.error)
        }
    }

    fun createDatabase(parentPageId: String, onResult: (Boolean, String?, String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.createNotionDatabase(parentPageId)
            onResult(result.success, result.databaseId, result.error)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val repository = remember { NoteRepository.getInstance(context) }
    val settings = remember { SettingsStore(context) }
    val viewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory { initializer { SettingsViewModel(repository, settings) } }
    )
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val notionToken by viewModel.notionToken.collectAsStateWithLifecycle()
    val databaseId by viewModel.databaseId.collectAsStateWithLifecycle()
    val groqKey by viewModel.groqKey.collectAsStateWithLifecycle()
    val groqEnabled by viewModel.groqEnabled.collectAsStateWithLifecycle()

    var parentPageId by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("设置") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(0.dp))

            // Notion 配置组
            SettingsCard(title = "Notion") {
                OutlinedTextField(
                    value = notionToken,
                    onValueChange = viewModel::setNotionToken,
                    label = { Text("API Token") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                OutlinedTextField(
                    value = databaseId,
                    onValueChange = viewModel::setDatabaseId,
                    label = { Text("Database ID") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true
                )
                Button(
                    onClick = {
                        viewModel.testConnection { success, error ->
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (success) "连接成功，数据库 schema 正确"
                                    else "连接失败：$error"
                                )
                            }
                        }
                    }
                ) {
                    Text("测试连接")
                }

                HorizontalDivider()

                Text(
                    text = "数据库不存在或 schema 不匹配？在下面填入一个 Notion 页面 ID，让 App 自动创建正确结构的数据库。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = parentPageId,
                    onValueChange = { parentPageId = it },
                    label = { Text("父页面 ID (page_id)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true
                )
                Button(
                    onClick = {
                        val pageId = parentPageId.trim()
                        if (pageId.isBlank()) {
                            scope.launch {
                                snackbarHostState.showSnackbar("请先填写父页面 ID")
                            }
                            return@Button
                        }
                        viewModel.createDatabase(pageId) { success, newDbId, error ->
                            scope.launch {
                                if (success && newDbId != null) {
                                    snackbarHostState.showSnackbar("数据库已创建：$newDbId")
                                } else {
                                    snackbarHostState.showSnackbar("创建失败：$error")
                                }
                            }
                        }
                    }
                ) {
                    Text("创建数据库")
                }
            }

            // AI 格式化组
            SettingsCard(title = "AI 格式化") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("启用 AI 格式化", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = groqEnabled,
                        onCheckedChange = viewModel::setGroqEnabled
                    )
                }
                if (groqEnabled) {
                    OutlinedTextField(
                        value = groqKey,
                        onValueChange = viewModel::setGroqKey,
                        label = { Text("Groq API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
            }

            // 关于组
            SettingsCard(title = "关于") {
                Text("Web to Notion", style = MaterialTheme.typography.bodyLarge)
                Text("版本 1.0.0", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}

