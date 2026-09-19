# 版本矩阵（实测记录）

**记录时间：** 2026-09-19
**环境：** Ubuntu 26.04.1 LTS，x86_64，无 root（`sudo` 需交互密码，故工具链装在用户目录）
**工具链位置：** `~/.local/toolchain/jdk-25.0.4.1+1`、`~/.local/toolchain/android-sdk`

---

## 1. 已实测通过

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| JDK | Eclipse Temurin 25.0.4.1+1 | tarball 安装，免 root |
| Gradle | 9.5.0 | 由 `gradle/wrapper/gradle-wrapper.properties` 指定并由 Wrapper 下载 |
| Android cmdline-tools | 16111833 | `sdkmanager` 已弃用，新入口为同目录 `android` 二进制 |
| Android Platform | `platforms/android-37.0` | 提供 `compileSdk 37` |
| Build-Tools | 37.0.0 | |
| Platform-Tools | 37.0.1 | |
| Kotlin | 2.2.10 | 现有工程 `libs.versions.toml` |
| AGP | 9.3.3 | 现有工程 |
| androidx Compose BOM | 2026.02.01 | 对应 androidx Compose 1.10.4 |

### 基线构建证据

```text
$ ./gradlew --version
Gradle 9.5.0
Launcher JVM:  25.0.4.1 (Eclipse Adoptium 25.0.4.1+1-LTS)
Daemon JVM:    Compatible with Java 25, any vendor (from gradle/gradle-daemon-jvm.properties)

$ ./gradlew :app:assembleDebug
BUILD SUCCESSFUL in 1m 8s
36 actionable tasks: 36 executed
```

---

## 2. 待 Task 2 实测（Phase 0）

| 组件 | 计划版本 | 选版依据 | 回退顺序 |
| --- | --- | --- | --- |
| Compose Multiplatform | 1.10.3 | 其 runtime 映射到 androidx Compose 1.10.5，与现有 1.10.4 同线；CMP 1.11.0 起要求 Kotlin 语言版本 ≥ 2.2（Android/JVM），原生目标要求 Kotlin ≥ 2.3 | `1.9.0` → 升 Kotlin 至 `2.2.20` |
| Ktor | 3.6.0 | Maven Central 当前 release | `3.5.x` |
| kotlinx-coroutines | 1.11.0 | Maven Central 当前 release | `1.10.x` |
| kotlinx-serialization | 1.11.0 | 最新稳定（1.12.0 仍是 RC） | `1.10.0` |
| kotlinx-datetime | 0.8.0 | 最新稳定（另有 `0.6.x-compat` 变体，不用） | `0.7.1` |
| Room | 未选 | Phase 2 再定；Phase 0 只用内存实现 | — |
| DataStore | 未选 | 最新仅 `1.3.0-alpha11`，不在 Phase 0 引入 alpha 依赖 | — |

每次回退都必须回到本文件更新版本与原因。

---

## 3. 环境注意事项

1. **SDK 平台包命名变更**：`platforms;android-37` 已不存在，必须写 `platforms;android-37.0`（同系列还有 `-37.1`、`-37.2`）。
2. **`gradlew` 可执行位缺失**：git 中记录为 `100644`，新克隆后执行 `./gradlew` 会得到 `Permission denied`。已修正为 `100755`。
3. **沙箱限制**：容器内无 JDK/SDK 且沙箱 DNS 不通，所有下载与构建都需要在沙箱外执行；`local.properties` 已被 `.gitignore` 忽略，不会误提交机器相关路径。
