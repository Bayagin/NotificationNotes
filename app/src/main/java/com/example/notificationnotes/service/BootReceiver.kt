package com.example.notificationnotes.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.*

/**
 * 开机广播接收器
 * 处理开机完成、直接启动完成、以及国产 ROM 的自启动广播
 */
class BootReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        // 标准开机广播 + 国产 ROM 常见自启动广播
        val isBootAction = action == Intent.ACTION_BOOT_COMPLETED
                || action == Intent.ACTION_LOCKED_BOOT_COMPLETED
                || action == "android.intent.action.QUICKBOOT_POWERON"
                || action == "com.htc.intent.action.QUICKBOOT_POWERON"
                || action == "android.intent.action.USER_PRESENT"

        if (isBootAction) {
            scope.launch {
                // 延迟一小段时间，等系统初始化完成
                delay(3000)
                startService(context)
            }
        }
    }

    private fun startService(context: Context) {
        val serviceIntent = Intent(context, NotificationForegroundService::class.java).apply {
            action = NotificationForegroundService.ACTION_START
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            // 某些 ROM 可能禁止后台启动服务，静默失败
            // 用户下次打开应用时会自动恢复
        }
    }
}
