# 构建与部署

## 环境要求
- JDK 21+（推荐 Oracle JDK 21.0.11）
- Android SDK（compileSdk 34, minSdk 26）
- Gradle 8.5+

## 构建命令

```bash
# 设置 JDK
export JAVA_HOME="D:/projectsoft/jdk-21.0.11"

# 清理并构建 Debug APK
./gradlew clean assembleDebug

# 输出
app/build/outputs/apk/debug/app-debug.apk
```

## 配置文件
- `gradle.properties`：JVM 参数、JDK 路径
- `local.properties`：SDK 路径（`sdk.dir=D:/projectsoft/sdk`）
- `app/build.gradle.kts`：版本号、依赖

## 当前版本
- versionCode: 4
- versionName: 4.0.0

## 关键依赖

| 库 | 版本 |
|----|------|
| AGP | 8.3.2 |
| Kotlin | 1.9.21 |
| Compose BOM | 2024.02.00 |
| Room | 2.6.1 |
| WorkManager | 2.9.0 |
| Gson | 2.10.1 |

## 发布前检查
1. [ ] 版本号已更新
2. [ ] `gradle.properties` 指向正确 JDK
3. [ ] `local.properties` 指向正确 SDK
4. [ ] `build.gradle.kts` 依赖版本无冲突
5. [ ] `./gradlew clean assembleDebug` 零 warning

## 签名发布
Debug 版本使用默认 debug.keystore。
Release 需配置签名文件。
