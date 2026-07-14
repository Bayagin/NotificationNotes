package com.example.notificationnotes.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.notificationnotes.MainActivity
import com.example.notificationnotes.NotesApplication
import com.example.notificationnotes.R
import com.example.notificationnotes.data.NoteEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 通知管理：每条便签独立通知 + 分组摘要折叠
 */
class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "notification_notes_channel"
        const val GROUP_KEY = "notification_notes_group"
        const val HIDE_ACTION = "com.example.notificationnotes.HIDE_NOTE"
        const val ADD_NOTE_ACTION = "com.example.notificationnotes.ADD_NOTE"
        const val EXTRA_NOTE_ID = "note_id"
        const val SUMMARY_ID = 1

        private var nextId = 100
        private val usedIds = mutableSetOf<Int>()
        @Volatile private var isRestoring = false

        @Synchronized fun generateNotificationId(): Int {
            while (usedIds.contains(nextId)) nextId++
            val id = nextId++
            usedIds.add(id)
            return id
        }
        @Synchronized fun registerNotificationId(id: Int) {
            usedIds.add(id)
            if (id >= nextId) nextId = id + 1
        }
        @Synchronized fun releaseNotificationId(id: Int) { usedIds.remove(id) }
    }

    init { createNotificationChannel() }

    private fun createNotificationChannel() {
        val ch = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = context.getString(R.string.notification_channel_desc); enableVibration(false); setShowBadge(false) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun createHideIntent(noteId: Long) = PendingIntent.getBroadcast(context, noteId.toInt(),
        Intent(context, NotificationActionReceiver::class.java).apply { action = HIDE_ACTION; putExtra(EXTRA_NOTE_ID, noteId) },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun createAddNoteIntent() = PendingIntent.getActivity(context, 9999,
        Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP; putExtra("open_add_dialog", true) },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun createContentIntent() = PendingIntent.getActivity(context, 0,
        Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    // ---- 不发送摘要，每条便签独立显示 ----

    @SuppressLint("MissingPermission")
    fun updateGroupSummary(noteCount: Int) {
        // 不再发送分组摘要——取消通知栏折叠，每条便签独立平铺
    }

    suspend fun refreshGroupSummary() {}

    // ---- 子通知（每条便签独立） ----

    @SuppressLint("MissingPermission")
    fun showNoteNotification(note: NoteEntity): Int {
        // notificationId > 0 且在 usedIds 中 → 复用（更新现有通知）
        // 否则 → 生成新 ID（隐藏后恢复等场景）
        val id = if (note.notificationId > 0 && usedIds.contains(note.notificationId)) {
            note.notificationId
        } else {
            generateNotificationId()
        }

        val time = formatRelativeTime(note.updatedAt)

        val rv = RemoteViews(context.packageName, R.layout.layout_notification_note).apply {
            setTextViewText(R.id.note_title, note.content)
            setTextViewText(R.id.note_time, time)
            setOnClickPendingIntent(R.id.note_title, createAddNoteIntent())
            setOnClickPendingIntent(R.id.btn_hide_note, createHideIntent(note.id))
        }

        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(note.content)
            .setContentText("$time")
            .setContentIntent(createAddNoteIntent())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(false)
            .setOngoing(note.isSticky)
            .setOnlyAlertOnce(true)
            .setCustomContentView(rv)
            .setCustomBigContentView(rv)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setColor(0xFF2979B3.toInt())
            .build().apply { if (note.isSticky) flags = flags or Notification.FLAG_NO_CLEAR or Notification.FLAG_ONGOING_EVENT }

        context.getSystemService(NotificationManager::class.java).notify(id, n)
        return id
    }

    fun cancelNotification(id: Int) {
        context.getSystemService(NotificationManager::class.java).cancel(id)
        releaseNotificationId(id)
    }

    fun updateNotification(note: NoteEntity) { if (note.notificationId > 0) showNoteNotification(note) }

    suspend fun hideNoteFromNotification(noteId: Long) {
        val app = context.applicationContext as NotesApplication
        val note = app.repository.getNoteById(noteId) ?: return
        cancelNotification(note.notificationId)
        app.repository.updateHiddenStatus(noteId, true)
        refreshGroupSummary()
    }

    suspend fun restoreNoteToNotification(noteId: Long) {
        val app = context.applicationContext as NotesApplication
        val note = app.repository.getNoteById(noteId) ?: return
        val restored = note.copy(hiddenFromNotification = false)
        val newId = showNoteNotification(restored)
        app.repository.updateNote(restored.copy(notificationId = newId))
        refreshGroupSummary()
    }

    suspend fun restoreAllVisibleNotes() {
        if (isRestoring) return
        isRestoring = true
        try {
            val notes = (context.applicationContext as NotesApplication).repository.getVisibleNotes()
            for (n in notes) {
                // 复用已有 notificationId，notify 同一 ID 只会更新，不会重复
                showNoteNotification(n)
            }
            updateGroupSummary(notes.size)
        } finally { isRestoring = false }
    }

    suspend fun refreshAllNotifications() { restoreAllVisibleNotes() }

    private fun formatRelativeTime(ts: Long): String {
        val diff = System.currentTimeMillis() - ts
        return when {
            diff < 60_000 -> "现在"
            diff < 3600_000 -> "${diff / 60_000}分钟前"
            diff < 86_400_000 -> "今天"
            diff < 172_800_000 -> "昨天"
            else -> SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(ts))
        }
    }
}
