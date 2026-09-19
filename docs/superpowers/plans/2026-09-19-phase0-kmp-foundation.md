# Phase 0：KMP 工程基础 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把当前单模块 Android 骨架改造为 monorepo 下的 KMP 工程，落地 domain / core / app 骨架、PC 状态机、网络与存储抽象、导航骨架与 CI，使 App 能启动并进入空 Dashboard 骨架页。

**Architecture:** 单一 Gradle 根（根目录 `./gradlew`），产品代码集中在 `mobile/composeApp` 模块；`commonMain` 承载全部业务逻辑且不依赖任何 Android API，Android 平台能力放 `androidMain`。另外声明一个 `jvm("desktop")` target，**仅用于在 PC 上跑 Compose UI 测试与预览**，不是产品形态。

**Tech Stack:** Kotlin 2.2.10、Compose Multiplatform 1.10.3、AGP 9.3.3、Gradle 9.5.0、Ktor 3.6.0、kotlinx-serialization 1.11.0、kotlinx-coroutines 1.11.0、kotlinx-datetime 0.8.0、kotlin-test。

**Spec:** [docs/superpowers/specs/2026-09-19-desktop-companion-design.md](../specs/2026-09-19-desktop-companion-design.md)

## Global Constraints

- `minSdk = 30`、`targetSdk = 37`、`compileSdk = 37`（沿用现有工程设置，不得下调）。
- 包名与 namespace 统一为 `com.xukunz.wakeupmywall`（规范 D6）。
- `commonMain` 中**禁止** import 任何 `android.*`；平台能力只能经接口或 `expect/actual` 暴露。
- 本阶段不实现任何 Agent 接口的真实调用，也不引入任意命令执行能力。
- 依赖版本一律写进 `gradle/libs.versions.toml`，不允许在模块脚本里硬编码版本字符串。
- 每笔提交前必须让 `./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest` 通过。
- 本容器**没有 JDK、没有 Android SDK，且沙箱 DNS 不通**；Task 0 的安装类命令必须在沙箱外执行（需用户批准）。
- 提交信息使用 Conventional Commits（`feat:` / `chore:` / `test:` / `refactor:` / `docs:` / `ci:`）。

---

## File Structure

| 路径 | 职责 |
| --- | --- |
| `settings.gradle.kts` | Gradle 根配置；把 `:composeApp` 指向 `mobile/composeApp` |
| `gradle/libs.versions.toml` | 唯一版本来源 |
| `mobile/composeApp/build.gradle.kts` | KMP + Android + Compose 目标与依赖 |
| `.../app/App.kt` | 应用根 Composable，连接导航与主题 |
| `.../app/AppNavigator.kt` | 工作空间导航状态机（纯 Kotlin，可测） |
| `.../domain/model/PcState.kt` | PC 状态枚举 + 每种状态的 UI 能力描述 |
| `.../domain/model/PcDevice.kt` | 多设备数据模型 |
| `.../domain/model/MacAddress.kt` | MAC 解析与校验（值对象） |
| `.../domain/usecase/PcStateMachine.kt` | 状态迁移纯函数 |
| `.../core/network/HttpClientFactory.kt` | Ktor client 构造（超时、JSON） |
| `.../core/network/ApiResult.kt` | 统一结果与错误映射 |
| `.../core/storage/SettingsStorage.kt` | 设置/设备持久化抽象（真实现落在 Phase 2） |
| `.../core/theme/Tokens.kt`、`AppTheme.kt` | 最小可用暗色主题与强调色令牌 |
| `.../ui/components/PlaceholderScreen.kt` | 骨架占位页（Phase 1 逐页替换） |
| `mobile/composeApp/src/androidMain/kotlin/.../MainActivity.kt` | Android 入口 |
| `mobile/composeApp/src/desktopTest/kotlin/...` | Compose UI 测试（PC 上运行，无需模拟器） |
| `docs/plans/version-matrix.md` | 实测通过的版本矩阵记录 |
| `.github/workflows/ci.yml` | CI：单测 + UI 测试 + 组装 |

---

### Task 0: 工具链准备与版本矩阵锁定

**Files:**
- Create: `docs/plans/version-matrix.md`
- Create: `local.properties`（本地文件，已被 `.gitignore` 忽略）

**Interfaces:**
- Consumes: 无
- Produces: 可用的 `java`、`ANDROID_SDK_ROOT`、可执行的 `./gradlew`；`docs/plans/version-matrix.md` 记录实测矩阵供后续任务引用

- [x] **Step 1: 安装 JDK 25（免 root，已实测）**

本容器是 Ubuntu 26.04 x86_64 且 `sudo` 需要交互密码，`apt-get` 不可用，因此改用 Temurin tarball 装到用户目录：

```bash
mkdir -p ~/.local/toolchain && cd ~/.local/toolchain
curl -fL --retry 3 -o jdk25.tar.gz \
  "https://github.com/adoptium/temurin25-binaries/releases/download/jdk-25.0.4.1%2B1/OpenJDK25U-jdk_x64_linux_hotspot_25.0.4.1_1.tar.gz"
tar xzf jdk25.tar.gz
export JAVA_HOME="$HOME/.local/toolchain/jdk-25.0.4.1+1"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

Expected: `openjdk version "25.0.4.1" 2026-08-18 LTS`（实测输出）。

- [x] **Step 2: 安装 Android SDK 命令行工具**

```bash
export ANDROID_SDK_ROOT="$HOME/.local/toolchain/android-sdk"
mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"
curl -fL -o /tmp/cmdline-tools.zip \
  https://dl.google.com/android/repository/commandlinetools-linux-16111833_latest.zip
unzip -q -o /tmp/cmdline-tools.zip -d /tmp/cmdline-extract
mv /tmp/cmdline-extract/cmdline-tools "$ANDROID_SDK_ROOT/cmdline-tools/latest"
```

Expected: `"$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"` 可执行。
（该 URL 与 `platforms;android-37`、`build-tools;37.0.0` 均已通过 `dl.google.com/android/repository/repository2-3.xml` 核实存在。）

- [x] **Step 3: 安装平台与构建工具并接受许可**

```bash
export JAVA_HOME="$HOME/.local/toolchain/jdk-25.0.4.1+1"
export ANDROID_SDK_ROOT="$HOME/.local/toolchain/android-sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$PATH"
yes | sdkmanager --sdk_root="$ANDROID_SDK_ROOT" --licenses
sdkmanager --sdk_root="$ANDROID_SDK_ROOT" "platform-tools" "platforms;android-37.0" "build-tools;37.0.0"
sdkmanager --sdk_root="$ANDROID_SDK_ROOT" --list_installed
```

Expected: 出现 `platform-tools 37.0.1`、`platforms/android-37.0`、`build-tools/37.0.0`。

> **重要（已实测）：** SDK 平台包已改为小版本命名，`platforms;android-37` 会报 `Package platforms/android-37 not found`，必须用 `platforms;android-37.0`。另外 `sdkmanager` 已标记弃用，新入口是同一目录下的 `android` 二进制（`android sdk`）。

- [x] **Step 4: 写入 `local.properties` 并验证 Gradle 可启动**

```bash
cd /home/xukunz/桌面/WakeUpMyWall
printf 'sdk.dir=%s\n' "$HOME/.local/toolchain/android-sdk" > local.properties
chmod +x gradlew          # 仓库里 gradlew 模式是 100644，新克隆后无法执行
./gradlew --version
```

Expected: `Gradle 9.5.0`、`Launcher JVM: 25.0.4.1`、`Daemon JVM: Compatible with Java 25`（实测输出）。

然后用**未改动的原始工程**建立基线：

```bash
./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL in 1m 8s`（实测输出，`compileSdk 37` 由 `platforms/android-37.0` 提供）。

- [x] **Step 5: 记录版本矩阵**

把实测结果写入 `docs/plans/version-matrix.md`，首版内容：

```markdown
| 组件 | 版本 | 依据 |
| --- | --- | --- |
| Kotlin | 2.2.10 | 现有工程 libs.versions.toml |
| AGP | 9.3.3 | 现有工程 |
| Gradle | 9.5.0 | gradle-wrapper.properties |
| JDK（Daemon） | 25 | gradle/gradle-daemon-jvm.properties |
| Compose Multiplatform | 1.10.3 | 对应 androidx Compose 1.10.5，与现有 BOM(androidx 1.10.4) 同线 |
| Ktor | 3.6.0 | Maven Central release |
```

若 `1.10.3` 在 Task 2 解析失败，回退顺序为 `1.9.0` → 升 Kotlin 至 `2.2.20`；每次回退都要更新本文件并注明原因。

已实测项（截至 2026-09-19）：JDK `25.0.4.1+1`、Gradle `9.5.0`、`platforms/android-37.0`、`build-tools 37.0.0`、`platform-tools 37.0.1`，原始工程 `:app:assembleDebug` 通过。**Compose Multiplatform 1.10.3 尚未实测**，由 Task 2 首次解析时确认。

- [x] **Step 6: Commit**

```bash
git add docs/plans/version-matrix.md
git commit -m "docs: record verified toolchain and version matrix"
```

---

### Task 1: 迁移到 monorepo 目录结构

**Files:**
- Modify: `settings.gradle.kts`
- Move: `app/` → `mobile/composeApp/`

**Interfaces:**
- Consumes: Task 0 的工具链
- Produces: Gradle 模块路径 `:composeApp`，物理路径 `mobile/composeApp`

- [x] **Step 1: 用 git mv 迁移模块目录（保留历史）**

```bash
cd /home/xukunz/桌面/WakeUpMyWall
mkdir -p mobile
git mv app mobile/composeApp
```

- [x] **Step 2: 更新 `settings.gradle.kts`**

把末尾的 `include(":app")` 替换为：

```kotlin
rootProject.name = "Wake Up My Wall"

include(":composeApp")
project(":composeApp").projectDir = file("mobile/composeApp")
// agent/ 在 Phase 4 引入；本阶段只有移动端
```

（文件顶部的 `pluginManagement` 与 `dependencyResolutionManagement` 段保持不变。）

- [x] **Step 3: 验证模块图与构建**

```bash
./gradlew projects
./gradlew :composeApp:assembleDebug
```

Expected: `projects` 中只有 `:composeApp`；`assembleDebug` 输出 `BUILD SUCCESSFUL`。

- [x] **Step 4: Commit**

```bash
git add -A
git commit -m "refactor: move app module under mobile/ as composeApp"
```

---

### Task 2: 接入 KMP + Compose Multiplatform 构建

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `mobile/composeApp/build.gradle.kts`
- Move: `src/main` → `src/androidMain`，`src/test` → `src/androidUnitTest`，`src/androidTest` → `src/androidInstrumentedTest`

**Interfaces:**
- Consumes: `:composeApp`
- Produces: 源集 `commonMain` / `androidMain` / `commonTest` / `androidUnitTest` / `desktopTest`；可运行任务 `testDebugUnitTest`、`desktopTest`

- [x] **Step 1: 在版本目录声明插件与库**

```toml
[versions]
agp = "9.3.3"
kotlin = "2.2.10"
composeMultiplatform = "1.10.3"
coroutines = "1.11.0"
serialization = "1.11.0"
ktor = "3.6.0"
datetime = "0.8.0"

[libraries]
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "datetime" }
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-kotlinx-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-client-mock = { module = "io.ktor:ktor-client-mock", version.ref = "ktor" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
compose-multiplatform = { id = "org.jetbrains.compose", version.ref = "composeMultiplatform" }
```

- [x] **Step 2: 迁移源集目录**

```bash
cd /home/xukunz/桌面/WakeUpMyWall/mobile/composeApp
git mv src/main src/androidMain
git mv src/test src/androidUnitTest
git mv src/androidTest src/androidInstrumentedTest
```

- [x] **Step 3: 重写 `mobile/composeApp/build.gradle.kts`**

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
    }
    // 仅用于 UI 测试与设计预览，不产出桌面产品
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(compose.uiTooling)
        }
        val desktopTest by getting {
            dependencies {
                implementation(compose.uiTest)
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

android {
    namespace = "com.example.wakeupmywall"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.wakeupmywall"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures { compose = true }
}
```

原文件里的 `androidx.compose.*` 依赖、`buildTypes { optimization { ... } }` 块一并删除（由 `compose.*` 与默认配置取代）。

- [x] **Step 4: 确认 Android 资源与清单位置**

KMP 的 android target 读取 `src/androidMain/AndroidManifest.xml` 与 `src/androidMain/res/`。用 `ls mobile/composeApp/src/androidMain` 确认二者存在；若缺，用 `git mv` 移动到该位置。

- [x] **Step 5: 验证构建与测试**

```bash
cd /home/xukunz/桌面/WakeUpMyWall
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:testDebugUnitTest
```

Expected: 两条命令均 `BUILD SUCCESSFUL`。

- [x] **Step 6: Commit**

```bash
git add -A
git commit -m "build: convert composeApp to Kotlin Multiplatform with Compose Multiplatform"
```

---

### Task 3: 包名迁移与 commonMain 应用根

**Files:**
- Modify: `mobile/composeApp/build.gradle.kts`（namespace / applicationId）
- Move: `src/androidMain/kotlin/com/example/wakeupmywall/**` → `com/xukunz/wakeupmywall/**`
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/app/App.kt`

**Interfaces:**
- Consumes: KMP 源集
- Produces: `@Composable fun App()`（无参数，位于 `commonMain`）

- [x] **Step 1: 迁移包目录**

```bash
cd /home/xukunz/桌面/WakeUpMyWall/mobile/composeApp/src/androidMain/kotlin
mkdir -p com/xukunz
git mv com/example/wakeupmywall com/xukunz/wakeupmywall
rmdir com/example 2>/dev/null || true
```

- [x] **Step 2: 全局替换包名**

```bash
cd /home/xukunz/桌面/WakeUpMyWall
grep -rl "com\.example\.wakeupmywall" --include='*.kt' --include='*.kts' --include='*.xml' mobile/ \
  | xargs -r sed -i 's/com\.example\.wakeupmywall/com.xukunz.wakeupmywall/g'
grep -rn "com\.example" --include='*.kt' --include='*.kts' mobile/ || echo "package rename clean"
```

Expected: 第二条命令输出 `package rename clean`。

- [x] **Step 3: 创建 `commonMain` 应用根**

```kotlin
package com.xukunz.wakeupmywall.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun App() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Text("WakeUpMyWall")
    }
}
```

- [x] **Step 4: Android 入口改为委托 `App()`，删除模板代码**

```kotlin
package com.xukunz.wakeupmywall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.xukunz.wakeupmywall.app.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}
```

```bash
cd /home/xukunz/桌面/WakeUpMyWall/mobile/composeApp
git rm -r src/androidMain/kotlin/com/xukunz/wakeupmywall/ui 2>/dev/null || true
git rm src/androidUnitTest/java/com/xukunz/wakeupmywall/ExampleUnitTest.kt 2>/dev/null || true
git rm src/androidInstrumentedTest/java/com/xukunz/wakeupmywall/ExampleInstrumentedTest.kt 2>/dev/null || true
```

- [x] **Step 5: 验证并提交**

```bash
./gradlew :composeApp:assembleDebug
git add -A
git commit -m "refactor: rename package to com.xukunz.wakeupmywall and add common App entry"
```

---

### Task 4: 领域模型与 PC 状态机（TDD）

**Files:**
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/domain/model/PcState.kt`
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/domain/model/MacAddress.kt`
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/domain/model/PcDevice.kt`
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/domain/usecase/PcStateMachine.kt`
- Test: `src/commonTest/kotlin/com/xukunz/wakeupmywall/domain/PcStateMachineTest.kt`
- Test: `src/commonTest/kotlin/com/xukunz/wakeupmywall/domain/MacAddressTest.kt`

**Interfaces:**
- Consumes: 无
- Produces:
  - `enum class PcState { UNCONFIGURED, OFFLINE, WOL_READY, WAKING, ONLINE, AGENT_UNAVAILABLE, SLEEPING, RESTARTING, SHUTTING_DOWN, ERROR }`
  - `sealed interface PcEvent`（`WakeRequested` / `WakeTimedOut` / `SleepRequested` / `ShutdownRequested` / `RestartRequested` / `AgentResponded` / `AgentLost` / `CommandFailed`）
  - `object PcStateMachine { fun reduce(current: PcState, event: PcEvent): PcState }`
  - `data class PcCapabilities(...)` 与 `fun PcState.capabilities(device: PcDevice?): PcCapabilities`
  - `value class MacAddress` 与 `MacAddress.parse(input: String): MacAddress?`
  - `data class PcDevice(...)`

- [x] **Step 1: 写失败测试（状态机）**

文件 `src/commonTest/kotlin/com/xukunz/wakeupmywall/domain/PcStateMachineTest.kt`：

```kotlin
package com.xukunz.wakeupmywall.domain

import com.xukunz.wakeupmywall.domain.model.PcEvent
import com.xukunz.wakeupmywall.domain.model.PcState
import com.xukunz.wakeupmywall.domain.usecase.PcStateMachine
import kotlin.test.Test
import kotlin.test.assertEquals

class PcStateMachineTest {

    @Test
    fun `wake command moves wol ready to waking`() {
        assertEquals(PcState.WAKING, PcStateMachine.reduce(PcState.WOL_READY, PcEvent.WakeRequested))
    }

    @Test
    fun `agent heartbeat moves waking to online`() {
        assertEquals(PcState.ONLINE, PcStateMachine.reduce(PcState.WAKING, PcEvent.AgentResponded))
    }

    @Test
    fun `wake timeout falls back to wol ready`() {
        assertEquals(PcState.WOL_READY, PcStateMachine.reduce(PcState.WAKING, PcEvent.WakeTimedOut))
    }

    @Test
    fun `shutdown command is ignored when offline`() {
        assertEquals(PcState.OFFLINE, PcStateMachine.reduce(PcState.OFFLINE, PcEvent.ShutdownRequested))
    }

    @Test
    fun `shutdown from online goes to shutting down then offline`() {
        val shuttingDown = PcStateMachine.reduce(PcState.ONLINE, PcEvent.ShutdownRequested)
        assertEquals(PcState.SHUTTING_DOWN, shuttingDown)
        assertEquals(PcState.OFFLINE, PcStateMachine.reduce(shuttingDown, PcEvent.AgentLost))
    }

    @Test
    fun `unconfigured device stays unconfigured when agent is lost`() {
        assertEquals(PcState.UNCONFIGURED, PcStateMachine.reduce(PcState.UNCONFIGURED, PcEvent.AgentLost))
    }

    @Test
    fun `restart keeps restarting until agent returns`() {
        val restarting = PcStateMachine.reduce(PcState.ONLINE, PcEvent.RestartRequested)
        assertEquals(PcState.RESTARTING, restarting)
        assertEquals(PcState.ONLINE, PcStateMachine.reduce(restarting, PcEvent.AgentResponded))
    }

    @Test
    fun `error state recovers to online when agent responds`() {
        assertEquals(PcState.ONLINE, PcStateMachine.reduce(PcState.ERROR, PcEvent.AgentResponded))
    }
}
```

- [x] **Step 2: 运行测试，确认失败**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*PcStateMachineTest*"
```

Expected: 编译失败，提示 `Unresolved reference: PcStateMachine` / `PcEvent`。

- [x] **Step 3: 写失败测试（MAC 校验）**

文件 `src/commonTest/kotlin/com/xukunz/wakeupmywall/domain/MacAddressTest.kt`：

```kotlin
package com.xukunz.wakeupmywall.domain

import com.xukunz.wakeupmywall.domain.model.MacAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MacAddressTest {

    @Test
    fun `parses colon separated mac and normalizes to uppercase`() {
        val mac = assertNotNull(MacAddress.parse("aa:bb:cc:dd:ee:ff"))
        assertEquals("AA:BB:CC:DD:EE:FF", mac.normalized)
    }

    @Test
    fun `parses dash separated mac`() {
        assertEquals("AA:BB:CC:DD:EE:FF", MacAddress.parse("AA-BB-CC-DD-EE-FF")?.normalized)
    }

    @Test
    fun `rejects wrong segment count`() {
        assertNull(MacAddress.parse("AA:BB:CC:DD:EE"))
    }

    @Test
    fun `rejects non hex characters`() {
        assertNull(MacAddress.parse("ZZ:BB:CC:DD:EE:FF"))
    }

    @Test
    fun `exposes six bytes`() {
        assertEquals(
            listOf(0xAA, 0xBB, 0xCC, 0xDD, 0xEE, 0xFF).map { it.toByte() },
            MacAddress.parse("AA:BB:CC:DD:EE:FF")?.bytes?.toList(),
        )
    }
}
```

- [x] **Step 4: 实现模型与状态机**

`MacAddress.kt`：

```kotlin
package com.xukunz.wakeupmywall.domain.model

@JvmInline
value class MacAddress private constructor(private val value: String) {
    val normalized: String get() = value
    val bytes: ByteArray get() = value.split(":").map { it.toInt(16).toByte() }.toByteArray()

    companion object {
        fun parse(input: String): MacAddress? {
            val cleaned = input.trim().replace('-', ':').replace(" ", "")
            val parts = cleaned.split(':')
            if (parts.size != 6) return null
            val allHexPairs = parts.all { part ->
                part.length == 2 && part.all { char -> char.digitToIntOrNull(16) != null }
            }
            if (!allHexPairs) return null
            return MacAddress(parts.joinToString(":") { it.uppercase() })
        }
    }
}
```

`PcState.kt`：

```kotlin
package com.xukunz.wakeupmywall.domain.model

enum class PcState {
    UNCONFIGURED, OFFLINE, WOL_READY, WAKING, ONLINE,
    AGENT_UNAVAILABLE, SLEEPING, RESTARTING, SHUTTING_DOWN, ERROR,
}

sealed interface PcEvent {
    data object WakeRequested : PcEvent
    data object WakeTimedOut : PcEvent
    data object SleepRequested : PcEvent
    data object ShutdownRequested : PcEvent
    data object RestartRequested : PcEvent
    data object AgentResponded : PcEvent
    data object AgentLost : PcEvent
    data object CommandFailed : PcEvent
}

data class PcCapabilities(
    val primaryLabel: String,
    val primaryEnabled: Boolean,
    val canSleep: Boolean,
    val canShutdown: Boolean,
    val canRestart: Boolean,
    val statusText: String,
)

fun PcState.capabilities(device: PcDevice?): PcCapabilities = when (this) {
    PcState.UNCONFIGURED -> PcCapabilities("Setup PC", true, false, false, false, "Add your PC to begin")
    PcState.OFFLINE -> PcCapabilities("Wake PC", false, false, false, false, "Wake-on-LAN requires MAC")
    PcState.WOL_READY -> PcCapabilities("Wake PC", device?.macAddress != null, false, false, false, "Wake-on-LAN Ready")
    PcState.WAKING -> PcCapabilities("Waking…", false, false, false, false, "Waiting for Agent")
    PcState.ONLINE -> PcCapabilities("PC Online", false, true, true, true, "Agent connected over LAN")
    PcState.AGENT_UNAVAILABLE -> PcCapabilities("Agent unavailable", false, false, false, false, "Host reachable, Agent not responding")
    PcState.SLEEPING -> PcCapabilities("Sleeping…", false, false, false, false, "Waiting for Agent")
    PcState.RESTARTING -> PcCapabilities("Restarting…", false, false, false, false, "Waiting for Agent")
    PcState.SHUTTING_DOWN -> PcCapabilities("Shutting down…", false, false, false, false, "Waiting for Agent")
    PcState.ERROR -> PcCapabilities("Retry", true, false, false, false, "Last command failed")
}
```

`PcStateMachine.kt`：

```kotlin
package com.xukunz.wakeupmywall.domain.usecase

import com.xukunz.wakeupmywall.domain.model.PcEvent
import com.xukunz.wakeupmywall.domain.model.PcState

object PcStateMachine {

    fun reduce(current: PcState, event: PcEvent): PcState = when (event) {
        PcEvent.WakeRequested -> when (current) {
            PcState.WOL_READY, PcState.OFFLINE, PcState.ERROR -> PcState.WAKING
            else -> current
        }
        PcEvent.WakeTimedOut -> if (current == PcState.WAKING) PcState.WOL_READY else current
        PcEvent.SleepRequested -> if (current == PcState.ONLINE) PcState.SLEEPING else current
        PcEvent.ShutdownRequested -> if (current == PcState.ONLINE) PcState.SHUTTING_DOWN else current
        PcEvent.RestartRequested -> if (current == PcState.ONLINE) PcState.RESTARTING else current
        PcEvent.AgentResponded -> when (current) {
            PcState.UNCONFIGURED, PcState.OFFLINE, PcState.WOL_READY -> current
            else -> PcState.ONLINE
        }
        PcEvent.AgentLost -> when (current) {
            PcState.UNCONFIGURED -> PcState.UNCONFIGURED
            PcState.RESTARTING -> PcState.RESTARTING
            else -> PcState.OFFLINE
        }
        PcEvent.CommandFailed -> PcState.ERROR
    }
}
```

`PcDevice.kt`（字段与规范 §6 一致；`@Serializable` 在 Task 6 补充）：

```kotlin
package com.xukunz.wakeupmywall.domain.model

import kotlinx.datetime.Instant

data class PcDevice(
    val id: String,
    val name: String,
    val macAddress: MacAddress? = null,
    val ipAddress: String? = null,
    val broadcastAddress: String = "192.168.1.255",
    val wolPort: Int = 9,
    val agentHost: String? = null,
    val agentPort: Int = 9876,
    val isDefault: Boolean = false,
    val lastSeen: Instant? = null,
)
```

- [x] **Step 5: 运行测试，确认通过**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*PcStateMachineTest*" --tests "*MacAddressTest*"
```

Expected: `BUILD SUCCESSFUL`，13 个测试全绿。

- [x] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: add pc state machine, device model and mac validation"
```

---

### Task 5: 网络层抽象（Ktor + MockEngine 测试）

**Files:**
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/core/network/ApiResult.kt`
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/core/network/HttpClientFactory.kt`
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/core/network/AgentApi.kt`
- Test: `src/commonTest/kotlin/com/xukunz/wakeupmywall/core/network/AgentApiTest.kt`

**Interfaces:**
- Consumes: 无
- Produces:
  - `sealed interface ApiResult<out T>`，含 `Success<T>(value)` 与 `Failure(reason, message)`
  - `enum class ApiFailure { TIMEOUT, UNAUTHORIZED, NOT_FOUND, SERVER, NETWORK, DECODING }`
  - `fun createAgentHttpClient(engine: HttpClientEngine? = null): HttpClient`（请求超时 2.5 秒、忽略未知字段）
  - `class AgentApi(client: HttpClient) { suspend fun status(baseUrl: String, token: String?): ApiResult<AgentStatus> }`
  - `@Serializable data class AgentStatus(val hostname: String, val agentVersion: String, val uptimeSeconds: Long)`

- [x] **Step 1: 写失败测试**

文件 `src/commonTest/kotlin/com/xukunz/wakeupmywall/core/network/AgentApiTest.kt`：

```kotlin
package com.xukunz.wakeupmywall.core.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

class AgentApiTest {

    @Test
    fun `parses valid status payload`() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"hostname":"Desktop-Alpha","agentVersion":"0.1.0","uptimeSeconds":1234}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders,
            )
        }
        val api = AgentApi(createAgentHttpClient(engine))

        val result = api.status("http://192.168.1.10:9876", token = "t")

        val value = (result as ApiResult.Success).value
        assertEquals("Desktop-Alpha", value.hostname)
        assertEquals(1234, value.uptimeSeconds)
    }

    @Test
    fun `maps 401 to unauthorized failure`() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.Unauthorized) }
        val api = AgentApi(createAgentHttpClient(engine))

        val result = api.status("http://192.168.1.10:9876", token = "bad")

        assertEquals(ApiFailure.UNAUTHORIZED, (result as ApiResult.Failure).reason)
    }

    @Test
    fun `maps malformed payload to decoding failure`() = runTest {
        val engine = MockEngine {
            respond("""{"unexpected":true}""", HttpStatusCode.OK, jsonHeaders)
        }
        val api = AgentApi(createAgentHttpClient(engine))

        val result = api.status("http://192.168.1.10:9876", token = "t")

        assertEquals(ApiFailure.DECODING, (result as ApiResult.Failure).reason)
    }

    @Test
    fun `sends bearer token when provided`() = runTest {
        var seen: String? = null
        val engine = MockEngine { request ->
            seen = request.headers[HttpHeaders.Authorization]
            respond("""{"hostname":"h","agentVersion":"1","uptimeSeconds":1}""", HttpStatusCode.OK, jsonHeaders)
        }
        AgentApi(createAgentHttpClient(engine)).status("http://host:9876", token = "secret")

        assertEquals("Bearer secret", seen)
    }
}
```

- [x] **Step 2: 运行测试，确认失败**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*AgentApiTest*"
```

Expected: 编译失败，`Unresolved reference: AgentApi`。

- [x] **Step 3: 实现**

`ApiResult.kt`：

```kotlin
package com.xukunz.wakeupmywall.core.network

sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Failure(val reason: ApiFailure, val message: String) : ApiResult<Nothing>
}

enum class ApiFailure { TIMEOUT, UNAUTHORIZED, NOT_FOUND, SERVER, NETWORK, DECODING }
```

`HttpClientFactory.kt`：

```kotlin
package com.xukunz.wakeupmywall.core.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private val agentJson = Json { ignoreUnknownKeys = true; explicitNulls = false }

fun createAgentHttpClient(engine: HttpClientEngine? = null): HttpClient {
    val configure: HttpClientConfig<*>.() -> Unit = {
        install(ContentNegotiation) { json(agentJson) }
        install(HttpTimeout) {
            requestTimeoutMillis = 2_500
            connectTimeoutMillis = 2_000
            socketTimeoutMillis = 2_500
        }
    }
    return engine?.let { HttpClient(it, configure) } ?: HttpClient(configure)
}
```

`AgentApi.kt`：

```kotlin
package com.xukunz.wakeupmywall.core.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable

@Serializable
data class AgentStatus(val hostname: String, val agentVersion: String, val uptimeSeconds: Long)

class AgentApi(private val client: HttpClient) {

    suspend fun status(baseUrl: String, token: String?): ApiResult<AgentStatus> = try {
        val response = client.get("$baseUrl/api/v1/status") {
            if (token != null) header(HttpHeaders.Authorization, "Bearer $token")
        }
        if (response.status.value in 200..299) {
            ApiResult.Success(response.body<AgentStatus>())
        } else {
            ApiResult.Failure(response.status.toFailure(), "HTTP ${response.status.value}")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpRequestTimeoutException) {
        ApiResult.Failure(ApiFailure.TIMEOUT, e.message ?: "timeout")
    } catch (e: SerializationException) {
        ApiResult.Failure(ApiFailure.DECODING, e.message ?: "decoding error")
    } catch (e: Throwable) {
        ApiResult.Failure(ApiFailure.NETWORK, e.message ?: "network error")
    }
}

private fun HttpStatusCode.toFailure(): ApiFailure = when (value) {
    401, 403 -> ApiFailure.UNAUTHORIZED
    404 -> ApiFailure.NOT_FOUND
    in 500..599 -> ApiFailure.SERVER
    else -> ApiFailure.NETWORK
}
```

注意：`createAgentHttpClient` 刻意不安装 `HttpCallValidator`，因此 4xx/5xx 不抛异常，而是走 `response.status` 分支，保证错误映射稳定。

- [x] **Step 4: 运行测试，确认通过**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*AgentApiTest*"
```

Expected: 4 个测试全绿。

- [x] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add ktor based agent api with timeout and error mapping"
```

---

### Task 6: 存储抽象（真实现留到 Phase 2）

**Files:**
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/core/storage/SettingsStorage.kt`
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/core/storage/MacAddressSerializer.kt`
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/data/settings/InMemorySettingsStorage.kt`
- Modify: `src/commonMain/kotlin/com/xukunz/wakeupmywall/domain/model/PcDevice.kt`
- Test: `src/commonTest/kotlin/com/xukunz/wakeupmywall/data/settings/SettingsStorageTest.kt`

**Interfaces:**
- Consumes: `PcDevice`、`MacAddress`
- Produces:
  - `@Serializable data class AppearanceSettings(val accent: String = "AuroraBlue", val widgetStyle: String = "Glass", val wallpaperId: String = "mountain")`
  - `interface SettingsStorage`（`readDevices` / `writeDevices` / `readAppearance` / `writeAppearance`，全部 `suspend`）
  - `class InMemorySettingsStorage : SettingsStorage`

**说明：** 真实持久化后端（Room 还是 DataStore）推迟到 Phase 2 与设备管理一起决策，避免在 Phase 0 引入未验证的 alpha 依赖。

- [x] **Step 1: 写失败测试**

文件 `src/commonTest/kotlin/com/xukunz/wakeupmywall/data/settings/SettingsStorageTest.kt`：

```kotlin
package com.xukunz.wakeupmywall.data.settings

import com.xukunz.wakeupmywall.domain.model.MacAddress
import com.xukunz.wakeupmywall.domain.model.PcDevice
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsStorageTest {

    @Test
    fun `default appearance is glass aurora blue`() = runTest {
        val appearance = InMemorySettingsStorage().readAppearance()
        assertEquals("AuroraBlue", appearance.accent)
        assertEquals("Glass", appearance.widgetStyle)
    }

    @Test
    fun `devices round trip keeps mac`() = runTest {
        val storage = InMemorySettingsStorage()
        val device = PcDevice(id = "1", name = "My PC", macAddress = MacAddress.parse("AA:BB:CC:DD:EE:FF"))

        storage.writeDevices(listOf(device))

        assertEquals(listOf(device), storage.readDevices())
    }

    @Test
    fun `empty storage returns empty device list`() = runTest {
        assertEquals(emptyList(), InMemorySettingsStorage().readDevices())
    }
}
```

- [x] **Step 2: 运行测试，确认失败**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*SettingsStorageTest*"
```

Expected: 编译失败，`Unresolved reference: InMemorySettingsStorage`。

- [x] **Step 3: 实现接口与序列化器**

`SettingsStorage.kt`：

```kotlin
package com.xukunz.wakeupmywall.core.storage

import com.xukunz.wakeupmywall.domain.model.PcDevice
import kotlinx.serialization.Serializable

@Serializable
data class AppearanceSettings(
    val accent: String = "AuroraBlue",
    val widgetStyle: String = "Glass",
    val wallpaperId: String = "dusk-lake",
)

interface SettingsStorage {
    suspend fun readDevices(): List<PcDevice>
    suspend fun writeDevices(devices: List<PcDevice>)
    suspend fun readAppearance(): AppearanceSettings
    suspend fun writeAppearance(value: AppearanceSettings)
}
```

`MacAddressSerializer.kt`（让 `MacAddress` 能安全落盘/上网）：

```kotlin
package com.xukunz.wakeupmywall.core.storage

import com.xukunz.wakeupmywall.domain.model.MacAddress
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object MacAddressSerializer : KSerializer<MacAddress> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("MacAddress", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: MacAddress) {
        encoder.encodeString(value.normalized)
    }

    override fun deserialize(decoder: Decoder): MacAddress =
        MacAddress.parse(decoder.decodeString())
            ?: throw SerializationException("Invalid MAC address")
}
```

- [x] **Step 4: 给 `PcDevice` 加序列化注解**

`PcDevice.kt` 完整内容：

```kotlin
package com.xukunz.wakeupmywall.domain.model

import com.xukunz.wakeupmywall.core.storage.MacAddressSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class PcDevice(
    val id: String,
    val name: String,
    @Serializable(with = MacAddressSerializer::class)
    val macAddress: MacAddress? = null,
    val ipAddress: String? = null,
    val broadcastAddress: String = "192.168.1.255",
    val wolPort: Int = 9,
    val agentHost: String? = null,
    val agentPort: Int = 9876,
    val isDefault: Boolean = false,
    val lastSeen: Instant? = null,
)
```

- [x] **Step 5: 实现内存存储**

`InMemorySettingsStorage.kt`：

```kotlin
package com.xukunz.wakeupmywall.data.settings

import com.xukunz.wakeupmywall.core.storage.AppearanceSettings
import com.xukunz.wakeupmywall.core.storage.SettingsStorage
import com.xukunz.wakeupmywall.domain.model.PcDevice
import kotlinx.coroutines.flow.MutableStateFlow

class InMemorySettingsStorage : SettingsStorage {
    private val devicesFlow = MutableStateFlow<List<PcDevice>>(emptyList())
    private val appearanceFlow = MutableStateFlow(AppearanceSettings())

    override suspend fun readDevices(): List<PcDevice> = devicesFlow.value
    override suspend fun writeDevices(devices: List<PcDevice>) { devicesFlow.value = devices }
    override suspend fun readAppearance(): AppearanceSettings = appearanceFlow.value
    override suspend fun writeAppearance(value: AppearanceSettings) { appearanceFlow.value = value }
}
```

- [x] **Step 6: 运行测试并提交**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*SettingsStorageTest*"
git add -A
git commit -m "feat: add settings storage abstraction with in-memory implementation"
```

Expected: 3 个测试全绿。

---

### Task 7: 应用骨架、工作空间导航与 UI 测试宿主

**Files:**
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/app/AppNavigator.kt`
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/ui/components/PlaceholderScreen.kt`
- Modify: `src/commonMain/kotlin/com/xukunz/wakeupmywall/app/App.kt`
- Test: `src/commonTest/kotlin/com/xukunz/wakeupmywall/app/AppNavigatorTest.kt`
- Test: `src/desktopTest/kotlin/com/xukunz/wakeupmywall/app/AppUiTest.kt`

**Interfaces:**
- Consumes: `App()`
- Produces:
  - `enum class Workspace { Dashboard, Monitor, Settings }`
  - `class AppNavigator { val current: StateFlow<Workspace>; fun goTo(workspace: Workspace); fun next(); fun previous() }`
  - `@Composable fun PlaceholderScreen(title: String, modifier: Modifier = Modifier)`
  - `@Composable fun App(navigator: AppNavigator = remember { AppNavigator() })`

**说明：** 不引入 Navigation 库。三个工作空间的切换在 Phase 1 只是状态与手势，自己实现的 `AppNavigator` 可以被单测覆盖；等出现深链或复杂返回栈再评估引入 `org.jetbrains.androidx.navigation`。

- [x] **Step 1: 写导航失败测试（纯逻辑）**

文件 `src/commonTest/kotlin/com/xukunz/wakeupmywall/app/AppNavigatorTest.kt`：

```kotlin
package com.xukunz.wakeupmywall.app

import kotlin.test.Test
import kotlin.test.assertEquals

class AppNavigatorTest {

    @Test
    fun `starts on dashboard`() {
        assertEquals(Workspace.Dashboard, AppNavigator().current.value)
    }

    @Test
    fun `next moves dashboard to monitor and stops at settings`() {
        val navigator = AppNavigator()
        navigator.next()
        assertEquals(Workspace.Monitor, navigator.current.value)
        navigator.next()
        assertEquals(Workspace.Settings, navigator.current.value)
        navigator.next()
        assertEquals(Workspace.Settings, navigator.current.value)
    }

    @Test
    fun `previous from dashboard stays on dashboard`() {
        val navigator = AppNavigator()
        navigator.previous()
        assertEquals(Workspace.Dashboard, navigator.current.value)
    }

    @Test
    fun `goTo jumps directly`() {
        val navigator = AppNavigator()
        navigator.goTo(Workspace.Settings)
        assertEquals(Workspace.Settings, navigator.current.value)
    }
}
```

- [x] **Step 2: 运行测试，确认失败**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*AppNavigatorTest*"
```

Expected: 编译失败，`Unresolved reference: AppNavigator`。

- [x] **Step 3: 实现导航与占位页**

`AppNavigator.kt`：

```kotlin
package com.xukunz.wakeupmywall.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Workspace { Dashboard, Monitor, Settings }

class AppNavigator {
    private val state = MutableStateFlow(Workspace.Dashboard)
    val current: StateFlow<Workspace> = state.asStateFlow()

    fun goTo(workspace: Workspace) { state.value = workspace }

    fun next() {
        val order = Workspace.entries
        state.value = order[(order.indexOf(state.value) + 1).coerceAtMost(order.lastIndex)]
    }

    fun previous() {
        val order = Workspace.entries
        state.value = order[(order.indexOf(state.value) - 1).coerceAtLeast(0)]
    }
}
```

`PlaceholderScreen.kt`：

```kotlin
package com.xukunz.wakeupmywall.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@Composable
fun PlaceholderScreen(title: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().testTag("screen:$title"),
        contentAlignment = Alignment.Center,
    ) {
        Text(title)
    }
}
```

`App.kt` 完整内容：

```kotlin
package com.xukunz.wakeupmywall.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.xukunz.wakeupmywall.ui.components.PlaceholderScreen

@Composable
fun App(navigator: AppNavigator = remember { AppNavigator() }) {
    val workspace by navigator.current.collectAsState()
    Surface(modifier = Modifier.fillMaxSize()) {
        when (workspace) {
            Workspace.Dashboard -> PlaceholderScreen("Dashboard")
            Workspace.Monitor -> PlaceholderScreen("PC Monitor")
            Workspace.Settings -> PlaceholderScreen("Settings")
        }
    }
}
```

- [x] **Step 4: 写 Compose UI 测试（desktopTest）**

文件 `src/desktopTest/kotlin/com/xukunz/wakeupmywall/app/AppUiTest.kt`：

```kotlin
package com.xukunz.wakeupmywall.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class AppUiTest {

    @Test
    fun `dashboard renders first`() = runComposeUiTest {
        setContent { App() }
        onNodeWithTag("screen:Dashboard").assertIsDisplayed()
    }

    @Test
    fun `switching workspace renders monitor`() = runComposeUiTest {
        val navigator = AppNavigator().apply { goTo(Workspace.Monitor) }
        setContent { App(navigator) }
        onNodeWithTag("screen:PC Monitor").assertIsDisplayed()
    }
}
```

- [x] **Step 5: 运行全部测试**

```bash
./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest
```

Expected: 两个任务均 `BUILD SUCCESSFUL`，UI 测试在 JVM 上直接运行，无需模拟器。

- [x] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: add workspace navigation with jvm ui test harness"
```

---

### Task 8: 最小主题与强调色令牌

**Files:**
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/core/theme/Tokens.kt`
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/core/theme/AppTheme.kt`
- Modify: `src/commonMain/kotlin/com/xukunz/wakeupmywall/app/App.kt`
- Test: `src/commonTest/kotlin/com/xukunz/wakeupmywall/core/theme/ThemeAccentTest.kt`

**Interfaces:**
- Consumes: 无
- Produces:
  - `enum class ThemeAccent { AuroraBlue, Emerald, Purple, Amber, Red, Dynamic }`
  - `data class AccentPalette(val accent: Color, val onAccent: Color, val onlineColor: Color)`
  - `fun ThemeAccent.palette(): AccentPalette`
  - `object DarkSurface { background, card, outline, textPrimary, textSecondary }` 与 `object Spacing`
  - `@Composable fun WakeUpMyWallTheme(accent: ThemeAccent = ThemeAccent.AuroraBlue, content: @Composable () -> Unit)`

- [x] **Step 1: 写失败测试（对比度与颜色区分）**

文件 `src/commonTest/kotlin/com/xukunz/wakeupmywall/core/theme/ThemeAccentTest.kt`：

```kotlin
package com.xukunz.wakeupmywall.core.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertTrue

class ThemeAccentTest {

    private val concreteAccents = ThemeAccent.entries.filter { it != ThemeAccent.Dynamic }

    @Test
    fun `every accent provides readable onAccent color`() {
        concreteAccents.forEach { accent ->
            val palette = accent.palette()
            val ratio = contrastRatio(palette.accent, palette.onAccent)
            assertTrue(ratio >= 4.5, "$accent contrast was $ratio")
        }
    }

    @Test
    fun `online color is distinct from accent for every theme`() {
        concreteAccents.forEach { accent ->
            assertTrue(accent.palette().onlineColor != accent.palette().accent, "$accent")
        }
    }

    @Test
    fun `palettes are pairwise distinct`() {
        val accents = concreteAccents.map { it.palette().accent }
        assertTrue(accents.toSet().size == accents.size, "duplicate accent colors: $accents")
    }

    private fun contrastRatio(a: Color, b: Color): Double {
        val la = a.luminance()
        val lb = b.luminance()
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun Color.luminance(): Double {
        val r = linear(red)
        val g = linear(green)
        val b = linear(blue)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun linear(channel: Float): Double {
        val v = channel.toDouble()
        return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }
}
```

- [x] **Step 2: 运行测试，确认失败**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*ThemeAccentTest*"
```

Expected: 编译失败，`Unresolved reference: ThemeAccent`。

- [x] **Step 3: 实现令牌与主题**

`Tokens.kt`：

```kotlin
package com.xukunz.wakeupmywall.core.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class ThemeAccent { AuroraBlue, Emerald, Purple, Amber, Red, Dynamic }

data class AccentPalette(val accent: Color, val onAccent: Color, val onlineColor: Color)

fun ThemeAccent.palette(): AccentPalette = when (this) {
    ThemeAccent.AuroraBlue -> AccentPalette(Color(0xFF4C8DFF), Color(0xFF06101F), Color(0xFF3DDC97))
    ThemeAccent.Emerald -> AccentPalette(Color(0xFF2FBF71), Color(0xFF03170B), Color(0xFF7BE495))
    ThemeAccent.Purple -> AccentPalette(Color(0xFF8B5CF6), Color(0xFF120726), Color(0xFF5EEAD4))
    ThemeAccent.Amber -> AccentPalette(Color(0xFFF5A524), Color(0xFF2A1A00), Color(0xFF7BE495))
    ThemeAccent.Red -> AccentPalette(Color(0xFFE5484D), Color(0xFF1F0407), Color(0xFF7BE495))
    ThemeAccent.Dynamic -> AccentPalette(Color(0xFF4C8DFF), Color(0xFF06101F), Color(0xFF3DDC97))
}

object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}

object DarkSurface {
    val background = Color(0xFF0B0F17)
    val card = Color(0xFF151B26)
    val outline = Color(0x33FFFFFF)
    val textPrimary = Color(0xFFE8EDF5)
    val textSecondary = Color(0xFF9AA6B8)
}
```

`AppTheme.kt`：

```kotlin
package com.xukunz.wakeupmywall.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

@Composable
fun WakeUpMyWallTheme(
    accent: ThemeAccent = ThemeAccent.AuroraBlue,
    content: @Composable () -> Unit,
) {
    val palette = accent.palette()
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = palette.accent,
            onPrimary = palette.onAccent,
            background = DarkSurface.background,
            onBackground = DarkSurface.textPrimary,
            surface = DarkSurface.card,
            onSurface = DarkSurface.textPrimary,
            outline = DarkSurface.outline,
        ),
        content = content,
    )
}
```

`App.kt` 的 `Surface` 外层包一层 `WakeUpMyWallTheme { ... }`。

- [x] **Step 4: 运行测试并提交**

```bash
./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest
git add -A
git commit -m "feat: add dark theme tokens and six accent palettes"
```

---

### Task 9: CI

**Files:**
- Create: `.github/workflows/ci.yml`

**Interfaces:**
- Consumes: 全部前述任务
- Produces: push / PR 自动执行单测、Compose UI 测试与 assemble

- [x] **Step 1: 写工作流**

```yaml
name: CI

on:
  push:
    branches: [master]
  pull_request:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '25'
      - uses: gradle/actions/setup-gradle@v4
      - name: Unit tests
        run: ./gradlew :composeApp:testDebugUnitTest --no-daemon
      - name: Compose UI tests
        run: ./gradlew :composeApp:desktopTest --no-daemon
      - name: Assemble debug
        run: ./gradlew :composeApp:assembleDebug --no-daemon
```

- [x] **Step 2: 本地复现同样三条命令**

```bash
./gradlew :composeApp:testDebugUnitTest --no-daemon
./gradlew :composeApp:desktopTest --no-daemon
./gradlew :composeApp:assembleDebug --no-daemon
```

Expected: 三条均 `BUILD SUCCESSFUL`。GitHub Actions 平台本身无法在本容器验证，需推送后确认。

- [x] **Step 3: Commit**

```bash
git add .github/workflows/ci.yml
git commit -m "ci: run unit tests, compose ui tests and debug assemble"
```

---

### Task 10: 清理模板残留与更新文档

**Files:**
- Delete: `mobile/composeApp/src/androidMain/keepRules/`（若仍存在）
- Delete: 模板示例测试文件残留
- Modify: `README.md`

**Interfaces:**
- Consumes: 全部前述任务
- Produces: 仓库内无 `com.example` 与 Hello World 残留；README 描述新的 monorepo 结构

- [x] **Step 1: 确认并删除残留**

```bash
cd /home/xukunz/桌面/WakeUpMyWall
grep -rn "example\|Greeting\|Hello Android" --include='*.kt' --include='*.kts' mobile/ || echo "no template leftovers"
find mobile/composeApp/src -name "Example*Test.kt" -print
git rm -r mobile/composeApp/src/androidMain/keepRules 2>/dev/null || true
```

Expected: 第一条命令输出 `no template leftovers`。

- [x] **Step 2: 更新 README 的技术栈 / 结构 / 测试三节**

```markdown
## 技术栈

- Kotlin 2.2.10 / Kotlin Multiplatform
- Compose Multiplatform 1.10.3
- Android Gradle Plugin 9.3.3 / Gradle 9.5.0
- Ktor 3.6.0、kotlinx-serialization、kotlinx-coroutines
- Android：minSdk 30 / targetSdk 37

## 项目结构

mobile/composeApp/    # 移动端（KMP；commonMain 不含 Android 依赖）
agent/                # PC Agent（Phase 4 引入）
docs/                 # 产品设计、规范与实施计划

## 测试

./gradlew :composeApp:testDebugUnitTest   # 单元测试（含 commonTest）
./gradlew :composeApp:desktopTest         # Compose UI 测试（PC 上运行，无需模拟器）
./gradlew :composeApp:assembleDebug       # 组装 Android Debug 包
```

- [x] **Step 3: 全量验证并提交**

```bash
./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest :composeApp:assembleDebug
git add -A
git commit -m "docs: refresh readme and remove template leftovers"
```

---

## Phase 0 完成标准

1. `./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest :composeApp:assembleDebug` 全绿。
2. `mobile/composeApp` 之下再无 `com.example`、再无模板 Hello World 代码。
3. Android 真机/模拟器启动后显示 Dashboard 占位页，工作空间可切换。
4. `docs/plans/version-matrix.md` 记录了实测通过的版本矩阵。

> 执行记录（2026-09-19）：上述四条标准均已满足，证据见 `docs/plans/version-matrix.md` §4（冷构建下 31 个单测 + 38 个桌面测试全绿、APK 产出、无模板残留）。
> 第 3 条在实现时追加了内置壁纸背景层（见下方偏差表），占位页浮在壁纸上。

## 已知偏差（需用户知情）

| 偏差 | 原因 |
| --- | --- |
| 不声明 iOS target（简报列为 CORE-004 P0） | Apple target 只能在 macOS 编译；简报 §49 已将 iOS 定为 V2；CMP 1.11+ 的原生目标要求 Kotlin 2.3+，与当前 Kotlin 2.2.10 冲突 |
| 引入 `jvm("desktop")` target | 让 Compose UI 测试与设计预览无需 Android 模拟器即可运行，是本阶段唯一的自动化 UI 验证手段 |
| 暂不引入 Navigation 库与 Room/DataStore | 三者都会带来版本兼容风险；先用可测的自有实现与内存存储把架构跑通，Phase 2 再按真实需求选型 |
| 追加「内置壁纸」实现（计划外的 Task 8.5） | 用户在 Task 9 之前提供了两张 4K 壁纸母版，要求"用作默认和测试"；默认背景因此固定为 Aurora，并新增资源级 Compose UI 测试。母版与派生规则见 `imgs/wallpaper/README.md` |
