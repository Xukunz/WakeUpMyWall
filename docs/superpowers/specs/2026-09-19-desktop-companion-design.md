# Desktop Companion 产品与技术设计规范

**状态：** 待评审（Draft for review）
**日期：** 2026-09-19
**产品简报（原始输入，逐字存档）：** [docs/design/2026-09-19-product-brief-desktop-companion.md](../../design/2026-09-19-product-brief-desktop-companion.md)

---

## 1. 产品定义

把闲置 Android 手机（横屏、常亮、长时间放在桌面）变成四合一桌面设备：

1. **桌面装饰屏** —— 壁纸、主题、玻璃卡片、布局
2. **StandBy 信息屏** —— 时间、天气、日历、Todo
3. **PC 监视器** —— CPU / GPU / RAM / 磁盘 / 温度 / 风扇 / 网络
4. **PC 遥控器** —— 开机（WOL）、睡眠、关机、重启、锁屏

系统由两个软件组件构成，职责边界是不可协商的：

| 组件 | 职责 | 明确不负责 |
| --- | --- | --- |
| **Mobile App**（Android 优先，KMP 架构） | 展示、交互、控制意图、WOL 魔法包、本地信息（天气/日历/Todo） | 不采集 PC 硬件指标；不直接执行 PC 命令 |
| **PC Agent**（Windows） | 电脑状态采集、硬件指标、电源控制、快捷启动、配对鉴权 | 不渲染 UI；不暴露任意 Shell 执行 |
| **主板 WOL**（固件能力） | 电脑关机状态下被唤醒 | 不参与任何在线通信 |

**核心修正（相对旧计划）：** WOL 不是"连接协议"，只在关机状态下用于开机。在线状态一律通过 Agent 的 HTTP/WebSocket 判定。

---

## 2. 工作空间与固定 Power Rail

```
Home ──┬── Dashboard Mode（信息卡主页）
       ├── PC Monitor（硬件监控）
       └── StandBy Mode（时钟浮层，兼 Idle 展示形态）

Settings Workspace ── Device Setup / Integrations / Display & Behavior /
                      Notifications / Appearance（Wallpaper & Personalization）/
                      Backup & Sync / About
```

除 StandBy 模式与全屏沉浸类界面外，所有页面采用 **72 / 28 双栏**：左侧主内容，右侧固定 **Power Rail**。

**Power Rail 精确构成**（依据概念图，术语按 §3 修正）：

1. 标题区：`My PC` + 副标 `POWER CONTROL`（Settings 页副标为 `DESKTOP COMPANION`）+ 右上齿轮（进入 Settings）
2. 状态行：状态色点 + 状态文案（`Ready to wake` / `Online` / `Waking PC…` 等）
3. 主电源：直径约 220dp 的环形按钮，状态色描边 + 外发光 + 中央电源字符；下方主标签（`Power On` 等）+ 全大写副标（`WAKE YOUR PC`）
4. 次级动作：三枚并排按钮，每枚 = 图标 + 标签 + 全大写副标：`Sleep` / `SNAP MODE`、`Shut Down` / `POWER OFF`、`Restart` / `FRESH START`
5. 连接条：通栏圆角条，图标 + 通道文案 + 箭头。文案随状态机切换（`Wake-on-LAN Ready` ↔ `Agent connected over LAN`），**不得**出现 `Connected via Wake-on-LAN`
6. 可选装饰卡：引用卡（`Better Tools A Calmer Mind` + `SAME PROGRESS A BRIGHTER TOMORROW`），可在 Appearance 中关闭

**StandBy 模式**为第三形态：去掉 Power Rail，改为右侧玻璃浮层卡（标题 + 电源环 + `Power On` + `Night Mode` / `Auto-Dim` 两个开关），底部通栏条显示 PC 在线状态。

**响应式要求：** 概念图按宽屏渲染。主区可用宽度 ≥ 1000dp 用概念图列数；600–1000dp 降为 3 列；< 600dp 用 2 列并允许纵向滚动。禁止在窄屏上强行保持 5 列。

---

## 3. PC 状态机（唯一定义源）

概念图存在逻辑冲突（同时显示 `My PC Online` + `Power On` + `Connected via Wake-on-LAN`）。正式实现以本节状态机为准，任何 UI 文案不得绕过它。

```kotlin
enum class PcState {
    UNCONFIGURED,        // 未添加设备
    OFFLINE,             // 已配置，Agent 不可达，WOL 不可用（缺 MAC/网段信息）
    WOL_READY,           // 已配置，Agent 不可达，具备 WOL 条件
    WAKING,              // 魔法包已发送，等待 Agent 上线
    ONLINE,              // Agent 可达
    AGENT_UNAVAILABLE,   // 主机在线（ping 通）但 Agent 未响应
    SLEEPING,
    RESTARTING,
    SHUTTING_DOWN,
    ERROR,               // 最近一次命令失败
}
```

| 状态 | 主按钮 | 辅助按钮 | 说明文案 |
| --- | --- | --- | --- |
| `UNCONFIGURED` | Setup PC | 禁用 | Add your PC to begin |
| `OFFLINE` | Wake PC（禁用直到配置完整） | 禁用 | Wake-on-LAN requires MAC |
| `WOL_READY` | Wake PC | 禁用 | Wake-on-LAN Ready |
| `WAKING` | Waking…（禁用） | 禁用 | Waiting for Agent |
| `ONLINE` | PC Online | Sleep / Shutdown / Restart（+ Lock） | Agent connected over LAN |
| `AGENT_UNAVAILABLE` | Agent unavailable（禁用） | 禁用 | Host reachable, Agent not responding |
| `SLEEPING` | Sleeping…（禁用） | 禁用 | — |
| `RESTARTING` | Restarting…（禁用） | 禁用 | — |
| `SHUTTING_DOWN` | Shutting down…（禁用） | 禁用 | — |
| `ERROR` | Retry | 禁用 | 附失败原因 |

**术语规则：** 只允许出现 `Wake-on-LAN Ready`（关机待唤醒）与 `Agent connected over LAN`（在线）。禁止 `Connected via Wake-on-LAN`。

---

## 4. 三条数据通路

```
                Phone
                  │
        ┌─────────┼───────────┐
        ▼         ▼           ▼
      UDP       HTTP      WebSocket
   (Magic包)  (命令/查询)   (指标流)
        │         │           │
        └─────────┴───────────┘
                  ▼
                 PC
```

1. **WOL（UDP 广播，端口 9）** —— 关机态专用；发送 3 次；发送后进入 `WAKING`，每 2 秒轮询 `GET /api/v1/status` 直到 Agent 响应或超时。
2. **HTTP（命令与一次性查询）** —— 电源命令、设备信息、诊断。
3. **WebSocket（`/ws/v1/metrics`）** —— 在线后的实时指标；V1 先用 1 秒间隔 HTTP 轮询跑通，指标全部正确后再切换（见 §9）。

---

## 5. Agent API 契约（V1 冻结）

```http
GET  /api/v1/status                # 存活 + 基础信息（配对前可用，仅返回最小信息）
GET  /api/v1/system                # 硬件指标快照
GET  /api/v1/actions               # 可用快捷动作注册表（id → 显示名）

POST /api/v1/power/sleep
POST /api/v1/power/shutdown
POST /api/v1/power/restart
POST /api/v1/power/lock

POST /api/v1/actions/{id}          # 仅允许注册表内 id

WS   /ws/v1/metrics
```

**安全红线：** 永不提供 `POST /execute?command=...` 一类任意命令接口。快捷动作必须是 `id → Agent 侧白名单` 的映射。

**鉴权：** 首次配对由 PC 端确认后下发 Token；此后所有请求携带 `Authorization: Bearer <token>`。Token 在手机端存入 Android Keystore，iOS 端为 Keychain。默认仅局域网可达，不做端口转发、不做公网直连。

**超时与重试：** 单请求超时 2–3 秒；WOL 重试 3 次；Agent 断线自动重连（指数退避，上限 30 秒）。

---

## 6. 数据模型

多 PC 从第一天就是一等公民，禁止出现 `pcMacAddress` 这类单设备字段。

```kotlin
data class PcDevice(
    val id: String,
    val name: String,
    val macAddress: MacAddress?,      // 必填才能 WOL
    val ipAddress: String?,
    val broadcastAddress: String?,    // 默认 192.168.1.255
    val wolPort: Int = 9,             // 默认 9
    val agentHost: String?,
    val agentPort: Int = 9876,        // 默认 9876
    val isDefault: Boolean = false,
    val lastSeen: Instant? = null,
    val hardwareInfo: HardwareInfo? = null,
)
```

Token 不放进该模型明文序列化路径，单独存入平台安全存储（Keystore/Keychain）。

---

## 7. 页面清单与默认布局

权威依据：概念图审阅报告 [docs/design/2026-09-19-concept-review.md](../../design/2026-09-19-concept-review.md)。

### 7.1 Dashboard Mode（左 72%，三行）

| 行 | 内容 |
| --- | --- |
| 1 | 问候语 `Good Evening`（强调词用 accent 色）+ 副标题 `A CALMER DESKTOP. A BRIGHTER YOU.`；右上装饰文案 `SAME ROOM / DIFFERENT / PERSPECTIVE` |
| 2 | Weather 卡（18° / Partly Cloudy / ↑22° ↓14° / Riverside, CA / 四列逐时）· Calendar 卡（`Tue, Apr 22` + `This Week` + 周条 + 三条带色点事件 + `+`）· My Tasks 卡（`3 of 5` + 五行任务 + `+ Add a task`） |
| 3 | My PC 摘要卡（绿点 Online、`Last seen 1 min ago`、缩略图、CPU/Temp/RAM/Network 四项含彩色细条、`>` 进入 Monitor）· 引用装饰卡 |
| 底部 | 品牌条 `A MORE FOCUSED TOMORROW` + 细分割线 |

### 7.2 PC Monitor（左 72%，三行）

| 行 | 内容 |
| --- | --- |
| 1 | My PC 身份卡（`DESKTOP-ALPHA` / Windows 11 Pro / Ryzen 7 7700X / RTX 4070 Ti）· Quick Actions（Open Browser / Open Discord / Launch Steam / Open Spotify） |
| 2 | CPU / GPU / RAM / Storage 四张指标卡（环形进度 + 中心百分数 + sparkline + 两行数值）· System Temps 卡（CPU/GPU/Motherboard/SSD + Fans 段） |
| 3 | Network（↓124.3 ↑31.7 Mbps）· Uptime（`3d 6h 24m` / `Since Apr 18, 2025`）· Recent Activity（四行应用 + 相对时间）· 引用装饰卡 |

### 7.3 StandBy 模式

超大时钟（`9:41`，分钟用 accent 色）+ 长日期；Weather 卡 + Next Event 卡；右侧 PC 浮层卡；底部通栏 PC 状态条。既可由滑动进入，也是 Idle 长时间无操作后的展示形态。

### 7.4 切换方式
点击 PC Summary Widget 进入 Monitor，左右滑动在 Dashboard ↔ Monitor ↔ StandBy 之间切换；Power Rail 在三个模式间保持不动。

### 7.5 Settings Workspace

左导航（方案 A 为基础）：`Device Setup`、`Integrations`、`Display & Behavior`、`Notifications`、`Appearance`、`Backup & Sync`、`About`，底部 `Reset to Default`；右栏保留完整 Power Rail + 引用装饰卡。

> 概念图方案 A（`image-gen-3(1)`）的导航只有 6 项，没有 `Display & Behavior`；该组设置只出现在方案 B（`4f74989a`）的卡片里。因为简报 §14 要求 Keep Screen On / Auto Dim / Idle Timeout / 亮度 / Pixel Shift 等设置必须有明确入口，这里把 `Display & Behavior` 保留为第 3 项导航。**这是相对概念图的刻意增加**，需用户知悉。

- **Device Setup**：Wake-on-LAN Configuration（PC Name / MAC / Broadcast IP / Port 步进器 + `Test Connection` + `WOL Ready` 状态块）· Saved Computers（多设备列表 + `Default` 徽标 + `Add Device`）· Integrated Services（Weather / Calendar / To-Do 三个来源与开关）
- **Appearance**：即 `Wallpaper & Personalization`，见 §7.6

Agent 相关字段（Agent Host / Agent Port / Token）在 Phase 4 以 `Advanced / Agent` 区补入 Device Setup；数据模型从第一天保留这些字段。

### 7.6 Wallpaper & Personalization

左导航（Wallpaper / Themes & Colors / Widgets / Layout / Appearance + `Reset to Default`）+ 中部 `Live Preview — Your Home Screen` + 右侧 Power Rail + 底部控制条：

- Wallpaper：`Dusk Lake`（默认，带对勾）、Mountains、Forest、City Night、Cozy Room、Minimal、Abstract、`+` More
- Theme & Accent：6 个色点（首个 `Aurora Blue`）
- Widget Style：Glass（默认）/ Solid / Minimal
- Appearance：Card Transparency（默认 70%）、Font Scale（默认 100%）、Layout Preset（Default / Compact / Minimal）

---

## 8. Agent 指标范围

| 指标 | 版本 | 备注 |
| --- | --- | --- |
| CPU 使用率 / 温度 / 频率 | V1 | 风扇与温度依赖硬件监控库 |
| GPU 使用率 / 温度 / VRAM | V1 | NVIDIA 可走 NVML；其他厂商需库支持 |
| RAM 使用率 | V1 | |
| 存储使用率 | V1 | |
| 上传 / 下载速度 | V1 | |
| Uptime | V1 | |
| CPU / GPU 风扇转速 | V1 | 需要 LibreHardwareMonitor 级能力 |
| SSD 温度 / 主板温度 | P1 | |
| 最近应用 / 历史曲线 | P2 | V1 不做长期历史 |

图表策略：只保留 **最近 60 秒**，数据放内存 Ring Buffer，V1 不落库。

---

## 9. 显示、功耗与寿命（正式功能，不是优化项）

应用状态机：

```
ACTIVE ──(Idle Timeout, 默认 60s)──► IDLE ──(触摸)──► ACTIVE
```

| 项 | ACTIVE | IDLE |
| --- | --- | --- |
| PC 指标刷新 | 1 秒 | 5 秒 |
| 天气刷新 | 30 分钟 | 30 分钟（不强刷） |
| 动画 | 正常 | 停止不必要动画 |
| 亮度 | Active 80% | Idle 15% |
| 像素位移 | 开 | 开（3–5 分钟，±1~3 dp） |

默认值：Keep Screen On = ON、Auto Dim = ON、Idle Timeout = 60s、Active Brightness = 80%、Idle Brightness = 15%、Pixel Shift = ON、Lock Orientation = Landscape、Fullscreen = ON。

OLED 烧屏防护（Pixel Shift）是 V1 必做项，因为设备可能每天亮屏 8–24 小时。

---

## 10. 架构约束

### 10.1 Mobile（KMP + Compose Multiplatform）
```
UI ──► ViewModel / Store ──► Domain ──► Repository ──► Data Sources
```
`commonMain` **禁止**依赖 Android API；平台能力通过 `expect/actual` 或接口在 `androidMain` 实现。

Repository 接口：`PcRepository`、`WeatherRepository`、`CalendarRepository`、`TodoRepository`、`SettingsRepository`、`PcMetricsRepository`。

### 10.2 PC Agent（Windows）
建议 ASP.NET Core Minimal API + LibreHardwareMonitor 级硬件库（风扇转速与温度的可行路径）。V1 以 Windows 服务/开机自启方式常驻。

### 10.3 仓库形态
Monorepo，以便 API Schema 与版本同步：
```
mobile/     ← KMP 应用（Android 优先）
agent/      ← PC Agent
docs/       ← 设计、规范、计划
```

---

## 11. 非功能需求

| 项 | 目标 |
| --- | --- |
| 冷启动 | < 2 秒进入 Dashboard |
| Idle CPU | 尽量 < 5% |
| UI 帧率 | 60 fps |
| 方向 | 横屏锁定 |
| PC 指标刷新 | ACTIVE 1s / IDLE 5s |
| 天气刷新 | 30 min |
| 离线能力 | 时钟 / Todo / 壁纸 / 设置可用 |
| 崩溃恢复 | 自动回到 Dashboard |
| Agent 重连 | 自动，指数退避 |
| 请求超时 | 2–3 s |
| WOL 重试 | 3 次 |
| 最低 Android | minSdk 30（沿用现有工程设置） |

---

## 12. V1.0 范围（锁定）

**做：** 横屏 Dashboard（问候语/时钟/天气/日历/Todo/PC 摘要/引用装饰卡/品牌条）、**StandBy 时钟模式（含 Night Mode 与 Auto-Dim 开关）**、Power Rail 五种电源操作（环形主按钮 + 三枚次级动作 + 连接条）、WOL 开机与在线检测、PC Monitor（CPU/GPU/RAM/存储环形进度 + 60 秒曲线 + System Temps/Fans + Network/Uptime/Recent Activity + Quick Actions 静态 UI）、7 张内置壁纸（默认 Dusk Lake）/6 个强调色/3 种 Widget 风格/透明度/字号/3 种布局预设、Widget 显隐（含装饰层开关）、横屏与全屏、Keep Screen On、Auto Dim、Pixel Shift、多 PC 与默认 PC、Agent 配对与 Token 鉴权、Local Todo、Android Calendar Provider、天气。

**明确不做（V1）：** 云账号体系、公网远程控制、端口转发、远程桌面、文件传输、完整 Google OAuth、PC 投屏、Android 桌面小组件、Apple Watch / WearOS、用户插件、任意命令执行、指标历史数据库。

**版本节奏：** V1.0 见上；V1.1 增加 Quick Actions、Widget Style、Layout Preset、Live Preview、Notifications、Todoist、更多指标；V2 才做 iOS（EventKit、iOS WOL、Keychain、StandBy 优化、跨设备同步）。

---

## 13. 已做出的技术决策

| # | 决策 | 理由 |
| --- | --- | --- |
| D1 | 单一 Gradle 根 + `mobile/composeApp` 模块，AGP 项目名保持一个 `./gradlew` | 避免嵌套构建与双份 wrapper；agent 若为 .NET 则独立构建 |
| D2 | Phase 0 **不声明 iOS target** | Apple target 只能在 macOS 上编译；产品简报亦将 iOS 定为 V2；CMP 1.11+ 的原生目标还要求 Kotlin 2.3+，与工程当前 Kotlin 2.2.10 冲突 |
| D3 | 保留 Kotlin 2.2.10，Compose Multiplatform 取与 androidx Compose 1.10.x 对齐的版本 | 现有工程 `compose-bom 2026.02.01` => androidx Compose 1.10.4；CMP 1.10.3 对应 androidx 1.10.5，兼容性风险最小 |
| D4 | `commonMain` 不引入 DI 框架，Phase 0 用手写组合根 | YAGNI；接口已足够隔离，后续需要时再引入 |
| D5 | V1 图表只做内存 Ring Buffer（60 秒窗口） | 避免过早引入数据库与存储设计 |
| D6 | 应用包名改为 `com.xukunz.wakeupmywall` | 开源项目不应保留 `com.example`；发布后改包名代价高，现在改几乎零成本 |
| D7 | Settings 采用「左导航 + 保留 Power Rail」（概念图 `image-gen-3(1)` 方案） | 与简报 §10 一致，且符合 Power Rail 常驻的全局原则；`4f74989a` 的卡片式布局留待后续做 Settings 概览页 |
| D8 | 主页升级为三种模式：Dashboard / Monitor / StandBy | 概念图 `image-gen-5` 提供了简报没有的时钟形态，且天然适合承担 Idle 展示 |
| D9 | 默认壁纸为 `Dusk Lake` | 概念图两张 Personalization 图中 `Dusk Lake` 均为选中态 |
| D10 | 装饰文案层（`SAME ROOM DIFFERENT PERSPECTIVE`、`A MORE FOCUSED TOMORROW`、引用卡）做成可关闭的 Widget | 概念图中它跨屏常驻；做成开关即可同时满足还原度与 OLED 保护 |
| D11 | Device Setup 的可见字段与概念图对齐（4 个），Agent 字段延后到 Phase 4 | 避免在 Agent 未落地时暴露无意义的端口/主机输入 |
| D12 | 概念图中的 `9:41`、信号、电量视为手机系统状态栏，`v1.0` 角标不实现 | 避免把 Mockup 装饰误实现为产品 UI |

---

## 14. 待用户裁决的开放问题

| # | 问题 | 选项 | 建议 |
| --- | --- | --- | --- |
| Q1 | PC Agent 技术栈 | (a) C# / .NET Minimal API + LibreHardwareMonitor；(b) Kotlin/JVM + Ktor + OSHI | **(a)**：V1 要求 CPU/GPU 风扇转速与温度，OSHI 在 Windows 上拿不到风扇转速 |
| Q2 | 开发与验证方式 | 已裁决：由我在容器内自建工具链并自主验证 | 已完成，见 §16 |

---

## 16. 概念图审阅结论（2026-09-19）

完整审阅报告：[docs/design/2026-09-19-concept-review.md](../../design/2026-09-19-concept-review.md)

- `imgs/concept` 共 10 张图，去重后为 **5 个屏幕**：Home Dashboard、PC Monitor、StandBy、Settings（Device Setup）、Wallpaper & Personalization。
- 最重要的新增信息是 **StandBy 时钟模式**（`image-gen-5.png`），它是简报未覆盖的第三块主页形态，含 `Night Mode` 与 `Auto-Dim` 两个快捷开关。
- 最重要的冲突是 **Settings 存在两套互不兼容的布局**（`image-gen-3(1)` 左导航式 vs `4f74989a` 卡片式），以及 **Power Rail 同时显示 `Ready to wake` / `Power On` / `Connected via Wake-on-LAN`**。前者按 D7 处理，后者按 §3 状态机与术语规则处理。
- 概念图按宽屏渲染，主区 5 列在真机上过窄，因此引入 §2 的响应式断点要求。
- 审阅未改动 §3 状态机、§5 API 契约、§6 数据模型与安全红线的任何结论。

### 环境验证结果（本次实测）

| 项 | 结果 |
| --- | --- |
| JDK | Eclipse Temurin 25.0.4.1+1（安装于 `~/.local/toolchain`，容器无 sudo 权限，故不用 apt） |
| Gradle | 9.5.0（Wrapper 自动下载） |
| Android SDK | cmdline-tools 16111833；`platforms;android-37.0`、`build-tools;37.0.0`、`platform-tools 37.0.1` |
| 基线构建 | 原始工程 `./gradlew :app:assembleDebug` → **BUILD SUCCESSFUL in 1m 8s** |
| 命名变更 | SDK 平台包已改为小版本命名（`platforms;android-37.0`），旧写法 `platforms;android-37` 会报 `Package not found` |
| 仓库缺陷 | `gradlew` 在 git 中的模式为 `100644`（不可执行），新克隆后 `./gradlew` 会因权限被拒；已修正为 `100755` |

---

## 15. 术语表

| 术语 | 含义 |
| --- | --- |
| Power Rail | 右侧常驻的 PC 控制栏（固定 28% 宽） |
| Dashboard Mode | 偏 StandBy 信息的主页形态 |
| Monitor Mode | 偏硬件监控的主页形态 |
| Widget Surface | Glass / Solid / Minimal 三种卡片承载组件 |
| Idle | App 自身的省电显示状态（区别于 Android 系统息屏） |
