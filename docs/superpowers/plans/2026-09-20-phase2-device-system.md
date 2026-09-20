# Phase 2：设备系统实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让设备（`PcDevice`）从 Mock 常量变成真实持久化的数据：可增删改、有唯一默认设备、重启后原样恢复，并且 `Test Connection` 在 PC 或 Agent 不在线时给出**具体失败原因**而不是静默失败。

**Architecture:** 存储边界是 Phase 0 就留下的 [`SettingsStorage`](../../../mobile/composeApp/src/commonMain/kotlin/com/xukunz/wakeupmywall/core/storage/SettingsStorage.kt)，本阶段把它落到 DataStore 上：`KeyValueStore`（平台键值读写）→ `JsonSettingsStorage`（序列化 + 坏数据兜底，纯 commonMain）→ `DeviceRepository`（列表状态 + 默认设备不变量 + 增删改）。UI 只与 `DeviceRepository` 和 `ConnectivityTester` 打交道，`commonMain` 依旧不 import 任何 `android.*`；平台实现（DataStore、TCP 探测）由 `MainActivity` 在启动时注入，与 Phase 0/1 的"默认参数 + 测试注入"风格一致。

**Tech Stack:** Kotlin 2.2.10 / Compose Multiplatform 1.10.3 / kotlinx-serialization 1.11.0 / kotlinx-coroutines 1.11.0 / **DataStore Preferences 1.2.1**（新增，仅 androidMain）/ Ktor 3.6.0（既有 `AgentApi`，HTTP 部分不新增依赖）

**Spec:** [docs/superpowers/specs/2026-09-19-desktop-companion-design.md](../specs/2026-09-19-desktop-companion-design.md) §6 数据模型、§7.5 Settings Workspace、§5 网络协议
**路标：** [2026-09-19-roadmap.md](2026-09-19-roadmap.md) Phase 2 段（本节即该段的落地计划）
**前序计划：** [2026-09-19-phase0-kmp-foundation.md](2026-09-19-phase0-kmp-foundation.md)、[2026-09-19-phase1-design-system-and-mock-ui.md](2026-09-19-phase1-design-system-and-mock-ui.md)

---

## 0. 计划期定案（选型与理由）

| 决策 | 选定 | 理由与备选 |
| --- | --- | --- |
| 设备持久化 | **DataStore Preferences 1.2.1 + kotlinx-serialization JSON** | Phase 2 的数据是"几台设备的列表"，没有查询/关系/迁移需求。Room 要引 KSP、schema 导出与迁移策略，收益要等 Phase 5 的指标时序存储才兑现。JSON 存在 Preferences 里的代价可忽略（列表 < 1 KB），而且序列化 schema 放在 commonMain 就能被两端共用。**Phase 5 若引入指标时序库，设备表仍可留在 DataStore。** |
| 坏数据处理 | 读不出来就退回默认值，**不抛异常、不清库** | 版本升级时 schema 可能不兼容；宁可先让 App 起来（用户看到的是默认状态），也不能白屏。写入端只在用户显式操作时发生，不会用默认值覆盖用户数据。 |
| TCP 探测 | 放在 **androidMain**（`java.net.Socket`），不在 commonMain 猜异常类型 | KMP common 代码里拿不到 `java.net.ConnectException` / `SocketTimeoutException` 这些类型；用 error message 字符串去判"连接被拒"是瞎猜。类型化的映射写在 androidMain，映射函数用合成异常单测，另外用 localhost 真连一次做端到端验证。 |
| 首次运行播种 | 第一次启动时把 `MockData.devices` 写进存储，之后以存储为准 | Phase 1 的 UI（Rail / Dashboard / Monitor）假设"一定有一台激活设备"，Phase 2 若引入空列表就要重写整屏空状态，超出本阶段范围。播种**只发生一次**（存储里没有 key 时），用户改过的数据不会被覆盖；Phase 3 接 WOL 时移除播种，改为空列表 + 引导添加。这是刻意留的过渡，不是产品行为。 |
| 网络权限 | 本阶段就加 `android.permission.INTERNET` | `Test Connection` 要真发请求。Phase 4/5 也必需，现在加一次即可。 |

## Global Constraints

- **多 PC 是一等公民**：不许出现 `pcMacAddress` 这类单设备字段（spec §6）；任何"当前设备"都从 `DeviceRepository.active` 取。
- **Token 不进模型**：`PcDevice` 里没有 token 字段，本阶段也不引入（Phase 4 配对时单独进 Keystore）。
- **commonMain 禁止 import `android.*`**；平台能力通过接口 + 构造参数注入。
- **颜色、间距、圆角、字号只能取自 `core/theme`**；Composable 内禁止字面量 `Color(0x...)` 与裸 `dp`（测试与令牌文件除外）。
- **每个 UI 元素必须有 `testTag`**，命名 `区域:元素`（本阶段新增：`device:connection`、`device:add`、`device:select:<id>`、`device:delete:<id>`、`device:wolPort-increment` 等）。
- **每笔提交前** `./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest` 必须全绿；涉及 androidMain 改动时再加 `:composeApp:assembleDebug`。
- **不引入新图标库**；删除/增加等图形继续用 `ui/icons/AppIcons.kt` 的代码绘制或既有字形。
- **失败必须可解释**：任何"测试连接"结果都要能落到一句具体原因上，禁止只显示 `Failed`。

---

## 1. 文件结构（先定边界，再拆任务）

| 文件 | 职责 |
| --- | --- |
| `core/storage/KeyValueStore.kt`（新） | 平台键值读写的最小边界（`read/write` 两个 suspend 方法）+ `InMemoryKeyValueStore` |
| `core/storage/JsonSettingsStorage.kt`（新） | `SettingsStorage` 的 JSON 实现；坏数据/缺 key 退回默认值 |
| `core/storage/SettingsStorage.kt`（改） | 保持接口不变，注释指向新实现（Phase 0 的"真实后端留到 Phase 2"到此兑现） |
| `data/settings/DataStoreKeyValueStore.kt`（新，androidMain） | DataStore Preferences 适配 `KeyValueStore` |
| `data/device/DeviceRepository.kt`（新） | 设备列表状态、增删改、默认设备不变量、首次播种 |
| `core/connectivity/TcpProbe.kt`（新） | `TcpProbe` 接口 + `TcpProbeResult` 结果类型 + `UnsupportedTcpProbe` |
| `core/connectivity/ConnectivityTester.kt`（新） | 把 TCP 结果与 `AgentApi.status()` 合成一句人话结论 |
| `core/connectivity/AndroidTcpProbe.kt`（新，androidMain） | `java.net.Socket` 实现 + 异常→结果映射 |
| `ui/settings/DeviceSetupScreen.kt`（改） | 表单接真实设备、Port 步进器、`Test Connection` 结果块、Saved Computers 变成选择器 |
| `ui/settings/ConnectionStatus.kt`（新） | 连接测试结果块（`device:connection`），只负责渲染 |
| `app/App.kt`（改） | 建 `DeviceRepository`、`load()`、把 active device 传给 Rail/Dashboard/Settings |
| `androidMain/.../MainActivity.kt`（改） | 组装 `JsonSettingsStorage(DataStoreKeyValueStore(context))` 与 `AndroidTcpProbe` 并注入 |
| `gradle/libs.versions.toml`、`mobile/composeApp/build.gradle.kts`（改） | DataStore 依赖与版本 |
| `androidMain/AndroidManifest.xml`（改） | `INTERNET` 权限 |

测试文件：

| 文件 | 覆盖 |
| --- | --- |
| `commonTest/.../core/storage/JsonSettingsStorageTest.kt`（新） | JSON 往返、缺 key、坏 JSON、未知字段、appearance 往返 |
| `commonTest/.../data/device/DeviceRepositoryTest.kt`（新） | 播种只做一次、增删改持久化、默认设备不变量、id 冲突 |
| `commonTest/.../core/connectivity/ConnectivityTesterTest.kt`（新） | 六种失败原因 + 成功路径（Ktor MockEngine + 假 probe） |
| `androidUnitTest/.../core/connectivity/AndroidTcpProbeTest.kt`（新） | 真连 localhost（Reachable / Refused）+ 合成异常的映射 |
| `desktopTest/.../ui/settings/DeviceSetupScreenTest.kt`（改） | 步进器、连接结果块、选择/删除/新增的回调 |
| `desktopTest/.../app/AppUiTest.kt`（改） | Rail 显示激活设备的名称（来自仓库，不是 Mock 常量） |

---

### Task 1: 存储骨架（KeyValueStore + JsonSettingsStorage）

**Files:**
- Modify: `mobile/composeApp/src/commonMain/kotlin/com/xukunz/wakeupmywall/core/storage/SettingsStorage.kt`
- Modify: `mobile/composeApp/src/commonMain/kotlin/com/xukunz/wakeupmywall/data/settings/InMemorySettingsStorage.kt`
- Create: `mobile/composeApp/src/commonMain/kotlin/com/xukunz/wakeupmywall/core/storage/KeyValueStore.kt`
- Create: `mobile/composeApp/src/commonMain/kotlin/com/xukunz/wakeupmywall/core/storage/JsonSettingsStorage.kt`
- Test: `mobile/composeApp/src/commonTest/kotlin/com/xukunz/wakeupmywall/core/storage/JsonSettingsStorageTest.kt`

**Interfaces:**
- Consumes: `SettingsStorage`、`AppearanceSettings`、`PcDevice`（均已存在）
- Produces:
  - `interface KeyValueStore { suspend fun read(key: String): String?; suspend fun write(key: String, value: String) }`
  - `class InMemoryKeyValueStore(initial: Map<String, String> = emptyMap()) : KeyValueStore`
  - `class JsonSettingsStorage(store: KeyValueStore) : SettingsStorage`
  - `SettingsStorage` 新增两个方法（**只播种一次的标记**，见 Task 3 的 `load()`）：
    `suspend fun isSeeded(): Boolean`、`suspend fun markSeeded()`

- [x] **Step 1: 写失败测试**

```kotlin
package com.xukunz.wakeupmywall.core.storage

import com.xukunz.wakeupmywall.domain.model.MacAddress
import com.xukunz.wakeupmywall.domain.model.PcDevice
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonSettingsStorageTest {

    private val desktop = PcDevice(
        id = "my-pc",
        name = "My PC",
        macAddress = MacAddress.parse("00:1A:2B:3C:4D:5E"),
        broadcastAddress = "192.168.1.255",
        wolPort = 9,
        isDefault = true,
    )

    @Test
    fun `devices survive a round trip`() = runTest {
        val storage = JsonSettingsStorage(InMemoryKeyValueStore())
        storage.writeDevices(listOf(desktop, desktop.copy(id = "study", name = "Study PC", isDefault = false)))

        val loaded = storage.readDevices()

        assertEquals(listOf("my-pc", "study"), loaded.map { it.id })
        assertEquals(MacAddress.parse("00:1A:2B:3C:4D:5E"), loaded.first().macAddress)
        assertEquals(true, loaded.first().isDefault)
    }

    @Test
    fun `missing keys fall back to defaults`() = runTest {
        val storage = JsonSettingsStorage(InMemoryKeyValueStore())

        assertEquals(emptyList(), storage.readDevices())
        assertEquals(AppearanceSettings(), storage.readAppearance())
    }

    @Test
    fun `corrupt json does not crash the app`() = runTest {
        val store = InMemoryKeyValueStore(mapOf("devices" to "{ this is not json"))
        val storage = JsonSettingsStorage(store)

        assertEquals(emptyList(), storage.readDevices())
    }

    @Test
    fun `unknown fields from a newer version are ignored`() = runTest {
        val raw = """[{"id":"my-pc","name":"My PC","broadcastAddress":"192.168.1.255","wolPort":9,"agentPort":9876,"isDefault":true,"sceneId":"den"}]"""
        val storage = JsonSettingsStorage(InMemoryKeyValueStore(mapOf("devices" to raw)))

        val loaded = storage.readDevices()

        assertEquals(1, loaded.size)
        assertEquals("My PC", loaded.single().name)
    }

    @Test
    fun `appearance round trip keeps accent, style and wallpaper`() = runTest {
        val storage = JsonSettingsStorage(InMemoryKeyValueStore())
        val value = AppearanceSettings(accent = "AuroraBlue", widgetStyle = "Solid", wallpaperId = "minimal")

        storage.writeAppearance(value)

        assertEquals(value, storage.readAppearance())
        assertTrue(storage.readDevices().isEmpty())
    }

    @Test
    fun `the seeded flag starts false and sticks once written`() = runTest {
        val store = InMemoryKeyValueStore()
        val storage = JsonSettingsStorage(store)

        assertEquals(false, storage.isSeeded())

        storage.markSeeded()
        assertEquals(true, storage.isSeeded())
        // 冷启动等价于"用同一个 store 重新建一个实例"：
        assertEquals(true, JsonSettingsStorage(store).isSeeded())
    }
}
```

- [x] **Step 2: 运行测试确认失败**

Run: `JAVA_HOME=<jdk25> ./gradlew :composeApp:desktopTest --tests "*JsonSettingsStorageTest*"`
Expected: FAIL —— `Unresolved reference 'JsonSettingsStorage'` / `KeyValueStore`

- [x] **Step 3: 实现**

```kotlin
// core/storage/SettingsStorage.kt —— 在既有接口上补两个"只播种一次"的方法
interface SettingsStorage {
    suspend fun readDevices(): List<PcDevice>
    suspend fun writeDevices(devices: List<PcDevice>)
    suspend fun readAppearance(): AppearanceSettings
    suspend fun writeAppearance(value: AppearanceSettings)

    /** 设备列表是否已经写过一次。用于"首次运行播种"，避免用户清空列表后又被种回来。 */
    suspend fun isSeeded(): Boolean
    suspend fun markSeeded()
}
```

```kotlin
// data/settings/InMemorySettingsStorage.kt —— 补上新增的两个方法
private var seeded = false

override suspend fun isSeeded(): Boolean = seeded
override suspend fun markSeeded() { seeded = true }
```

```kotlin
// core/storage/KeyValueStore.kt
package com.xukunz.wakeupmywall.core.storage

/**
 * 平台键值存储的最小边界：只有字符串读写，序列化与领域规则都在 commonMain。
 * Android 侧由 DataStore Preferences 适配，测试与预览用内存实现。
 */
interface KeyValueStore {
    suspend fun read(key: String): String?
    suspend fun write(key: String, value: String)
}

class InMemoryKeyValueStore(initial: Map<String, String> = emptyMap()) : KeyValueStore {
    private val values = initial.toMutableMap()

    override suspend fun read(key: String): String? = values[key]

    override suspend fun write(key: String, value: String) {
        values[key] = value
    }
}
```

```kotlin
// core/storage/JsonSettingsStorage.kt
package com.xukunz.wakeupmywall.core.storage

import com.xukunz.wakeupmywall.domain.model.PcDevice
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * `SettingsStorage` 的 JSON 实现。读取路径一律"失败即默认值"：
 * 缺 key、JSON 损坏、字段被新版本改过，都不能让 App 起不来。
 */
class JsonSettingsStorage(private val store: KeyValueStore) : SettingsStorage {

    override suspend fun readDevices(): List<PcDevice> = decode(DevicesKey, emptyList())

    override suspend fun writeDevices(devices: List<PcDevice>) {
        store.write(DevicesKey, json.encodeToString(devices))
    }

    override suspend fun readAppearance(): AppearanceSettings = decode(AppearanceKey, AppearanceSettings())

    override suspend fun writeAppearance(value: AppearanceSettings) {
        store.write(AppearanceKey, json.encodeToString(value))
    }

    override suspend fun isSeeded(): Boolean = store.read(SeededKey) == "true"

    override suspend fun markSeeded() {
        store.write(SeededKey, "true")
    }

    private suspend inline fun <reified T> decode(key: String, fallback: T): T {
        val raw = store.read(key) ?: return fallback
        return try {
            json.decodeFromString<T>(raw)
        } catch (e: SerializationException) {
            fallback
        } catch (e: IllegalArgumentException) {
            fallback
        }
    }

    private companion object {
        const val DevicesKey = "devices"
        const val AppearanceKey = "appearance"
        const val SeededKey = "seeded"

        // ignoreUnknownKeys：旧版本读到新版本写的字段时要能降级而不是清空。
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }
}
```

- [x] **Step 4: 运行测试确认通过**

Run: `JAVA_HOME=<jdk25> ./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest`
Expected: BUILD SUCCESSFUL，`JsonSettingsStorageTest` 5 条全绿

- [x] **Step 5: 提交**

```bash
git add mobile/composeApp/src/commonMain/kotlin/com/xukunz/wakeupmywall/core/storage mobile/composeApp/src/commonTest/kotlin/com/xukunz/wakeupmywall/core/storage
git commit -m "feat: add a json settings storage behind the platform key value seam"
```

---

### Task 2: DataStore 落地（androidMain + 注入 + 权限）

**Files:**
- Create: `mobile/composeApp/src/androidMain/kotlin/com/xukunz/wakeupmywall/data/settings/DataStoreKeyValueStore.kt`
- Modify: `gradle/libs.versions.toml`（`datastorePreferences = "1.2.1"` + library 条目）
- Modify: `mobile/composeApp/build.gradle.kts`（`androidMain.dependencies` 加 `libs.androidx.datastore.preferences`）
- Modify: `mobile/composeApp/src/androidMain/AndroidManifest.xml`（`INTERNET` 权限）
- Modify: `mobile/composeApp/src/androidMain/kotlin/com/xukunz/wakeupmywall/MainActivity.kt`
- Modify: `mobile/composeApp/src/commonMain/kotlin/com/xukunz/wakeupmywall/app/App.kt`（新增 `storage` 参数）

**Interfaces:**
- Consumes: Task 1 的 `KeyValueStore` / `JsonSettingsStorage`
- Produces: `class DataStoreKeyValueStore(context: Context) : KeyValueStore`；`App(..., storage: SettingsStorage = remember { InMemorySettingsStorage() })`

- [x] **Step 1: 加依赖与权限**

```toml
# gradle/libs.versions.toml
[versions]
datastorePreferences = "1.2.1"

[libraries]
androidx-datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastorePreferences" }
```

```kotlin
// mobile/composeApp/build.gradle.kts -> androidMain.dependencies 内追加
implementation(libs.androidx.datastore.preferences)
```

```xml
<!-- AndroidManifest.xml：Test Connection 要真的发请求 -->
<uses-permission android:name="android.permission.INTERNET" />
```

- [x] **Step 2: 写平台适配**

```kotlin
// androidMain/kotlin/com/xukunz/wakeupmywall/data/settings/DataStoreKeyValueStore.kt
package com.xukunz.wakeupmywall.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.xukunz.wakeupmywall.core.storage.KeyValueStore
import kotlinx.coroutines.flow.first

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** DataStore Preferences 只是"键 → 字符串"的容器；设备与外观怎么序列化由 commonMain 决定。 */
class DataStoreKeyValueStore(private val context: Context) : KeyValueStore {

    override suspend fun read(key: String): String? =
        context.settingsDataStore.data.first()[stringPreferencesKey(key)]

    override suspend fun write(key: String, value: String) {
        context.settingsDataStore.edit { it[stringPreferencesKey(key)] = value }
    }
}
```

```kotlin
// MainActivity.kt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 平台能力在这里注入：commonMain 只认 SettingsStorage 接口。
        val storage = JsonSettingsStorage(DataStoreKeyValueStore(applicationContext))
        setContent { App(storage = storage) }
    }
}
```

```kotlin
// App.kt 签名（默认值保持"无平台依赖"，桌面测试与预览照旧）
@Composable
fun App(
    navigator: AppNavigator = remember { AppNavigator() },
    wallpaperId: String = BuiltInWallpapers.DefaultId,
    initialPcState: PcState = PcState.ONLINE,
    storage: SettingsStorage = remember { InMemorySettingsStorage() },
)
```

- [x] **Step 3: 验证编译与既有测试**

Run: `JAVA_HOME=<jdk25> ./gradlew :composeApp:assembleDebug :composeApp:testDebugUnitTest :composeApp:desktopTest`
Expected: BUILD SUCCESSFUL（`assembleDebug` 证明 DataStore 依赖在 androidMain 可用）

- [x] **Step 4: 真机侧确认 store 文件会被创建**

```bash
JAVA_HOME=<jdk25> ./gradlew :composeApp:installDebug
adb -s emulator-5554 shell am force-stop com.xukunz.wakeupmywall
adb -s emulator-5554 shell am start -n com.xukunz.wakeupmywall/.MainActivity && sleep 6
adb -s emulator-5554 shell run-as com.xukunz.wakeupmywall ls -l files/datastore
```
Expected: 出现 `settings.preferences_pb`（写入发生在 Task 3 接上仓库后；此步只要目录/文件存在或为空目录不报错即可，真正断言在 Task 8）

- [x] **Step 5: 提交**

```bash
git add gradle/libs.versions.toml mobile/composeApp/build.gradle.kts mobile/composeApp/src/androidMain
git commit -m "feat: persist settings through datastore on android"
```

---

### Task 3: DeviceRepository（增删改 + 默认设备不变量 + 首次播种）

**Files:**
- Create: `mobile/composeApp/src/commonMain/kotlin/com/xukunz/wakeupmywall/data/device/DeviceRepository.kt`
- Test: `mobile/composeApp/src/commonTest/kotlin/com/xukunz/wakeupmywall/data/device/DeviceRepositoryTest.kt`

**Interfaces:**
- Consumes: `SettingsStorage`（含 Task 1 新增的 `isSeeded/markSeeded`）、`PcDevice`
- Produces:
  - `class DeviceRepository(storage: SettingsStorage, seed: List<PcDevice> = emptyList())`
  - `val devices: StateFlow<List<PcDevice>>`、`val active: PcDevice?`
  - `suspend fun load()`、`suspend fun add(device: PcDevice)`、`suspend fun update(id: String, device: PcDevice)`、`suspend fun delete(id: String)`、`suspend fun setDefault(id: String)`

- [x] **Step 1: 写失败测试**

```kotlin
package com.xukunz.wakeupmywall.data.device

import com.xukunz.wakeupmywall.core.storage.InMemoryKeyValueStore
import com.xukunz.wakeupmywall.core.storage.JsonSettingsStorage
import com.xukunz.wakeupmywall.domain.model.PcDevice
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeviceRepositoryTest {

    private fun device(id: String, name: String = id, isDefault: Boolean = false) =
        PcDevice(id = id, name = name, isDefault = isDefault)

    private fun repo(storage: JsonSettingsStorage, seed: List<PcDevice> = emptyList()) =
        DeviceRepository(storage, seed)

    @Test
    fun `seeding happens once and never overwrites user data`() = runTest {
        val storage = JsonSettingsStorage(InMemoryKeyValueStore())
        val seed = listOf(device("my-pc", "My PC", isDefault = true))

        val first = repo(storage, seed).apply { load() }
        assertEquals(listOf("my-pc"), first.devices.value.map { it.id })

        // 用户删掉种子设备后再冷启动：不能又被种回来。
        first.delete("my-pc")
        val second = repo(storage, seed).apply { load() }
        assertEquals(emptyList(), second.devices.value)
        assertNull(second.active)
    }

    @Test
    fun `add persists and the first device becomes default`() = runTest {
        val storage = JsonSettingsStorage(InMemoryKeyValueStore())
        val repository = repo(storage).apply { load() }

        repository.add(device("study", "Study PC"))

        assertEquals(true, repository.active?.isDefault)
        assertEquals("study", DeviceRepository(storage).apply { load() }.active?.id)
    }

    @Test
    fun `set default moves the badge and keeps exactly one default`() = runTest {
        val storage = JsonSettingsStorage(InMemoryKeyValueStore())
        val repository = repo(storage).apply { load() }
        repository.add(device("a", "A", isDefault = true))
        repository.add(device("b", "B"))

        repository.setDefault("b")

        assertEquals(listOf(false, true), repository.devices.value.map { it.isDefault })
        assertEquals("b", repository.active?.id)
    }

    @Test
    fun `deleting the default promotes the first remaining device`() = runTest {
        val storage = JsonSettingsStorage(InMemoryKeyValueStore())
        val repository = repo(storage).apply { load() }
        repository.add(device("a", "A", isDefault = true))
        repository.add(device("b", "B"))

        repository.delete("a")

        assertEquals(listOf("b"), repository.devices.value.map { it.id })
        assertEquals(true, repository.devices.value.single().isDefault)
    }

    @Test
    fun `update writes the edited fields back`() = runTest {
        val storage = JsonSettingsStorage(InMemoryKeyValueStore())
        val repository = repo(storage).apply { load() }
        repository.add(device("a", "A", isDefault = true))

        repository.update("a", repository.devices.value.single().copy(name = "Desk PC", wolPort = 7))

        val reloaded = DeviceRepository(storage).apply { load() }.devices.value.single()
        assertEquals("Desk PC", reloaded.name)
        assertEquals(7, reloaded.wolPort)
    }

    @Test
    fun `an id collision gets a suffix instead of overwriting`() = runTest {
        val storage = JsonSettingsStorage(InMemoryKeyValueStore())
        val repository = repo(storage).apply { load() }
        repository.add(device("home", "Home"))

        repository.add(device("home", "Home again"))

        assertEquals(listOf("home", "home-2"), repository.devices.value.map { it.id })
    }
}
```

- [x] **Step 2: 运行测试确认失败**

Run: `JAVA_HOME=<jdk25> ./gradlew :composeApp:desktopTest --tests "*DeviceRepositoryTest*"`
Expected: FAIL —— `Unresolved reference 'DeviceRepository'`

- [x] **Step 3: 实现**

```kotlin
// data/device/DeviceRepository.kt
package com.xukunz.wakeupmywall.data.device

import com.xukunz.wakeupmywall.core.storage.SettingsStorage
import com.xukunz.wakeupmywall.domain.model.PcDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 设备列表的唯一状态源。不变量：列表非空时**恰好一台** `isDefault = true`，
 * `active` 就是它；列表为空时 `active = null`（Phase 3 会为这个分支补空状态 UI）。
 *
 * [seed] 只在存储里从来没有写过设备时使用一次（Phase 2 的过渡手段）。
 */
class DeviceRepository(
    private val storage: SettingsStorage,
    private val seed: List<PcDevice> = emptyList(),
) {
    private val state = MutableStateFlow<List<PcDevice>>(emptyList())
    val devices: StateFlow<List<PcDevice>> = state.asStateFlow()

    val active: PcDevice?
        get() = state.value.firstOrNull { it.isDefault } ?: state.value.firstOrNull()

    suspend fun load() {
        val stored = storage.readDevices()
        if (!storage.isSeeded()) {
            // 首次运行：写入种子并打标记。之后即使用户把设备全删光，也不会被重新种回来。
            state.value = normalise(stored.ifEmpty { seed })
            storage.writeDevices(state.value)
            storage.markSeeded()
            return
        }
        state.value = normalise(stored)
    }

    suspend fun add(device: PcDevice) {
        state.value = normalise(state.value + device.copy(id = uniqueId(device.id, state.value)))
        persist()
    }

    suspend fun update(id: String, device: PcDevice) {
        state.value = normalise(state.value.map { if (it.id == id) device.copy(id = id) else it })
        persist()
    }

    suspend fun delete(id: String) {
        state.value = normalise(state.value.filterNot { it.id == id })
        persist()
    }

    suspend fun setDefault(id: String) {
        state.value = normalise(state.value.map { it.copy(isDefault = it.id == id) })
        persist()
    }

    private suspend fun persist() {
        storage.writeDevices(state.value)
    }

    /** 列表非空时保证恰好一台默认设备（默认取第一台）。 */
    private fun normalise(devices: List<PcDevice>): List<PcDevice> {
        if (devices.isEmpty()) return devices
        val defaultIndex = devices.indexOfFirst { it.isDefault }.takeIf { it >= 0 } ?: 0
        return devices.mapIndexed { index, device -> device.copy(isDefault = index == defaultIndex) }
    }

    private fun uniqueId(candidate: String, existing: List<PcDevice>): String {
        if (existing.none { it.id == candidate }) return candidate
        var suffix = 2
        while (existing.any { it.id == "$candidate-$suffix" }) suffix++
        return "$candidate-$suffix"
    }
}
```

- [x] **Step 4: 运行测试确认通过**

Run: `JAVA_HOME=<jdk25> ./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest`
Expected: BUILD SUCCESSFUL，`DeviceRepositoryTest` 6 条全绿

- [x] **Step 5: 提交**

```bash
git add mobile/composeApp/src/commonMain/kotlin/com/xukunz/wakeupmywall/data/device mobile/composeApp/src/commonTest/kotlin/com/xukunz/wakeupmywall/data/device
git commit -m "feat: add a device repository with persistence and default device rules"
```

---

### Task 4: 连通性测试（TCP 探测 + Agent 状态）

**Files:**
- Create: `mobile/composeApp/src/commonMain/kotlin/com/xukunz/wakeupmywall/core/connectivity/TcpProbe.kt`
- Create: `mobile/composeApp/src/commonMain/kotlin/com/xukunz/wakeupmywall/core/connectivity/ConnectivityTester.kt`
- Create: `mobile/composeApp/src/androidMain/kotlin/com/xukunz/wakeupmywall/core/connectivity/AndroidTcpProbe.kt`
- Test: `mobile/composeApp/src/commonTest/kotlin/com/xukunz/wakeupmywall/core/connectivity/ConnectivityTesterTest.kt`
- Test: `mobile/composeApp/src/androidUnitTest/kotlin/com/xukunz/wakeupmywall/core/connectivity/AndroidTcpProbeTest.kt`

**Interfaces:**
- Consumes: `PcDevice`、`AgentApi`（既有）、`ApiResult` / `ApiFailure` / `createAgentHttpClient`
- Produces:
  - `sealed interface TcpProbeResult { Reachable / Refused / TimedOut / Unresolved(host) / Failed(reason) }`
  - `interface TcpProbe { suspend fun probe(host: String, port: Int, timeoutMillis: Long = 2_000): TcpProbeResult }`
  - `class ConnectivityTester(probe: TcpProbe, api: AgentApi, token: String? = null) { suspend fun test(device: PcDevice): ConnectionReport }`
  - `sealed interface ConnectionReport { WolOnly(mac) / AgentOnline(hostname, version) / Failed(reason, message) }`
  - `enum class ConnectionFailure { MISSING_MAC, PORT_REFUSED, PORT_TIMEOUT, HOST_UNRESOLVED, PROBE_UNAVAILABLE, NOT_AN_AGENT, UNAUTHORIZED, AGENT_ERROR, NETWORK }`

- [ ] **Step 1: 写失败测试**

```kotlin
package com.xukunz.wakeupmywall.core.connectivity

import com.xukunz.wakeupmywall.core.network.AgentApi
import com.xukunz.wakeupmywall.core.network.createAgentHttpClient
import com.xukunz.wakeupmywall.domain.model.MacAddress
import com.xukunz.wakeupmywall.domain.model.PcDevice
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeProbe(private val result: TcpProbeResult) : TcpProbe {
    override suspend fun probe(host: String, port: Int, timeoutMillis: Long) = result
}

class ConnectivityTesterTest {

    private val device = PcDevice(
        id = "my-pc",
        name = "My PC",
        macAddress = MacAddress.parse("00:1A:2B:3C:4D:5E"),
        ipAddress = "192.168.1.10",
        agentHost = "192.168.1.10",
    )

    private fun tester(probe: TcpProbeResult, status: HttpStatusCode = HttpStatusCode.OK) = ConnectivityTester(
        probe = FakeProbe(probe),
        api = AgentApi(
            createAgentHttpClient(
                MockEngine {
                    respond(
                        content = """{"hostname":"Desktop-Alpha","agentVersion":"0.1.0","uptimeSeconds":42}""",
                        status = status,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                },
            ),
        ),
    )

    @Test
    fun `without a mac there is nothing to wake`() = runTest {
        val report = tester(TcpProbeResult.Reachable).test(device.copy(macAddress = null))

        assertEquals(ConnectionFailure.MISSING_MAC, (report as ConnectionReport.Failed).reason)
    }

    @Test
    fun `a mac without a host reports wol only instead of a fake failure`() = runTest {
        val report = tester(TcpProbeResult.Reachable).test(device.copy(ipAddress = null, agentHost = null))

        assertTrue(report is ConnectionReport.WolOnly)
    }

    @Test
    fun `a refused port names the port that did not answer`() = runTest {
        val report = tester(TcpProbeResult.Refused).test(device)

        val failed = report as ConnectionReport.Failed
        assertEquals(ConnectionFailure.PORT_REFUSED, failed.reason)
        assertTrue(failed.message.contains("9876"))
    }

    @Test
    fun `a timeout is reported as a timeout`() = runTest {
        val report = tester(TcpProbeResult.TimedOut).test(device)

        assertEquals(ConnectionFailure.PORT_TIMEOUT, (report as ConnectionReport.Failed).reason)
    }

    @Test
    fun `an unresolvable host is reported as such`() = runTest {
        val report = tester(TcpProbeResult.Unresolved("nope.local")).test(device.copy(agentHost = "nope.local"))

        assertEquals(ConnectionFailure.HOST_UNRESOLVED, (report as ConnectionReport.Failed).reason)
    }

    @Test
    fun `a 404 means the port answers but is not the agent`() = runTest {
        val report = tester(TcpProbeResult.Reachable, HttpStatusCode.NotFound).test(device)

        assertEquals(ConnectionFailure.NOT_AN_AGENT, (report as ConnectionReport.Failed).reason)
    }

    @Test
    fun `a 401 is reported as a token problem, not as offline`() = runTest {
        val report = tester(TcpProbeResult.Reachable, HttpStatusCode.Unauthorized).test(device)

        assertEquals(ConnectionFailure.UNAUTHORIZED, (report as ConnectionReport.Failed).reason)
    }

    @Test
    fun `a healthy agent reports its version`() = runTest {
        val report = tester(TcpProbeResult.Reachable).test(device)

        val online = report as ConnectionReport.AgentOnline
        assertEquals("0.1.0", online.version)
        assertEquals("Desktop-Alpha", online.hostname)
    }
}
```

```kotlin
package com.xukunz.wakeupmywall.core.connectivity

import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AndroidTcpProbeTest {

    @Test
    fun `a listening port is reachable`() = runTest {
        val server = ServerSocket(0)
        try {
            assertEquals(
                TcpProbeResult.Reachable,
                AndroidTcpProbe().probe("127.0.0.1", server.localPort, timeoutMillis = 2_000),
            )
        } finally {
            server.close()
        }
    }

    @Test
    fun `a closed port is refused`() = runTest {
        val closed = ServerSocket(0).also { it.close() }.localPort

        assertEquals(
            TcpProbeResult.Refused,
            AndroidTcpProbe().probe("127.0.0.1", closed, timeoutMillis = 2_000),
        )
    }

    @Test
    fun `exception mapping is type based, not message based`() {
        assertEquals(TcpProbeResult.Refused, classifyTcpError(ConnectException("Connection refused")))
        assertEquals(TcpProbeResult.TimedOut, classifyTcpError(SocketTimeoutException("connect timed out")))
        assertEquals(TcpProbeResult.Refused, classifyTcpError(NoRouteToHostException("No route to host")))
        assertEquals(
            TcpProbeResult.Unresolved("nope.local"),
            classifyTcpError(UnknownHostException("nope.local")),
        )
        assertTrue(classifyTcpError(IOException("boom")) is TcpProbeResult.Failed)
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `JAVA_HOME=<jdk25> ./gradlew :composeApp:testDebugUnitTest --tests "*AndroidTcpProbeTest*" :composeApp:desktopTest --tests "*ConnectivityTesterTest*"`
Expected: FAIL —— `Unresolved reference 'ConnectivityTester'` / `AndroidTcpProbe`

- [ ] **Step 3: 实现**

```kotlin
// core/connectivity/TcpProbe.kt
package com.xukunz.wakeupmywall.core.connectivity

sealed interface TcpProbeResult {
    data object Reachable : TcpProbeResult
    data object Refused : TcpProbeResult
    data object TimedOut : TcpProbeResult
    data class Unresolved(val host: String) : TcpProbeResult
    data class Failed(val reason: String) : TcpProbeResult
}

interface TcpProbe {
    suspend fun probe(host: String, port: Int, timeoutMillis: Long = 2_000): TcpProbeResult
}

/** 桌面预览与纯 UI 测试的默认实现：不猜结果，直接说这条平台路径不可用。 */
object UnsupportedTcpProbe : TcpProbe {
    override suspend fun probe(host: String, port: Int, timeoutMillis: Long) =
        TcpProbeResult.Failed("TCP probe is not available on this platform")
}
```

```kotlin
// core/connectivity/ConnectivityTester.kt
package com.xukunz.wakeupmywall.core.connectivity

import com.xukunz.wakeupmywall.core.network.AgentApi
import com.xukunz.wakeupmywall.core.network.ApiFailure
import com.xukunz.wakeupmywall.core.network.ApiResult
import com.xukunz.wakeupmywall.domain.model.PcDevice

enum class ConnectionFailure {
    MISSING_MAC, PORT_REFUSED, PORT_TIMEOUT, HOST_UNRESOLVED, PROBE_UNAVAILABLE,
    NOT_AN_AGENT, UNAUTHORIZED, AGENT_ERROR, NETWORK,
}

sealed interface ConnectionReport {
    /** 有 MAC、没配 Agent 主机：WOL 可用，但没有可探测的 Agent。 */
    data class WolOnly(val mac: String) : ConnectionReport
    data class AgentOnline(val hostname: String, val version: String) : ConnectionReport
    data class Failed(val reason: ConnectionFailure, val message: String) : ConnectionReport
}

/**
 * 把"TCP 通不通"与"Agent 答不答"合成一句可解释的结论。
 * 顺序有意义：先排除输入缺失，再报网络层具体原因，最后才是 HTTP 语义。
 */
class ConnectivityTester(
    private val probe: TcpProbe,
    private val api: AgentApi,
    private val token: String? = null,
) {
    suspend fun test(device: PcDevice): ConnectionReport {
        val mac = device.macAddress?.value
            ?: return failed(ConnectionFailure.MISSING_MAC, "Add a MAC address first — wake-on-LAN needs it")

        val host = device.agentHost ?: device.ipAddress
            ?: return ConnectionReport.WolOnly(mac)

        return when (val tcp = probe.probe(host, device.agentPort)) {
            TcpProbeResult.Reachable -> checkAgent(host, device.agentPort)
            TcpProbeResult.Refused -> failed(
                ConnectionFailure.PORT_REFUSED,
                "Nothing is listening on $host:${device.agentPort} — the PC may be asleep, or the Agent is not running",
            )
            TcpProbeResult.TimedOut -> failed(
                ConnectionFailure.PORT_TIMEOUT,
                "$host:${device.agentPort} did not answer within 2 s — wrong IP, or the firewall is dropping it",
            )
            is TcpProbeResult.Unresolved -> failed(
                ConnectionFailure.HOST_UNRESOLVED,
                "Cannot resolve \"${tcp.host}\" — check the Agent host name",
            )
            is TcpProbeResult.Failed -> failed(ConnectionFailure.PROBE_UNAVAILABLE, tcp.reason)
        }
    }

    private suspend fun checkAgent(host: String, port: Int): ConnectionReport =
        when (val result = api.status("http://$host:$port", token)) {
            is ApiResult.Success -> ConnectionReport.AgentOnline(result.value.hostname, result.value.agentVersion)
            is ApiResult.Failure -> when (result.reason) {
                ApiFailure.NOT_FOUND -> failed(
                    ConnectionFailure.NOT_AN_AGENT,
                    "$host:$port answers, but /api/v1/status returned 404 — this is not the WakeUpMyWall Agent",
                )
                ApiFailure.UNAUTHORIZED -> failed(
                    ConnectionFailure.UNAUTHORIZED,
                    "The Agent rejected the token — pair the device again after Phase 4 lands",
                )
                ApiFailure.SERVER -> failed(
                    ConnectionFailure.AGENT_ERROR,
                    "The Agent answered with a server error (${result.message})",
                )
                ApiFailure.DECODING -> failed(
                    ConnectionFailure.AGENT_ERROR,
                    "The Agent answered, but the payload was not a status document",
                )
                ApiFailure.TIMEOUT, ApiFailure.NETWORK -> failed(
                    ConnectionFailure.NETWORK,
                    "TCP connected but the status call failed (${result.message})",
                )
            }
        }

    private fun failed(reason: ConnectionFailure, message: String) = ConnectionReport.Failed(reason, message)
}
```

```kotlin
// androidMain/core/connectivity/AndroidTcpProbe.kt
package com.xukunz.wakeupmywall.core.connectivity

import java.io.IOException
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.NoRouteToHostException
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 真实 TCP 探测。异常→结果的映射只看**类型**：用 error message 里的字符串猜"连接被拒"
 * 会在非英文环境或不同 JVM 实现上翻车。
 */
class AndroidTcpProbe : TcpProbe {
    override suspend fun probe(host: String, port: Int, timeoutMillis: Long): TcpProbeResult =
        withContext(Dispatchers.IO) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), timeoutMillis.toInt())
                }
                TcpProbeResult.Reachable
            } catch (e: IOException) {
                classifyTcpError(e)
            }
        }
}

internal fun classifyTcpError(error: IOException): TcpProbeResult = when (error) {
    is UnknownHostException -> TcpProbeResult.Unresolved(error.message ?: "unknown host")
    is SocketTimeoutException -> TcpProbeResult.TimedOut
    is ConnectException, is NoRouteToHostException -> TcpProbeResult.Refused
    else -> TcpProbeResult.Failed(error.message ?: error::class.simpleName ?: "connect failed")
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `JAVA_HOME=<jdk25> ./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest`
Expected: BUILD SUCCESSFUL —— `ConnectivityTesterTest` 8 条 + `AndroidTcpProbeTest` 3 条全绿

- [ ] **Step 5: 提交**

```bash
git add mobile/composeApp/src/commonMain/kotlin/com/xukunz/wakeupmywall/core/connectivity \
        mobile/composeApp/src/androidMain/kotlin/com/xukunz/wakeupmywall/core/connectivity \
        mobile/composeApp/src/commonTest/kotlin/com/xukunz/wakeupmywall/core/connectivity \
        mobile/composeApp/src/androidUnitTest/kotlin/com/xukunz/wakeupmywall/core/connectivity
git commit -m "feat: report explicit connection failures for a saved device"
```

---

### Task 5: Device Setup 表单接真实设备（Port 步进器 + 连接结果块）

**Files:**
- Create: `mobile/composeApp/src/commonMain/kotlin/com/xukunz/wakeupmywall/ui/settings/ConnectionStatus.kt`
- Modify: `mobile/composeApp/src/commonMain/kotlin/com/xukunz/wakeupmywall/ui/settings/DeviceSetupScreen.kt`
- Test: `mobile/composeApp/src/desktopTest/kotlin/com/xukunz/wakeupmywall/ui/settings/DeviceSetupFormTest.kt`

**Interfaces:**
- Consumes: `DeviceSetupInput`、`DeviceSetupResult`、`ConnectionReport`（Task 4）
- Produces: `DeviceSetupScreen(input, result, devices, report: ConnectionReport?, isTesting: Boolean, onInputChange, onSave, onTestConnection, onSelectDevice, onDeleteDevice, onAddDevice, modifier)`；新 testTag `device:connection`、`device:wolPort-increment`、`device:wolPort-decrement`

- [ ] **Step 1: 写失败测试**

```kotlin
package com.xukunz.wakeupmywall.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.xukunz.wakeupmywall.core.connectivity.ConnectionFailure
import com.xukunz.wakeupmywall.core.connectivity.ConnectionReport
import com.xukunz.wakeupmywall.core.theme.WakeUpMyWallTheme
import com.xukunz.wakeupmywall.domain.usecase.DeviceSetupInput
import com.xukunz.wakeupmywall.domain.usecase.DeviceSetupValidator
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class DeviceSetupFormTest {

    private val input = DeviceSetupInput(
        name = "My PC",
        mac = "00:1A:2B:3C:4D:5E",
        ip = "192.168.1.10",
        broadcast = "192.168.1.255",
        wolPort = "9",
        agentPort = "9876",
        agentHost = "192.168.1.10",
    )

    @Test
    fun `the wol port stepper walks up and down`() = runComposeUiTest {
        var current by mutableStateOf(input)
        setContent {
            WakeUpMyWallTheme {
                DeviceSetupScreen(
                    input = current,
                    result = DeviceSetupValidator.validate(current),
                    devices = emptyList(),
                    report = null,
                    isTesting = false,
                    onInputChange = { current = it },
                    onSave = {},
                    onTestConnection = {},
                    onSelectDevice = {},
                    onDeleteDevice = {},
                    onAddDevice = {},
                )
            }
        }

        onNodeWithTag("device:wolPort-increment").performClick()
        assertEquals("10", current.wolPort)

        onNodeWithTag("device:wolPort-decrement").performClick()
        assertEquals("9", current.wolPort)
    }

    @Test
    fun `a refused connection is shown together with its reason`() = runComposeUiTest {
        val message = "Nothing is listening on 192.168.1.10:9876 — the PC may be asleep, or the Agent is not running"
        setContent {
            WakeUpMyWallTheme {
                DeviceSetupScreen(
                    input = input,
                    result = DeviceSetupValidator.validate(input),
                    devices = emptyList(),
                    report = ConnectionReport.Failed(ConnectionFailure.PORT_REFUSED, message),
                    isTesting = false,
                    onInputChange = {},
                    onSave = {},
                    onTestConnection = {},
                    onSelectDevice = {},
                    onDeleteDevice = {},
                    onAddDevice = {},
                )
            }
        }

        onNodeWithTag("device:connection", useUnmergedTree = true).assertTextEquals(message)
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `JAVA_HOME=<jdk25> ./gradlew :composeApp:desktopTest --tests "*DeviceSetupFormTest*"`
Expected: FAIL —— 新参数不存在 / 找不到 `device:wolPort-increment`

- [ ] **Step 3: 实现**

```kotlin
// ui/settings/ConnectionStatus.kt
package com.xukunz.wakeupmywall.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.xukunz.wakeupmywall.core.connectivity.ConnectionReport

/**
 * 连接结论块：失败用错误色，并且**永远带具体原因**（roadmap Phase 2 的验收要求）。
 */
@Composable
fun ConnectionStatus(report: ConnectionReport?, isTesting: Boolean, modifier: Modifier = Modifier) {
    val text = when {
        isTesting -> "Testing…"
        report is ConnectionReport.AgentOnline -> "Agent online · ${report.hostname} · v${report.version}"
        report is ConnectionReport.WolOnly -> "WOL Ready · no Agent configured yet"
        report is ConnectionReport.Failed -> report.message
        else -> "Not tested yet"
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = if (report is ConnectionReport.Failed) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = modifier.testTag("device:connection"),
    )
}
```

`DeviceSetupScreen` 的签名与表单改动：

```kotlin
@Composable
fun DeviceSetupScreen(
    input: DeviceSetupInput,
    result: DeviceSetupResult,
    devices: List<PcDevice>,
    report: ConnectionReport?,
    isTesting: Boolean,
    onInputChange: (DeviceSetupInput) -> Unit,
    onSave: () -> Unit,
    onTestConnection: () -> Unit,
    onSelectDevice: (String) -> Unit,
    onDeleteDevice: (String) -> Unit,
    onAddDevice: () -> Unit,
    modifier: Modifier = Modifier,
)
```

```kotlin
// WolForm 内：WOL Port 由文本框换成步进器（spec §7.5 明确写的是 "Port 步进器"）
PortStepper(
    label = "WOL Port",
    value = input.wolPort,
    error = result.errors["wolPort"],
    tag = "wolPort",
    onValueChange = { onInputChange(input.copy(wolPort = it)) },
)
// 原来的字段行改为渲染步进器；Agent Port 保持文本框（它不是用户会微调的旋钮）
```

```kotlin
@Composable
private fun PortStepper(
    label: String,
    value: String,
    error: String?,
    tag: String,
    onValueChange: (String) -> Unit,
) {
    val current = value.toIntOrNull() ?: 9
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = { onValueChange((current - 1).coerceAtLeast(1).toString()) },
                modifier = Modifier.testTag("device:$tag-decrement"),
            ) { Text("-") }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("device:$tag"),
            )
            OutlinedButton(
                onClick = { onValueChange((current + 1).coerceAtMost(65535).toString()) },
                modifier = Modifier.testTag("device:$tag-increment"),
            ) { Text("+") }
        }
        if (error != null) {
            Text(error, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        }
    }
}
```

`Test Connection` 按钮下方把原来的静态 `WOL Ready / Fix the highlighted fields` 换成：

```kotlin
ConnectionStatus(report = report, isTesting = isTesting, modifier = Modifier.fillMaxWidth())
```

- [ ] **Step 4: 运行测试确认通过**

Run: `JAVA_HOME=<jdk25> ./gradlew :composeApp:desktopTest`
Expected: BUILD SUCCESSFUL —— 新用例通过；既有的 `DeviceSetupScreenTest` / `AppScreenshotTest` 同步补齐新参数（渲染内容不变，`settings.png` 允许重出）

- [ ] **Step 5: 提交**

```bash
git add mobile/composeApp/src/commonMain/kotlin/com/xukunz/wakeupmywall/ui/settings mobile/composeApp/src/desktopTest/kotlin/com/xukunz/wakeupmywall/ui/settings
git commit -m "feat: bind the device setup form to a real device and surface test results"
```

---

### Task 6: Saved Computers 变成设备选择器（新增 / 选择 / 删除）

**Files:**
- Modify: `mobile/composeApp/src/commonMain/kotlin/com/xukunz/wakeupmywall/ui/settings/DeviceSetupScreen.kt`（`SavedComputers`）
- Test: `mobile/composeApp/src/desktopTest/kotlin/com/xukunz/wakeupmywall/ui/settings/SavedComputersTest.kt`

**Interfaces:**
- Consumes: `List<PcDevice>`、`onSelectDevice` / `onDeleteDevice` / `onAddDevice`（Task 5 已定签名）
- Produces: 每行 `device:row:<id>`（整行可点 = 设为激活设备）、`device:delete:<id>`、`device:default:<id>`（Default 徽标）、`device:add`

- [ ] **Step 1: 写失败测试**

```kotlin
package com.xukunz.wakeupmywall.ui.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.xukunz.wakeupmywall.core.theme.WakeUpMyWallTheme
import com.xukunz.wakeupmywall.domain.model.MacAddress
import com.xukunz.wakeupmywall.domain.model.PcDevice
import com.xukunz.wakeupmywall.domain.usecase.DeviceSetupInput
import com.xukunz.wakeupmywall.domain.usecase.DeviceSetupValidator
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SavedComputersTest {

    private val devices = listOf(
        PcDevice(id = "my-pc", name = "My PC", macAddress = MacAddress.parse("00:1A:2B:3C:4D:5E"), isDefault = true),
        PcDevice(id = "study", name = "Study PC", macAddress = MacAddress.parse("00:1B:44:11:3A:9C")),
    )

    private val input = DeviceSetupInput("My PC", "00:1A:2B:3C:4D:5E", "", "192.168.1.255", "9", "9876", "")

    @Test
    fun `rows select a device, delete reports an id and add is reachable`() = runComposeUiTest {
        var selected: String? = null
        var deleted: String? = null
        var added = false
        setContent {
            WakeUpMyWallTheme {
                DeviceSetupScreen(
                    input = input,
                    result = DeviceSetupValidator.validate(input),
                    devices = devices,
                    report = null,
                    isTesting = false,
                    onInputChange = {},
                    onSave = {},
                    onTestConnection = {},
                    onSelectDevice = { selected = it },
                    onDeleteDevice = { deleted = it },
                    onAddDevice = { added = true },
                )
            }
        }

        onNodeWithTag("device:row:study").performClick()
        assertEquals("study", selected)

        onNodeWithTag("device:delete:study").performClick()
        assertEquals("study", deleted)

        onNodeWithTag("device:add").performClick()
        assertEquals(true, added)
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `JAVA_HOME=<jdk25> ./gradlew :composeApp:desktopTest --tests "*SavedComputersTest*"`
Expected: FAIL —— 找不到 `device:row:study`（现在只是静态行，没有点击与删除）

- [ ] **Step 3: 实现**

```kotlin
@Composable
private fun SavedComputers(
    devices: List<PcDevice>,
    onSelectDevice: (String) -> Unit,
    onDeleteDevice: (String) -> Unit,
    onAddDevice: () -> Unit,
    modifier: Modifier,
) {
    WidgetSurface(style = WidgetStyle.Glass, modifier = modifier) {
        SectionHeader(title = "Saved Computers")
        devices.forEach { device ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectDevice(device.id) }
                    .testTag("device:row:${device.id}"),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(device.name, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = device.macAddress?.value ?: "No MAC yet",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (device.isDefault) {
                    Text(
                        text = "Default",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.testTag("device:default:${device.id}"),
                    )
                }
                IconButton(
                    onClick = { onDeleteDevice(device.id) },
                    modifier = Modifier.testTag("device:delete:${device.id}"),
                ) {
                    AppIcon(kind = AppIconKind.Close, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        OutlinedButton(
            onClick = onAddDevice,
            modifier = Modifier.fillMaxWidth().testTag("device:add"),
        ) {
            Text("+ Add Device")
        }
    }
}
```

> `AppIconKind.Close` 若在 `ui/icons/AppIcons.kt` 里还不存在，就按既有种类加一个代码绘制的叉号（不引图标库、不用第三方素材）。

- [ ] **Step 4: 运行测试确认通过**

Run: `JAVA_HOME=<jdk25> ./gradlew :composeApp:desktopTest`
Expected: BUILD SUCCESSFUL；把重出的 `settings.png` 复制进 `docs/plans/screenshots/`

- [ ] **Step 5: 提交**

```bash
git add mobile/composeApp/src/commonMain/kotlin/com/xukunz/wakeupmywall/ui/settings mobile/composeApp/src/desktopTest docs/plans/screenshots
git commit -m "feat: turn saved computers into the device selector"
```

---

### Task 7: 把仓库接进 App（Rail / Dashboard / Settings 都读激活设备）

**Files:**
- Modify: `mobile/composeApp/src/commonMain/kotlin/com/xukunz/wakeupmywall/app/App.kt`
- Modify: `mobile/composeApp/src/commonMain/kotlin/com/xukunz/wakeupmywall/ui/powerrail/PowerRail.kt`（标题加 `powerrail:title` testTag，若已有则跳过）
- Test: `mobile/composeApp/src/desktopTest/kotlin/com/xukunz/wakeupmywall/app/AppUiTest.kt`（追加）

**Interfaces:**
- Consumes: `DeviceRepository`（Task 3）、`ConnectivityTester` + `TcpProbe`（Task 4）、`SettingsStorage`（Task 1/2）、`MockData.devices`（仅作首次播种）
- Produces: `App(..., storage: SettingsStorage = remember { InMemorySettingsStorage() }, probe: TcpProbe = UnsupportedTcpProbe)`

- [ ] **Step 1: 写失败测试**

```kotlin
    @Test
    fun `the rail shows the active device that came from storage`() = runComposeUiTest {
        val storage = JsonSettingsStorage(InMemoryKeyValueStore())
        runBlocking {
            storage.writeDevices(
                listOf(
                    PcDevice(
                        id = "den",
                        name = "Den PC",
                        macAddress = MacAddress.parse("00:1A:2B:3C:4D:5E"),
                        isDefault = true,
                    ),
                ),
            )
        }

        setContent { App(storage = storage) }

        // 标题必须来自存储里的激活设备，而不是 MockData 常量（"My PC"）。
        onNodeWithTag("powerrail:title", useUnmergedTree = true).assertTextEquals("Den PC")
    }
```

> 执行时先 `rg -n "powerrail" mobile/composeApp/src/commonMain/.../ui/powerrail/PowerRail.kt` 确认标题节点的
> 实际结构：若没有 `powerrail:title`，给 `My PC` 那个 `Text` 补上（这是本步要建立的可断言点）。

- [ ] **Step 2: 运行测试确认失败**

Run: `JAVA_HOME=<jdk25> ./gradlew :composeApp:desktopTest --tests "*AppUiTest*"`
Expected: FAIL —— `App` 没有 `storage` 参数（编译失败），或标题仍是 `My PC`

- [ ] **Step 3: 实现（App.kt 的三处改动）**

```kotlin
@Composable
fun App(
    navigator: AppNavigator = remember { AppNavigator() },
    wallpaperId: String = BuiltInWallpapers.DefaultId,
    initialPcState: PcState = PcState.ONLINE,
    storage: SettingsStorage = remember { InMemorySettingsStorage() },
    probe: TcpProbe = UnsupportedTcpProbe,
) {
    // 设备列表的唯一来源。seed 只在存储里从来没写过设备时用一次（Phase 3 会移除 seeding）。
    val repository = remember(storage) { DeviceRepository(storage, seed = MockData.devices) }
    val devices by repository.devices.collectAsState()
    val device = repository.active ?: MockData.defaultDevice
    val scope = rememberCoroutineScope()
    val connectivity = remember(probe) { ConnectivityTester(probe, AgentApi(createAgentHttpClient())) }
    var connectionReport by remember { mutableStateOf<ConnectionReport?>(null) }
    var isTestingConnection by remember { mutableStateOf(false) }

    LaunchedEffect(repository) { repository.load() }
    // 激活设备换人时，把表单切到新设备（否则表单还停在上一次的输入上）
    LaunchedEffect(device.id) { deviceInput = device.toSetupInput() }
    ...
}
```

```kotlin
// 原 `val device = MockData.defaultDevice` 删除；这些地方自动改为读仓库：
val railModel = powerRailModel(pcState, device)
// Settings 分支里的 DeviceSetupScreen 换成 Task 5 的新签名：
DeviceSetupScreen(
    input = deviceInput,
    result = DeviceSetupValidator.validate(deviceInput),
    devices = devices,
    report = connectionReport,
    isTesting = isTestingConnection,
    onInputChange = { deviceInput = it },
    onSave = {
        DeviceSetupValidator.validate(deviceInput).device?.let { edited ->
            scope.launch { repository.update(device.id, edited.copy(isDefault = device.isDefault)) }
        }
    },
    onTestConnection = {
        scope.launch {
            isTestingConnection = true
            connectionReport = connectivity.test(device)
            isTestingConnection = false
        }
    },
    onSelectDevice = { id -> scope.launch { repository.setDefault(id) } },
    onDeleteDevice = { id -> scope.launch { repository.delete(id) } },
    onAddDevice = {
        scope.launch {
            repository.add(
                PcDevice(
                    id = "new-pc-${devices.size + 1}",
                    name = "New PC",
                    macAddress = null,
                ),
            )
        }
    },
)
```

```kotlin
/** `PcDevice` → 表单输入。放在 App.kt 文件末尾的私有扩展里，避免 UI 组件知道模型细节。 */
private fun PcDevice.toSetupInput() = DeviceSetupInput(
    name = name,
    mac = macAddress?.value.orEmpty(),
    ip = ipAddress.orEmpty(),
    broadcast = broadcastAddress,
    wolPort = wolPort.toString(),
    agentPort = agentPort.toString(),
    agentHost = agentHost.orEmpty(),
)
```

`MainActivity` 同步注入真实实现（Task 2 已建 storage，这里补 probe）：

```kotlin
val storage = JsonSettingsStorage(DataStoreKeyValueStore(applicationContext))
setContent { App(storage = storage, probe = AndroidTcpProbe()) }
```

- [ ] **Step 4: 运行全量测试确认通过**

Run: `JAVA_HOME=<jdk25> ./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest :composeApp:assembleDebug`
Expected: BUILD SUCCESSFUL —— 其余 `AppUiTest` 用例不受影响（默认参数仍是内存存储 + 同一份 Mock 播下的设备）

- [ ] **Step 5: 提交**

```bash
git add mobile/composeApp/src/commonMain/kotlin/com/xukunz/wakeupmywall/app mobile/composeApp/src/androidMain mobile/composeApp/src/desktopTest/kotlin/com/xukunz/wakeupmywall/app
git commit -m "feat: drive the shell from the persisted active device"
```

---

### Task 8: 端到端验收（真机持久化 + 失败原因）

**Files:**
- Modify: `docs/plans/version-matrix.md`（依赖矩阵新增 DataStore / ktor-network 两行 + 模拟器小节补一句数据落点）
- Modify: 本计划（把 Task 1–8 的复选框勾上，并追加一段"验收实录"：真机命令、期望 vs 实际、截图路径）

> 路标与 README 已在计划就绪时更新过（Phase 2 段指向本计划、项目状态推进到 Phase 2），
> 执行阶段只需补 version-matrix 的依赖矩阵与验收实录。

- [ ] **Step 1: 装机并验证"重启后数据还在"**

```bash
JAVA_HOME=<jdk25> ./gradlew :composeApp:installDebug
ADB=/home/xukunz/.local/toolchain/android-sdk/platform-tools/adb
"$ADB" -s emulator-5554 shell am force-stop com.xukunz.wakeupmywall
"$ADB" -s emulator-5554 shell am start -n com.xukunz.wakeupmywall/.MainActivity && sleep 8
"$ADB" -s emulator-5554 exec-out screencap -p > /tmp/phase2-before.png     # Settings → Device Setup
# 用 adb input tap 点 "+ Add Device"（坐标从截图量），再改个名字
"$ADB" -s emulator-5554 shell am force-stop com.xukunz.wakeupmywall
"$ADB" -s emulator-5554 shell am start -n com.xukunz.wakeupmywall/.MainActivity && sleep 8
"$ADB" -s emulator-5554 exec-out screencap -p > /tmp/phase2-after.png
"$ADB" -s emulator-5554 shell run-as com.xukunz.wakeupmywall ls -l files/datastore
```
Expected: 两张截图里设备列表一致（新设备还在），`files/datastore/settings.preferences_pb` 存在且非 0 字节。

- [ ] **Step 2: 验证失败原因不是静默失败**

```bash
# 场景 A：没有任何东西在监听 9876 → 期望 "Nothing is listening on <ip>:9876 — the PC may be asleep, ..."
# 场景 B：宿主起一个普通 HTTP 服务，设备指向 10.0.2.2:9876 → 期望 "... returned 404 — this is not the WakeUpMyWall Agent"
python3 -m http.server 9876 --bind 0.0.0.0 &
"$ADB" -s emulator-5554 shell input tap <Test Connection 坐标>   # 期望连接结论块出现上面那句 404 文案
kill %1
```

- [ ] **Step 3: 更新文档并提交**

```bash
git add docs README.md
git commit -m "docs: record the phase 2 acceptance run and update the roadmap"
```

---

## 2. 验收标准（对照 roadmap Phase 2 段）

| roadmap 要求 | 本计划的落点 | 验证方式 |
| --- | --- | --- |
| 冷启动后设备列表与默认设备正确恢复 | Task 1–3、7 | `DeviceRepositoryTest` 6 条 + Task 8 Step 1 真机前后对比 |
| 无 Agent 时连通性测试给出明确失败原因而非静默失败 | Task 4、5 | `ConnectivityTesterTest` 8 条（拒连/超时/解析失败/404/401/成功）+ Task 8 Step 2 真机两种场景 |
| 增删改与默认 PC | Task 3、6 | `DeviceRepositoryTest` + `SavedComputersTest` |
| 多 PC 一等公民 | Global Constraints + Task 7 | `AppUiTest` 断言 Rail 跟随存储里的激活设备；运行路径不再读 `MockData.defaultDevice`（仅作空列表兜底） |
| 存储选型在计划期定案 | §0 决策表 | DataStore Preferences **1.2.1**（版本号已核对 Google Maven 元数据，不是记忆值） |

## 3. 计划自检（writing-plans 要求的三项）

1. **Spec 覆盖**：spec §6 数据模型 → Task 1/3（字段与默认设备不变量）；§7.5 Device Setup（表单 + Saved Computers + Port 步进器）→ Task 5/6；§5 网络（短超时 + `GET /api/v1/status`）→ Task 4（TCP 2 s、HTTP 沿用既有的 2.5 s）；roadmap Phase 2 的四条完成标准 → §2 验收表。**刻意不做**：Agent 配对与 Token（Phase 4）、WOL 发包（Phase 3）、空设备列表的空状态 UI（Phase 3，本阶段用 `active ?: MockData.defaultDevice` 兜底并在 §0 记明）。
2. **占位符扫描**：没有 TBD / "稍后补" / "类似 Task N"。每个 Step 要么给完整代码，要么给可运行命令与期望输出。Task 8 Step 1/2 里的 `adb input tap <坐标>` 是**已知需要现场量坐标的真机操作**，期望输出写死了，不算占位。
3. **类型一致性**：`KeyValueStore.read/write` → `JsonSettingsStorage` → `SettingsStorage.readDevices/writeDevices/readAppearance/writeAppearance` → `DeviceRepository.load/add/update/delete/setDefault` + `devices/active` → UI 回调 `onSelectDevice/onDeleteDevice/onAddDevice/onTestConnection`；连接侧 `TcpProbeResult` → `ConnectionReport` → `ConnectionStatus`。测试与实现引用的名字逐处核对过（含 `DeviceSetupScreen` 的新参数顺序，Task 5 定义、Task 6/7 复用）。
