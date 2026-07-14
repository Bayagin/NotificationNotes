# 📌 通知栏便签（所有代码和文档均由AI生成）

> 让便签直接显示在手机通知栏，最大化效率，最小化干扰。

[![Version](https://img.shields.io/badge/version-4.0.0-blue)](app/build.gradle.kts)
[![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-brightgreen)](#)
[![Language](https://img.shields.io/badge/language-Kotlin-purple)](#)

---

## 🎯 一句话功能

**在通知栏直接创建、查看、管理便签——不打开 App 就能随手记录和回顾。**

---

## ✨ 核心功能

### 📲 通知栏便签

| 特性 | 说明 |
|------|------|
| **便签直显** | 便签内容直接展示在通知栏，不用打开 App |
| **Blue UI** | 蓝色半透明渐变背景 + 白色文字，清晰醒目 |
| **时间标记** | 每条便签显示相对时间：`现在` / `X分钟前` / `今天` / `MM-dd` |
| **独立平铺** | 每条便签独立通知，不折叠，一屏全览 |

### 📝 快捷操作

| 操作 | 方式 |
|------|------|
| **新建便签** | 点击任意通知 → 弹出输入框 → 输入内容 → 保存 |
| **隐藏便签** | 点击通知右侧 `✕` 按钮 → 从通知栏移除（数据保留） |
| **悬浮输入** | 主界面「悬浮窗输入」→ 桌面任意位置呼出输入框 |
| **编辑便签** | 主界面点击便签 → 编辑内容 / 切换固定状态 |

### 📌 固定便签（Sticky）

- 固定的便签带 `FLAG_NO_CLEAR | FLAG_ONGOING_EVENT`，清理通知时不会被移除
- 重启手机后自动恢复显示
- 适用于待办事项、购物清单等需要长期提醒的场景

### ⏰ 定时提醒

- 支持设置提醒时间：精确到分钟
- 多级重复规则：
  - **不重复** — 单次提醒
  - **每天** — 按日循环
  - **每周** — 按周循环
  - **每月** — 按月循环
  - **自定义间隔** — 指定小时数重复
- 支持设置重复次数（1~N 次或无限次）

### 💾 数据管理

| 功能 | 说明 |
|------|------|
| **隐藏 / 恢复** | 从通知栏隐藏（保留数据），应用内「已移除」分区可一键恢复 |
| **永久删除** | 彻底清除数据，不可恢复 |
| **批量清空** | 「清空全部」一键清除所有隐藏便签 |
| **导出 JSON** | 系统文件选择器 → 选择目录 → 保存 `通知栏便签备份_日期.json` |

### 🔄 后台持久化

| 机制 | 说明 |
|------|------|
| **前台服务** | `NotificationForegroundService` 保持通知不丢失 |
| **开机自启** | `BootReceiver` + `directBootAware` + 国产 ROM 兼容广播 |
| **电池白名单** | 自动跳转系统设置，引导用户关闭电池优化 |
| **厂商优化** | 支持小米/华为/OPPO/Vivo/三星自启动管理页面自动跳转 |

---

## 🎨 界面预览

```
┌── 通知栏 ──────────────────────────┐
│                                    │
│ ┌────────────────────────────────┐ │
│ │ 翻译  ·  现在              [✕] │ │  ← 蓝色渐变背景
│ ├────────────────────────────────┤ │
│ │ 背单词  ·  3分钟前          [✕] │ │
│ ├────────────────────────────────┤ │
│ │ 看书  ·  今天              [✕] │ │
│ └────────────────────────────────┘ │
│                                    │
└────────────────────────────────────┘
```

```
┌── 主界面 ──────────────────────────┐
│ 📌 通知栏便签                     │
│ ├─ 翻译                📌固定      │  ← 可点击编辑
│ ├─ 背单词              📌固定      │
│ ├─ 看书                           │
│ ─────────────────────────────────  │
│ 🗑️ 已从通知栏移除                 │
│ ├─ 旧便签1      [恢复] [删除]      │
│ └─ 旧便签2      [恢复] [删除]      │
│ ─────────────────────────────────  │
│             [清空全部]              │
│                       [➕ 大按钮]   │
└────────────────────────────────────┘
```

---

## 🏗 技术架构

```
Kotlin + Jetpack Compose + Material 3
├── 数据层: Room (SQLite)
├── 服务层: Foreground Service + WorkManager + BroadcastReceiver
├── 通知层: 自定义 RemoteViews + NotificationCompat
└── 导出: Gson + SAF CreateDocument
```

| 组件 | 职责 |
|------|------|
| `NotificationHelper` | 通知创建/更新/删除，RemoteViews 布局 |
| `NotificationForegroundService` | START_STICKY 前台服务，保活通知 |
| `BootReceiver` | 开机+国产 ROM 广播监听，重启服务 |
| `OverlayInputService` | 悬浮窗输入框，桌面快速添加 |
| `ReminderWorker` | WorkManager 定时提醒调度 |
| `JsonExportUtil` | JSON 导入/导出/分享 |
| `MainViewModel` | 便签 CRUD + 隐藏/恢复/删除管理 |
| `AddEditDialog` | Compose 对话框，支持滚动 |
| `ReminderDialog` | Compose 提醒设置，支持滚动 |

详细架构见 [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)

---

## 🚀 快速开始

### 环境要求

- **JDK**: 21+
- **Android SDK**: compileSdk 34, minSdk 26
- **Gradle**: 8.5+

### 构建

```bash
export JAVA_HOME="你的 JDK 路径"
./gradlew clean assembleDebug
# 输出: app/build/outputs/apk/debug/app-debug.apk
```

### 安装后首次设置

1. 打开应用 → 授予**通知权限**（Android 13+）
2. 弹出电池优化 → 点击**允许**
3. 国产手机 → 进入自启动管理 → 开启开关
4. 如需悬浮窗 → 主界面点击「悬浮窗输入」→ 授权

---

## 📁 项目结构

```
NotificationNotes/
├── app/src/main/java/com/example/notificationnotes/
│   ├── data/          # Room 数据层
│   ├── service/       # 通知、前台服务、广播接收器
│   ├── ui/            # Compose UI（主界面、对话框、主题）
│   ├── util/          # JSON 工具
│   └── MainActivity.kt
├── docs/              # 需求、架构、设计、构建文档
├── devlog/            # 开发日志、待办事项
├── CLAUDE.md          # AI 助手指引
└── build.gradle.kts   # 构建配置
```

---

## 🔧 配置说明

`app/build.gradle.kts`：
```kotlin
versionCode = 4
versionName = "4.0.0"
compileSdk = 34
minSdk = 26
```

---

## 📄 相关文档

| 文档 | 说明 |
|------|------|
| [CLAUDE.md](CLAUDE.md) | AI 开发工作指引 |
| [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md) | 功能需求清单 |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | 技术架构详解 |
| [docs/DESIGN.md](docs/DESIGN.md) | 颜色/尺寸/通知样式规范 |
| [docs/BUILD.md](docs/BUILD.md) | 构建命令与发布检查 |
| [docs/CHANGELOG.md](docs/CHANGELOG.md) | 版本变更记录 |
| [devlog/TODO.md](devlog/TODO.md) | 待办事项 |

---

## 📋 版本历史

| 版本 | 关键更新 |
|------|---------|
| **v4.0** | 独立通知平铺、✕ 隐藏按钮嵌入、ID 冲突修复、CreateDocument 导出、厂商自启引导 |
| **v3.0** | SAF 路径选择导出、后台持久化服务 |
| **v2.0** | 通知分组、隐藏/恢复/删除分层操作、开机自启 |
| **v1.0** | 初始版本：便签 CRUD、Sticky、悬浮窗、提醒、JSON 导出 |
