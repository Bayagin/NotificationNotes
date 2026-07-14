package com.example.notificationnotes.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.notificationnotes.data.NoteEntity
import com.example.notificationnotes.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onExport: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showAddDialog by viewModel.showAddDialog.collectAsStateWithLifecycle()
    val editingNote by viewModel.editingNote.collectAsStateWithLifecycle()
    val showReminder by viewModel.showReminderDialog.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = White,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "📌 通知栏便签",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // 导出按钮
                    IconButton(onClick = onExport) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "导出 JSON",
                            tint = DarkGray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = PrimaryBlue,
                contentColor = White,
                shape = CircleShape,
                modifier = Modifier.shadow(8.dp, CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加便签")
            }
        }
    ) { padding ->
        if (uiState.notes.isEmpty() && !uiState.hasHiddenNotes) {
            // 空状态
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("📝", style = MaterialTheme.typography.displayLarge)
                    Text(
                        "还没有便签",
                        style = MaterialTheme.typography.titleMedium,
                        color = DarkGray
                    )
                    Text(
                        "点击右下角的 + 号快速添加\n或从通知栏点击添加按钮",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DarkGray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 96.dp, top = 8.dp)
            ) {
                // 可见便签
                items(
                    items = uiState.notes.filter { !it.hiddenFromNotification },
                    key = { "visible_${it.id}" }
                ) { note ->
                    NoteCard(
                        note = note,
                        onEdit = { viewModel.startEditNote(note) },
                        onHide = { viewModel.hideNote(note) },
                        onPermanentDelete = { viewModel.permanentDeleteNote(note) },
                        onToggleSticky = { viewModel.toggleSticky(note) },
                        onSetReminder = { viewModel.showReminderSettings(note) }
                    )
                }

                // 隐藏便签分隔区域
                if (uiState.hasHiddenNotes) {
                    item(key = "hidden_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "🗑️ 已从通知栏移除（${uiState.hiddenNotes.size}）",
                                style = MaterialTheme.typography.titleSmall,
                                color = DarkGray,
                                fontWeight = FontWeight.Medium
                            )
                            TextButton(onClick = { viewModel.clearAllHiddenNotes() }) {
                                Text("清空全部", color = ErrorRed, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    items(
                        items = uiState.hiddenNotes,
                        key = { "hidden_${it.id}" }
                    ) { note ->
                        HiddenNoteCard(
                            note = note,
                            onRestore = { viewModel.restoreNote(note) },
                            onPermanentDelete = { viewModel.permanentDeleteNote(note) }
                        )
                    }
                }
            }
        }
    }

    // 添加/编辑对话框
    if (showAddDialog) {
        AddEditDialog(
            editingNote = editingNote,
            onSave = { content, isSticky ->
                if (editingNote != null) {
                    viewModel.updateNote(editingNote!!, content, isSticky)
                } else {
                    viewModel.addNote(content, isSticky)
                }
            },
            onDismiss = { viewModel.hideAddDialog() }
        )
    }

    // 定时提醒对话框
    showReminder?.let { note ->
        ReminderDialog(
            note = note,
            onSave = { time, mode, interval, count ->
                viewModel.saveReminder(note, time, mode, interval, count)
            },
            onDismiss = { viewModel.hideReminderDialog() }
        )
    }
}

@Composable
fun NoteCard(
    note: NoteEntity,
    onEdit: () -> Unit,
    onHide: () -> Unit,
    onPermanentDelete: () -> Unit,
    onToggleSticky: () -> Unit,
    onSetReminder: () -> Unit
) {
    var showDeleteOptions by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(tween(300)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OffWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 标签行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sticky 标签
                    if (note.isSticky) {
                        Surface(
                            color = StickyGold.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "📌 固定",
                                style = MaterialTheme.typography.labelSmall,
                                color = StickyGold,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // 提醒标签
                    if (note.hasReminder && note.reminderTimeMs != null) {
                        Surface(
                            color = PrimaryBlue.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "⏰ ${formatReminderTime(note.reminderTimeMs)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryBlue,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    formatDate(note.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = DarkGray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 内容
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyLarge,
                color = NearBlack,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 提醒按钮
                IconButton(onClick = onSetReminder, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = "设置提醒",
                        tint = if (note.hasReminder) PrimaryBlue else DarkGray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Sticky 切换
                IconButton(onClick = onToggleSticky, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = "切换固定",
                        tint = if (note.isSticky) StickyGold else DarkGray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // 编辑按钮
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "编辑",
                        tint = DarkGray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // 删除按钮（弹出选项）
                IconButton(onClick = { showDeleteOptions = true }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "删除",
                        tint = ErrorRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    // 删除选项对话框（通知栏移除 vs 永久删除）
    if (showDeleteOptions) {
        AlertDialog(
            onDismissRequest = { showDeleteOptions = false },
            title = { Text("移除便签") },
            text = {
                Column {
                    Text("请选择移除方式：")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "• 从通知栏移除：保留数据，可随时恢复",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkGray
                    )
                    Text(
                        "• 永久删除：彻底删除数据，不可恢复",
                        style = MaterialTheme.typography.bodySmall,
                        color = ErrorRed
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteOptions = false
                        onPermanentDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                ) {
                    Text("永久删除")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showDeleteOptions = false }) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            showDeleteOptions = false
                            onHide()
                        }
                    ) {
                        Text("从通知栏移除")
                    }
                }
            }
        )
    }
}

@Composable
fun HiddenNoteCard(
    note: NoteEntity,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(tween(300)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OffWhite.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 标签行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = ErrorRed.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "已隐藏",
                        style = MaterialTheme.typography.labelSmall,
                        color = ErrorRed.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    formatDate(note.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = DarkGray.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 内容（半透明）
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyLarge,
                color = NearBlack.copy(alpha = 0.5f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 恢复到通知栏
                TextButton(onClick = onRestore) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("恢复到通知栏", color = PrimaryBlue, style = MaterialTheme.typography.labelMedium)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 永久删除
                TextButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("永久删除", color = ErrorRed, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }

    // 永久删除确认
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认永久删除") },
            text = { Text("此操作不可恢复，确定要永久删除这条便签吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onPermanentDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                ) {
                    Text("永久删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatReminderTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
