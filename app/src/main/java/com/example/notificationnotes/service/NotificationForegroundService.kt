package com.example.notificationnotes.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.notificationnotes.MainActivity
import com.example.notificationnotes.NotesApplication
import com.example.notificationnotes.R
import kotlinx.coroutines.*

/**
 * 后台持久化前台服务
 * 确保应用退出后通知不丢失，开机自动恢复通知
 */
class NotificationForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var notificationHelper: NotificationHelper

    companion object {
        const val ACTION_START = "com.example.notificationnotes.START_FOREGROUND"
        const val ACTION_REFRESH = "com.example.notificationnotes.REFRESH_NOTIFICATIONS"
    }

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundNotification()

        when (intent?.action) {
            ACTION_START, null -> {
                serviceScope.launch {
                    // 恢复所有可见便签到通知栏
                    notificationHelper.restoreAllVisibleNotes()
                }
            }
            ACTION_REFRESH -> {
                serviceScope.launch {
                    notificationHelper.refreshAllNotifications()
                }
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("MissingPermission")
    private fun startForegroundNotification() {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Low-priority persistent notification
        val notification = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.service_running_title))
            .setContentText(getString(R.string.service_running_desc))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .build().apply {
                flags = flags or Notification.FLAG_NO_CLEAR
            }

        startForeground(2, notification)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        // Don't remove notifications - they should persist
        super.onDestroy()
    }
}
