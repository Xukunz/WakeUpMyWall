# Phase 1：Design System 与 Mock UI 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 用 Mock Data 把 Dashboard（含 Dashboard/Monitor 双模式）、Power Rail、Settings Workspace、Appearance 五个核心页面还原到概念图的 80–90%，并把视觉规则固化进 Design System 与可复用的 WidgetSurface 抽象。

**Architecture:** 所有页面由 `AppShell`（左 72% 主内容 + 右 28% Power Rail）承载；Power Rail 是唯一常驻组件，状态完全由 `PcState` + `PcCapabilities` 驱动，不允许在 UI 里写死按钮可用性。卡片统一经 `WidgetSurface` 渲染，风格由 `WidgetStyle` 决定。指标曲线数据来自 `MetricRingBuffer`（纯逻辑，可测）。

**Tech Stack:** Compose Multiplatform 1.10.3（`commonMain` 全部 UI）、kotlin-test、`compose.uiTest`（desktopTest 中运行）、Phase 0 的 domain / core 骨架。

**Spec:** [docs/superpowers/specs/2026-09-19-desktop-companion-design.md](../specs/2026-09-19-desktop-companion-design.md)
**概念图审阅（本计划的还原依据）：** [docs/design/2026-09-19-concept-review.md](../../design/2026-09-19-concept-review.md)
**原始图片：** `imgs/concept/`（10 张，去重后 5 个屏幕）

**前置：** Phase 0 全部任务完成，`./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest` 全绿。

## Global Constraints

- 本阶段**只用 Mock Data**：不接 WOL、不接 Agent、不发任何网络请求（`AgentApi` 只保留单测覆盖）。
- 横屏锁定：所有布局按 `Row` + 权重 72/28 设计，不写竖屏分支。
- 颜色、间距、圆角、字号只能取自 `core/theme`；Composable 内禁止出现字面量 `Color(0x...)` 或裸 `dp` 值（测试与令牌文件除外）。
- Power Rail 的按钮可用性只能来自 `PcState.capabilities(device)`，禁止 `if (state == ONLINE)` 这类散落判断。
- 每个 UI 元素必须有 `testTag`，命名规则 `区域:元素`（如 `powerrail:primary`、`dashboard:weather`）。
- 每笔提交前 `./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest` 必须全绿。
- 本阶段不引入图标库与字体资源；用文字与几何图形完成布局验证，图标在视觉打磨阶段统一替换。
- **响应式断点**：主区可用宽度 ≥ 1000dp 用概念图列数；600–1000dp 降为 3 列；< 600dp 用 2 列并允许纵向滚动。窄屏禁止强排 5 列。
- **不实现 Mockup 装饰**：概念图中的 `9:41`、信号、电量属于手机系统状态栏；右下角 `v1.0` 是图片水印。二者都不得写进 UI。
- **品牌资源**：Quick Actions 中的 Discord / Steam / Spotify 图标属于第三方商标，不得内置；用通用几何字形 + 文字标签代替。
- **装饰层可关闭**：`SAME ROOM / DIFFERENT / PERSPECTIVE`、`A MORE FOCUSED TOMORROW`、引用卡属于可开关的装饰层（规范 D10），默认开启，开关接入 Appearance。
- **主页三形态**：Dashboard / Monitor / StandBy 共用同一个 Power Rail（StandBy 除外，它用浮层卡）；StandBy 由 Task 13 实现。

---

## 概念图还原规格（权威）

**若后续任务描述与本节冲突，以本节为准。** 每一项都来自 `imgs/concept` 的实际像素内容。

### A. Power Rail（28%）

| 序号 | 元素 | 规格 |
| --- | --- | --- |
| A1 | 标题区 | `My PC`（约 24sp）+ 副标 `POWER CONTROL`（全大写、字距加宽、次要色）；右上齿轮进入 Settings |
| A2 | 状态行 | 状态色点 + 文案，取 `PcCapabilities.statusText` 的短形态（`Ready to wake` / `Online` / `Waking PC…`） |
| A3 | 主电源环 | 直径约 220dp 圆形按钮；环粗约 3dp，用状态色；外部柔和发光；中央为电源字形；下方 `Power On` + `WAKE YOUR PC` |
| A4 | 次级动作 | 三枚等宽按钮并排：`Sleep`/`SNAP MODE`、`Shut Down`/`POWER OFF`、`Restart`/`FRESH START`，每枚含图标位 |
| A5 | 连接条 | 通栏圆角条 + 图标 + 通道文案 + 箭头；文案随状态为 `Wake-on-LAN Ready` 或 `Agent connected over LAN` |
| A6 | 装饰卡 | 引用卡，可在 Appearance 关闭（仅部分屏幕显示，如 Device Setup） |

### B. Dashboard Mode

| 行 | 槽位 | 内容 |
| --- | --- | --- |
| 1 | 左 | `Good Evening`（`Evening` 用 accent 色）+ `A CALMER DESKTOP. A BRIGHTER YOU.` |
| 1 | 右 | `SAME ROOM / DIFFERENT / PERSPECTIVE`（三行、右对齐、低透明度） |
| 2 | 1 | Weather：图标、`18°`、`Partly Cloudy`、`↑22° ↓14°`、`📍Riverside, CA`、四列逐时（10PM 17° / 1AM 16° / 4AM 15° / 7AM 16°） |
| 2 | 2 | Calendar：`Tue, Apr 22` + `This Week` + `+`；周条 `S M T W T F S` 日期 `20–26`，选中日实心圆；事件三条（青/琥珀/红圆点 + 时间 + 标题） |
| 2 | 3 | My Tasks：`My Tasks` + `3 of 5`；五行任务（完成项实心勾 + 删除线，未完成项空心圈）；`+ Add a task` |
| 3 | 1 | My PC 摘要（宽）：绿点 + `My PC` + `Online` + `Last seen 1 min ago`；缩略图；`CPU 12%`/`Temp 42°C`/`RAM 38%`/`Network ↓12.4 Mbps ↑3.1 Mbps` 各带彩色细条；`>` 进入 Monitor |
| 3 | 2 | 引用装饰卡：衬线两行 + 分割线 + `SAME PROGRESS A BRIGHTER TOMORROW` |
| 底 | 通栏 | `A MORE FOCUSED TOMORROW` + 细分割线 |

### C. PC Monitor

| 行 | 内容 |
| --- | --- |
| 1 | My PC 身份卡（缩略图 + `DESKTOP-ALPHA` + `Windows 11 Pro` + `AMD Ryzen 7 7700X` + `NVIDIA GeForce RTX 4070 Ti` + `>`）· Quick Actions（`+` + 四个动作块：Open Browser / Open Discord / Launch Steam / Open Spotify） |
| 2 | CPU `28%`（Ryzen 7 7700X；`4.9 GHz`、`8 cores 16 threads`）· GPU `62%`（RTX 4070 Ti；`67 °C`、`8.1 / 12 GB VRAM`）· RAM `38%`（32 GB DDR5；`12.1 / 32 GB`）· Storage `54%`（2 TB NVMe SSD；`1.1 / 2.0 TB`、`554 GB free`）· System Temps（CPU 68°C / GPU 67°C / Motherboard 42°C / SSD 38°C + Fans：CPU Fan 1,240 RPM / GPU Fan 1,560 RPM / Case Fans 820 RPM） |
| 3 | Network（`↓124.3 Mbps`、`↑31.7 Mbps` + 曲线）· Uptime（`3d 6h 24m` / `Since Apr 18, 2025`）· Recent Activity（Edge 5 min ago / Steam 12 min ago / VS Code 28 min ago / Spotify 1 hr ago）· 引用装饰卡 |

指标卡统一结构：图标 + 名称 + 型号小字 → **环形进度（中心百分数）** → sparkline → 两行数值。

### D. StandBy（Task 13）

超大时钟（`9:41`，分钟用 accent 色）+ `PM`；下一行长日期 `Tuesday, April 22`；Weather 卡（含 `Clearer skies later tonight.`）+ Next Event 卡（`In 1 hr 19 min`、`Team sync`、`11:00 PM – 12:00 AM`、`Microsoft Teams`）；右侧 PC 浮层卡（标题 + 环 + `Power On` + `Night Mode`/`ON` + `Auto-Dim`/`ACTIVE`）；底部通栏 PC 状态条。

### E. Settings（左导航，方案 A）

左导航条目固定为：`Device Setup`、`Integrations`、`Notifications`、`Appearance`、`Backup & Sync`、`About`，底部 `Reset to Default`。

Device Setup 内容：Wake-on-LAN Configuration（PC Name / MAC Address / Broadcast IP / Port 步进器 + `Test Connection` + `WOL Ready` / `Last tested 2 min ago`）· Saved Computers（4 台设备：My PC `00:1A:2B:3C:4D:5E` Default、Living Room PC `00:1B:44:11:3A:9C`、Workstation `00:25:96:AF:12:BC`、Media Server `00:16:3E:70:9F:21` + `Add Device`）· Integrated Services（Weather Provider `OpenWeatherMap` + Location `Riverside, CA`；Calendar Source `Android Calendar`；Task Source `Local`；各带 `Show on home screen` 开关）。

### F. Wallpaper & Personalization（Task 12）

底部控制条四段：Wallpaper（`Dusk Lake` 默认带对勾 + Mountains / Forest / City Night / Cozy Room / Minimal / Abstract + `+` More）· Theme & Accent（6 色点，首个 `Aurora Blue`）· Widget Style（Glass 默认 / Solid / Minimal，各带小卡预览）· Appearance（Card Transparency 70%、Font Scale 100%、Layout Preset Default / Compact / Minimal）。

---

## File Structure

| 路径 | 职责 |
| --- | --- |
| `core/theme/Typography.kt` | 字号与字重令牌（标题/正文/数值/单位） |
| `core/theme/Shape.kt` | 圆角令牌（卡片 / 按钮 / 徽标） |
| `ui/components/WidgetSurface.kt` | Glass / Solid / Minimal 三种卡片承载 |
| `ui/components/WidgetStyle.kt` | `enum class WidgetStyle` 与透明度量表 |
| `ui/components/SectionHeader.kt` | 卡片标题行（标题 + 右侧动作） |
| `ui/components/BrandStrip.kt` | 品牌条 `A MORE FOCUSED TOMORROW` + 装饰文案块 |
| `ui/components/QuoteCard.kt` | 引用装饰卡（可开关） |
| `ui/components/Breakpoints.kt` | 响应式断点（列数决策，纯逻辑可测） |
| `ui/powerrail/PowerRail.kt` | 常驻 PC 控制栏（A1–A6 构成） |
| `ui/powerrail/PowerRingButton.kt` | 环形电源主按钮（含发光与状态色） |
| `ui/powerrail/PowerRailState.kt` | Rail 的展示模型（由 `PcState` 映射而来） |
| `ui/dashboard/DashboardShell.kt` | 72/28 外壳 |
| `ui/dashboard/DashboardMode.kt` | StandBy 信息模式 |
| `ui/dashboard/widgets/*.kt` | Greeting / Clock / Weather / Calendar / Todo / PcSummary / Decorative |
| `ui/standby/StandByMode.kt` | 时钟浮层模式 |
| `ui/monitor/MonitorMode.kt` | 硬件监控模式 |
| `ui/monitor/MetricCard.kt`、`MetricSparkline.kt` | 指标卡片与 60 秒曲线 |
| `ui/monitor/ProgressRing.kt`、`QuickActionsCard.kt` | 环形进度与快捷动作 |
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

- [x] **Step 1: 实现排版令牌**

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

- [x] **Step 2: 实现形状令牌**

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

- [x] **Step 3: 接入主题**

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

- [x] **Step 4: 验证并提交**

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

- [x] **Step 1: 写失败测试**

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

- [x] **Step 2: 运行测试，确认失败**

```bash
./gradlew :composeApp:desktopTest --tests "*WidgetSurfaceTest*"
```

Expected: 编译失败，`Unresolved reference: WidgetSurface`。

- [x] **Step 3: 实现**

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

- [x] **Step 4: 运行测试并提交**

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

- [x] **Step 1: 写失败测试**

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

- [x] **Step 2: 运行测试，确认失败**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*DashboardWidgetTest*"
```

Expected: 编译失败，`Unresolved reference: DashboardLayout`。

- [x] **Step 3: 实现 Widget 抽象**

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

- [x] **Step 4: 实现 Mock 数据**

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

- [x] **Step 5: 运行测试并提交**

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

- [x] **Step 1: 写失败测试（状态映射）**

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

- [x] **Step 2: 运行测试，确认失败**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*PowerRailStateTest*"
```

Expected: 编译失败，`Unresolved reference: powerRailModel`。

- [x] **Step 3: 实现状态映射**

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

- [x] **Step 4: 实现 Power Rail 骨架**

先按可用性规则搭结构：`Column`（标题区 + 状态行 + 主按钮 + 三个次级按钮 + 连接条 + Settings 入口），每个可交互元素带 `testTag("powerrail:...")`；`enabled` 一律绑定模型字段；`Spacing` 只取令牌。**Step 4b 会把它升级为概念图的最终构成。**

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

- [x] **Step 4b: 升级为概念图构成（A1–A6）**

替换 Step 4 的布局，得到标题区 + 状态行 + 环形主按钮 + 三枚双行次级按钮 + 连接条。为此新增模型字段与环形组件。

`PowerRailModel` 增加四个字段（`PowerRailState.kt`）：

```kotlin
data class PowerRailModel(
    val pcName: String,
    val subtitle: String,          // "POWER CONTROL" / "DESKTOP COMPANION"
    val stateLabel: String,        // "Ready to wake" / "Online" / "Waking PC…"
    val primaryLabel: String,      // "Power On" / "Waking…" / "Setup PC"
    val primaryCaption: String,    // "WAKE YOUR PC" / "WAITING FOR AGENT"
    val primaryEnabled: Boolean,
    val canSleep: Boolean,
    val canShutdown: Boolean,
    val canRestart: Boolean,
    val connectionLabel: String,   // "Wake-on-LAN Ready" / "Agent connected over LAN"
    val statusLine: String,
)
```

`powerRailModel` 里把 `stateLabel` 映射为状态短句、`primaryCaption` 映射为全大写副标、`connectionLabel` 按 `ONLINE` / 其他状态二选一。**这是唯一允许决定连接条文案的地方。**

`PowerRingButton.kt`：

```kotlin
@Composable
fun PowerRingButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    diameter: Dp = 220.dp,
) {
    val ringColor = if (enabled) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    Box(
        modifier = modifier
            .size(diameter)
            .clip(CircleShape)
            .drawBehind {
                val stroke = 3.dp.toPx()
                drawCircle(color = ringColor.copy(alpha = 0.18f), radius = size.minDimension / 2 - stroke, style = Stroke(stroke * 4))
                drawCircle(color = ringColor, radius = size.minDimension / 2 - stroke, style = Stroke(stroke))
            }
            .clickable(enabled = enabled, onClick = onClick)
            .testTag("powerrail:primary"),
        contentAlignment = Alignment.Center,
    ) {
        Text("⏻", style = MaterialTheme.typography.displayLarge, color = ringColor)
    }
}
```

（`⏻` 只是过渡期的字形占位，视觉打磨阶段换成矢量路径；不要为它引入图标库。）

最终 `PowerRail` 结构：

```kotlin
Column(modifier = modifier.fillMaxHeight().padding(Spacing.lg).testTag("powerrail"), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
    // A1 标题区 + 齿轮
    // A2 状态行：状态色点 + model.stateLabel
    PowerRingButton(enabled = model.primaryEnabled, onClick = onPrimary)
    // 主标签 model.primaryLabel + 副标 model.primaryCaption
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        RailAction("Sleep", "SNAP MODE", model.canSleep, onSleep, "powerrail:sleep")
        RailAction("Shut Down", "POWER OFF", model.canShutdown, onShutdown, "powerrail:shutdown")
        RailAction("Restart", "FRESH START", model.canRestart, onRestart, "powerrail:restart")
    }
    // A5 连接条：model.connectionLabel + 箭头
    // A6 可选引用卡（由参数 quoteCard: Boolean 控制，默认 false）
}
```

`RailAction(label, caption, enabled, onClick, tag)` 是一个私有 Composable：`OutlinedButton` + 上下两行文字（标签 + 全大写副标）。

- [x] **Step 5: 写 UI 测试（逐状态断言）**

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

    @Test
    fun `connection label follows the state machine terminology`() = runComposeUiTest {
        setContent { WakeUpMyWallTheme { PowerRail(model = powerRailModel(PcState.WOL_READY, device), {}, {}, {}, {}, {}) } }
        onNodeWithTag("powerrail:connection").assertTextEquals("Wake-on-LAN Ready")
    }

    @Test
    fun `online connection label never mentions wake on lan`() = runComposeUiTest {
        setContent { WakeUpMyWallTheme { PowerRail(model = powerRailModel(PcState.ONLINE, device), {}, {}, {}, {}, {}) } }
        onNodeWithTag("powerrail:connection").assertTextEquals("Agent connected over LAN")
    }
}
```

> `powerrail:connection` 的文案由 `PowerRailModel.connectionLabel` 唯一决定；任何 `Connected via Wake-on-LAN` 文案都视为回归缺陷（规范 §3 术语规则）。

- [x] **Step 6: 运行测试并提交**

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

- [x] **Step 1: 写失败测试**

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

- [x] **Step 2: 运行测试，确认失败**

```bash
./gradlew :composeApp:desktopTest --tests "*AppShellTest*"
```

Expected: 编译失败，`Unresolved reference: AppShell`。

- [x] **Step 3: 实现**

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

- [x] **Step 4: `App.kt` 接入 AppShell**

`App()` 改为：持有 `PcState`（Phase 1 用 `remember { mutableStateOf(PcState.ONLINE) }` 与 Mock 设备）、构造 `powerRailModel`、把 `RailEvent.Settings` 导航到 `Workspace.Settings`，其余事件暂时只更新本地状态（真实调用在 Phase 3/4）。

- [x] **Step 5: 运行测试并提交**

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

- [x] **Step 1: 写失败测试**

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
    fun `renders greeting weather calendar todo and pc summary`() = runComposeUiTest {
        setContent { WakeUpMyWallTheme { DashboardMode(data, WidgetStyle.Glass, {}) } }
        listOf("dashboard:greeting", "dashboard:weather", "dashboard:calendar", "dashboard:todo", "dashboard:pc-summary")
            .forEach { onNodeWithTag(it).assertIsDisplayed() }
    }

    @Test
    fun `quote card brand strip and perspective mark are rendered`() = runComposeUiTest {
        setContent { WakeUpMyWallTheme { DashboardMode(data, WidgetStyle.Glass, {}) } }
        onNodeWithTag("dashboard:quote").assertIsDisplayed()
        onNodeWithTag("dashboard:brand").assertIsDisplayed()
        onNodeWithTag("dashboard:perspective").assertIsDisplayed()
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

- [x] **Step 2: 运行测试，确认失败**

```bash
./gradlew :composeApp:desktopTest --tests "*DashboardModeTest*"
```

Expected: 编译失败，`Unresolved reference: DashboardMode`。

- [x] **Step 3: 实现各卡片**

每张卡片都是 `WidgetSurface` 内的独立 Composable，`testTag` 固定：

| 卡片 | testTag | 内容（严格按权威规格 B 表） |
| --- | --- | --- |
| Greeting | `dashboard:greeting` | `Good Evening`（`Evening` 用 accent 色）+ `A CALMER DESKTOP. A BRIGHTER YOU.` |
| Weather | `dashboard:weather` | 图标、`18°`、`Partly Cloudy`、`↑22° ↓14°`、`📍Riverside, CA`、四列逐时（10PM 17° / 1AM 16° / 4AM 15° / 7AM 16°） |
| Calendar | `dashboard:calendar` | `Tue, Apr 22` + `This Week` + `+`；周条 `S M T W T F S` / `20–26`，选中日 `22` 实心圆；三条事件（青/琥珀/红圆点 + `10:00 Team sync` / `1:00 Lunch break` / `4:00 Plan next week`） |
| Todo | `dashboard:todo` | `My Tasks` + `3 of 5`；五行任务（完成项实心勾 + 删除线，未完成项空心圈）；`+ Add a task` |
| PC Summary | `dashboard:pc-summary` | 绿点 + `My PC` + `Online` + `Last seen 1 min ago`；缩略图；`CPU 12%` / `Temp 42°C` / `RAM 38%` / `Network ↓12.4 Mbps ↑3.1 Mbps` 各带彩色细条；`>` |
| Quote | `dashboard:quote` | 引用装饰卡：衬线两行 + 分割线 + `SAME PROGRESS A BRIGHTER TOMORROW` |
| Perspective | `dashboard:perspective` | 右上三行 `SAME ROOM / DIFFERENT / PERSPECTIVE` |
| Brand Strip | `dashboard:brand` | 底部 `A MORE FOCUSED TOMORROW` + 细分割线 |
| Decorative | `dashboard:decorative` | 纯几何装饰（默认关闭） |

> **与简报的差异：** 概念图首页**没有独立时钟卡**（时钟只在 StandBy 出现），因此 `WidgetType.Clock` 默认 `enabled = false`，不出现在默认布局；需要时可在 Appearance 中打开。

- [x] **Step 4: 实现 DashboardMode 布局**

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

- [x] **Step 5: 运行测试并提交**

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

- [x] **Step 1: 写失败测试**

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

- [x] **Step 2: 运行测试，确认失败**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*MetricRingBufferTest*"
```

Expected: 编译失败，`Unresolved reference: MetricRingBuffer`。

- [x] **Step 3: 实现**

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

- [x] **Step 4: 运行测试并提交**

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

- [x] **Step 1: 写失败测试**

```kotlin
@OptIn(ExperimentalTestApi::class)
class MonitorModeTest {

    private val history = mapOf(
        MetricKeys.Cpu to List(60) { 20f + it % 10 },
        MetricKeys.Gpu to List(60) { 60f + it % 5 },
        MetricKeys.Ram to List(60) { 38f },
    )

    @Test
    fun `renders every monitor block from the concept`() = runComposeUiTest {
        setContent { WakeUpMyWallTheme { MonitorMode(MockData.metrics, history, WidgetStyle.Glass) } }
        listOf(
            "monitor:identity", "monitor:quick-actions",
            "metric:cpu", "metric:gpu", "metric:ram", "metric:storage",
            "metric:temps", "metric:fans", "metric:network", "metric:uptime",
            "monitor:activity", "monitor:quote",
        ).forEach { onNodeWithTag(it).assertExists() }
    }

    @Test
    fun `quick actions expose the four concept entries`() = runComposeUiTest {
        setContent { WakeUpMyWallTheme { MonitorMode(MockData.metrics, history, WidgetStyle.Glass) } }
        listOf("Browser", "Discord", "Steam", "Spotify").forEach { action ->
            onNodeWithTag("action:$action").assertExists()
        }
    }

    @Test
    fun `metric card shows ring percent and footer values`() = runComposeUiTest {
        setContent { WakeUpMyWallTheme { MonitorMode(MockData.metrics, history, WidgetStyle.Glass) } }
        onNodeWithTag("metric:cpu-ring").assertTextEquals("28%")
        onNodeWithTag("metric:cpu-footer-primary").assertTextEquals("4.9 GHz")
        onNodeWithTag("metric:cpu-footer-secondary").assertTextEquals("8 cores 16 threads")
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

- [x] **Step 2: 运行测试，确认失败**

```bash
./gradlew :composeApp:desktopTest --tests "*MonitorModeTest*"
```

Expected: 编译失败，`Unresolved reference: MonitorMode`。

- [x] **Step 3: 实现曲线**

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

- [x] **Step 4: 实现 Monitor Mode 布局**

严格按权威规格 C 表构建：

1. 第 1 行左：My PC 身份卡（`monitor:identity`）：缩略图 + `DESKTOP-ALPHA` + `Windows 11 Pro` + `AMD Ryzen 7 7700X` + `NVIDIA GeForce RTX 4070 Ti` + `>`。
2. 第 1 行右：`QuickActionsCard`（`monitor:quick-actions`）：右上 `+`，四个动作块，每个 `testTag("action:<Name>")`，**不使用第三方商标图标**。
3. 第 2 行：四张 `MetricCard`（`metric:cpu|gpu|ram|storage`），每张 = 图标 + 名称 + 型号小字 + `ProgressRing`（`metric:<key>-ring`，中心显示百分数）+ `MetricSparkline` + 两行页脚（`metric:<key>-footer-primary` / `-secondary`）。
4. 第 2 行末：`metric:temps` 卡（CPU 68°C / GPU 67°C / Motherboard 42°C / SSD 38°C，每行彩色点 + 进度条 + 数值）与 `metric:fans` 段（CPU Fan 1,240 RPM / GPU Fan 1,560 RPM / Case Fans 820 RPM）。
5. 第 3 行：`metric:network`（↓124.3 Mbps / ↑31.7 Mbps + 曲线）、`metric:uptime`（`3d 6h 24m` + `Since Apr 18, 2025`）、`monitor:activity`（四行应用 + 相对时间）、`monitor:quote`（引用装饰卡）。
6. 列数按 `Breakpoints` 决策：可用宽度 ≥ 1000dp 用 5 列（概念图），600–1000dp 用 3 列，< 600dp 用 2 列并允许纵向滚动。

`ProgressRing` 用 `Canvas` 画背景环 + 前景弧（`drawArc`，`useCenter = false`，`Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)`），中心叠 `Text` 显示百分数；颜色按指标取 `AccentPalette` 对应色。

- [x] **Step 5: 运行测试并提交**

```bash
./gradlew :composeApp:desktopTest --tests "*MonitorModeTest*"
git add -A
git commit -m "feat: add monitor mode with metric cards and sparklines"
```

---

### Task 9: 主页模式切换（Task 13 会扩为三态）

> 本任务先实现 Dashboard ↔ Monitor 两态；Task 13 会把 `HomeMode` 扩为 `Dashboard / Monitor / StandBy` 三态并补上边界测试。本任务的测试写两态断言即可，Task 13 同步升级。

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

- [x] **Step 1: 写失败测试（控制器）**

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

- [x] **Step 2: 运行测试，确认失败**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*HomeSurfaceStateTest*"
```

Expected: 编译失败，`Unresolved reference: HomeModeController`。

- [x] **Step 3: 实现控制器与 UI 切换**

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

- [x] **Step 4: 写 UI 测试**

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

- [x] **Step 5: 运行测试并提交**

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

**与概念图的差异（需知悉）：** 概念图方案 A（`image-gen-3(1)`）的左导航只有 6 项，没有 `Display & Behavior`；本任务保留 7 项，因为简报 §14 的显示/省电设置必须有入口。`Appearance` 条目即概念图的 `Wallpaper & Personalization`（Task 12）。

- [x] **Step 1: 写失败测试**

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

- [x] **Step 2: 运行测试，确认失败**

```bash
./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest --tests "*Settings*"
```

Expected: 编译失败，`Unresolved reference: SettingsWorkspace`。

- [x] **Step 3: 实现**

左列固定宽度约 240dp 的 `Column` 放七个 `SettingsSection` 的 `NavigationDrawerItem` 风格按钮（`testTag("settings:nav:<name>")`）与底部 `ResetToDefaultRow`（`testTag("settings:reset")`）；右侧为 `content` 插槽。外层仍由 `AppShell` 提供 Power Rail。

- [x] **Step 4: 运行测试并提交**

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

- [x] **Step 1: 写失败测试**

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

- [x] **Step 2: 运行测试，确认失败**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*DeviceSetupValidatorTest*"
```

Expected: 编译失败，`Unresolved reference: DeviceSetupValidator`。

- [x] **Step 3: 实现校验器**

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

- [x] **Step 4: 实现表单 UI**

六个输入框（PC Name / MAC / IP / Broadcast / WOL Port / Agent Port / Agent Host），每个下方在其 `errors` 非空时显示错误文案；底部两个按钮 `Save`（`testTag("device:save")`，`enabled = result.isValid`）与 `Test Connection`（`testTag("device:test")`）。

- [x] **Step 5: 写 UI 测试**

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

- [x] **Step 6: 运行测试并提交**

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
  - `object BuiltInWallpapers { val ids: List<String> }`（顺序固定：`dusk-lake` / `mountains` / `forest` / `city-night` / `cozy-room` / `minimal` / `abstract`；`dusk-lake` 为默认）
  - `@Composable fun AppearanceScreen(state: AppearanceState, onStateChange: (AppearanceState) -> Unit, preview: @Composable (AppearanceState) -> Unit, modifier: Modifier = Modifier)`

- [x] **Step 1: 写失败测试**

```kotlin
class AppearanceStateTest {

    private val state = AppearanceState(
        accent = ThemeAccent.AuroraBlue,
        widgetStyle = WidgetStyle.Glass,
        wallpaperId = "dusk-lake",
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
        assertEquals("dusk-lake", AppearanceReducer.setWallpaper(state, "unknown").wallpaperId)
    }

    @Test
    fun `built in wallpapers match the concept order`() {
        assertEquals(
            listOf("dusk-lake", "mountains", "forest", "city-night", "cozy-room", "minimal", "abstract"),
            BuiltInWallpapers.ids,
        )
    }

    @Test
    fun `default appearance uses dusk lake glass and aurora blue`() {
        assertEquals("dusk-lake", state.wallpaperId)
        assertEquals(WidgetStyle.Glass, state.widgetStyle)
        assertEquals(ThemeAccent.AuroraBlue, state.accent)
    }
}
```

- [x] **Step 2: 运行测试，确认失败**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*AppearanceStateTest*"
```

Expected: 编译失败，`Unresolved reference: AppearanceState`。

- [x] **Step 3: 实现状态与归约器**

`AppearanceReducer` 全部为纯函数（`copy` 返回新状态）；`setWallpaper` 对非法 id 保持原值，不抛异常。

- [x] **Step 4: 实现 UI（含 Live Preview）**

严格按权威规格 F 表构建：

1. 左侧导航（`appearance:nav:<name>`）：Wallpaper（默认选中）、Themes & Colors、Widgets、Layout、Appearance；分割线后 `Reset to Default`。
2. 中部 `Live Preview — Your Home Screen` 面板（`appearance:preview`）：内嵌 `DashboardMode` 渲染，随 `WidgetStyle` / `accent` / 壁纸实时变化。
3. 右侧保留 Power Rail（由外层 `AppShell` 提供）。
4. 底部控制条四段：
   - **Wallpaper**：7 张缩略图（`appearance:wallpaper:<id>`），选中项带对勾角标（`appearance:wallpaper-selected`），末尾 `appearance:wallpaper-more` 打开系统图片选择器
   - **Theme & Accent**：6 个色点（`appearance:accent:<AccentName>`），首个标注 `Aurora Blue`
   - **Widget Style**：Glass / Solid / Minimal（`appearance:style:<Name>`），每项带小卡预览并标记当前选中
   - **Appearance**：Card Transparency 滑杆（`appearance:transparency`，默认 70%）、Font Scale 滑杆（`appearance:font-scale`，默认 100%）、Layout Preset 三选一（`appearance:layout:<Name>`，默认 Default）

滑杆取值写入 `AppearanceState.transparency` 与 `fontScale`；`fontScale` 通过 `LocalDensity` 的 `fontScale` 覆盖应用到 Live Preview 内的预览子树。

- [x] **Step 5: 写 UI 测试**

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

- [x] **Step 6: 运行测试并提交**

```bash
./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest
git add -A
git commit -m "feat: add appearance screen with live preview"
```

---

### Task 13: StandBy 模式（概念图 image-gen-5）

**Files:**
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/ui/standby/StandByMode.kt`
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/ui/standby/StandByClock.kt`
- Modify: `src/commonMain/kotlin/com/xukunz/wakeupmywall/ui/dashboard/HomeSurface.kt`
- Modify: `src/commonMain/kotlin/com/xukunz/wakeupmywall/app/AppNavigator.kt`（`HomeMode` 增加第三态）
- Test: `src/desktopTest/kotlin/com/xukunz/wakeupmywall/ui/standby/StandByModeTest.kt`

**Interfaces:**
- Consumes: `DashboardData`、`PowerRailModel`
- Produces:
  - `enum class HomeMode { Dashboard, Monitor, StandBy }`（替换 Task 9 的两态定义）
  - `@Composable fun StandByMode(data: DashboardData, rail: PowerRailModel, onRailEvent: (RailEvent) -> Unit, onOpenMonitor: () -> Unit, modifier: Modifier = Modifier)`
  - `@Composable fun StandByClock(time: String, meridiem: String, date: String, modifier: Modifier = Modifier)`

- [x] **Step 1: 写失败测试**

```kotlin
@OptIn(ExperimentalTestApi::class)
class StandByModeTest {

    @Test
    fun `renders clock weather next event and pc card`() = runComposeUiTest {
        setContent { WakeUpMyWallTheme { StandByMode(standByData, railModel, {}, {}) } }
        listOf("standby:clock", "standby:weather", "standby:next-event", "standby:pc-card", "standby:status-bar")
            .forEach { onNodeWithTag(it).assertIsDisplayed() }
    }

    @Test
    fun `clock shows minute in accent color`() = runComposeUiTest {
        setContent { WakeUpMyWallTheme { StandByClock("9:41", "PM", "Tuesday, April 22") } }
        onNodeWithTag("standby:clock-hour").assertTextEquals("9:")
        onNodeWithTag("standby:clock-minute").assertTextEquals("41")
    }

    @Test
    fun `pc card exposes night mode and auto dim instead of power actions`() = runComposeUiTest {
        setContent { WakeUpMyWallTheme { StandByMode(standByData, railModel, {}, {}) } }
        onNodeWithTag("standby:night-mode").assertExists()
        onNodeWithTag("standby:auto-dim").assertExists()
        onNodeWithTag("powerrail:sleep").assertDoesNotExist()
    }

    @Test
    fun `swipe left from standby opens monitor`() {
        val controller = HomeModeController()
        controller.onSwipeLeft()
        controller.onSwipeLeft()
        assertEquals(HomeMode.StandBy, controller.current.value)
    }
}
```

- [x] **Step 2: 运行测试，确认失败**

```bash
./gradlew :composeApp:desktopTest --tests "*StandByModeTest*"
```

Expected: 编译失败，`Unresolved reference: StandByMode`。

- [x] **Step 3: 实现 StandByClock**

```kotlin
@Composable
fun StandByClock(time: String, meridiem: String, date: String, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val hour = time.substringBefore(':') + ":"
    val minute = time.substringAfter(':')
    Column(modifier = modifier.testTag("standby:clock")) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(hour, style = AppTypography.hugeClock, modifier = Modifier.testTag("standby:clock-hour"))
            Text(minute, style = AppTypography.hugeClock, color = accent, modifier = Modifier.testTag("standby:clock-minute"))
            Text(meridiem, style = AppTypography.metricUnit, modifier = Modifier.padding(start = Spacing.sm).testTag("standby:clock-meridiem"))
        }
        Text(date, style = AppTypography.title, modifier = Modifier.testTag("standby:clock-date"))
    }
}
```

同时在 `core/theme/Typography.kt` 增加 `val hugeClock = TextStyle(fontSize = 120.sp, fontWeight = FontWeight.Light)`。

- [x] **Step 4: 实现 StandByMode**

按权威规格 D 表：全屏壁纸 + 左上品牌条 + 右上 `SAME ROOM / DIFFERENT / PERSPECTIVE`；主区左 `StandByClock`、中排 Weather 卡（含 `Clearer skies later tonight.`）与 Next Event 卡（`In 1 hr 19 min` / `Team sync` / `11:00 PM – 12:00 AM` / `Microsoft Teams`）；右侧 PC 浮层卡（`standby:pc-card`）：标题 + `PowerRingButton` + `Power On` + `WAKE YOUR PC` + 两个开关按钮 `standby:night-mode`（`Night Mode` / `ON`）与 `standby:auto-dim`（`Auto-Dim` / `ACTIVE`）；底部通栏条 `standby:status-bar`（显示器图标 + 绿点 + `My PC` + `Online` + `Last seen 1 min ago` + `>`）。

**注意：** StandBy 不渲染 `PowerRail`，因此不得出现 `powerrail:*` 标签；这一点由 Step 1 的第三个测试守住。

- [x] **Step 5: 接入三态切换**

`HomeModeController` 由两态扩为三态：`Dashboard -(+1)-> Monitor -(+1)-> StandBy`，`onSwipeLeft()` 前进一态、`onSwipeRight()` 后退一态，到边界停住。Task 9 的两态测试需同步改为三态断言。

- [x] **Step 6: 运行测试并提交**

```bash
./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest
git add -A
git commit -m "feat: add standby clock mode with compact pc card"
```

---

### Task 14: 响应式断点

**Files:**
- Create: `src/commonMain/kotlin/com/xukunz/wakeupmywall/ui/components/Breakpoints.kt`
- Test: `src/commonTest/kotlin/com/xukunz/wakeupmywall/ui/components/BreakpointsTest.kt`

**Interfaces:**
- Consumes: 无
- Produces:
  - `enum class LayoutWidth { Compact, Medium, Expanded }`
  - `object Breakpoints { fun widthFor(availableDp: Int): LayoutWidth; fun columnsFor(availableDp: Int): Int }`

- [ ] **Step 1: 写失败测试**

```kotlin
class BreakpointsTest {

    @Test
    fun `below six hundred dp is compact with two columns`() {
        assertEquals(LayoutWidth.Compact, Breakpoints.widthFor(560))
        assertEquals(2, Breakpoints.columnsFor(560))
    }

    @Test
    fun `between six hundred and one thousand dp is medium with three columns`() {
        assertEquals(LayoutWidth.Medium, Breakpoints.widthFor(600))
        assertEquals(LayoutWidth.Medium, Breakpoints.widthFor(999))
        assertEquals(3, Breakpoints.columnsFor(800))
    }

    @Test
    fun `one thousand dp and above is expanded with concept columns`() {
        assertEquals(LayoutWidth.Expanded, Breakpoints.widthFor(1000))
        assertEquals(5, Breakpoints.columnsFor(1200))
    }

    @Test
    fun `boundary values are inclusive on the lower edge`() {
        assertEquals(LayoutWidth.Compact, Breakpoints.widthFor(599))
        assertEquals(LayoutWidth.Medium, Breakpoints.widthFor(600))
        assertEquals(LayoutWidth.Expanded, Breakpoints.widthFor(1000))
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*BreakpointsTest*"
```

Expected: 编译失败，`Unresolved reference: Breakpoints`。

- [ ] **Step 3: 实现**

```kotlin
package com.xukunz.wakeupmywall.ui.components

enum class LayoutWidth { Compact, Medium, Expanded }

object Breakpoints {
    const val CompactMax = 599
    const val MediumMax = 999

    fun widthFor(availableDp: Int): LayoutWidth = when {
        availableDp <= CompactMax -> LayoutWidth.Compact
        availableDp <= MediumMax -> LayoutWidth.Medium
        else -> LayoutWidth.Expanded
    }

    fun columnsFor(availableDp: Int): Int = when (widthFor(availableDp)) {
        LayoutWidth.Compact -> 2
        LayoutWidth.Medium -> 3
        LayoutWidth.Expanded -> 5
    }
}
```

- [ ] **Step 4: 接入 Dashboard 与 Monitor**

在 `DashboardMode` 与 `MonitorMode` 内用 `BoxWithConstraints` 取 `maxWidth`，按 `Breakpoints.columnsFor(maxWidth.value.toInt())` 决定每行卡片数量；`Compact` 时整屏改为 `Column` + `verticalScroll`。禁止用固定 `1f` 权重的 Row 强排。

- [ ] **Step 5: 运行测试并提交**

```bash
./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest
git add -A
git commit -m "feat: add responsive breakpoints for dashboard and monitor grids"
```

---

### Task 15: 视觉还原复核

**Files:**
- Create: `docs/plans/phase1-visual-review.md`
- Modify: `src/commonMain/kotlin/com/xukunz/wakeupmywall/core/theme/Tokens.kt`（取色校准结果）

**Interfaces:**
- Consumes: Task 1–14 的全部 UI
- Produces: 逐屏「已还原 / 有偏差」清单 + 校准后的主题色值

- [ ] **Step 1: 用 desktop target 渲染五个屏幕并截图**

为每个屏幕写一个 `@Preview` 或 desktop `main` 入口渲染，把截图保存到 `docs/plans/screenshots/`；文件名与概念图屏幕名对应（`dashboard.png`、`monitor.png`、`standby.png`、`device-setup.png`、`personalization.png`）。

- [ ] **Step 2: 逐屏比对并记录偏差**

`docs/plans/phase1-visual-review.md` 必须逐屏列出：布局顺序、卡片数量、文案、数值、颜色、圆角与间距的差异，以及每项差异的处理结论（立即修 / 留到哪个 Phase）。**不允许写"基本一致"这类无法验证的结论。**

- [ ] **Step 3: 取色校准写入令牌**

从概念图取色后更新 `Tokens.kt` 的 `DarkSurface` 与 `AccentPalette`，并保持 Task 1 的对比度测试（`ThemeAccentTest`）全绿；若某色值导致对比度不达标，必须调整并在复核文档里记录原因。

- [ ] **Step 4: 运行全量测试并提交**

```bash
./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest
git add -A
git commit -m "docs: add phase 1 visual parity review and calibrated tokens"
```

---

## Phase 1 完成标准

1. `./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest` 全绿，且测试覆盖：Power Rail 五种状态与连接条术语、Dashboard 卡片显隐、Ring Buffer 边界、设备表单校验、外观状态归约、StandBy 组件构成、响应式断点边界。
2. **五个屏幕**（Home Dashboard、PC Monitor、StandBy、Settings/Device Setup、Wallpaper & Personalization）在 desktop 渲染与 Android 真机上均正常，72/28 布局不溢出。
3. 概念图逐屏比对结论记录到 `docs/plans/phase1-visual-review.md`，偏差项写明归属 Phase。
4. 全部数值来自 `data/mock/MockData.kt`；UI 包内不出现字面量色值（只允许命中 `core/theme`）。
5. 术语红线成立：代码库中不存在 `Connected via Wake-on-LAN` 字符串（除规范与审阅文档中的引用）。
6. 概念图中的手机状态栏与 `v1.0` 水印没有被实现。

## 自检结果（写作时执行）

| 检查项 | 结果 |
| --- | --- |
| 规范覆盖 | 规范 §2 三工作空间 → Task 5/9/10；§3 状态机 → Task 4；§7 页面清单 → Task 6/8；§8 指标与 60 秒曲线 → Task 7/8；§9 显示默认值中的亮度/像素位移 → Phase 8；§10 架构约束 → Phase 0 |
| 概念图覆盖 | 审阅报告 §3–§7 的五个屏幕 → Task 6/8/10/12/13；权威规格 A–F 表分别对应 Task 4b/6/8/13/10/12 |
| 类型一致性 | `PowerRailModel` 由 Phase 0 的 `PcState.capabilities` 派生，Task 4b/5/6/9/12/13 复用同一模型；`HomeMode` 在 Task 9 定义、Task 13 扩为三态，无重复定义 |
| 占位符扫描 | 无 TBD；刻意留到后续阶段的项（第三方图标、Recent Activity 真实数据）已标注归属 |
| 已知取舍 | 不引入图标库与 Navigation 库；`⏻` 与动作图标为过渡占位；第三方商标图标一律不内置；视觉校准集中在 Task 15 |

---

## 执行记录：Task 1–3（2026-09-19）

已完成 Task 1、Task 2、Task 3（复选框已勾）。交这一批时的证据：`./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest` 全绿（44 个单测 + 55 个桌面测试，其中 8 个是真实 Compose UI 测试）。

### 与本计划正文的偏差（均已按"全局约束/权威规格优先"处理）

| 位置 | 计划原文 | 实际做法 | 原因 |
| --- | --- | --- | --- |
| Task 2 的 `WidgetSurface` | 描边写 `1.dp` | 新增 `Spacing.hairline` 令牌 | 全局约束禁止 `ui/` 出现裸 dp |
| Task 3 的 `MockData` 天气 | `location = "Home"`、`21/12`、`nextHoursC = listOf(18,19,20,18)` | `Riverside, CA`、`22/14`、四个带标签的逐时（10PM 17° / 1AM 16° / 4AM 15° / 7AM 16°） | 「概念图还原规格」是权威来源，且 B2 要求逐时带时刻标签，故 `WeatherSnapshot.nextHours` 用 `HourlyForecast(label, temperatureC)` |
| Task 3 的 `MockData` 指标 | 简化版字段（如 `cpuTempC = 54`） | 字段覆盖 C1/C2 全部显示项（含主板/SSD 温度、三组风扇、`recentActivity`） | 计划要求 MockData"覆盖概念图上的所有数字"；型号小字统一收进 `HardwareIdentity`，避免同一事实两处存储 |
| Task 3 的 `DashboardWidgetTest` | 断言 `visible.size == widgets.size` | 该测试先启用全部 widget 再验保序，另加一条测试断言默认布局会过滤掉 disabled 的 `decorative` | 原断言与 `visible = 过滤 disabled` 的契约自相矛盾（默认布局里 `decorative` 本就是关闭的），属计划缺陷 |

### 新增的执行器（计划外，但服务于全局约束）

`desktopTest/.../DesignTokenDisciplineTest.kt`：扫描 `ui/` 全部 Kotlin 源码，禁止字面量 `Color(0x…)`、裸 `dp`、裸 `sp`。它把"颜色/间距/圆角/字号只能取自 core/theme"从口头约定变成可执行规则，并用红-绿验证过（临时插入违规文件时确实失败，并精确报出文件:行号）。

---

## 执行记录：Task 4–5（2026-09-19）

已完成 Task 4、Task 5（复选框已勾）。证据：`testDebugUnitTest` 51 + `desktopTest` 76 全绿；截图工具重新渲染，Power Rail 真实成帧见图。

### 偏差

| 位置 | 计划原文 | 实际做法 | 原因 |
| --- | --- | --- | --- |
| Task 4 Step 5 UI 测试 | 断言 `powerrail:primary` 的文字等于 `Wake PC` | 断言可用/不可用，并把 `Power On` 与 `WAKE YOUR PC` 放在独立的 `powerrail:primary-label` / `powerrail:primary-caption` 节点 | 该断言写在 Step 4b 之前；Step 4b 与权威规格 A3 都把主按钮定为环形（环内是电源字形，文案在环下方），环形按钮没有文字可断言 |
| Task 4 的 `primaryLabel` / `primaryCaption` | 仅给出示例值 `Power On` / `Waking…` / `Setup PC` | 权威规格只钉死 WOL_READY（`Power On` + `WAKE YOUR PC`），其余状态由本任务补全成表 | 计划未给全表；这些文案属**我补写的 UI 文案，请在 Task 15 视觉复核时确认** |
| Task 4 环形按钮尺寸 | `220.dp` / `3.dp` 写在 Composable 里 | 新增 `AppSizes` 令牌 | 全局约束禁止 `ui/` 出现裸 dp（由 `DesignTokenDisciplineTest` 强制） |
| Task 4 状态点颜色 | 未指定 | 新增 `LocalAccentPalette`（`WakeUpMyWallTheme` 下发 palette），在线时用 `onlineColor` | Material3 的 colorScheme 拿不到"在线色"；Phase 7 的强调色切换也需要这个入口 |
| Task 4 `statusLine` 与 `connectionLabel` | 两个字段都保留 | 两个都渲染：连接条主行 = `connectionLabel`，次行 = `statusLine` | 稳定状态下两者文案相同（都来自 Phase 0 的能力描述），过渡状态才分工（如"Wake-on-LAN Ready" + "Waiting for Agent"）。Task 15 可决定是否只留一行 |
| Task 5 `AppShell` | `PowerRail(modifier = Modifier.weight(0.28f))` | 槽位标签放外层 `Box`，不复用 PowerRail 的 modifier 传 tag | 同一节点上链式 `testTag` 只有先写的生效，直接传 tag 会把 Rail 的 `powerrail` 标签顶掉（实测踩到） |
| Task 5 App 接入 | 只更新本地状态 | 复用 Phase 0 的 `PcStateMachine.reduce`，把 4 个电源事件映射为 `PcEvent` | 计划说"暂时只更新本地状态"，状态机已经存在且可测，直接接上比再写一份映射好 |

### 计划外新增的执行器

`desktopTest/.../core/TerminologyDisciplineTest.kt`：扫描产品源码，禁止出现规范 §3 明令禁止的 `Connected via Wake-on-LAN`，落实 Phase 1 完成标准第 5 条。

### 已知视觉缺陷（交给 Task 14 断点 / Task 15 视觉复核）

在 1280dp 宽（= 2560×1600 @320dpi 的真实墙面屏尺寸）下，28% 的 Power Rail 只有约 358dp，A4 的三枚并排次级按钮被挤到每枚约 98dp：文字仍完整、但 `Shut Down` / `POWER OFF` 会各占两行，与概念图的一行排布有差距。计划风险 R7 已预见此类窄屏问题并安排在 Task 14 用断点解决（候选方案：Rail 宽度 < 400dp 时动作区改为纵向堆叠或缩短副标）。

---

## 执行记录：Task 6（2026-09-19）

已完成 Task 6（复选框已勾）。证据：`testDebugUnitTest` 51 + `desktopTest` 82 全绿；Home Dashboard 真实成帧见 `build/screenshots/dashboard-aurora.png`（问候卡 / 透视装饰 / 天气 / 日历 / 任务 / PC 摘要 / 引用卡 / 品牌条全部到位）。

### 偏差

| 位置 | 计划原文 | 实际做法 | 原因 |
| --- | --- | --- | --- |
| PC 摘要卡的读数 | `PcSummaryWidget(data.pc, …)` 只传 `PowerRailModel` | 新增 `PcSummarySnapshot` + `MockData.summaryMetrics`，单独建模 | 权威规格 B3 的首页读数是 `CPU 12% / Temp 42°C / RAM 38% / ↓12.4 ↑3.1 Mbps`，与 Monitor（C2）的 `28% / 68°C / 124.3 / 31.7` 不是同一组数字；`PowerRailModel` 里没有指标字段 |
| 日历事件文案 | 计划 Task 6 表格给 `10:00 Team sync` / `1:00 Lunch break` / `4:00 Plan next week` | 已按此覆盖 Task 3 的临时文案 | Task 3 时权威规格没给事件标题，Task 6 表格给了具体值，取更具体者 |
| 任务卡计数 | 表格写死 `3 of 5` | 头衔改为按数据实算 `"${done} of ${size}"`，并把 Mock 数据调成 3 条已完成 | 写死字符串会在数据变化时立刻失真；实算 + Mock 对齐两者兼得 |
| `WidgetType.Clock` | 表格注明"首页默认不显示时钟" | `DashboardLayout.default` 里 clock 改为 `enabled = false`，并同步修正 `DashboardWidgetTest` 的可见项断言 | 落实该条注记 |
| `WidgetSurface` 结构 | 调用方 modifier 与内部 `surface:*` 标签同节点 | 外层加 `Box` 承载调用方 modifier，内层 `Column` 才是卡片 | 同节点链式 `testTag` 只有先写的生效，否则 `dashboard:*` 会把 `surface:*` 顶掉 |
| PC 摘要卡的子节点断言 | — | UI 测试用 `useUnmergedTree = true` 读取卡内文字 | 整卡可点击会合并子节点语义（这对无障碍是正确的），断言需显式读未合并树 |
| 引用卡文案 | 只规定"衬线两行 + 标语" | 两行正文写成 `Small steps, / steady light.` | 权威规格没给引用正文；**属我补写的 UI 文案，请在 Task 15 视觉复核时确认或替换** |

### 已知视觉缺陷（交给 Task 14 / Task 15）

天气卡的四列逐时在 1280dp 下被挤成 `10PM 17°1AM 16°…`（列间无呼吸空间）；任务卡标题行与首行任务贴得偏紧。二者都需要在断点任务里按可用宽度调整列数与间距。

---

## 执行记录：Task 7–8（2026-09-19）

已完成 Task 7、Task 8（复选框已勾）。证据：`testDebugUnitTest` 62 + `desktopTest` 99 全绿；PC Monitor 真实成帧见 `build/screenshots/monitor-aurora.png`。

### 偏差

| 位置 | 计划原文 | 实际做法 | 原因 |
| --- | --- | --- | --- |
| `MetricCard` 签名 | `(label, value, unit, values, style, modifier)` | 改为 `(key, label, percent, modelLine, values, footerPrimary, footerSecondary, style, modifier, onOpen)` | 权威规格 C 的卡片要显示"名称 + 型号小字 + 环心百分数 + 两行页脚"，原签名缺型号与页脚，且 `key` 是 `metric:<key>-*` 标签的来源 |
| `metric:<key>-value` 断言 | 断言另有一个大号数值节点 | 删掉重复的大号数值，测试改断言环心 `metric:<key>-ring` | 规格写明百分数只在环心；实测 32sp 数值在 120dp 宽的卡里被挤成竖排 `2/8/%` |
| `Breakpoints` | Task 14 才创建 | 本任务先落地（列数 + 是否需要滚动），阈值放进 `AppSizes` | Task 8 Step 4.6 要求按断点决定列数；阈值属设计令牌，不能裸写在 `ui/`（会被 `DesignTokenDisciplineTest` 拦下） |
| 滚动条件 | "< 600dp 用 2 列并允许纵向滚动" | 改为"只要不是 5 列就必须允许滚动" | 实测 3 列时卡片换行堆叠、内容变高，固定高度下第三行被裁掉 |
| Mock 曲线 | 未指定来源 | `ui/monitor/mockMetricHistory()` 生成确定性 60 点曲线 | 曲线卡需要数据；Phase 5 接真实轮询后删除该函数即可 |
| 最近活动两列 | 未处理 | 应用名单行省略号截断 | 实测两列文字在窄卡里重叠 |
| 引用卡 tag | `monitor:quote` | 复用 Dashboard 的 `QuoteCard`，由调用方传入带 tag 的 modifier | 组件复用；`WidgetSurface` 已改成外层 Box 承载调用方 modifier |

### 断点实测结论（供 Task 14）

- 1280dp 宽的整屏下主区约 921dp → 落进 3 列区间，内容需要滚动；概念图的 5 列排布要求主区 ≥ 1000dp，即整屏约 1400dp 以上。
- 这说明"72/28 + 5 列"是宽屏形态；Task 14 需要决定在 1000–1400dp 区间是放宽 Rail 还是改指标卡密度。

---

## 执行记录：Task 9（2026-09-19）

已完成 Task 9。证据：`testDebugUnitTest` 67 + `desktopTest` 109 全绿；`HomeSurfaceTest` 用 `performTouchInput { swipeLeft()/swipeRight() }` 验证的是**真实手势路径**，不只是控制器。

### 偏差

| 位置 | 计划原文 | 实际做法 | 原因 |
| --- | --- | --- | --- |
| 滑动判定 | 单次 `dragAmount` 与 ±60 比较 | 累计本次拖拽位移，在 `onDragEnd` 判定 | 单次拖拽事件通常只有几像素，永远达不到 60dp 阈值，原写法手势实际不会触发 |
| 阈值来源 | 代码里写 `60f` | `AppSizes.swipeThreshold` 令牌 + `LocalDensity` 转 px | 原有写法把 dp 阈值当像素用；而且裸 dp 会被 `DesignTokenDisciplineTest` 拦下 |
| `HomeSurface` 签名 | `(mode, dashboard, metrics, history, style, onModeChange, modifier)` | 追加 `identity: HardwareIdentity? = null` | 否则 Monitor 里的身份卡会丢掉主机名/系统/CPU/GPU（Task 8 已引入这些字段） |
| Monitor 返回入口 | "MonitorMode 顶部提供返回 Dashboard 的入口" | 身份卡的 `>` 变成可点返回 | 复用既有 `onOpenDevice` 回调，避免再加一个按钮 |
| App 接入 | 只写"用 HomeSurface" | 工作空间作为入口 + `LaunchedEffect(workspace)` 同步形态；形态由 HomeSurface 自己管 | 保留 Phase 0 的三工作空间导航语义（`Workspace.Monitor` 仍能直接落到 Monitor 形态），同时不让导航状态与主页形态互相打架 |

---

## 执行记录：Task 10（2026-09-19）

已完成 Task 10。证据：`testDebugUnitTest` 70 + `desktopTest` 116 全绿；Settings 现在也走 `AppShell`，因此常驻 Power Rail 在三工作空间里保持一致。

### 偏差

| 位置 | 计划原文 | 实际做法 | 原因 |
| --- | --- | --- | --- |
| 返回入口 | 未提；概念图左导航"固定为 6 项" | 在导航列表**之上**加一行 `← Home`（`settings:back`），列表本身仍严格是 7 项 | 否则 Settings 是死路：Rail 的齿轮只能进不能出。放在列表之外可以同时满足"列表固定"和"能回去" |
| `ResetToDefaultRow` 参数顺序 | `(onReset, modifier)` | `(modifier, onReset)` | Compose 约定 modifier 为首个可选参数；计划自带的测试用尾随 lambda `ResetToDefaultRow { }`，原顺序会把 lambda 绑到 modifier 上（实测编译失败） |
| Settings 内容 | 由 Task 11/12 填充 | 当前每个 section 渲染占位页（`screen:<title>`） | 本任务只交付外壳，符合"Task 11/12 替换"的排期 |
| `AppUiTest` 断言 | 断言 `screen:Settings` 占位页 | 改为断言 `settings:nav` | Settings 不再是占位页 |

---

## 执行记录：Task 11（2026-09-19）

已完成 Task 11。证据：`testDebugUnitTest` 79 + `desktopTest` 129 全绿；Device Setup 真实成帧见 `build/screenshots/settings-aurora.png`。

### 偏差

| 位置 | 计划原文 | 实际做法 | 原因 |
| --- | --- | --- | --- |
| `isValidHost` | 计划给出的实现后附警告"两个条件必须用括号明确优先级" | 用提前 return 拆成三段，不用依赖 `&&`/`||` 优先级 | 计划自己也标了这是个坑；拆开后读起来不需要记忆优先级 |
| 屏幕内容范围 | Task 11 只描述表单（7 个输入 + Save/Test） | 同时实现权威规格 E 的 Saved Computers 与 Integrated Services 两块（数据取自 `MockData`） | 规格 E 是整屏的权威描述，只做表单会让该屏缺两块内容；Mock 数据本来就有 4 台设备 |
| 表单按钮排布 | 两枚按钮并排 | 纵向堆叠且各自 `fillMaxWidth` | 实测两栏布局下表单列只有约 300dp，并排会把 `Test Connection` 挤成三行 |
| Saved Computers 行 | 未指定排布 | 名称 + MAC 竖排，"Default" 作为右侧徽标 | 实测横向排会把 MAC 拆成逐字符换行 |
| `ip` 字段 | 校验器里要求"非空时必须合法" | 补了一条测试固定该行为 | 计划只有实现没有测试覆盖，容易在重构时丢失 |
| App 接线 | 未指定初值 | 用 `MockData.defaultDevice` 预填表单 | 概念图 E 显示的就是一台已配置好、处于 `WOL Ready` 的设备 |

---

## 执行记录：Task 12（2026-09-19）

已完成 Task 12。证据：`testDebugUnitTest` 86 + `desktopTest` 143 全绿；Appearance 真实成帧见 `build/screenshots/appearance-live-preview.png`。

### 需要用户裁决的缺口：壁纸素材

权威规格 F 列出 7 张壁纸（`dusk-lake` / `mountains` / `forest` / `city-night` / `cozy-room` / `minimal` / `abstract`），但**只有用户提供的两张有真实母版**（`aurora` / `minimal`）。因此目录里只保留这两张：

- 若把另外 5 个 id 塞进目录，`WallpaperBackground.drawable()` 会命中 `error("Unmapped built-in wallpaper")` —— 也就是说必须要么补素材，要么承认目录只有两张。
- `AppearanceStateTest` 用一条测试把"目录只暴露有素材的壁纸"固定下来，避免以后有人只改 id 不改素材。
- **待裁决**：这 5 张是否要补（自己拍/生成/买图），还是把概念图 F 行的 7 张缩略图视为"示意"、正式版只出 2–3 张？

### 偏差

| 位置 | 计划原文 | 实际做法 | 原因 |
| --- | --- | --- | --- |
| `BuiltInWallpapers` 位置 | 新建在 `data/mock`，7 个固定 id | 复用 Task 8.5 已建的 `core/wallpaper/BuiltInWallpapers`（2 个真实 id） | 该文件已存在且被 `WallpaperBackground` / `AppearanceSettings` 引用；重建会同时出现两套 id |
| 归约器接口 | 只列了 4 个函数 | 追加 `setTransparency` / `setFontScale`（带钳制，字号上下限进 `AppSizes` 令牌） | 规格 F 有这两个滑杆；不钳制的话滑杆能把字号推到荒谬值 |
| 底部控制条 | 步骤 4 读起来像竖直四段 | 四段**并排**在同一行 | 规格 F 原文是"底部控制条四段"；实测竖排会把 Live Preview 挤到几乎看不见 |
| `AppearanceScreen` 签名 | `(state, onStateChange, preview, modifier)` | `(state, onStateChange, modifier, selectedSection, onSectionChange, preview)` | 左导航需要受控选中态；`preview` 放最后以便尾随 lambda |
| Appearance 的作用域 | 只描述"Live Preview 会跟着变" | **Accent / Wallpaper / WidgetStyle 现在是全应用外观的唯一来源**：`WakeUpMyWallTheme(accent=…)`、`WallpaperBackground(appearance.wallpaperId)`、`HomeSurface(style=appearance.widgetStyle)` 全部读它 | 否则用户在 Appearance 里改完，回到主页发现没变——那不叫外观设置，只是预览器 |
| 截图组合 | — | 截图测试补上壁纸底层 | 首次截图是白底：`AppearanceScreen` 本身不铺背景，真实 App 里它浮在壁纸上 |

---

## 执行记录：Task 13（2026-09-19）

已完成 Task 13。证据：`testDebugUnitTest` 87 + `desktopTest` 152 全绿；StandBy 真实成帧见 `build/screenshots/standby-aurora.png`（超大时钟、天气、下一场日程、右侧 PC 浮层卡、底部状态条）。

### 偏差

| 位置 | 计划原文 | 实际做法 | 原因 |
| --- | --- | --- | --- |
| `HomeMode` 所在文件 | "Modify `app/AppNavigator.kt`（HomeMode 增加第三态）" | 改的是 `ui/dashboard/HomeSurface.kt`（`HomeMode` 实际定义处） | `AppNavigator` 管的是工作空间（含 Settings），`HomeMode` 是主页形态，两者不是一回事；Task 9 已经把 `HomeMode` 落在 HomeSurface.kt |
| `toggle()` 语义 | Task 9 测试要求"两态交替" | 三态下改为按 `Dashboard → Monitor → StandBy → Dashboard` 循环，并同步改 Task 9 的断言 | 三态下"交替"没有定义；循环 + 边界钳制最符合左滑前进的直觉 |
| 超大时钟 | `120.sp` | `104.sp` + 显式 `lineHeight` | 实测 120sp 且不指定行高时，行盒小于字形墨迹范围，数字顶部被裁掉 |
| PC 浮层卡宽度 | 未指定 | 占宽 34%（`AppSizes.standbyCardWidthFraction`） | 首次实现铺满整行，与"右侧浮层卡"不符 |
| `nextEvent` 数据 | 只给了文案 | 新增 `NextEvent` 数据类 + `MockData.nextEvent`；`WeatherSnapshot` 增加 `summary` 字段 | 权威规格 D 的下一场日程与一句话天气在既有模型里无处安放 |
| `HomeSurface` 参数 | `(mode, dashboard, metrics, history, style, onModeChange, modifier)` | 追加 `nextEvent`（默认取 `MockData.nextEvent`） | StandBy 需要日程数据；给默认值可以少改调用方，代价是 `ui/dashboard` 引用了 `data/mock`（纯 Mock 阶段的临时耦合，Phase 2 接真实数据源时一起清掉） |
