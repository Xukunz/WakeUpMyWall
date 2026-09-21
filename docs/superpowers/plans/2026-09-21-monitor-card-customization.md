# Monitor 卡片自定义（位置 + 信息类别）实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让用户自己决定 Monitor（硬件界面）上有哪些卡片、按什么顺序排、每张卡显示哪一类信息 —— 而不是像现在这样布局写死在 `MonitorMode` 里。

**Architecture:** 新增 `MonitorLayout`（有序的卡片配置列表）挂在既有的 `AppearanceState` 上（与 `widgets: List<DashboardWidget>` 同一套做法，不引入新的持久化通道）；`MonitorMode` 从"写死的卡片列表"改成"按配置渲染"；编辑入口放在 Appearance 屏（spec §7.6 的 `Widgets` 一节）。

**Spec:** spec §7.2（Monitor 卡片）、§7.6（Wallpaper & Personalization 的 Widgets 一节）
**前置：** Phase 1（卡片组件与令牌纪律）、Phase 5B（真实读数与 `—` 规则）、本次的多磁盘 Storage 卡片

---

## 0. 计划期定案

| 决策 | 选定 | 理由与代价 |
| --- | --- | --- |
| 配置存哪 | 放进 `AppearanceState`（内存态，与 `widgets` 一致） | 现在整套 Appearance 都还没落盘（Phase 7 才做持久化），单独给卡片开一个落盘通道会造成"半持久化"的错觉。代价：重启 App 后自定义会丢，直到 Phase 7。 |
| 卡片标识 | `MonitorCardId` 枚举：`CPU/GPU/RAM/Storage/Temps/Network/Uptime/Activity/Quote` | 用枚举而不是字符串，编译期就能拦住拼错的 id。九张卡与现状一一对应，**默认顺序 = 现状顺序**（用户不设置时零变化）。 |
| "信息类别"怎么表达 | 每张卡一个 `detail` 槽位，取值来自该卡支持的固定集合（如 CPU 卡：`Clock` / `Cores` / `Temp`），决定页脚主行显示什么；次行保持"辅助信息" | 完全自由的自定义字段会让卡片宽度与排版不可控（Phase 1 的断点与字号都是按固定文案实测的）。代价：类别是从预设里选，不能任意组合。 |
| 可否隐藏 | 每张卡带 `enabled`；关掉就是从布局里移除 | 需求里的"自定义条件卡片"最实用的部分就是隐藏不关心的卡。 |
| 排序交互 | Appearance 里用 **↑ / ↓ 按钮**（不做拖拽） | 拖拽在 Compose 里要处理长按、自动滚动、无障碍，成本远高于收益；按钮在桌面/手机上都可靠、可测。 |
| 最少保留 | 至少要留一张卡（全关掉时移除按钮禁用） | 空白页比"少一张卡"更让人困惑。 |

## Global Constraints

- `ui/` 下只能用 `core/theme` 的令牌（有守卫测试 `DesignTokenDisciplineTest`）。
- 所有新文案必须进 `core/i18n/AppStrings.kt` 的中英两套表（英文值与原字面量**逐字一致**）。
- 默认配置必须与当前布局**完全一致**，既有 `MonitorModeTest` / 截图基线零变化。
- 新 UI 元素都要 `testTag`：`appearance:card-<id>-up/down/toggle`、`appearance:card-<id>-detail`。

---

## 1. 文件结构

| 文件 | 职责 |
| --- | --- |
| `domain/model/MonitorLayout.kt`（新） | `MonitorCardId`、`MonitorCardDetail`、`MonitorCardConfig`、`MonitorLayout.default` 与纯函数归约（move/enable/setDetail） |
| `ui/settings/AppearanceScreen.kt`（改） | `AppearanceState` 增加 `monitorCards`；新增 `MonitorCardsSegment` 编辑器 |
| `ui/monitor/MonitorMode.kt`（改） | 按配置渲染：顺序、隐藏、每卡 detail；Storage 卡继续走自己的多盘 Pager |
| 测试 | `commonTest/.../domain/MonitorLayoutTest.kt`、`desktopTest/.../ui/settings/MonitorCardsEditorTest.kt`、`desktopTest/.../ui/monitor/MonitorLayoutRenderTest.kt` |

---

### Task C1: 模型与纯函数归约（完成）

- `enum class MonitorCardId { Cpu, Gpu, Ram, Storage, Temps, Network, Uptime, Activity, Quote }`
- `enum class MonitorCardDetail { Clock, Cores, Temp, Usage, Vram, Free }`（每张卡声明自己支持哪些）
- `data class MonitorCardConfig(val id: MonitorCardId, val enabled: Boolean = true, val detail: MonitorCardDetail? = null)`
- `object MonitorLayout { val Default: List<MonitorCardConfig> = …; fun move(list, id, delta): List<…>; fun toggle(list, id): List<…>; fun setDetail(list, id, detail): List<…> }`
- 测试：默认顺序与现状一致；`move` 到头/到尾不越界；不允许全部关闭；detail 只接受该卡支持的取值（不支持时保持原值）。

### Task C2: Appearance 里的编辑器（完成）

- `AppearanceState.monitorCards: List<MonitorCardConfig> = MonitorLayout.Default`
- `MonitorCardsSegment`：每行 = 卡片名（`strings` 查表）+ `↑`/`↓` + 开关 + detail 循环按钮。
- 测试：点 `↓` 后顺序变化；关掉最后一卡时开关被禁用；切到中文后卡片名变中文。

### Task C3: Monitor 按配置渲染（完成）

- `MonitorMode(..., cards: List<MonitorCardConfig> = MonitorLayout.Default)`
- 指标卡区按 `cards` 顺序生成槽位（跳过 `enabled = false`），detail 决定 `footerPrimary` 取哪一项；`Temps/Activity/Quote` 保持整卡语义（detail 无效）。
- 测试：默认渲染与现状一致（既有断言不动）；隐藏 CPU 后 `metric:cpu` 不存在；把 Storage 移到第一位时它排在 `metric:gpu` 之前。

### Task C4: 验收与文档（完成）

- 模拟器：把 CPU 卡隐藏、Storage 移到第一位、切到中文，截图存 `docs/plans/screenshots/phase-card-customization-*.png`；
- 更新 README 的"已完成"清单与 roadmap（Phase 5.5/7 的交叉点）。

---

## 2. 验收标准

| 要求 | 落点 | 验证方式 |
| --- | --- | --- |
| 卡片位置可自定义 | C1/C2/C3 | `MonitorLayoutTest` + `MonitorCardsEditorTest` + `MonitorLayoutRenderTest` |
| 卡片信息类别可自定义 | C1/C2/C3 | detail 归约测试 + 渲染测试 |
| 不设置时与现在完全一致 | C3 | 既有 `MonitorModeTest` 与截图基线不动 |
| 中英文都覆盖新文案 | C2 | 新字符串进 `AppStrings`，切中文断言卡片名 |

---

## 3. 验收实录（2026-09-21）

### 3.1 测试

| 步骤 | 命令 | 实测结果 |
| --- | --- | --- |
| C1 | `./gradlew :composeApp:testDebugUnitTest` | `MonitorLayoutTest` 6 条：默认顺序、两端不越界、末卡不可关、单卡隐藏、非法类别被拒、类别循环回绕 |
| C2/C3 | `./gradlew :composeApp:desktopTest` | `MonitorCardLayoutTest` 3 条：隐藏卡不渲染（邻卡仍在）、类别决定页脚（核心数 ⇒ 主行 `8 cores 16 threads`、次行 `4.9 GHz`）、**默认布局仍渲染全部九张卡** |
| 回归 | 同上（全量） | 既有 `MonitorModeTest` / `MonitorMetricsTest` 断言**一条未改**，全绿；`assembleDebug` 通过 |

### 3.2 验收帧（无头桌面渲染管线，1280×720）

用与 Phase 1 相同的 `runDesktopComposeUiTest` + `captureToImage` 出图（`CardCustomizationScreenshotTest`，可重跑）：

- [card-customization-default.png](../../plans/screenshots/card-customization-default.png)：默认布局（英文），与现状一致；
- [card-customization-custom.png](../../plans/screenshots/card-customization-custom.png)：隐藏 CPU、Storage 提到第一位、GPU 类别改成"使用率"；
- [card-customization-zh.png](../../plans/screenshots/card-customization-zh.png)：中文界面（`处理器 / 显卡 / 内存 / 存储 / 系统温度 / 风扇 / 网络 / 运行时长 / 最近活动 / 快捷操作`，Storage 卡片显示 `C:\ · 1/2` 与翻页点）。

### 3.3 已知边界（不掩盖）

- **配置只存在内存**：与整套 Appearance 一样，重启回到默认布局；持久化按 roadmap 属 Phase 7（壁纸/主题/布局一起落盘）。
- 信息类别只覆盖四张指标卡（CPU/GPU/RAM/Storage）；`System Temps / Network / Uptime / Recent Activity / Quote` 是整卡语义，不参与类别选择。
- 这一轮的中文只覆盖**卡片标题与页脚**：卡内的型号小字、`Browser/Discord/Steam/Spotify`、`CPU/GPU/Motherboard/SSD` 行、`5 min ago` 这类**数据内容**仍是英文（它们来自设备/Agent 数据或专有名词，等 Phase 6/7 统一处理）。
