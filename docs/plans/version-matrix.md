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

## 2. Task 2 实测结果（2026-09-19 更新）

| 组件 | 版本 | 实测结论 |
| --- | --- | --- |
| Compose Multiplatform | **1.10.3** | **可用**：与 Kotlin 2.2.10 + AGP 9.3.3 组合构建通过，无需回退到 1.9.0 |
| Ktor | 3.6.0 | 已解析（commonMain / commonTest 依赖均通过） |
| kotlinx-coroutines | 1.11.0 | 已解析 |
| kotlinx-serialization | 1.11.0 | 已解析 |
| kotlinx-datetime | 0.8.0 | 已解析 |
| Room / DataStore | 仍未引入 | 按计划推迟到 Phase 2 选型 |

### AGP 9 与 KMP 的关键限制（实测）

`com.android.application` 与 `org.jetbrains.kotlin.multiplatform` 自 AGP 9.0 起**不能共存**，报错：

```text
The 'com.android.library' (or 'com.android.application') plugin is not compatible with the
'org.jetbrains.kotlin.multiplatform' plugin since AGP 9.0.
```

当前项目在 `gradle.properties` 中启用 AGP 官方临时旁路：

```properties
android.builtInKotlin=false
android.newDsl=false
```

副作用：只能用经典 DSL（`compileSdk = 37`，不能使用 `compileSdk { version = release(37) }` 与 `buildTypes.release.optimization`）。
**技术债：AGP 10 会移除该旁路**，届时需拆成两个模块：

```text
mobile/composeApp     com.android.kotlin.multiplatform.library   （commonMain 全部业务代码）
mobile/androidApp     com.android.application                    （MainActivity + 清单，依赖 :composeApp）
```

---

## 3. 仍待实测（后续 Phase）

| 组件 | 何时实测 | 选版依据 |
| --- | --- | --- |
| Room | Phase 2 | 设备持久化选型时对比 DataStore |
| DataStore | Phase 2 | 最新仅 `1.3.0-alpha11`，不引入 alpha |
| iOS / Kotlin/Native target | V2 | Apple target 需 macOS 编译；CMP 1.11+ 的原生目标要求 Kotlin ≥ 2.3 |

每次回退都必须回到本文件更新版本与原因。

---

## 3. 环境注意事项

1. **SDK 平台包命名变更**：`platforms;android-37` 已不存在，必须写 `platforms;android-37.0`（同系列还有 `-37.1`、`-37.2`）。
2. **`gradlew` 可执行位缺失**：git 中记录为 `100644`，新克隆后执行 `./gradlew` 会得到 `Permission denied`。已修正为 `100755`。
3. **沙箱限制**：容器内无 JDK/SDK 且沙箱 DNS 不通，所有下载与构建都需要在沙箱外执行；`local.properties` 已被 `.gitignore` 忽略，不会误提交机器相关路径。
