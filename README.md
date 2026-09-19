# Wake Up My Wall

一个基于 Jetpack Compose 的 Android 应用项目。

## 项目状态

当前项目处于初始开发阶段，包含可运行的 Android 应用骨架和 Compose 示例界面。

## 技术栈

- Kotlin 2.2.10
- Jetpack Compose
- Material 3
- Android Gradle Plugin 9.3.3
- Gradle Wrapper
- 最低 Android SDK 30
- 目标 Android SDK 37

## 环境要求

- Android Studio（建议使用支持当前 Android Gradle Plugin 的版本）
- JDK 17 或更高版本
- Android SDK 37

## 构建与运行

1. 使用 Android Studio 打开项目根目录。
2. 等待 Gradle 同步完成，并确保本机已安装对应的 Android SDK。
3. 连接 Android 设备或启动模拟器。
4. 运行 `app` 配置。

也可以在项目根目录执行：

```bash
./gradlew assembleDebug
```

Windows PowerShell：

```powershell
.\gradlew.bat assembleDebug
```

## 测试

运行 JVM 单元测试：

```bash
./gradlew test
```

运行 Android 仪器测试（需要连接设备或启动模拟器）：

```bash
./gradlew connectedAndroidTest
```

## 项目结构

```text
app/
├── src/main/java/       # 应用源码
├── src/main/res/        # Android 资源
├── src/test/            # JVM 单元测试
└── src/androidTest/     # Android 仪器测试
gradle/                  # Gradle Wrapper 与版本目录
```

## 许可证

本项目采用 [MIT License](LICENSE) 开源。
