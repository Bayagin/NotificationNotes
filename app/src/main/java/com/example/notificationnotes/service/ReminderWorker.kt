package com.example.notificationnotes.service

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.example.notificationnotes.MainActivity
import com.example.notificationnotes.NotesApplication
import com.example.notificationnotes.R
import com.example.notificationnotes.data.NoteEntity
import java.util.concurrent.TimeUnit

/**
 * 定时提醒 Worker
 * 在指定时间震动/响铃提醒用户
 */
class ReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_NOTE_ID = "note_id"
        const val KEY_NOTE_CONTENT = "note_content"
        const val UNIQUE_WORK_PREFIX = "reminder_"

        /**
         * 安排提醒任务
         */
        fun scheduleReminder(context: Context, note: NoteEntity) {
            if (note.reminderTimeMs == null || note.reminderTimeMs <= System.currentTimeMillis()) return

            val delay = note.reminderTimeMs - System.currentTimeMillis()
            val workName = "$UNIQUE_WORK_PREFIX${note.id}"

            val inputData = Data.Builder()
                .putLong(KEY_NOTE_ID, note.id)
                .putString(KEY_NOTE_CONTENT, note.content)
                .build()

            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag(workName)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    workName,
                    ExistingWorkPolicy.REPLACE,
                    request
                )
        }

        /**
         * 取消提醒
         */
        fun cancelReminder(context: Context, noteId: Long) {
            val workName = "$UNIQUE_WORK_PREFIX$noteId"
            WorkManager.getInstance(context)
                .cancelUniqueWork(workName)
        }
    }

    override suspend fun doWork(): Result {
        val noteId = inputData.getLong(KEY_NOTE_ID, -1)
        val content = inputData.getString(KEY_NOTE_CONTENT) ?: ""

        if (noteId <= 0) return Result.success()

        showReminderNotification(noteId, content)

        // 处理重复提醒
        val app = applicationContext as NotesApplication
        val note = app.repository.getNoteById(noteId)

        if (note != null && note.repeatMode != NoteEntity.REPEAT_NONE) {
            scheduleNextReminder(note)
        }

        return Result.success()
    }

    private fun showReminderNotification(noteId: Long, content: String) {
        val context = applicationContext

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("note_id", noteId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            noteId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("⏰ 便签提醒")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE or NotificationCompat.DEFAULT_LIGHTS)

        // 设置响铃
        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        builder.setSound(ringtoneUri)

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify((noteId.toInt() + 10000), builder.build())
    }

    /**
     * 安排下一次重复提醒
     */
    private suspend fun scheduleNextReminder(note: NoteEntity) {
        val app = applicationContext as NotesApplication
        val context = applicationContext

        // 如果设置了重复次数限制
        if (note.repeatCount > 0) {
            val updatedNote = note.copy(repeatCount = note.repeatCount - 1)
            app.repository.updateNote(updatedNote)

            if (updatedNote.repeatCount <= 0) return
        }

        // 计算下次提醒时间
        val nextTime = calculateNextReminderTime(note)
        val updatedNote = note.copy(reminderTimeMs = nextTime)
        app.repository.updateNote(updatedNote)
        scheduleReminder(context, updatedNote)
    }

    private fun calculateNextReminderTime(note: NoteEntity): Long {
        val calendar = java.util.Calendar.getInstance()

        when (note.repeatMode) {
            NoteEntity.REPEAT_DAILY -> calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
            NoteEntity.REPEAT_WEEKLY -> calendar.add(java.util.Calendar.WEEK_OF_YEAR, 1)
            NoteEntity.REPEAT_MONTHLY -> calendar.add(java.util.Calendar.MONTH, 1)
            NoteEntity.REPEAT_CUSTOM -> {
                calendar.add(java.util.Calendar.HOUR_OF_DAY, note.repeatIntervalHours)
            }
        }

        return calendar.timeInMillis
    }
}
