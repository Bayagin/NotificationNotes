# CLAUDE.md — 通知栏便签 (Notification Notes)

本项目为 Android 原生应用，在通知栏直接显示和管理便签。

## 标准文件路径

| 类别 | 文件 | 说明 |
|------|------|------|
| **需求** | `docs/REQUIREMENTS.md` | 完整功能需求清单 |
| **架构** | `docs/ARCHITECTURE.md` | 技术栈、项目结构、数据流 |
| **设计** | `docs/DESIGN.md` | 颜色、间距、通知栏样式规范 |
| **构建** | `docs/BUILD.md` | 环境要求、构建命令、发布检查 |
| **变更** | `docs/CHANGELOG.md` | 各版本功能变更记录 |
| **待办** | `devlog/TODO.md` | 待实现功能 |
| **日志** | `devlog/YYYY-MM-DD.md` | 每日开发记录 |

## 工作说明

### 每次开发前
1. 阅读 `devlog/TODO.md` 确认当前待办
2. 阅读对应 `docs/` 文件了解规范

### 开发过程中
- 修改代码后运行 `./gradlew assembleDebug` 确认零 warning
- 涉及 UI 变更参考 `docs/DESIGN.md` 颜色/尺寸规范
- 重大改动更新 `docs/ARCHITECTURE.md`

### 开发完成后
1. 更新 `devlog/YYYY-MM-DD.md` 记录完成事项
2. 更新 `devlog/TODO.md` 标记完成/新增待办
3. 更新 `docs/CHANGELOG.md` 记录版本变更
4. 构建 APK 并存档

### 版本发布前
1. 更新 `app/build.gradle.kts` 中 `versionCode/versionName`
2. 检查 `docs/BUILD.md` 发布检查清单
3. 运行 `./gradlew clean assembleDebug` 确认零 warning

### 关键约束
- JDK: `D:/projectsoft/jdk-21.0.11`
- SDK: `D:/projectsoft/sdk`
- 构建命令: `export JAVA_HOME="D:/projectsoft/jdk-21.0.11" && ./gradlew clean assembleDebug`
- 必须保持 `gradle.properties` 中 `org.gradle.java.home` 指向正确 JDK
