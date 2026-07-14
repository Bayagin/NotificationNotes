package com.example.notificationnotes

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.notificationnotes.service.NotificationForegroundService
import com.example.notificationnotes.service.OverlayInputService
import com.example.notificationnotes.ui.MainScreen
import com.example.notificationnotes.ui.MainViewModel
import com.example.notificationnotes.ui.theme.NotificationNotesTheme
import com.example.notificationnotes.util.JsonExportUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "需要通知权限才能显示便签", Toast.LENGTH_LONG).show()
        }
    }

    // SAF 导出文件选择（直接选目录+创建文件，返回可写 URI）
    private val exportFileLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { onFileSelected(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = MainViewModel(application)

        requestNotificationPermission()
        requestAutoStartPermissions()

        setContent {
            NotificationNotesTheme {
                MainScreen(
                    viewModel = viewModel,
                    onExport = { onExportJson() }
                )
            }
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val noteId = intent?.getLongExtra("note_id", -1) ?: -1
        val openAddDialog = intent?.getBooleanExtra("open_add_dialog", false) ?: false

        if (noteId > 0) {
            lifecycleScope.launch {
                val note = (application as NotesApplication).repository.getNoteById(noteId)
                note?.let { viewModel.startEditNote(it) }
            }
        }

        if (openAddDialog) {
            viewModel.showAddDialog()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /**
     * 请求忽略电池优化 + 引导自启动权限（覆盖各大国产 ROM）
     * 仅首次打开时请求一次
     */
    private fun requestAutoStartPermissions() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("auto_start_requested", false)) return

        // 标记已请求
        prefs.edit().putBoolean("auto_start_requested", true).apply()

        // 1. 电池优化白名单
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(PowerManager::class.java)
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    })
                } catch (_: Exception) {}
            }
        }

        // 2. 尝试打开厂商自启动管理页面
        if (!tryOpenAutoStartSettings()) {
            // 3. 都不行就打开应用详情页引导用户手动设置
            try {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                })
            } catch (_: Exception) {}
        }
    }

    /**
     * 尝试打开各厂商的自启动管理页面
     */
    private fun tryOpenAutoStartSettings(): Boolean {
        val manufacturers = listOf(
            // 小米
            Triple("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity", null),
            Triple("com.miui.securitycenter", "com.miui.appmanager.ApplicationsDetailsActivity", "package=$packageName"),
            // 华为
            Triple("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity", null),
            Triple("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity", null),
            // OPPO
            Triple("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity", null),
            Triple("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.FakeActivity", null),
            // Vivo
            Triple("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity", null),
            Triple("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity", null),
            // 三星
            Triple("com.samsung.android.sm", "com.samsung.android.sm.app.detail.AppDetailActivity", "package=$packageName"),
        )

        for ((pkg, cls, extra) in manufacturers) {
            try {
                val intent = Intent().apply {
                    setClassName(pkg, cls)
                    if (extra != null) data = Uri.parse(extra)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                if (packageManager.resolveActivity(intent, 0) != null) {
                    startActivity(intent)
                    return true
                }
            } catch (_: Exception) {}
        }
        return false
    }

    fun onExportJson() {
        lifecycleScope.launch {
            val notes = withContext(Dispatchers.IO) {
                (application as NotesApplication).repository.getAllNotesList()
            }
            if (notes.isEmpty()) {
                Toast.makeText(this@MainActivity, "没有可导出的便签", Toast.LENGTH_SHORT).show()
                return@launch
            }

            try {
                val fileName = "通知栏便签备份_${JsonExportUtil.formatTimestamp()}.json"
                exportFileLauncher.launch(fileName)
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "无法打开文件选择器", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onFileSelected(fileUri: Uri) {
        lifecycleScope.launch {
            try {
                val notes = withContext(Dispatchers.IO) {
                    (application as NotesApplication).repository.getAllNotesList()
                }
                val jsonString = withContext(Dispatchers.IO) {
                    JsonExportUtil.toJsonString(notes)
                }
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(fileUri)?.use { out ->
                        out.write(jsonString.toByteArray(Charsets.UTF_8))
                    } ?: throw Exception("无法写入文件")
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "导出成功", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "导出失败：${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun onOpenOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
                Toast.makeText(this, "请先授予悬浮窗权限", Toast.LENGTH_SHORT).show()
                return
            }
        }
        val intent = Intent(this, OverlayInputService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
