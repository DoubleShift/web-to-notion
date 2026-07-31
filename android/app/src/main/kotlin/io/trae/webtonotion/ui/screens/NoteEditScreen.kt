package io.trae.webtonotion.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.trae.webtonotion.data.local.NoteStatus
import io.trae.webtonotion.data.repository.NoteRepository
import io.trae.webtonotion.ui.theme.MemoYellow
import io.trae.webtonotion.work.SaveNoteWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NoteEditViewModel(private val repository: NoteRepository) : ViewModel() {
    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content

    private val _tags = MutableStateFlow("")
    val tags: StateFlow<String> = _tags

    private val _isPinned = MutableStateFlow(false)
    val isPinned: StateFlow<Boolean> = _isPinned

    private var noteId: Long = -1
    private var loaded: Boolean = false

    fun load(id: Long) {
        if (loaded || id < 0) return
        noteId = id
        loaded = true
        viewModelScope.launch {
            val note = repository.observeById(id).first()
            note?.let {
                _title.value = it.title
                _content.value = it.content
                _tags.value = it.tags
                _isPinned.value = it.isPinned
            }
        }
    }

    fun updateTitle(v: String) { _title.value = v }
    fun updateContent(v: String) { _content.value = v }
    fun updateTags(v: String) { _tags.value = v }
    fun updatePinned(v: Boolean) { _isPinned.value = v }

    suspend fun save(): Long {
        val tagList = _tags.value.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return if (noteId >= 0) {
            repository.observeById(noteId).first()?.let { existing ->
                repository.updateNote(existing.copy(
                    title = _title.value.ifBlank { "无标题" },
                    content = _content.value,
                    tags = tagList.joinToString(","),
                    isPinned = _isPinned.value,
                    status = NoteStatus.PENDING
                ))
            }
            noteId
        } else {
            repository.createNote(
                title = _title.value.ifBlank { "无标题" },
                content = _content.value,
                tags = tagList,
                isPinned = _isPinned.value
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    noteId: Long,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { NoteRepository.getInstance(context) }
    val viewModel: NoteEditViewModel = viewModel(
        factory = viewModelFactory { initializer { NoteEditViewModel(repository) } }
    )
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(noteId) {
        viewModel.load(noteId)
    }

    val title by viewModel.title.collectAsStateWithLifecycle()
    val content by viewModel.content.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val isPinned by viewModel.isPinned.collectAsStateWithLifecycle()
    var saving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (noteId < 0) "新建便签" else "编辑便签") },
                navigationIcon = {
                    IconButton(onClick = onSaved) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (!saving) {
                                saving = true
                                scope.launch {
                                    val id = viewModel.save()
                                    SaveNoteWorker.enqueue(context, id)
                                    saving = false
                                    onSaved()
                                }
                            }
                        },
                        enabled = !saving
                    ) {
                        Icon(Icons.Outlined.Save, contentDescription = "保存")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MemoYellow,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = viewModel::updateTitle,
                label = { Text("标题") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                singleLine = true
            )
            OutlinedTextField(
                value = content,
                onValueChange = viewModel::updateContent,
                label = { Text("内容") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp),
                shape = MaterialTheme.shapes.medium
            )
            OutlinedTextField(
                value = tags,
                onValueChange = viewModel::updateTags,
                label = { Text("标签（逗号分隔）") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                singleLine = true
            )

            // 置顶开关
            CardLikeRow(
                icon = { Icon(Icons.Outlined.PushPin, contentDescription = null, tint = MemoYellow) },
                title = "置顶便签",
                trailing = {
                    Switch(
                        checked = isPinned,
                        onCheckedChange = viewModel::updatePinned
                    )
                }
            )
        }
    }
}

@Composable
private fun CardLikeRow(
    icon: @Composable () -> Unit,
    title: String,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        trailing()
    }
}
