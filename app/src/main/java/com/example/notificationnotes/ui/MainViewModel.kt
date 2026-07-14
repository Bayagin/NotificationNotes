package com.example.notificationnotes.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.notificationnotes.NotesApplication
import com.example.notificationnotes.data.NoteEntity
import com.example.notificationnotes.service.NotificationForegroundService
import com.example.notificationnotes.service.NotificationHelper
import com.example.notificationnotes.service.ReminderWorker
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MainUiState(
    val notes: List<NoteEntity> = emptyList(),
    val hiddenNotes: List<NoteEntity> = emptyList(),
    val hasHiddenNotes: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as NotesApplication
    private val repository = app.repository
    private val notificationHelper = NotificationHelper(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    private val _editingNote = MutableStateFlow<NoteEntity?>(null)
    val editingNote: StateFlow<NoteEntity?> = _editingNote.asStateFlow()

    private val _showReminderDialog = MutableStateFlow<NoteEntity?>(null)
    val showReminderDialog: StateFlow<NoteEntity?> = _showReminderDialog.asStateFlow()

    init {
        observeNotes()
        observeHiddenNotes()
        // 应用打开后自动恢复所有可见便签到通知栏
        restoreNotificationsOnStart()
        // 启动后台持久服务
        startForegroundService()
    }

    private fun observeNotes() {
        viewModelScope.launch {
            repository.allNotes
                .catch { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
                .collect { notes ->
                    _uiState.update { it.copy(notes = notes, isLoading = false) }
                }
        }
    }

    private fun observeHiddenNotes() {
        viewModelScope.launch {
            repository.getHiddenNotes()
                .catch { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
                .collect { hidden ->
                    _uiState.update { it.copy(hiddenNotes = hidden, hasHiddenNotes = hidden.isNotEmpty()) }
                }
        }
    }

    /**
     * 应用打开后自动恢复可见便签到通知栏
     */
    private fun restoreNotificationsOnStart() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                notificationHelper.restoreAllVisibleNotes()
            }
        }
    }

    /**
     * 启动后台持久化前台服务
     */
    private fun startForegroundService() {
        val context = getApplication<Application>()
        val intent = Intent(context, NotificationForegroundService::class.java).apply {
            action = NotificationForegroundService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun showAddDialog() {
        _showAddDialog.value = true
    }

    fun hideAddDialog() {
        _showAddDialog.value = false
        _editingNote.value = null
    }

    fun startEditNote(note: NoteEntity) {
        _editingNote.value = note
        _showAddDialog.value = true
    }

    fun showReminderSettings(note: NoteEntity) {
        _showReminderDialog.value = note
    }

    fun hideReminderDialog() {
        _showReminderDialog.value = null
    }

    /**
     * 添加新便签
     */
    fun addNote(content: String, isSticky: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val note = NoteEntity(content = content, isSticky = isSticky)
                val noteId = repository.insertNote(note)
                val notificationId = notificationHelper.showNoteNotification(note.copy(id = noteId))
                repository.updateNote(note.copy(id = noteId, notificationId = notificationId))
                notificationHelper.refreshGroupSummary()
            }
            hideAddDialog()
        }
    }

    /**
     * 更新便签
     */
    fun updateNote(note: NoteEntity, newContent: String, newSticky: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val updated = note.copy(
                    content = newContent,
                    isSticky = newSticky,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateNote(updated)
                notificationHelper.updateNotification(updated)
            }
            hideAddDialog()
        }
    }

    /**
     * 从通知栏隐藏便签（保留数据）
     */
    fun hideNote(note: NoteEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                notificationHelper.hideNoteFromNotification(note.id)
            }
        }
    }

    /**
     * 恢复便签到通知栏
     */
    fun restoreNote(note: NoteEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                notificationHelper.restoreNoteToNotification(note.id)
            }
        }
    }

    /**
     * 永久删除便签
     */
    fun permanentDeleteNote(note: NoteEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (note.notificationId > 0) {
                    notificationHelper.cancelNotification(note.notificationId)
                }
                ReminderWorker.cancelReminder(getApplication(), note.id)
                repository.deleteNote(note)
                notificationHelper.refreshGroupSummary()
            }
        }
    }

    /**
     * 清空所有隐藏便签（永久删除）
     */
    fun clearAllHiddenNotes() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteAllHiddenNotes()
            }
        }
    }

    /**
     * 切换 Sticky 状态
     */
    fun toggleSticky(note: NoteEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val updated = note.copy(
                    isSticky = !note.isSticky,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateNote(updated)
                // 重新发送通知以更新 ongoing 状态
                notificationHelper.cancelNotification(note.notificationId)
                val newId = notificationHelper.showNoteNotification(updated)
                if (newId != note.notificationId) {
                    repository.updateNote(updated.copy(notificationId = newId))
                }
            }
        }
    }

    /**
     * 保存定时提醒设置
     */
    fun saveReminder(
        note: NoteEntity,
        reminderTimeMs: Long?,
        repeatMode: String,
        repeatIntervalHours: Int,
        repeatCount: Int
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // 先取消旧的提醒
                ReminderWorker.cancelReminder(getApplication(), note.id)

                val updated = note.copy(
                    hasReminder = reminderTimeMs != null,
                    reminderTimeMs = reminderTimeMs,
                    repeatMode = repeatMode,
                    repeatIntervalHours = repeatIntervalHours,
                    repeatCount = repeatCount,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateNote(updated)

                // 安排新的提醒
                if (reminderTimeMs != null) {
                    ReminderWorker.scheduleReminder(getApplication(), updated)
                }
            }
            hideReminderDialog()
        }
    }
}
