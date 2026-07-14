# 「通知栏便签」Android 应用 — 开发完成

## 项目概览

**应用名称**：通知栏便签
**平台**：Android 原生（Kotlin + Jetpack Compose）
**最低 Android 版本**：8.0（API 26）
**开发日期**：2026-07-13

---

## <¨ 已实现功能

| 功能 | 状态 |
|------|------|
| 悬浮输入框快速添加便签 | ✅ |
| 便签显示在系统通知栏 | ✅ |
| Sticky 固定（不被清除通知清掉） | ✅ |
| 关机/重启后 Sticky 通知自动恢复 | ✅ |
| 通知栏「删除」按钮 | ✅ |
| 应用内便签列表管理 | ✅ |
| 编辑已有便签 | ✅ |
| 手动切换 Sticky 状态 | ✅ |
| 定时提醒（支持响铃/震动） | ✅ |
| 重复提醒（每天/每周/每月/自定义） | ✅ |
| 自定义重复次数/频率 | ✅ |
| 本地 Room 数据库存储 | ✅ |
| JSON 格式导出 | ✅ |

### 项目结构

```
NotificationNotes/
├── app/src/main/java/com/example/notificationnotes/
│   ├── NotesApplication.kt          # Application 入口
│   ├── MainActivity.kt               # 主界面
│   ├── data/
│   │   ├── NoteEntity.kt             # 数据库实体
│   │   ├── NoteDao.kt                # DAO 层
│   │   ├── NoteDatabase.kt           # Room 数据库
│   │   └── NoteRepository.kt         # 数据仓库
│   ├── service/
│   │   ├── NotificationHelper.kt     # 通知管理
│   │   ├── BootReceiver.kt           # 开机自启恢复
│   │   ├── NotificationActionReceiver.kt  # 通知操作
│   │   ├── OverlayInputService.kt    # 悬浮输入框
│   │   └── ReminderWorker.kt         # 定时提醒
│   ├── ui/
│   │   ├── MainViewModel.kt          # 主界面逻辑
│   │   ├── MainScreen.kt             # 主界面 UI
│   │   ├── AddEditDialog.kt          # 添加/编辑对话框
│   │   ├── ReminderDialog.kt         # 定时提醒设置
│   │   └── theme/                    # Material 3 主题
│   └── util/
│       └── JsonExportUtil.kt         # JSON 导出工具
```

### 技术栈

- Compose BOM 2024.02 + Material 3
- Room 2.6.1（本地数据库）
- WorkManager 2.9.0（定时提醒调度）
- Gson 2.10.1（JSON 序列化）
- Kotlin Coroutines + Flow

### 权限需求

- `POST_NOTIFICATIONS` — 显示通知
- `SYSTEM_ALERT_WINDOW` — 悬浮窗
- `RECEIVE_BOOT_COMPLETED` — 开机自启
- `SCHEDULE_EXACT_ALARM` — 精确定时
- `VIBRATE` — 震动提醒

### 构建方式

用 Android Studio 打开 `NotificationNotes/` 目录，Sync Gradle 后运行即可。
或命令行：`./gradlew assembleDebug`
