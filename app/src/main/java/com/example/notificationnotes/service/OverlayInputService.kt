package com.example.notificationnotes.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.notificationnotes.MainActivity
import com.example.notificationnotes.NotesApplication
import com.example.notificationnotes.R
import com.example.notificationnotes.data.NoteEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 悬浮输入框服务
 * 在主屏幕上显示一个半透明的输入框，快速添加便签
 */
class OverlayInputService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showOverlay()
        startForegroundNotification()
        return START_NOT_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startForegroundNotification() {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("快速输入模式")
            .setContentText("正在使用悬浮输入框")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(999, notification)
    }

    @SuppressLint("InflateParams")
    private fun showOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // 动态创建悬浮窗布局
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(0xF0FFFFFF.toInt())
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // 顶部操作栏
        val topBar = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER_VERTICAL
        }

        // Sticky 切换按钮
        val stickyButton = android.widget.ToggleButton(this).apply {
            textOff = "普通"
            textOn = "固定"
            isChecked = true
            setTextColor(0xFF333333.toInt())
            setBackgroundColor(0xFFE0E0E0.toInt())
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                72
            ).apply {
                setMargins(0, 0, 12, 0)
            }
        }

        // 关闭按钮
        val closeButton = android.widget.Button(this).apply {
            text = "✕"
            setBackgroundColor(0xFFF5F5F5.toInt())
            setTextColor(0xFF666666.toInt())
            layoutParams = android.widget.LinearLayout.LayoutParams(72, 72).apply {
                gravity = Gravity.END
            }
        }
        topBar.addView(stickyButton)
        topBar.addView(android.view.View(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                0, 0, 1f
            )
        })
        topBar.addView(closeButton)

        // 输入框
        val inputEditText = EditText(this).apply {
            hint = getString(R.string.input_hint)
            setHintTextColor(0xFF999999.toInt())
            setTextColor(0xFF1A1A1A.toInt())
            maxLines = 5
            minLines = 2
            setBackgroundColor(0xFFFAFAFA.toInt())
            setPadding(24, 16, 24, 16)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 16)
            }
        }

        // 底部按钮行
        val bottomBar = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.END
        }

        // 添加按钮
        val addButton = android.widget.Button(this).apply {
            text = "+"
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF1976D2.toInt())
            textSize = 22f
            layoutParams = android.widget.LinearLayout.LayoutParams(64, 64)
        }

        bottomBar.addView(addButton)

        container.addView(topBar)
        container.addView(inputEditText)
        container.addView(bottomBar)

        overlayView = container

        // 按钮点击事件
        closeButton.setOnClickListener { stopSelf() }
        addButton.setOnClickListener {
            val content = inputEditText.text.toString().trim()
            if (content.isEmpty()) {
                Toast.makeText(this@OverlayInputService, "请输入内容", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val isSticky = stickyButton.isChecked
            addNote(content, isSticky)
            inputEditText.text?.clear()
            stopSelf()
        }

        // 窗口参数
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            width = (resources.displayMetrics.widthPixels * 0.88).toInt()
        }

        windowManager?.addView(overlayView, params)
    }

    private fun addNote(content: String, isSticky: Boolean) {
        scope.launch {
            val app = applicationContext as NotesApplication
            val notificationHelper = NotificationHelper(app)

            val note = NoteEntity(
                content = content,
                isSticky = isSticky
            )

            val noteId = app.repository.insertNote(note)
            val notificationId = notificationHelper.showNoteNotification(note)
            app.repository.updateNote(note.copy(id = noteId, notificationId = notificationId))
            notificationHelper.refreshGroupSummary()
            Toast.makeText(this@OverlayInputService, "已添加到通知栏", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        if (overlayView != null) {
            windowManager?.removeView(overlayView)
        }
        super.onDestroy()
    }
}
