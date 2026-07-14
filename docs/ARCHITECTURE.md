# 技术架构

## 技术栈
- **语言**：Kotlin 1.9.21
- **UI**：Jetpack Compose + Material 3
- **数据库**：Room (SQLite)
- **后台任务**：WorkManager
- **构建工具**：Gradle 8.5 + AGP 8.3.2
- **JDK**：Oracle JDK 21.0.11

## 项目结构

```
NotificationNotes/
├── app/src/main/java/com/example/notificationnotes/
│   ├── data/                     # 数据层
│   │   ├── NoteEntity.kt         # Room 实体
│   │   ├── NoteDao.kt            # 数据访问
│   │   ├── NoteDatabase.kt       # 数据库
│   │   └── NoteRepository.kt     # 仓库
│   ├── service/                  # 服务层
│   │   ├── NotificationHelper.kt # 通知管理
│   │   ├── NotificationForegroundService.kt # 前台服务
│   │   ├── NotificationActionReceiver.kt   # 通知交互
│   │   ├── BootReceiver.kt       # 开机广播
│   │   ├── OverlayInputService.kt # 悬浮输入
│   │   └── ReminderWorker.kt     # 提醒调度
│   ├── ui/                       # UI 层
│   │   ├── MainViewModel.kt      # 主视图模型
│   │   ├── MainScreen.kt         # 主界面
│   │   ├── AddEditDialog.kt      # 新建/编辑对话框
│   │   ├── ReminderDialog.kt     # 提醒设置对话框
│   │   └── theme/                # 主题
│   ├── util/
│   │   └── JsonExportUtil.kt     # JSON 导入导出
│   ├── MainActivity.kt           # 主 Activity
│   ├── NotesApplication.kt       # Application 类
│   └── AndroidManifest.xml
├── docs/                         # 项目文档
├── devlog/                       # 开发日志
└── build.gradle.kts              # 构建配置
```

## 架构模式
- **MVVM**：ViewModel + StateFlow + Compose
- **Repository Pattern**：统一数据访问
- **单例**：NoteDatabase（双重检查锁）

## 通知架构
```
每个 NoteEntity → showNoteNotification() → 独立 RemoteViews 通知
                                       → notify(notificationId)
                                       → ✕ 按钮 → Broadcast → 隐藏
                                       → 点击 → PendingIntent → AddEditDialog
```

## 数据流
```
用户操作 → ViewModel → Repository → DAO → Room DB
                    → NotificationHelper → NotificationManager
                    → WorkManager → ReminderWorker
```

## 数据库版本
v2: 新增 `hiddenFromNotification` 字段
