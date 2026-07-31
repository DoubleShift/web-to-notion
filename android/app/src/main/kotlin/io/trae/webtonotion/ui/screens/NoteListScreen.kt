package io.trae.webtonotion.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.trae.webtonotion.data.local.NoteEntity
import io.trae.webtonotion.data.repository.NoteRepository
import io.trae.webtonotion.ui.components.EmptyState
import io.trae.webtonotion.ui.theme.BackgroundGrey
import io.trae.webtonotion.ui.theme.MemoYellow
import io.trae.webtonotion.ui.theme.TextSecondary
import io.trae.webtonotion.ui.theme.TextTertiary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteListViewModel(private val repository: NoteRepository) : ViewModel() {
    val notes: StateFlow<List<NoteEntity>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteNote(id: Long) {
        viewModelScope.launch { repository.deleteNote(id) }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NoteListScreen(
    onNoteClick: (Long) -> Unit,
    onNewNote: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { NoteRepository.getInstance(context) }
    val viewModel: NoteListViewModel = viewModel(
        factory = viewModelFactory { initializer { NoteListViewModel(repository) } }
    )
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val pinned = notes.filter { it.isPinned }
    val recent = notes.filter { !it.isPinned }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MemoDrawerSheet(
        noteCount = notes.size,
        scope = scope,
        drawerState = drawerState,
        onMyNotes = { },
        onAllNotes = { },
        onSettings = onNavigateToSettings,
        onTrash = { }
    )
        },
        gesturesEnabled = false
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "我的便签",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Outlined.Menu, contentDescription = "菜单")
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* 搜索占位 */ }) {
                            Icon(Icons.Outlined.Search, contentDescription = "搜索")
                        }
                        IconButton(onClick = { onNavigateToSettings() }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = "更多")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onNewNote,
                    shape = CircleShape,
                    containerColor = MemoYellow,
                    contentColor = Color.White,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "新建便签",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        ) { padding ->
            if (notes.isEmpty()) {
                EmptyState(
                    title = "还没有便签",
                    subtitle = "点击右下角按钮新建"
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = padding.calculateTopPadding(),
                        bottom = 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (pinned.isNotEmpty()) {
                        item { SectionTitle("置顶") }
                        items(pinned, key = { it.id }) { note ->
                            MemoCard(
                                note = note,
                                dateFormat = dateFormat,
                                onClick = { onNoteClick(note.id) },
                                onEdit = { onNoteClick(note.id) },
                                onDelete = { viewModel.deleteNote(note.id) },
                                onOpenInNotion = {
                                    note.notionPageId?.let { pageId ->
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://notion.so/$pageId"))
                                        context.startActivity(intent)
                                    }
                                }
                            )
                        }
                    }

                    if (recent.isNotEmpty()) {
                        item { SectionTitle("最近") }
                        items(recent, key = { it.id }) { note ->
                            MemoCard(
                                note = note,
                                dateFormat = dateFormat,
                                onClick = { onNoteClick(note.id) },
                                onEdit = { onNoteClick(note.id) },
                                onDelete = { viewModel.deleteNote(note.id) },
                                onOpenInNotion = {
                                    note.notionPageId?.let { pageId ->
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://notion.so/$pageId"))
                                        context.startActivity(intent)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemoDrawerSheet(
    noteCount: Int,
    scope: CoroutineScope,
    drawerState: DrawerState,
    onMyNotes: () -> Unit,
    onAllNotes: () -> Unit,
    onSettings: () -> Unit,
    onTrash: () -> Unit
) {
    var groupsExpanded by remember { mutableStateOf(false) }

    ModalDrawerSheet(
        modifier = Modifier.width(300.dp),
        drawerContainerColor = Color.White
    ) {
        // 顶部用户区
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MemoYellow),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column {
                    Text(
                        text = "我的账号",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "本地便签",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)

        // 菜单项
        NavigationDrawerItem(
            icon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
            label = { DrawerItemLabel("我的便签", noteCount) },
            selected = true,
            onClick = { scope.launch { drawerState.close(); onMyNotes() } },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp)
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Outlined.Label, contentDescription = null) },
            label = { DrawerItemLabel("全部便签", noteCount) },
            selected = false,
            onClick = { scope.launch { drawerState.close(); onAllNotes() } },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp)
        )

        // 分组（可展开）
        Surface(
            onClick = { groupsExpanded = !groupsExpanded },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.Transparent
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Label,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "分组",
                        fontSize = 15.sp,
                        color = Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (groupsExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                }
                if (groupsExpanded) {
                    Text(
                        text = "暂无自定义分组",
                        color = TextTertiary,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 44.dp, bottom = 12.dp)
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)

        NavigationDrawerItem(
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
            label = { Text("回收站", fontSize = 15.sp, color = Color.Black) },
            selected = false,
            onClick = { scope.launch { drawerState.close(); onTrash() } },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp)
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
            label = { Text("设置", fontSize = 15.sp, color = Color.Black) },
            selected = false,
            onClick = { scope.launch { drawerState.close(); onSettings() } },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun DrawerItemLabel(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 15.sp, color = Color.Black)
        Text("($count)", fontSize = 14.sp, color = TextSecondary)
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        color = TextTertiary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MemoCard(
    note: NoteEntity,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenInNotion: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Text(
                    text = note.title.ifBlank { "无标题" },
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = dateFormat.format(Date(note.updatedAt)),
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("编辑") },
                onClick = { showMenu = false; onEdit() },
                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) }
            )
            if (note.notionPageId != null) {
                DropdownMenuItem(
                    text = { Text("在 Notion 打开") },
                    onClick = { showMenu = false; onOpenInNotion() },
                    leadingIcon = { Icon(Icons.Outlined.OpenInNew, contentDescription = null) }
                )
            }
            DropdownMenuItem(
                text = { Text("删除") },
                onClick = { showMenu = false; onDelete() },
                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) }
            )
        }
    }
}
