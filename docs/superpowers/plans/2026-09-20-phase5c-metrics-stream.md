# Phase 5C：`/ws/v1/metrics` 流式通道与自适应节奏

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把"每 2 秒拉一次"换成 Agent 主动推：手机连上 `/ws/v1/metrics` 后按 1 秒收一帧；界面进入 Idle 时只接受每 5 秒一帧（spec §9 的 ACTIVE 1s / IDLE 5s）；连接断了自动重连，Agent 不支持 WebSocket（Phase 5A/4A 的老版本）时退回 Phase 5B 的 HTTP 轮询。

**Architecture:** Agent 侧用 ASP.NET Core 的 `WebSockets`：握手时先查 Bearer（不通过就在升级前 401），之后按固定间隔把 `ISystemMetricsProvider.Read()` 的结果逐帧推给客户端。手机侧新加 `MetricsStream` 抽象（`Flow<AgentMetrics>`）与 `MetricsCadence`/`ActivityClock` 两个**纯策略**类；`App` 里 WebSocket 优先、HTTP 轮询兜底，节奏由 cadence 决定。

**Tech Stack:** ASP.NET Core WebSockets（无新依赖）、Ktor client `ktor-client-websockets`（新增一处依赖）、kotlinx-coroutines `Flow`。

**Spec:** [spec §5（鉴权）](../specs/2026-09-19-desktop-companion-design.md)、§8（指标范围）、§9（ACTIVE 1s / IDLE 5s）
**前置：** [Phase 5A](2026-09-20-phase5a-agent-metrics.md)（HTTP 指标端点与契约）、[Phase 5B](2026-09-20-phase5b-app-monitor.md)（HTTP 轮询、可空读数、Ring Buffer）

---

## 0. 计划期定案

| 决策 | 选定 | 理由与代价 |
| --- | --- | --- |
| 鉴权怎么套在 WebSocket 上 | **握手阶段**检查 `Authorization: Bearer`，不通过直接 401（`AcceptWebSocketAsync` 之前）；不引入"连上再发 Token"的消息协议 | spec §5 的红线是"受保护端点缺 Token 一律 401 且无副作用"，握手阶段拒绝最贴合，也最容易测（`TestServer.CreateWebSocketClient()` 能直接断言握手失败）。代价：浏览器 JS 客户端无法自定义头（本项目没有 Web 客户端）。 |
| 推送节奏谁说了算 | **服务端固定 1 秒**（`Agent:MetricsIntervalMs` 可配，测试用短值）；手机端 Idle 时**只接受**每 5 秒一帧 | 服务端不做 per-client 调速，简单且不会因客户端算错节奏而互相打架；Idle 的收益主要在"渲染与唤醒 CPU"，1 Hz 的小 JSON 在局域网里可忽略。代价：Idle 时仍有 1 Hz 网络流量（Phase 8 若要做省电可再谈）。 |
| 与 HTTP 轮询的关系 | **WebSocket 优先，HTTP 兜底**：WS 连上就暂停 HTTP 采样循环；WS 反复失败（含老 Agent 的 404）就回到 HTTP 2 秒轮询 | 老版本 Agent（Phase 5A/4A）没有这个端点，硬切会让用户直接看不到指标。代价：多一条兜底路径要维护（已在 5B 里实现并有测试）。 |
| 重连策略 | 指数退避 1s → 2s → 4s → 8s，封顶 8 秒；`agentToken` 或设备变化时立刻重连并重置退避 | 单次抖动不该立刻打回 HTTP 轮询，持续失败才降级。代价：最多 8 秒的观测延迟。 |
| Idle 判定放哪 | `ActivityClock`（纯类，记录最后一次交互时刻）+ App 根节点上的指针事件喂它；阈值 60 秒（spec §9 默认值） | Phase 8 的"自动调光 / 像素位移"要的是同一份判定，做成纯类可以复用与单测。代价：现在只有触摸喂它，键盘/遥控等输入方式要等 Phase 8 一起补。 |
| 帧格式 | 与 `GET /api/v1/system` **完全相同的 JSON** | 复用同一份契约与同一个映射器，手机端不会出现"流式数据比轮询数据少字段"这种事。代价：每帧约 700 B（1 Hz 下无所谓）。 |

## Global Constraints

- 不新增除 `ktor-client-websockets` 之外的依赖；Agent 侧用框架自带 WebSockets。
- WebSocket 端点必须与 HTTP 指标端点**同一份契约**（字段名、单位、可空性都不许分叉）。
- 手机端所有新策略必须是**纯类/纯函数**（可单测），UI 只做接线。
- 每笔提交前 `./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest` 与 `dotnet test agent/WakeUpMyWall.Agent.slnx` 全绿。

---

## 1. 文件结构

| 文件 | 职责 |
| --- | --- |
| `agent/src/WakeUpMyWall.Agent/Api/MetricsSocketEndpoints.cs`（新） | `/ws/v1/metrics`：握手鉴权 + 按间隔推帧 |
| `agent/src/WakeUpMyWall.Agent/Program.cs`（改） | `app.UseWebSockets()`；把 WS 端点挂到 Bearer 保护组之外（自己查 Token，因为过滤器不覆盖升级请求） |
| `agent/tests/WakeUpMyWall.Agent.Tests/MetricsSocketTests.cs`（新） | 收到帧 / 无 Token 被拒 / 间隔可配 |
| `mobile/.../core/metrics/MetricsStream.kt`（新） | `interface MetricsStream`、`KtorMetricsStream`、`parseMetricsFrame` |
| `mobile/.../core/metrics/MetricsCadence.kt`（新） | `MetricsCadence`（ACTIVE 1s / IDLE 5s）、`ActivityClock`（60 秒无交互 = idle） |
| `mobile/.../app/App.kt`（改） | WS 优先 + 兜底、cadence 门控、重连退避 |
| 测试 | `commonTest/.../core/metrics/MetricsCadenceTest.kt`、`MetricsStreamTest.kt`、`desktopTest/.../app/MetricsStreamIntegrationTest.kt` |

---

### Task C1: Agent 的 `/ws/v1/metrics`

**Files:**
- Create: `agent/src/WakeUpMyWall.Agent/Api/MetricsSocketEndpoints.cs`
- Modify: `agent/src/WakeUpMyWall.Agent/Program.cs`
- Test: `agent/tests/WakeUpMyWall.Agent.Tests/MetricsSocketTests.cs`

**Interfaces:**
- Consumes: `ISystemMetricsProvider.Read()`、`ITokenStore`
- Produces: `MapMetricsSocket(this WebApplication app)`；配置项 `Agent:MetricsIntervalMs`（默认 1000）

- [x] **Step 1: 写失败测试**

```csharp
[Fact]
public async Task A_websocket_client_with_a_token_receives_a_metrics_frame()

[Fact]
public async Task A_websocket_client_without_a_token_is_rejected_before_the_upgrade()

[Fact]
public async Task Frames_follow_the_configured_interval()
```

- [x] **Step 2: 跑测试确认失败** → `dotnet test --filter MetricsSocketTests`
- [x] **Step 3: 实现**（握手查 Token → `AcceptWebSocketAsync` → 循环 `SendAsync` 一帧 JSON → `Task.Delay(interval, ct)`；客户端断开或取消即退出循环）
- [x] **Step 4: 跑全量测试确认通过**（`dotnet test WakeUpMyWall.Agent.slnx`）
- [x] **Step 5: 提交** `feat: push metrics over a websocket`

### Task C2: 手机端的 `MetricsStream` 与节奏策略

**Files:**
- Create: `core/metrics/MetricsStream.kt`、`core/metrics/MetricsCadence.kt`
- Modify: `gradle/libs.versions.toml`、`mobile/composeApp/build.gradle.kts`（加 `ktor-client-websockets`）
- Test: `commonTest/.../core/metrics/MetricsStreamTest.kt`、`MetricsCadenceTest.kt`

**Interfaces:**
- Produces:
  - `interface MetricsStream { fun frames(baseUrl: String, token: String): Flow<AgentMetrics> }`
  - `class KtorMetricsStream(client: HttpClient) : MetricsStream`（`client.webSocket("$baseUrl/ws/v1/metrics")`，每帧走 `parseMetricsFrame`）
  - `fun parseMetricsFrame(frame: String): AgentMetrics?`（解析失败返回 null：坏帧不该打断整条流）
  - `data class MetricsCadence(val activeMillis: Long = 1_000, val idleMillis: Long = 5_000) { fun intervalMillis(isIdle: Boolean): Long }`
  - `class ActivityClock(val idleTimeoutMillis: Long = 60_000) { fun touch(nowMillis: Long); fun isIdle(nowMillis: Long): Boolean }`

- [x] **Step 1: 写失败测试**（坏帧返回 null、好帧解析出字段、cadence 两条分支、ActivityClock 的边界：刚好 60 秒算 idle）
- [x] **Step 2: 跑测试确认失败**
- [x] **Step 3: 实现**
- [x] **Step 4: 跑测试确认通过**
- [x] **Step 5: 提交** `feat: add a websocket metrics stream and the act-idle cadence`

### Task C3: App 接线（WS 优先 + 兜底 + 重连）

**Files:**
- Modify: `app/App.kt`、`ui/dashboard/HomeSurface.kt`（把指针事件喂给 `ActivityClock`）
- Test: `desktopTest/.../app/MetricsStreamIntegrationTest.kt`

**Interfaces:**
- Produces: `App(..., metricsStreamFactory: () -> MetricsStream = { KtorMetricsStream(createAgentHttpClient()) }, cadence: MetricsCadence = MetricsCadence())`
- 行为：
  1. 连上 WS → 收帧 → 按 cadence 接受（Idle 只接受每 5 秒一帧）；HTTP 轮询循环此时不采样；
  2. WS 连接失败且失败原因像"不支持"（404/握手失败）→ 立刻回退 HTTP 轮询（Phase 5B 的循环不变）；
  3. WS 中途断开 → 指数退避重连（1/2/4/8 秒封顶），期间保留最后一次读数（不立刻打回 `—`，由 5B 的 3 次失败规则收口）；
  4. 60 秒无触摸 → `ActivityClock.isIdle` 为真 → cadence 切到 5 秒；再次触摸立刻回到 1 秒。

- [x] **Step 1: 写失败测试**（注入假 stream：`flowOf(frame)` → Monitor 显示帧里的数字；`flow { throw }` → 回退到 HTTP 探针；Idle 时被丢掉的帧不进 UI）
- [x] **Step 2: 跑测试确认失败**
- [x] **Step 3: 实现**
- [x] **Step 4: 跑测试 + `assembleDebug`**
- [x] **Step 5: 提交** `feat: stream live metrics on the phone with an idle-aware cadence`

### Task C4: 端到端验收与文档

- [x] **Step 1: 模拟器 + 本机 Agent 走一遍**（WS 帧进界面 → 拔掉 Agent 观察重连 → 恢复）
- [x] **Step 2: 截图存 `docs/plans/screenshots/phase5c-*.png`，把实录写进本计划 §4**
- [x] **Step 3: 更新 roadmap（Phase 5 全部完成）、README、version-matrix（ktor-client-websockets 行）并提交** `docs: record the phase 5c acceptance run`

---

## 2. 验收标准（对照 roadmap Phase 5 的流式部分）

| roadmap 要求 | 落点 | 验证方式 |
| --- | --- | --- |
| `/ws/v1/metrics` 可用且鉴权 | Task C1 | Agent 侧 3 条测试（帧 / 401 / 间隔） |
| 手机端换成流式通道 | Task C2/C3 | `MetricsStreamTest` + `MetricsStreamIntegrationTest` + 模拟器实录 |
| ACTIVE 1s / IDLE 5s | Task C2/C3 | `MetricsCadenceTest`（含 60 秒边界）+ 集成测试（Idle 丢帧） |
| 断线自动重连 | Task C3 | 集成测试（断开 → 退避 → 重连后读数回来）+ 模拟器实录 |

## 3. 计划自检

1. **Spec 覆盖**：§5 鉴权（握手 Bearer）→ C1；§8 指标范围（同一份契约）→ C1/C2；§9 ACTIVE/IDLE 节奏 → C2/C3。**不做**：服务端 per-client 调速、亮度/像素位移（Phase 8）。
2. **占位符扫描**：无 TBD；C1/C2 的接口与测试意图写死，C3 给出四条可执行行为契约。
3. **类型一致性**：`MetricsStream.frames(baseUrl, token): Flow<AgentMetrics>`（C2）→ C3 的 `metricsStreamFactory`；`MetricsCadence.intervalMillis(isIdle)`（C2）→ C3 的门控；`ActivityClock.touch/isIdle`（C2）→ C3 与 HomeSurface 的指针喂入。

## 4. 验收实录

**运行环境：** 模拟器 `wall`（emulator-5554）+ 本机 Linux Agent（`--fake-metrics --fake-power`）+ `adb reverse tcp:9876 tcp:9876`，2026-09-20 21:23–21:31 EDT。

### 4.1 测试

| 步骤 | 命令 | 实测结果 |
| --- | --- | --- |
| C1 | `dotnet test WakeUpMyWall.Agent.slnx` | `Passed: 27`（含 `/ws/v1/metrics` 的 4 条：收帧 / 无 Token 401 / 普通 HTTP 被要求用 WS / 间隔可配 120 ms） |
| C2/C3 | `./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest` | 455 条全绿（新增：cadence 门控 2 条、ActivityClock 3 条、坏帧解析 2 条、`ws://` 换 scheme 1 条、流超时闸门 2 条、流式集成 2 条） |
| 组装 | `./gradlew :composeApp:assembleDebug` | BUILD SUCCESSFUL |

### 4.2 端到端（模拟器 + 本机 Agent）

| 步骤 | 操作 | 实测结果 |
| --- | --- | --- |
| 1 | 装上本阶段 APK，冷启动 | Agent 日志出现 `Request starting GET /ws/v1/metrics` 并**保持打开**（没有 `Request finished` 行）——手机已升级成 WebSocket |
| 2 | Monitor 连拍两帧（相隔 3 秒） | `Updated 21:23:32` → `Updated 21:23:35`，数值逐秒在动（CPU 25%→…、GPU 6%、网络 13.3 Mbps），即 1 Hz 的 ACTIVE 节奏 |
| 3 | 停掉 Agent，等 10 秒 | 流断开 → `Last update 21:24:02`、`No fresh metrics`，原因就是流的报错 `unexpected end of stream on http://127.0.0.1:9876/…`；读数全部 `—`；rail 落 `Ready to wake` |
| 4 | 重启 Agent，等约 20 秒 | **自动重连**：Agent 日志出现新的 `GET /ws/v1/metrics`（同样保持打开），Monitor 回到 `Updated 21:30:56` 且曲线重新变密（1 Hz 采样把 sparkline 填满） |

证据帧：

- [phase5c-stream-1.png](../../plans/screenshots/phase5c-stream-1.png) / [phase5c-stream-2.png](../../plans/screenshots/phase5c-stream-2.png)（相隔 3 秒的两次 1 Hz 更新时间）
- [phase5c-stream-drop.png](../../plans/screenshots/phase5c-stream-drop.png)（断流：全 `—` + 流的报错原文）
- [phase5c-stream-recovered.png](../../plans/screenshots/phase5c-stream-recovered.png)（重连后）

### 4.3 执行中发现并修掉的两个真问题（都在模拟器上实测暴露）

1. **共享 HTTP 客户端没装 WebSockets 插件** → `client.webSocket` 直接失败，App 静默退回 HTTP 轮询（Agent 日志里全是 `api/v1/system`，没有一条 `/ws/v1/metrics`）。修法：`createAgentHttpClient` 里 `install(WebSockets)`。
2. **拿 `http://…` 去连 WebSocket** → Agent 那边收到的是普通 GET，如实回 `400`（日志证据：`GET /ws/v1/metrics - 400`）。修法：`webSocketUrl()` 把 scheme 换成 `ws://`/`wss://`，并加了单测。
3. **对端"接住连接但不回帧"时流会永久挂着** → 重连逻辑轮不上；实测表现为 Agent 恢复后 App 一直留在 HTTP 兜底、20 秒都不回流式。修法：`Flow.withIdleTimeout(3 秒)` 闸门（对端 1 Hz 推，3 秒无帧即判死并取消上游）+ 只有"从来没收到过帧"才降级成 60 秒一探。

### 4.4 明确没验到的部分（不掩盖）

- **Idle 5 秒节奏没有在真机界面上验**：`MetricsCadence` / `ActivityClock` / `MetricsGate` 有 6 条纯逻辑单测（含"刚好 60 秒算 Idle"与"Idle 下 1 Hz 的帧被丢掉"），但"在模拟器上等 60 秒看它变慢"没有做——那需要 60 秒静置 + 精确计时，收益不抵噪声。
- 真机（Windows + 真指标）的读数一致性仍待用户按 agent/README 的清单验（与 Phase 5A 同一条）。
