# Phase 1：Design System 与 Mock UI 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 用 Mock Data 把 Dashboard（含 Dashboard/Monitor 双模式）、Power Rail、Settings Workspace、Appearance 五个核心页面还原到概念图的 80–90%，并把视觉规则固化进 Design System 与可复用的 WidgetSurface 抽象。

**Architecture:** 所有页面由 `AppShell`（左 72% 主内容 + 右 28% Power Rail）承载；Power Rail 是唯一常驻组件，状态完全由 `PcState` + `PcCapabilities` 驱动，不允许在 UI 里写死按钮可用性。卡片统一经 `WidgetSurface` 渲染，风格由 `WidgetStyle` 决定。指标曲线数据来自 `MetricRingBuffer`（纯逻辑，可测）。

**Tech Stack:** Compose Multiplatform 1.10.3（`commonMain` 全部 UI）、kotlin-test、`compose.uiTest`（desktopTest 中运行）、Phase 0 的 domain / core 骨架。

**Spec:** [docs/superpowers/specs/2026-09-19-desktop-companion-design.md](../specs/2026-09-19-desktop-companion-design.md)

**前置：** Phase 0 全部任务完成，`./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest` 全绿。

## Global Constraints

- 本阶段**只用 Mock Data**：不接 WOL、不接 Agent、不发任何网络请求（`AgentApi` 只保留单测覆盖）。
- 横屏锁定：所有布局按 `Row` + 权重 72/28 设计，不写竖屏分支。
- 颜色、间距、圆角、字号只能取自 `core/theme`；Composable 内禁止出现字面量 `Color(0x...)` 或裸 `dp` 值（测试与令牌文件除外）。
- Power Rail 的按钮可用性只能来自 `PcState.capabilities(device)`，禁止 `if (state == ONLINE)` 这类散落判断。
- 每个 UI 元素必须有 `testTag`，命名规则 `区域:元素`（如 `powerrail:primary`、`dashboard:weather`）。
- 每笔提交前 `./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest` 必须全绿。
- 本阶段不引入图标库与字体资源；用文字与几何图形完成布局验证，图标在视觉打磨阶段统一替换。

---

## File Structure

| 路径 | 职责 |
| --- | --- |
| `core/theme/Typography.kt` | 字号与字重令牌（标题/正文/数值/单位） |
| `core/theme/Shape.kt` | 圆角令牌（卡片 / 按钮 / 徽标） |
| `ui/components/WidgetSurface.kt` | Glass / Solid / Minimal 三种卡片承载 |
| `ui/components/WidgetStyle.kt` | `enum class WidgetStyle` 与透明度量表 |
| `ui/components/SectionHeader.kt` | 卡片标题行（标题 + 右侧动作） |
| `ui/powerrail/PowerRail.kt` | 常驻 PC 控制栏 |
| `ui/powerrail/PowerRailState.kt` | Rail 的展示模型（由 `PcState` 映射而来） |
| `ui/dashboard/DashboardShell.kt` | 72/28 外壳 |
| `ui/dashboard/DashboardMode.kt` | StandBy 信息模式 |
| `ui/dashboard/widgets/*.kt` | Greeting / Clock / Weather / Calendar / Todo / PcSummary / Decorative |
| `ui/monitor/MonitorMode.kt` | 硬件监控模式 |
| `ui/monitor/MetricCard.kt`、`MetricSparkline.kt` | 指标卡片与 60 秒曲线 |
| `ui/settings/SettingsWorkspace.kt` | Settings 左导航 + 内容区 |
| `ui/settings/DeviceSetupScreen.kt` | 设备配置表单 |
| `ui/settings/AppearanceScreen.kt` | 壁纸 / 强调色 / Widget 风格 / 布局 + Live Preview |
| `domain/model/DashboardWidget.kt` | Widget 抽象（id / type / enabled / order） |
| `domain/usecase/DeviceSetupValidator.kt` | 设备表单校验（纯逻辑） |
| `core/metrics/MetricRingBuffer.kt` | 60 秒环形缓冲（纯逻辑） |
| `data/mock/MockData.kt` | 全部 Mock 数据 |

---

### Task 1: 排版与形状令牌

**Files:**
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/core/theme/Typography.kt`
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/core/theme/Shape.kt`
- Modify: `src/commonMain/kotlin/com/xukunz/wakeupmywall/core/theme/AppTheme.kt`

**Interfaces:**
- Consumes: `WakeUpMyWallTheme`
- Produces:
  - `object AppTypography { val display, title, body, label, metricValue, metricUnit: TextStyle }`
  - `object AppShapes { val card, button, badge: RoundedCornerShape }`
  - `WakeUpMyWallTheme` 接入 `typography` 与 `shapes`

- [ ] **Step 1: 实现排版令牌**

```kotlin
package com.xukunz.wakeupmywall.core.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object AppTypography {
    val display = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.Light)
    val title = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    val body = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal)
    val label = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)
    val metricValue = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
    val metricUnit = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal)
}
```

- [ ] **Step 2: 实现形状令牌**

```kotlin
package com.xukunz.wakeupmywall.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object AppShapes {
    val card = RoundedCornerShape(20.dp)
    val button = RoundedCornerShape(16.dp)
    val badge = RoundedCornerShape(999.dp)
}
```

- [ ] **Step 3: 接入主题**

在 `AppTheme.kt` 的 `MaterialTheme(...)` 中补两个参数：

```kotlin
        shapes = Shapes(
            medium = AppShapes.card,
            large = AppShapes.card,
            small = AppShapes.badge,
        ),
        typography = Typography(
            titleLarge = AppTypography.title,
            bodyMedium = AppTypography.body,
            labelSmall = AppTypography.label,
            displayLarge = AppTypography.display,
        ),
```

- [ ] **Step 4: 验证并提交**

```bash
./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest
git add -A
git commit -m "feat: add typography and shape design tokens"
```

---

### Task 2: WidgetStyle 与 WidgetSurface

**Files:**
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/ui/components/WidgetStyle.kt`
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/ui/components/WidgetSurface.kt`
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/ui/components/SectionHeader.kt`
- Test: `src/desktopTest/kotlin/com/xukunz/wakeupmywall/ui/components/WidgetSurfaceTest.kt`

**Interfaces:**
- Consumes: `AppShapes`、`DarkSurface`、`Spacing`
- Produces:
  - `enum class WidgetStyle { Glass, Solid, Minimal }` 与 `fun WidgetStyle.alpha(): Float`
  - `@Composable fun WidgetSurface(style: WidgetStyle, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit)`
  - `@Composable fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null)`

- [ ] **Step 1: 写失败测试**

文件 `src/desktopTest/kotlin/com/xukunz/wakeupmywall/ui/components/WidgetSurfaceTest.kt`：

```kotlin
package com.xukunz.wakeupmywall.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class WidgetSurfaceTest {

    @Test
    fun `glass surface renders its content`() = runComposeUiTest {
        setContent {
            WidgetSurface(style = WidgetStyle.Glass, modifier = Modifier.fillMaxSize()) {
                Text("CPU 28%", modifier = Modifier.testTagOrNothing("widget:body"))
            }
        }
        onNodeWithTag("widget:body").assertIsDisplayed()
    }

    @Test
    fun `each style renders a tagged surface`() = runComposeUiTest {
        setContent {
            WidgetStyle.entries.forEach { style ->
                WidgetSurface(style = style, modifier = Modifier.fillMaxSize()) { }
            }
        }
        WidgetStyle.entries.forEach { style ->
            onNodeWithTag("surface:${style.name}").assertExists()
        }
    }
}
```

（`testTagOrNothing` 属于多余包装，直接写 `Modifier.testTag("widget:body")` 并 `import androidx.compose.ui.platform.testTag`。）

- [ ] **Step 2: 运行测试，确认失败**

```bash
./gradlew :composeApp:desktopTest --tests "*WidgetSurfaceTest*"
```

Expected: 编译失败，`Unresolved reference: WidgetSurface`。

- [ ] **Step 3: 实现**

`WidgetStyle.kt`：

```kotlin
package com.xukunz.wakeupmywall.ui.components

enum class WidgetStyle { Glass, Solid, Minimal }

fun WidgetStyle.surfaceAlpha(): Float = when (this) {
    WidgetStyle.Glass -> 0.35f
    WidgetStyle.Solid -> 1f
    WidgetStyle.Minimal -> 0.08f
}

fun WidgetStyle.showsBorder(): Boolean = this != WidgetStyle.Minimal
```

`WidgetSurface.kt`：

```kotlin
package com.xukunz.wakeupmywall.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.xukunz.wakeupmywall.core.theme.AppShapes
import com.xukunz.wakeupmywall.core.theme.DarkSurface
import com.xukunz.wakeupmywall.core.theme.Spacing

@Composable
fun WidgetSurface(
    style: WidgetStyle,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val base = Modifier
        .clip(AppShapes.card)
        .background(DarkSurface.card.copy(alpha = style.surfaceAlpha()))
        .let { if (style.showsBorder()) it.border(1.dp, DarkSurface.outline, AppShapes.card) else it }
        .padding(Spacing.md)
        .testTag("surface:${style.name}")

    Column(modifier = modifier.then(base), content = content)
}
```

（`1.dp` 需 `import androidx.compose.ui.unit.dp`；`Color` 的 import 若未使用则删除。）

`SectionHeader.kt`：

```kotlin
package com.xukunz.wakeupmywall.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().testTag("header:$title"),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        trailing?.invoke()
    }
}
```

- [ ] **Step 4: 运行测试并提交**

```bash
./gradlew :composeApp:desktopTest --tests "*WidgetSurfaceTest*"
git add -A
git commit -m "feat: add widget surface with glass solid and minimal styles"
```

---

### Task 3: Widget 抽象与 Mock 数据

**Files:**
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/domain/model/DashboardWidget.kt`
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/data/mock/MockData.kt`
- Test: `src/commonTest/kotlin/com/xukunz/wakeupmywall/domain/DashboardWidgetTest.kt`

**Interfaces:**
- Consumes: 无
- Produces:
  - `enum class WidgetType { Greeting, Clock, Weather, Calendar, Todo, PcSummary, Decorative }`
  - `data class DashboardWidget(val id: String, val type: WidgetType, val enabled: Boolean = true, val order: Int = 0)`
  - `object DashboardLayout { val default: List<DashboardWidget>; fun visible(widgets: List<DashboardWidget>): List<DashboardWidget>; fun toggle(widgets: List<DashboardWidget>, id: String): List<DashboardWidget> }`
  - `object MockData { val devices: List<PcDevice>; val weather: WeatherSnapshot; val calendarEvents: List<CalendarEvent>; val todos: List<TodoItem>; val metrics: MetricsSnapshot }`

- [ ] **Step 1: 写失败测试**

```kotlin
package com.xukunz.wakeupmywall.domain

import com.xukunz.wakeupmywall.domain.model.DashboardLayout
import com.xukunz.wakeupmywall.domain.model.WidgetType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DashboardWidgetTest {

    @Test
    fun `default layout contains all seven widget types`() {
        assertEquals(
            WidgetType.entries.toSet(),
            DashboardLayout.default.map { it.type }.toSet(),
        )
    }

    @Test
    fun `visible filters disabled widgets and respects order`() {
        val widgets = DashboardLayout.default.map { it.copy(order = WidgetType.entries.size - it.order) }
        val visible = DashboardLayout.visible(widgets)

        assertEquals(widgets.size, visible.size)
        assertEquals(visible.sortedBy { it.order }, visible)
    }

    @Test
    fun `toggle disables a widget without dropping it`() {
        val toggled = DashboardLayout.toggle(DashboardLayout.default, "weather")

        assertEquals(DashboardLayout.default.size, toggled.size)
        assertTrue(toggled.first { it.id == "weather" }.enabled.not())
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*DashboardWidgetTest*"
```

Expected: 编译失败，`Unresolved reference: DashboardLayout`。

- [ ] **Step 3: 实现 Widget 抽象**

`DashboardWidget.kt`：

```kotlin
package com.xukunz.wakeupmywall.domain.model

enum class WidgetType { Greeting, Clock, Weather, Calendar, Todo, PcSummary, Decorative }

data class DashboardWidget(
    val id: String,
    val type: WidgetType,
    val enabled: Boolean = true,
    val order: Int = 0,
)

object DashboardLayout {
    val default: List<DashboardWidget> = listOf(
        DashboardWidget("greeting", WidgetType.Greeting, order = 0),
        DashboardWidget("clock", WidgetType.Clock, order = 1),
        DashboardWidget("weather", WidgetType.Weather, order = 2),
        DashboardWidget("calendar", WidgetType.Calendar, order = 3),
        DashboardWidget("todo", WidgetType.Todo, order = 4),
        DashboardWidget("pc-summary", WidgetType.PcSummary, order = 5),
        DashboardWidget("decorative", WidgetType.Decorative, enabled = false, order = 6),
    )

    fun visible(widgets: List<DashboardWidget>): List<DashboardWidget> =
        widgets.filter { it.enabled }.sortedBy { it.order }

    fun toggle(widgets: List<DashboardWidget>, id: String): List<DashboardWidget> =
        widgets.map { if (it.id == id) it.copy(enabled = it.enabled.not()) else it }
}
```

- [ ] **Step 4: 实现 Mock 数据**

`MockData.kt` 需覆盖概念图上的所有数字，且数值集中在文件顶部常量，便于后续替换真实数据源：

```kotlin
package com.xukunz.wakeupmywall.data.mock

import com.xukunz.wakeupmywall.domain.model.CalendarEvent
import com.xukunz.wakeupmywall.domain.model.MacAddress
import com.xukunz.wakeupmywall.domain.model.MetricsSnapshot
import com.xukunz.wakeupmywall.domain.model.PcDevice
import com.xukunz.wakeupmywall.domain.model.TodoItem
import com.xukunz.wakeupmywall.domain.model.WeatherSnapshot

object MockData {
    val devices: List<PcDevice> = listOf(
        PcDevice(
            id = "desktop-alpha",
            name = "Desktop-Alpha",
            macAddress = MacAddress.parse("AA:BB:CC:DD:EE:FF"),
            ipAddress = "192.168.1.10",
            agentHost = "192.168.1.10",
            isDefault = true,
        ),
        PcDevice(id = "living-room", name = "Living Room PC"),
        PcDevice(id = "workstation", name = "Workstation"),
    )

    val weather = WeatherSnapshot(
        location = "Home",
        temperatureC = 18,
        condition = "Partly cloudy",
        highC = 21,
        lowC = 12,
        nextHoursC = listOf(18, 19, 20, 18),
    )

    val calendarEvents = listOf(
        CalendarEvent(time = "10:00", title = "Team Sync"),
        CalendarEvent(time = "13:00", title = "Lunch"),
        CalendarEvent(time = "16:00", title = "Plan next week"),
    )

    val todos = listOf(
        TodoItem(id = "1", title = "Review PR", done = false),
        TodoItem(id = "2", title = "Backup NAS", done = true),
    )

    val metrics = MetricsSnapshot(
        cpuPercent = 28f,
        cpuTempC = 54,
        cpuMhz = 4400,
        gpuPercent = 62f,
        gpuTempC = 61,
        vramUsedMb = 5200,
        vramTotalMb = 12288,
        ramPercent = 38f,
        storagePercent = 54f,
        downloadMbps = 12.4f,
        uploadMbps = 1.8f,
        cpuFanRpm = 980,
        gpuFanRpm = 1240,
        uptimeSeconds = 3 * 24 * 3600 + 5 * 3600,
        hardwareSummary = "Ryzen 7 7700X / RTX 4070 Ti",
    )
}
```

同时新建 `domain/model/Snapshots.kt`，定义 `WeatherSnapshot`、`CalendarEvent`、`TodoItem`、`MetricsSnapshot` 四个纯数据类（字段与上面 Mock 调用一致）。

- [ ] **Step 5: 运行测试并提交**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*DashboardWidgetTest*"
git add -A
git commit -m "feat: add dashboard widget abstraction and mock data set"
```

---

### Task 4: Power Rail（状态驱动）

**Files:**
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/ui/powerrail/PowerRailState.kt`
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/ui/powerrail/PowerRail.kt`
- Test: `src/commonTest/kotlin/com/xukunz/wakeupmywall/ui/powerrail/PowerRailStateTest.kt`
- Test: `src/desktopTest/kotlin/com/xukunz/wakeupmywall/ui/powerrail/PowerRailUiTest.kt`

**Interfaces:**
- Consumes: `PcState.capabilities(device)`、`PcDevice`
- Produces:
  - `data class PowerRailModel(val pcName: String, val stateLabel: String, val primaryLabel: String, val primaryEnabled: Boolean, val canSleep: Boolean, val canShutdown: Boolean, val canRestart: Boolean, val statusLine: String)`
  - `fun powerRailModel(state: PcState, device: PcDevice?): PowerRailModel`
  - `@Composable fun PowerRail(model: PowerRailModel, onPrimary: () -> Unit, onSleep: () -> Unit, onShutdown: () -> Unit, onRestart: () -> Unit, onSettings: () -> Unit, modifier: Modifier = Modifier)`

- [ ] **Step 1: 写失败测试（状态映射）**

```kotlin
package com.xukunz.wakeupmywall.ui.powerrail

import com.xukunz.wakeupmywall.domain.model.MacAddress
import com.xukunz.wakeupmywall.domain.model.PcDevice
import com.xukunz.wakeupmywall.domain.model.PcState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PowerRailStateTest {

    private val device = PcDevice(
        id = "1",
        name = "My PC",
        macAddress = MacAddress.parse("AA:BB:CC:DD:EE:FF"),
    )

    @Test
    fun `wol ready shows wake label and enabled primary`() {
        val model = powerRailModel(PcState.WOL_READY, device)
        assertEquals("Wake PC", model.primaryLabel)
        assertTrue(model.primaryEnabled)
        assertEquals("Wake-on-LAN Ready", model.statusLine)
    }

    @Test
    fun `offline without mac disables wake`() {
        val model = powerRailModel(PcState.OFFLINE, device.copy(macAddress = null))
        assertFalse(model.primaryEnabled)
    }

    @Test
    fun `online enables secondary actions only`() {
        val model = powerRailModel(PcState.ONLINE, device)
        assertEquals("PC Online", model.primaryLabel)
        assertTrue(model.canSleep && model.canShutdown && model.canRestart)
        assertEquals("Agent connected over LAN", model.statusLine)
    }

    @Test
    fun `terminal states disable every action`() {
        listOf(PcState.WAKING, PcState.SLEEPING, PcState.RESTARTING, PcState.SHUTTING_DOWN).forEach { state ->
            val model = powerRailModel(state, device)
            assertFalse(model.primaryEnabled, "$state primary")
            assertFalse(model.canSleep || model.canShutdown || model.canRestart, "$state secondary")
        }
    }

    @Test
    fun `unconfigured shows setup pc`() {
        val model = powerRailModel(PcState.UNCONFIGURED, null)
        assertEquals("Setup PC", model.primaryLabel)
        assertEquals("My PC", model.pcName)
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*PowerRailStateTest*"
```

Expected: 编译失败，`Unresolved reference: powerRailModel`。

- [ ] **Step 3: 实现状态映射**

```kotlin
package com.xukunz.wakeupmywall.ui.powerrail

import com.xukunz.wakeupmywall.domain.model.PcDevice
import com.xukunz.wakeupmywall.domain.model.PcState
import com.xukunz.wakeupmywall.domain.model.capabilities

data class PowerRailModel(
    val pcName: String,
    val stateLabel: String,
    val primaryLabel: String,
    val primaryEnabled: Boolean,
    val canSleep: Boolean,
    val canShutdown: Boolean,
    val canRestart: Boolean,
    val statusLine: String,
)

fun powerRailModel(state: PcState, device: PcDevice?): PowerRailModel {
    val capabilities = state.capabilities(device)
    return PowerRailModel(
        pcName = device?.name ?: "My PC",
        stateLabel = state.name.replace('_', ' '),
        primaryLabel = capabilities.primaryLabel,
        primaryEnabled = capabilities.primaryEnabled,
        canSleep = capabilities.canSleep,
        canShutdown = capabilities.canShutdown,
        canRestart = capabilities.canRestart,
        statusLine = capabilities.statusText,
    )
}
```

- [ ] **Step 4: 实现 Power Rail UI**

结构：`Column`（PC 名 + 状态徽标 + 主按钮 + 三个次级按钮 + 状态行 + Settings 入口），每个可交互元素带 `testTag("powerrail:...")`；主按钮用 `Button`，次级按钮用 `OutlinedButton`，`enabled` 一律绑定模型字段；`Spacing` 只取令牌。

```kotlin
@Composable
fun PowerRail(
    model: PowerRailModel,
    onPrimary: () -> Unit,
    onSleep: () -> Unit,
    onShutdown: () -> Unit,
    onRestart: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxHeight().padding(Spacing.md).testTag("powerrail"),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(model.pcName, style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("powerrail:name"))
        Text(model.stateLabel, style = MaterialTheme.typography.labelSmall, modifier = Modifier.testTag("powerrail:state"))
        Button(
            onClick = onPrimary,
            enabled = model.primaryEnabled,
            modifier = Modifier.fillMaxWidth().testTag("powerrail:primary"),
        ) { Text(model.primaryLabel) }
        OutlinedButton(onClick = onSleep, enabled = model.canSleep, modifier = Modifier.fillMaxWidth().testTag("powerrail:sleep")) { Text("Sleep") }
        OutlinedButton(onClick = onShutdown, enabled = model.canShutdown, modifier = Modifier.fillMaxWidth().testTag("powerrail:shutdown")) { Text("Shutdown") }
        OutlinedButton(onClick = onRestart, enabled = model.canRestart, modifier = Modifier.fillMaxWidth().testTag("powerrail:restart")) { Text("Restart") }
        Text(model.statusLine, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("powerrail:status"))
        TextButton(onClick = onSettings, modifier = Modifier.testTag("powerrail:settings")) { Text("Settings") }
    }
}
```

- [ ] **Step 5: 写 UI 测试（逐状态断言）**

```kotlin
@OptIn(ExperimentalTestApi::class)
class PowerRailUiTest {

    @Test
    fun `wol ready shows wake label`() = runComposeUiTest {
        setContent { WakeUpMyWallTheme { PowerRail(model = powerRailModel(PcState.WOL_READY, device), {}, {}, {}, {}, {}) } }
        onNodeWithTag("powerrail:primary").assertTextEquals("Wake PC")
        onNodeWithTag("powerrail:status").assertTextEquals("Wake-on-LAN Ready")
    }

    @Test
    fun `online disables primary and enables shutdown`() = runComposeUiTest {
        setContent { WakeUpMyWallTheme { PowerRail(model = powerRailModel(PcState.ONLINE, device), {}, {}, {}, {}, {}) } }
        onNodeWithTag("powerrail:primary").assertIsNotEnabled()
        onNodeWithTag("powerrail:shutdown").assertIsEnabled()
    }

    @Test
    fun `waking disables every action`() = runComposeUiTest {
        setContent { WakeUpMyWallTheme { PowerRail(model = powerRailModel(PcState.WAKING, device), {}, {}, {}, {}, {}) } }
        onNodeWithTag("powerrail:primary").assertIsNotEnabled()
        onNodeWithTag("powerrail:sleep").assertIsNotEnabled()
        onNodeWithTag("powerrail:shutdown").assertIsNotEnabled()
        onNodeWithTag("powerrail:restart").assertIsNotEnabled()
    }
}
```

- [ ] **Step 6: 运行测试并提交**

```bash
./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest
git add -A
git commit -m "feat: add state driven power rail with per-state ui tests"
```

---

### Task 5: AppShell 与 72/28 双栏布局

**Files:**
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/ui/AppShell.kt`
- Modify: `src/commonMain/kotlin/com/xukunz/wakeupmywall/app/App.kt`
- Test: `src/desktopTest/kotlin/com/xukunz/wakeupmywall/ui/AppShellTest.kt`

**Interfaces:**
- Consumes: `PowerRail`、`PowerRailModel`
- Produces: `@Composable fun AppShell(rail: PowerRailModel, onRailEvent: (RailEvent) -> Unit, content: @Composable BoxScope.() -> Unit)` 与 `enum class RailEvent { Primary, Sleep, Shutdown, Restart, Settings }`

- [ ] **Step 1: 写失败测试**

```kotlin
@OptIn(ExperimentalTestApi::class)
class AppShellTest {

    @Test
    fun `renders main content and rail side by side`() = runComposeUiTest {
        setContent {
            WakeUpMyWallTheme {
                AppShell(rail = powerRailModel(PcState.ONLINE, device), onRailEvent = {}) {
                    Text("MAIN", modifier = Modifier.testTag("shell:main"))
                }
            }
        }
        onNodeWithTag("shell:main").assertIsDisplayed()
        onNodeWithTag("powerrail").assertIsDisplayed()
    }

    @Test
    fun `rail events are forwarded`() = runComposeUiTest {
        var received: RailEvent? = null
        setContent {
            WakeUpMyWallTheme {
                AppShell(rail = powerRailModel(PcState.ONLINE, device), onRailEvent = { received = it }) {
                    Text("MAIN")
                }
            }
        }
        onNodeWithTag("powerrail:shutdown").performClick()
        assertEquals(RailEvent.Shutdown, received)
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

```bash
./gradlew :composeApp:desktopTest --tests "*AppShellTest*"
```

Expected: 编译失败，`Unresolved reference: AppShell`。

- [ ] **Step 3: 实现**

```kotlin
package com.xukunz.wakeupmywall.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.weight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import com.xukunz.wakeupmywall.ui.powerrail.PowerRail
import com.xukunz.wakeupmywall.ui.powerrail.PowerRailModel

enum class RailEvent { Primary, Sleep, Shutdown, Restart, Settings }

@Composable
fun AppShell(
    rail: PowerRailModel,
    onRailEvent: (RailEvent) -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(0.72f).fillMaxHeight(), content = content)
        PowerRail(
            model = rail,
            onPrimary = { onRailEvent(RailEvent.Primary) },
            onSleep = { onRailEvent(RailEvent.Sleep) },
            onShutdown = { onRailEvent(RailEvent.Shutdown) },
            onRestart = { onRailEvent(RailEvent.Restart) },
            onSettings = { onRailEvent(RailEvent.Settings) },
            modifier = Modifier.weight(0.28f),
        )
    }
}
```

- [ ] **Step 4: `App.kt` 接入 AppShell**

`App()` 改为：持有 `PcState`（Phase 1 用 `remember { mutableStateOf(PcState.ONLINE) }` 与 Mock 设备）、构造 `powerRailModel`、把 `RailEvent.Settings` 导航到 `Workspace.Settings`，其余事件暂时只更新本地状态（真实调用在 Phase 3/4）。

- [ ] **Step 5: 运行测试并提交**

```bash
./gradlew :composeApp:desktopTest --tests "*AppShellTest*"
git add -A
git commit -m "feat: add app shell with 72-28 main rail layout"
```

---

### Task 6: Dashboard Mode 卡片

**Files:**
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/ui/dashboard/DashboardMode.kt`
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/ui/dashboard/widgets/GreetingWidget.kt`
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/ui/dashboard/widgets/ClockWidget.kt`
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/ui/dashboard/widgets/WeatherWidget.kt`
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/ui/dashboard/widgets/CalendarWidget.kt`
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/ui/dashboard/widgets/TodoWidget.kt`
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/ui/dashboard/widgets/PcSummaryWidget.kt`
- Test: `src/desktopTest/kotlin/com/xukunz/wakeupmywall/ui/dashboard/DashboardModeTest.kt`

**Interfaces:**
- Consumes: `WidgetSurface`、`MockData`、`DashboardLayout`
- Produces:
  - `@Composable fun DashboardMode(data: DashboardData, style: WidgetStyle, onPcSummaryClick: () -> Unit, modifier: Modifier = Modifier)`
  - `data class DashboardData(val greeting: String, val time: String, val date: String, val weather: WeatherSnapshot, val events: List<CalendarEvent>, val todos: List<TodoItem>, val pc: PowerRailModel, val widgets: List<DashboardWidget>)`

- [ ] **Step 1: 写失败测试**

```kotlin
@OptIn(ExperimentalTestApi::class)
class DashboardModeTest {

    private val data = DashboardData(
        greeting = "Good Evening",
        time = "21:04",
        date = "Tue, Apr 22",
        weather = MockData.weather,
        events = MockData.calendarEvents,
        todos = MockData.todos,
        pc = powerRailModel(PcState.ONLINE, MockData.devices.first()),
        widgets = DashboardLayout.default,
    )

    @Test
    fun `renders greeting clock weather calendar and todo`() = runComposeUiTest {
        setContent { WakeUpMyWallTheme { DashboardMode(data, WidgetStyle.Glass, {}) } }
        listOf("dashboard:greeting", "dashboard:clock", "dashboard:weather", "dashboard:calendar", "dashboard:todo")
            .forEach { onNodeWithTag(it).assertIsDisplayed() }
    }

    @Test
    fun `disabled widgets are not rendered`() = runComposeUiTest {
        val dataWithoutDecorative = data.copy(widgets = DashboardLayout.default)
        setContent { WakeUpMyWallTheme { DashboardMode(dataWithoutDecorative, WidgetStyle.Glass, {}) } }
        onNodeWithTag("dashboard:decorative").assertDoesNotExist()
    }

    @Test
    fun `pc summary click is forwarded`() = runComposeUiTest {
        var clicked = false
        setContent { WakeUpMyWallTheme { DashboardMode(data, WidgetStyle.Glass, onPcSummaryClick = { clicked = true }) } }
        onNodeWithTag("dashboard:pc-summary").performClick()
        assertTrue(clicked)
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

```bash
./gradlew :composeApp:desktopTest --tests "*DashboardModeTest*"
```

Expected: 编译失败，`Unresolved reference: DashboardMode`。

- [ ] **Step 3: 实现各卡片**

每张卡片都是 `WidgetSurface` 内的独立 Composable，`testTag` 固定：

| 卡片 | testTag | 内容 |
| --- | --- | --- |
| Greeting | `dashboard:greeting` | `Good Evening` |
| Clock | `dashboard:clock` | `21:04` + `Tue, Apr 22` |
| Weather | `dashboard:weather` | 18° / Partly cloudy / H 21 L 12 / Home / 未来 4 小时 |
| Calendar | `dashboard:calendar` | 三条事件（时间 + 标题） |
| Todo | `dashboard:todo` | 未完成在前，完成后带删除线 |
| PC Summary | `dashboard:pc-summary` | 名称、状态、硬件摘要、CPU/GPU/RAM/NET 四个数值 |
| Decorative | `dashboard:decorative` | 纯几何装饰图形 |

- [ ] **Step 4: 实现 DashboardMode 布局**

```kotlin
@Composable
fun DashboardMode(
    data: DashboardData,
    style: WidgetStyle,
    onPcSummaryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = DashboardLayout.visible(data.widgets).map { it.type }.toSet()
    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            if (WidgetType.Greeting in visible) GreetingWidget(data.greeting, style, Modifier.weight(1f))
            if (WidgetType.Clock in visible) ClockWidget(data.time, data.date, style, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            if (WidgetType.Weather in visible) WeatherWidget(data.weather, style, Modifier.weight(1f))
            if (WidgetType.Calendar in visible) CalendarWidget(data.events, style, Modifier.weight(1f))
            if (WidgetType.Todo in visible) TodoWidget(data.todos, style, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            if (WidgetType.PcSummary in visible) PcSummaryWidget(data.pc, style, onPcSummaryClick, Modifier.weight(1.4f))
            if (WidgetType.Decorative in visible) DecorativeWidget(style, Modifier.weight(1f))
        }
    }
}
```

- [ ] **Step 5: 运行测试并提交**

```bash
./gradlew :composeApp:desktopTest --tests "*DashboardModeTest*"
git add -A
git commit -m "feat: add dashboard mode widgets with mock data"
```

---

### Task 7: 60 秒指标环形缓冲

**Files:**
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/core/metrics/MetricRingBuffer.kt`
- Test: `src/commonTest/kotlin/com/xukunz/wakeupmywall/core/metrics/MetricRingBufferTest.kt`

**Interfaces:**
- Consumes: 无
- Produces:
  - `class MetricRingBuffer(val capacity: Int = 60) { val size: Int; fun add(value: Float); fun values(): List<Float>; fun latest(): Float?; fun average(): Float?; fun min(): Float?; fun max(): Float? }`

- [ ] **Step 1: 写失败测试**

```kotlin
package com.xukunz.wakeupmywall.core.metrics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MetricRingBufferTest {

    @Test
    fun `empty buffer reports nulls`() {
        val buffer = MetricRingBuffer(capacity = 3)
        assertEquals(0, buffer.size)
        assertNull(buffer.latest())
        assertNull(buffer.average())
    }

    @Test
    fun `keeps insertion order until capacity`() {
        val buffer = MetricRingBuffer(capacity = 3)
        listOf(1f, 2f, 3f).forEach(buffer::add)
        assertEquals(listOf(1f, 2f, 3f), buffer.values())
    }

    @Test
    fun `drops oldest value when full`() {
        val buffer = MetricRingBuffer(capacity = 3)
        listOf(1f, 2f, 3f, 4f).forEach(buffer::add)
        assertEquals(listOf(2f, 3f, 4f), buffer.values())
        assertEquals(3, buffer.size)
    }

    @Test
    fun `computes latest average min and max`() {
        val buffer = MetricRingBuffer(capacity = 60)
        listOf(10f, 20f, 30f).forEach(buffer::add)
        assertEquals(30f, buffer.latest())
        assertEquals(20f, buffer.average())
        assertEquals(10f, buffer.min())
        assertEquals(30f, buffer.max())
    }

    @Test
    fun `sixty samples represent sixty seconds`() {
        val buffer = MetricRingBuffer(capacity = 60)
        repeat(60) { buffer.add(it.toFloat()) }
        assertEquals(60, buffer.size)
        assertEquals(59f, buffer.latest())
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*MetricRingBufferTest*"
```

Expected: 编译失败，`Unresolved reference: MetricRingBuffer`。

- [ ] **Step 3: 实现**

```kotlin
package com.xukunz.wakeupmywall.core.metrics

class MetricRingBuffer(val capacity: Int = 60) {
    init { require(capacity > 0) { "capacity must be positive" } }

    private val storage = ArrayDeque<Float>(capacity)

    val size: Int get() = storage.size

    fun add(value: Float) {
        if (storage.size == capacity) storage.removeFirst()
        storage.addLast(value)
    }

    fun values(): List<Float> = storage.toList()
    fun latest(): Float? = storage.lastOrNull()
    fun average(): Float? = if (storage.isEmpty()) null else storage.sum() / storage.size
    fun min(): Float? = storage.minOrNull()
    fun max(): Float? = storage.maxOrNull()
}
```

- [ ] **Step 4: 运行测试并提交**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*MetricRingBufferTest*"
git add -A
git commit -m "feat: add 60 second metric ring buffer"
```

---

### Task 8: Monitor Mode 与曲线卡片

**Files:**
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/ui/monitor/MetricCard.kt`
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/ui/monitor/MetricSparkline.kt`
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/ui/monitor/MonitorMode.kt`
- Test: `src/desktopTest/kotlin/com/xukunz/wakeupmywall/ui/monitor/MonitorModeTest.kt`

**Interfaces:**
- Consumes: `MetricRingBuffer`、`MetricsSnapshot`、`WidgetSurface`
- Produces:
  - `@Composable fun MetricSparkline(values: List<Float>, modifier: Modifier = Modifier)`
  - `@Composable fun MetricCard(label: String, value: String, unit: String, values: List<Float>, style: WidgetStyle, modifier: Modifier = Modifier)`
  - `@Composable fun MonitorMode(metrics: MetricsSnapshot, history: Map<String, List<Float>>, style: WidgetStyle, modifier: Modifier = Modifier)`
  - `object MetricKeys { const val Cpu = "cpu"; const val Gpu = "gpu"; const val Ram = "ram"; const val Storage = "storage"; const val Network = "network" }`

- [ ] **Step 1: 写失败测试**

```kotlin
@OptIn(ExperimentalTestApi::class)
class MonitorModeTest {

    private val history = mapOf(
        MetricKeys.Cpu to List(60) { 20f + it % 10 },
        MetricKeys.Gpu to List(60) { 60f + it % 5 },
        MetricKeys.Ram to List(60) { 38f },
    )

    @Test
    fun `renders the eight v1 metric cards`() = runComposeUiTest {
        setContent { WakeUpMyWallTheme { MonitorMode(MockData.metrics, history, WidgetStyle.Glass) } }
        listOf("metric:cpu", "metric:gpu", "metric:ram", "metric:storage", "metric:temps", "metric:fans", "metric:network", "metric:uptime")
            .forEach { onNodeWithTag(it).assertExists() }
    }

    @Test
    fun `cpu card shows percent value`() = runComposeUiTest {
        setContent { WakeUpMyWallTheme { MonitorMode(MockData.metrics, history, WidgetStyle.Glass) } }
        onNodeWithTag("metric:cpu-value").assertTextEquals("28%")
    }

    @Test
    fun `sparkline renders without history`() = runComposeUiTest {
        setContent { WakeUpMyWallTheme { MonitorMode(MockData.metrics, emptyMap(), WidgetStyle.Glass) } }
        onNodeWithTag("metric:cpu").assertExists()
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

```bash
./gradlew :composeApp:desktopTest --tests "*MonitorModeTest*"
```

Expected: 编译失败，`Unresolved reference: MonitorMode`。

- [ ] **Step 3: 实现曲线**

`MetricSparkline` 用 `Canvas` 画折线；空列表时画一条水平基准线，不得抛异常（对应上面第三个测试）：

```kotlin
@Composable
fun MetricSparkline(values: List<Float>, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier.testTag("sparkline")) {
        if (values.size < 2) {
            drawLine(color = accent.copy(alpha = 0.3f), start = Offset(0f, size.height), end = Offset(size.width, size.height))
            return@Canvas
        }
        val maxValue = values.max().coerceAtLeast(1f)
        val dx = size.width / (values.size - 1)
        values.forEachIndexed { index, value ->
            val x = index * dx
            val y = size.height - (value / maxValue) * size.height
            if (index > 0) {
                val prevX = (index - 1) * dx
                val prevY = size.height - (values[index - 1] / maxValue) * size.height
                drawLine(color = accent, start = Offset(prevX, prevY), end = Offset(x, y), strokeWidth = 2f)
            }
        }
    }
}
```

- [ ] **Step 4: 实现 Monitor Mode 布局**

结构：顶部 `PC 名 + 硬件摘要 + Quick Actions 占位`；四张百分比卡（CPU / GPU / RAM / Storage）各含 `MetricCard` + `MetricSparkline`；下排 `Network / Temps & Fans`；底排 `Uptime / Recent Activity（P2 占位）`。标签与 `testTag` 严格用上表。

- [ ] **Step 5: 运行测试并提交**

```bash
./gradlew :composeApp:desktopTest --tests "*MonitorModeTest*"
git add -A
git commit -m "feat: add monitor mode with metric cards and sparklines"
```

---

### Task 9: Dashboard ↔ Monitor 双模式切换

**Files:**
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/ui/dashboard/HomeSurface.kt`
- Modify: `src/commonMain/kotlin/com/xukunz/wakeupmywall/app/App.kt`
- Test: `src/commonTest/kotlin/com/xukunz/wakeupmywall/ui/dashboard/HomeSurfaceStateTest.kt`
- Test: `src/desktopTest/kotlin/com/xukunz/wakeupmywall/ui/dashboard/HomeSurfaceTest.kt`

**Interfaces:**
- Consumes: `DashboardMode`、`MonitorMode`
- Produces:
  - `enum class HomeMode { Dashboard, Monitor }`
  - `class HomeModeController { val current: StateFlow<HomeMode>; fun show(mode: HomeMode); fun toggle(); fun onSwipeLeft(); fun onSwipeRight() }`（左滑进 Monitor，右滑回 Dashboard）
  - `@Composable fun HomeSurface(mode: HomeMode, dashboard: DashboardData, metrics: MetricsSnapshot, history: Map<String, List<Float>>, style: WidgetStyle, onModeChange: (HomeMode) -> Unit, modifier: Modifier = Modifier)`

- [ ] **Step 1: 写失败测试（控制器）**

```kotlin
class HomeSurfaceStateTest {

    @Test
    fun `starts on dashboard`() {
        assertEquals(HomeMode.Dashboard, HomeModeController().current.value)
    }

    @Test
    fun `swipe left shows monitor and swipe right returns`() {
        val controller = HomeModeController()
        controller.onSwipeLeft()
        assertEquals(HomeMode.Monitor, controller.current.value)
        controller.onSwipeRight()
        assertEquals(HomeMode.Dashboard, controller.current.value)
    }

    @Test
    fun `toggle alternates`() {
        val controller = HomeModeController()
        controller.toggle()
        assertEquals(HomeMode.Monitor, controller.current.value)
    }

    @Test
    fun `swipe right on dashboard stays on dashboard`() {
        val controller = HomeModeController()
        controller.onSwipeRight()
        assertEquals(HomeMode.Dashboard, controller.current.value)
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*HomeSurfaceStateTest*"
```

Expected: 编译失败，`Unresolved reference: HomeModeController`。

- [ ] **Step 3: 实现控制器与 UI 切换**

`HomeSurface` 用 `Modifier.pointerInput` 处理水平拖动（阈值 60dp 判定方向），并在 `DashboardMode` 的 PC Summary 点击回调里 `onModeChange(HomeMode.Monitor)`；`MonitorMode` 顶部提供返回 Dashboard 的入口。

```kotlin
@Composable
fun HomeSurface(
    mode: HomeMode,
    dashboard: DashboardData,
    metrics: MetricsSnapshot,
    history: Map<String, List<Float>>,
    style: WidgetStyle,
    onModeChange: (HomeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount < -60f) onModeChange(HomeMode.Monitor)
                    if (dragAmount > 60f) onModeChange(HomeMode.Dashboard)
                }
            }
            .testTag("home:surface"),
    ) {
        when (mode) {
            HomeMode.Dashboard -> DashboardMode(
                data = dashboard,
                style = style,
                onPcSummaryClick = { onModeChange(HomeMode.Monitor) },
            )
            HomeMode.Monitor -> MonitorMode(metrics = metrics, history = history, style = style)
        }
    }
}
```

- [ ] **Step 4: 写 UI 测试**

```kotlin
@OptIn(ExperimentalTestApi::class)
class HomeSurfaceTest {

    @Test
    fun `pc summary click switches to monitor`() = runComposeUiTest {
        var mode = HomeMode.Dashboard
        setContent {
            WakeUpMyWallTheme {
                HomeSurface(mode, dashboardData, MockData.metrics, emptyMap(), WidgetStyle.Glass, { mode = it })
            }
        }
        onNodeWithTag("dashboard:pc-summary").performClick()
        assertEquals(HomeMode.Monitor, mode)
    }

    @Test
    fun `monitor mode renders metric cards`() = runComposeUiTest {
        setContent {
            WakeUpMyWallTheme {
                HomeSurface(HomeMode.Monitor, dashboardData, MockData.metrics, emptyMap(), WidgetStyle.Glass, {})
            }
        }
        onNodeWithTag("metric:cpu").assertExists()
    }
}
```

- [ ] **Step 5: 运行测试并提交**

```bash
./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest
git add -A
git commit -m "feat: add dashboard monitor mode switching"
```

---

### Task 10: Settings Workspace 外壳

**Files:**
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/ui/settings/SettingsWorkspace.kt`
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/ui/settings/SettingsSection.kt`
- Test: `src/commonTest/kotlin/com/xukunz/wakeupmywall/ui/settings/SettingsSectionTest.kt`
- Test: `src/desktopTest/kotlin/com/xukunz/wakeupmywall/ui/settings/SettingsWorkspaceTest.kt`

**Interfaces:**
- Consumes: `AppShell`
- Produces:
  - `enum class SettingsSection(val title: String) { DeviceSetup("Device Setup"), Integrations("Integrations"), Display("Display & Behavior"), Appearance("Appearance"), Notifications("Notifications"), Backup("Backup & Sync"), About("About") }`
  - `@Composable fun SettingsWorkspace(section: SettingsSection, onSectionChange: (SettingsSection) -> Unit, content: @Composable BoxScope.(SettingsSection) -> Unit, modifier: Modifier = Modifier)`
  - `@Composable fun ResetToDefaultRow(onReset: () -> Unit, modifier: Modifier = Modifier)`

- [ ] **Step 1: 写失败测试**

```kotlin
class SettingsSectionTest {

    @Test
    fun `navigation order matches spec`() {
        assertEquals(
            listOf("Device Setup", "Integrations", "Display & Behavior", "Appearance", "Notifications", "Backup & Sync", "About"),
            SettingsSection.entries.map { it.title },
        )
    }

    @Test
    fun `device setup is the default section`() {
        assertEquals(SettingsSection.DeviceSetup, SettingsSection.entries.first())
    }
}
```

```kotlin
@OptIn(ExperimentalTestApi::class)
class SettingsWorkspaceTest {

    @Test
    fun `renders all navigation entries and forwards selection`() = runComposeUiTest {
        var selected = SettingsSection.DeviceSetup
        setContent {
            WakeUpMyWallTheme {
                SettingsWorkspace(SettingsSection.DeviceSetup, { selected = it }) { section ->
                    Text(section.title, modifier = Modifier.testTag("settings:content"))
                }
            }
        }
        SettingsSection.entries.forEach { onNodeWithTag("settings:nav:${it.name}").assertExists() }
        onNodeWithTag("settings:nav:Appearance").performClick()
        assertEquals(SettingsSection.Appearance, selected)
    }

    @Test
    fun `reset to default is available`() = runComposeUiTest {
        var reset = false
        setContent { WakeUpMyWallTheme { ResetToDefaultRow { reset = true } } }
        onNodeWithTag("settings:reset").performClick()
        assertTrue(reset)
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

```bash
./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest --tests "*Settings*"
```

Expected: 编译失败，`Unresolved reference: SettingsWorkspace`。

- [ ] **Step 3: 实现**

左列固定宽度约 240dp 的 `Column` 放七个 `SettingsSection` 的 `NavigationDrawerItem` 风格按钮（`testTag("settings:nav:<name>")`）与底部 `ResetToDefaultRow`（`testTag("settings:reset")`）；右侧为 `content` 插槽。外层仍由 `AppShell` 提供 Power Rail。

- [ ] **Step 4: 运行测试并提交**

```bash
./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest --tests "*Settings*"
git add -A
git commit -m "feat: add settings workspace shell"
```

---

### Task 11: Device Setup 表单与校验

**Files:**
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/domain/usecase/DeviceSetupValidator.kt`
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/ui/settings/DeviceSetupScreen.kt`
- Test: `src/commonTest/kotlin/com/xukunz/wakeupmywall/domain/DeviceSetupValidatorTest.kt`
- Test: `src/desktopTest/kotlin/com/xukunz/wakeupmywall/ui/settings/DeviceSetupScreenTest.kt`

**Interfaces:**
- Consumes: `MacAddress`
- Produces:
  - `data class DeviceSetupInput(val name: String, val mac: String, val ip: String, val broadcast: String, val wolPort: String, val agentPort: String, val agentHost: String)`
  - `data class DeviceSetupResult(val isValid: Boolean, val errors: Map<String, String>, val device: PcDevice?)`
  - `object DeviceSetupValidator { fun validate(input: DeviceSetupInput): DeviceSetupResult }`
  - `@Composable fun DeviceSetupScreen(input: DeviceSetupInput, result: DeviceSetupResult, onInputChange: (DeviceSetupInput) -> Unit, onSave: () -> Unit, onTestConnection: () -> Unit, modifier: Modifier = Modifier)`

- [ ] **Step 1: 写失败测试**

```kotlin
class DeviceSetupValidatorTest {

    private val valid = DeviceSetupInput(
        name = "My PC",
        mac = "AA:BB:CC:DD:EE:FF",
        ip = "192.168.1.10",
        broadcast = "192.168.1.255",
        wolPort = "9",
        agentPort = "9876",
        agentHost = "192.168.1.10",
    )

    @Test
    fun `valid input produces device`() {
        val result = DeviceSetupValidator.validate(valid)
        assertTrue(result.isValid)
        assertEquals("AA:BB:CC:DD:EE:FF", result.device?.macAddress?.normalized)
        assertEquals(9, result.device?.wolPort)
        assertEquals(9876, result.device?.agentPort)
    }

    @Test
    fun `missing name is rejected`() {
        val result = DeviceSetupValidator.validate(valid.copy(name = "  "))
        assertFalse(result.isValid)
        assertEquals("Name is required", result.errors["name"])
    }

    @Test
    fun `invalid mac is rejected`() {
        val result = DeviceSetupValidator.validate(valid.copy(mac = "ZZ:BB:CC:DD:EE:FF"))
        assertEquals("Enter a valid MAC address", result.errors["mac"])
    }

    @Test
    fun `invalid broadcast address is rejected`() {
        val result = DeviceSetupValidator.validate(valid.copy(broadcast = "999.1.1.1"))
        assertEquals("Enter a valid IPv4 address", result.errors["broadcast"])
    }

    @Test
    fun `port out of range is rejected`() {
        assertFalse(DeviceSetupValidator.validate(valid.copy(wolPort = "0")).isValid)
        assertFalse(DeviceSetupValidator.validate(valid.copy(agentPort = "70000")).isValid)
        assertFalse(DeviceSetupValidator.validate(valid.copy(agentPort = "abc")).isValid)
    }

    @Test
    fun `agent host is optional but must be valid when provided`() {
        assertTrue(DeviceSetupValidator.validate(valid.copy(agentHost = "")).isValid)
        assertFalse(DeviceSetupValidator.validate(valid.copy(agentHost = "http://bad host")).isValid)
    }

    @Test
    fun `multiple errors are reported together`() {
        val result = DeviceSetupValidator.validate(valid.copy(name = "", mac = "x", wolPort = "0"))
        assertEquals(setOf("name", "mac", "wolPort"), result.errors.keys)
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*DeviceSetupValidatorTest*"
```

Expected: 编译失败，`Unresolved reference: DeviceSetupValidator`。

- [ ] **Step 3: 实现校验器**

规则（写死为可读常量，供 UI 复用）：

```kotlin
object DeviceSetupValidator {
    private const val NAME_REQUIRED = "Name is required"
    private const val MAC_INVALID = "Enter a valid MAC address"
    private const val IP_INVALID = "Enter a valid IPv4 address"
    private const val PORT_INVALID = "Port must be between 1 and 65535"

    fun validate(input: DeviceSetupInput): DeviceSetupResult {
        val errors = buildMap {
            if (input.name.isBlank()) put("name", NAME_REQUIRED)
            if (MacAddress.parse(input.mac) == null) put("mac", MAC_INVALID)
            if (!isValidIpv4(input.broadcast)) put("broadcast", IP_INVALID)
            if (input.ip.isNotBlank() && !isValidIpv4(input.ip)) put("ip", IP_INVALID)
            if (!isValidPort(input.wolPort)) put("wolPort", PORT_INVALID)
            if (!isValidPort(input.agentPort)) put("agentPort", PORT_INVALID)
            if (input.agentHost.isNotBlank() && !isValidHost(input.agentHost)) put("agentHost", "Enter a valid host")
        }
        val device = if (errors.isEmpty()) {
            PcDevice(
                id = input.name.trim().lowercase().replace(' ', '-'),
                name = input.name.trim(),
                macAddress = MacAddress.parse(input.mac),
                ipAddress = input.ip.ifBlank { null },
                broadcastAddress = input.broadcast.trim(),
                wolPort = input.wolPort.toInt(),
                agentHost = input.agentHost.ifBlank { null },
                agentPort = input.agentPort.toInt(),
            )
        } else {
            null
        }
        return DeviceSetupResult(errors.isEmpty(), errors, device)
    }

    private fun isValidIpv4(value: String): Boolean {
        val parts = value.trim().split('.')
        return parts.size == 4 && parts.all { part ->
            val number = part.toIntOrNull()
            number != null && number in 0..255 && part.none { it == '+' } && (part.length == 1 || !part.startsWith("0"))
        }
    }

    private fun isValidPort(value: String): Boolean = value.toIntOrNull()?.let { it in 1..65535 } ?: false

    private fun isValidHost(value: String): Boolean =
        value.none { it.isWhitespace() || it == '/' } && isValidIpv4(value) || value.matches(Regex("[A-Za-z0-9.-]+"))
}
```

注意：`isValidHost` 的两个条件必须用括号明确优先级，写成 `!value.any { ... } && (isValidIpv4(value) || value.matches(...))`。

- [ ] **Step 4: 实现表单 UI**

六个输入框（PC Name / MAC / IP / Broadcast / WOL Port / Agent Port / Agent Host），每个下方在其 `errors` 非空时显示错误文案；底部两个按钮 `Save`（`testTag("device:save")`，`enabled = result.isValid`）与 `Test Connection`（`testTag("device:test")`）。

- [ ] **Step 5: 写 UI 测试**

```kotlin
@OptIn(ExperimentalTestApi::class)
class DeviceSetupScreenTest {

    @Test
    fun `invalid form disables save and shows mac error`() = runComposeUiTest {
        val input = DeviceSetupInput("My PC", "bad", "192.168.1.10", "192.168.1.255", "9", "9876", "192.168.1.10")
        val result = DeviceSetupValidator.validate(input)
        setContent { WakeUpMyWallTheme { DeviceSetupScreen(input, result, {}, {}, {}) } }
        onNodeWithTag("device:save").assertIsNotEnabled()
        onNodeWithTag("device:error:mac").assertTextEquals("Enter a valid MAC address")
    }

    @Test
    fun `valid form enables save`() = runComposeUiTest {
        val input = DeviceSetupInput("My PC", "AA:BB:CC:DD:EE:FF", "192.168.1.10", "192.168.1.255", "9", "9876", "192.168.1.10")
        setContent { WakeUpMyWallTheme { DeviceSetupScreen(input, DeviceSetupValidator.validate(input), {}, {}, {}) } }
        onNodeWithTag("device:save").assertIsEnabled()
    }
}
```

- [ ] **Step 6: 运行测试并提交**

```bash
./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest
git add -A
git commit -m "feat: add device setup form with validation"
```

---

### Task 12: Appearance 与 Live Preview

**Files:**
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/ui/settings/AppearanceScreen.kt`
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/data/mock/BuiltInWallpapers.kt`
- Test: `src/commonTest/kotlin/com/xukunz/wakeupmywall/ui/settings/AppearanceStateTest.kt`
- Test: `src/desktopTest/kotlin/com/xukunz/wakeupmywall/ui/settings/AppearanceScreenTest.kt`

**Interfaces:**
- Consumes: `AppearanceSettings`、`ThemeAccent`、`WidgetStyle`、`DashboardMode`
- Produces:
  - `data class AppearanceState(val accent: ThemeAccent, val widgetStyle: WidgetStyle, val wallpaperId: String, val transparency: Float, val fontScale: Float, val widgets: List<DashboardWidget>)`
  - `object AppearanceReducer { fun toggleWidget(state: AppearanceState, id: String): AppearanceState; fun setAccent(state: AppearanceState, accent: ThemeAccent): AppearanceState; fun setWidgetStyle(state: AppearanceState, style: WidgetStyle): AppearanceState; fun setWallpaper(state: AppearanceState, id: String): AppearanceState }`
  - `object BuiltInWallpapers { val ids: List<String> }`（`mountain` / `forest` / `city-night` / `cozy-room` / `minimal` / `abstract`）
  - `@Composable fun AppearanceScreen(state: AppearanceState, onStateChange: (AppearanceState) -> Unit, preview: @Composable (AppearanceState) -> Unit, modifier: Modifier = Modifier)`

- [ ] **Step 1: 写失败测试**

```kotlin
class AppearanceStateTest {

    private val state = AppearanceState(
        accent = ThemeAccent.AuroraBlue,
        widgetStyle = WidgetStyle.Glass,
        wallpaperId = "mountain",
        transparency = 0.35f,
        fontScale = 1f,
        widgets = DashboardLayout.default,
    )

    @Test
    fun `toggle widget flips enabled flag`() {
        val updated = AppearanceReducer.toggleWidget(state, "weather")
        assertFalse(updated.widgets.first { it.id == "weather" }.enabled)
    }

    @Test
    fun `set accent replaces accent only`() {
        val updated = AppearanceReducer.setAccent(state, ThemeAccent.Emerald)
        assertEquals(ThemeAccent.Emerald, updated.accent)
        assertEquals(state.widgets, updated.widgets)
    }

    @Test
    fun `set wallpaper validates against built in list`() {
        assertEquals("forest", AppearanceReducer.setWallpaper(state, "forest").wallpaperId)
        assertEquals("mountain", AppearanceReducer.setWallpaper(state, "unknown").wallpaperId)
    }

    @Test
    fun `built in wallpapers match spec list`() {
        assertEquals(
            listOf("mountain", "forest", "city-night", "cozy-room", "minimal", "abstract"),
            BuiltInWallpapers.ids,
        )
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*AppearanceStateTest*"
```

Expected: 编译失败，`Unresolved reference: AppearanceState`。

- [ ] **Step 3: 实现状态与归约器**

`AppearanceReducer` 全部为纯函数（`copy` 返回新状态）；`setWallpaper` 对非法 id 保持原值，不抛异常。

- [ ] **Step 4: 实现 UI（含 Live Preview）**

布局：左侧控件列（Wallpaper 网格 6 项、Accent 6 个色点、Widget Style 三选一、Transparency 滑杆、Font Scale 滑杆、Widget 显隐开关列表），中间 `Live Preview` 区域直接渲染 `DashboardMode`（用 `WidgetStyle` 与 `accent` 参数驱动，`testTag("appearance:preview")`），底部 `Reset to Default` 复用 `ResetToDefaultRow`。

- [ ] **Step 5: 写 UI 测试**

```kotlin
@OptIn(ExperimentalTestApi::class)
class AppearanceScreenTest {

    @Test
    fun `preview renders dashboard widgets`() = runComposeUiTest {
        setContent {
            WakeUpMyWallTheme {
                AppearanceScreen(state, {}) { previewState ->
                    Text(previewState.wallpaperId, modifier = Modifier.testTag("appearance:preview"))
                }
            }
        }
        onNodeWithTag("appearance:preview").assertTextEquals("mountain")
    }

    @Test
    fun `accent swatch selection is forwarded`() = runComposeUiTest {
        var latest: AppearanceState? = null
        setContent { WakeUpMyWallTheme { AppearanceScreen(state, { latest = it }) { } } }
        onNodeWithTag("appearance:accent:Emerald").performClick()
        assertEquals(ThemeAccent.Emerald, latest?.accent)
    }
}
```

- [ ] **Step 6: 运行测试并提交**

```bash
./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest
git add -A
git commit -m "feat: add appearance screen with live preview"
```

---

## Phase 1 完成标准

1. `./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest` 全绿，且新增测试覆盖：Power Rail 五种状态、Dashboard 卡片显隐、Ring Buffer 边界、设备表单校验、外观状态归约。
2. 五个核心界面（Dashboard Mode、PC Monitor、Settings、Appearance、Power Rail）在 desktop 预览与 Android 真机上均能渲染，横向 72/28 布局不溢出。
3. 概念图比对结论记录到 `docs/plans/phase1-visual-review.md`：逐页列出"已还原 / 有偏差"，偏差项写明是留到哪个 Phase。
4. 全部数值来自 `data/mock/MockData.kt`；`grep -rn "0x[0-9A-Fa-f]\{8\}" mobile/composeApp/src/commonMain/kotlin/com/xukunz/wakeupmywall/ui` 只应命中令牌文件。

## 自检结果（写作时执行）

| 检查项 | 结果 |
| --- | --- |
| 规范覆盖 | 规范 §2 三工作空间 → Task 5/9/10；§3 状态机 → Task 4；§7 页面清单 → Task 6/8；§8 指标与 60 秒曲线 → Task 7/8；§9 显示默认值中的亮度/像素位移 → Phase 8；§10 架构约束 → Phase 0 |
| 类型一致性 | `PowerRailModel` 由 Phase 0 的 `PcState.capabilities` 派生，Task 5/6/9/12 复用同一模型，无重复定义 |
| 占位符扫描 | 无 TBD；两处刻意留到后续阶段的项（`DecorativeWidget` 造型打磨、Recent Activity）已标注 P2 并归入 Phase 8 |
| 已知取舍 | 不引入图标库与 Navigation 库；文案与图形用文字/几何占位，视觉打磨放在 Phase 1 末尾的视觉复核 |
