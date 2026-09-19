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

## 2. 三个工作空间 + 固定 Power Rail

```
Home Dashboard ──┬── Dashboard Mode（StandBy 信息）
                 └── PC Monitor（硬件监控）

Settings Workspace ── Device Setup / Integrations / Display & Behavior /
                      Appearance / Notifications / About
```

除全屏沉浸类界面外，所有页面采用 **72 / 28 双栏**：左侧主内容，右侧固定 **PC Power Rail**。

Power Rail 是整个 App 的常驻组件，展示 `PC Name / PC State / 主电源按钮 / Sleep / Shutdown / Restart / Network-Agent 状态 / Settings 入口`。

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

### 7.1 Dashboard Mode（左 72%）
Greeting + Clock + Weather + Calendar + Todo + PC Status Summary + 可选装饰 Widget。

### 7.2 PC Monitor（左 72%）
头部：PC 名 / 硬件摘要（如 `Ryzen 7 7700X`、`RTX 4070 Ti`）+ Quick Actions。
主体卡片：CPU / GPU / RAM / Storage → Network / Temps & Fans → Uptime / Recent Activity。

### 7.3 切换方式
点击 PC Summary Widget 或左右滑动，在 Dashboard ↔ PC Monitor 之间切换；Power Rail 固定不动。

### 7.4 Settings Workspace
左导航：Device Setup、Integrations、Display & Behavior、Appearance、Notifications、Backup & Sync、About；底部 `Reset to Default`。右侧仍保留 Power Rail。

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

**做：** 横屏 Dashboard（时钟/天气/日历/Todo/PC 摘要）、Power Rail 五种电源操作、WOL 开机与在线检测、PC Monitor（CPU/GPU/RAM/存储/温度/风扇/网络/Uptime + 60 秒曲线）、壁纸/主题色/Widget 显隐、横屏与全屏、Keep Screen On、Auto Dim、Pixel Shift、多 PC 与默认 PC、Agent 配对与 Token 鉴权、Local Todo、Android Calendar Provider、天气。

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

---

## 14. 待用户裁决的开放问题

| # | 问题 | 选项 | 建议 |
| --- | --- | --- | --- |
| Q1 | PC Agent 技术栈 | (a) C# / .NET Minimal API + LibreHardwareMonitor；(b) Kotlin/JVM + Ktor + OSHI | **(a)**：V1 要求 CPU/GPU 风扇转速与温度，OSHI 在 Windows 上拿不到风扇转速 |
| Q2 | 开发与验证方式 | (a) 在本机（Android Studio）编译验证，我只产出代码与计划；(b) 由我在容器内安装 JDK 25 + Android SDK 并自建验证 | 取决于你是否希望我这边能独立跑通构建 |

---

## 15. 术语表

| 术语 | 含义 |
| --- | --- |
| Power Rail | 右侧常驻的 PC 控制栏（固定 28% 宽） |
| Dashboard Mode | 偏 StandBy 信息的主页形态 |
| Monitor Mode | 偏硬件监控的主页形态 |
| Widget Surface | Glass / Solid / Minimal 三种卡片承载组件 |
| Idle | App 自身的省电显示状态（区别于 Android 系统息屏） |
