package com.example.notificationnotes.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.notificationnotes.NotesApplication
import com.example.notificationnotes.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 通知操作广播接收器
 * 处理通知栏中的「隐藏」按钮和「添加」按钮
 */
class NotificationActionReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            NotificationHelper.HIDE_ACTION -> {
                val noteId = intent.getLongExtra(NotificationHelper.EXTRA_NOTE_ID, -1)
                if (noteId <= 0) return

                scope.launch {
                    val notificationHelper = NotificationHelper(context)
                    notificationHelper.hideNoteFromNotification(noteId)
                }
            }

            NotificationHelper.ADD_NOTE_ACTION -> {
                // 点击"添加便签"：打开应用主界面
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("open_add_dialog", true)
                }
                context.startActivity(launchIntent)
            }
        }
    }
}
