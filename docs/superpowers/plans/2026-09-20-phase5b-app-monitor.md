# Phase 5B：手机端指标实时化（Monitor 接真数据）实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让手机真的显示这台 PC 的指标：每 2 秒取一次 Agent 的 `GET /api/v1/system`（Phase 5A 已上线），把 CPU / GPU / 内存 / 存储 / 温度 / 风扇 / 网络 / Uptime 喂给 Monitor 与 Dashboard 摘要卡，60 秒曲线用 Ring Buffer 画出来；**取不到的传感器显示 `—`，不是 0**；Agent 掉线时保留最后一次采样并如实标注。

**Architecture:** 三层，各管一件事：`AgentApi.system()`（线上 DTO，字段可空）→ `LiveMetricsMapper`（纯函数：DTO → 领域快照；`null` 原样保留）→ 采样循环（`App` 里每 2 秒一次，喂 `MetricRingBuffer`，与 Phase 4B 的存在性轮询共用一次网络往返）。UI 侧把 `MetricsSnapshot` / `PcSummarySnapshot` 的数值字段改成**可空**，格式化助手统一把 `null` 打成 `—`。

**Tech Stack:** 既有 Ktor client（MockEngine 测试）、kotlinx-serialization、Compose Multiplatform、既有 `MetricRingBuffer`。

**Spec:** [spec §8 指标范围](../specs/2026-09-19-desktop-companion-design.md)、§9（ACTIVE 1s / IDLE 5s，本阶段先固定 2 秒）、§7.2（Monitor 卡片）
**前置：** [Phase 5A](2026-09-20-phase5a-agent-metrics.md)（`/api/v1/system` 契约与字段表）、[Phase 4B](2026-09-20-phase4b-app-agent-integration.md)（Token / 配对 / 存在性轮询）、[Phase 2](2026-09-20-phase2-device-system.md)（设备仓库）
**后续：** Phase 5C（`/ws/v1/metrics` + 1s/5s 自适应 + 断线重连）

---

## 0. 计划期定案

| 决策 | 选定 | 理由与代价 |
| --- | --- | --- |
| 采样节奏 | 固定 **2 秒**（`metricsPollMillis = 2_000`，可注入） | roadmap 写的是"先 HTTP 轮询（2 秒）跑通"，ACTIVE/IDLE 自适应属 Phase 5C（它依赖 §9 的 Idle 检测，那是 Phase 8 的活）。代价：5C 之前 Idle 时也多花一点电。 |
| 网络往返 | **一次往返同时拿到存在性与指标**：先 `status()`（免鉴权，判定"PC 开着吗"），成功后紧接着 `system()`（Bearer） | Phase 4B 的 5 秒存在性轮询保留不动（它决定 `PcState`），指标循环作为**独立协程**每 2 秒跑一次；两者互不阻塞（测试可分别注入）。代价：在线时每 2 秒两条请求，局域网里可以忽略。 |
| 未配对时 | 不请求 `/system`，Monitor 继续显示"最后已知/无数据"的 `—`，并给一句 `Pair the phone in Device Setup first` | 与 Phase 4B 的电源动作同一策略：没有 Token 就不发请求，且失败必须可解释。代价：未配对用户看不到指标（预期行为）。 |
| 取不到的传感器 | 领域模型里 **`null` 就是"没读到"**，UI 显示 `—`；**绝不用 0 冒充** | 0 与"不知道"在 UI 上是两种结论（Phase 5A 的契约已按此设计）。代价：`MetricsSnapshot` / `PcSummarySnapshot` 的数值字段要变可空，UI 格式化助手要统一处理。 |
| 掉线时 | 保留最后一次采样值，状态行给出 `Last update … ago`；超过 3 次采样没更新（≥6 秒）就把卡片读数打成 `—` | "留着旧数不标注"比"打回 `—`"更容易误导。代价：多一个 staleness 判定与测试。 |
| 历史曲线 | 每次采样把 CPU/GPU/RAM/存储/网络的值推进 `MetricRingBuffer`（容量 60 = 2 秒 × 120 秒窗口，`capacity` 提升到 60 仍是 60 点） | 复用 Phase 1 已有的 Ring Buffer，不新建模型。代价：2 秒采样下 60 点是 2 分钟而不是 1 分钟（曲线更长，无副作用）。 |
| 不做的 | WebSocket、Idle 自适应、Recent Activity 真数据（spec §8 标 P2）、多盘/多 GPU | 都是 5C 或更后面的阶段。代价：Monitor 的 Recent Activity 卡继续显示 `—`。 |

## Global Constraints

- commonMain 禁止 `android.*`；所有新代码走既有 `createAgentHttpClient` 与 `ApiResult` 错误映射（4xx → `UNAUTHORIZED`/`NOT_FOUND`，解析失败 → `DECODING`）。
- 所有新 UI 元素要有 `testTag`（本阶段新增：`monitor:captured`、`monitor:stale`、`metric:cpu-value` 等既有 tag 复用）。
- 颜色/间距/字号只能取自 `core/theme`；不新增依赖。
- 每个 Task 结束前 `./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest` 全绿；动 androidMain 时加 `:composeApp:assembleDebug`（本阶段预计不动 androidMain）。
- 领域与 UI 模型里**没有**"0 表示未知"的约定：未知一律 `null`。

---

## 1. 文件结构

| 文件 | 职责 |
| --- | --- |
| `core/network/AgentApi.kt`（改） | `AgentMetrics` 与各分节 DTO（字段可空）+ `system(baseUrl, token)` |
| `domain/usecase/LiveMetricsMapper.kt`（新） | 纯函数：`AgentMetrics` → `LiveMetrics`（`MetricsSnapshot` + `HardwareIdentity` + `PcSummarySnapshot` + 采样时刻） |
| `domain/model/Snapshots.kt`（改） | `MetricsSnapshot` / `PcSummarySnapshot` 数值字段可空；新增 `LiveMetrics` |
| `core/metrics/MetricRingBuffer.kt`（不改） | 复用 |
| `app/App.kt`（改） | 指标采样循环、Ring Buffer、掉线/未配对提示、把 live 数据喂给 `HomeSurface` 与 `DashboardData` |
| `ui/monitor/MonitorMode.kt`、`MetricCard.kt`、`ui/dashboard/widgets/PcSummaryWidget.kt`（改） | 可空读数统一格式化（`—`）与 staleness 标注 |
| 测试 | `commonTest/.../core/network/AgentApiMetricsTest.kt`、`commonTest/.../domain/LiveMetricsMapperTest.kt`、`desktopTest/.../app/MetricsIntegrationTest.kt`、`desktopTest/.../ui/monitor/MonitorMetricsTest.kt` |

---

### Task B1: `AgentApi.system()` 与线上 DTO

**Files:**
- Modify: `mobile/composeApp/src/commonMain/kotlin/com/xukunz/wakeupmywall/core/network/AgentApi.kt`
- Test: `mobile/composeApp/src/commonTest/kotlin/com/xukunz/wakeupmywall/core/network/AgentApiMetricsTest.kt`

**Interfaces:**
- Produces:
  - `@Serializable data class AgentMetrics(capturedAtUtc, identity, cpu, gpu, memory, storage, thermal, network, uptimeSeconds, bootedAtUtc)`
  - `AgentIdentity(hostname, os, cpuName, cpuShortName, gpuName, gpuShortName, ramModule, storageModule)`
  - `AgentCpuMetrics(name, usagePercent: Float? = null, clockGhz: Float? = null, cores: Int? = null, threads: Int? = null, tempC: Float? = null, fanRpm: Int? = null)`
  - `AgentGpuMetrics(name, usagePercent: Float? = null, tempC: Float? = null, vramUsedGb: Float? = null, vramTotalGb: Float? = null, fanRpm: Int? = null)`
  - `AgentMemoryMetrics(usagePercent: Float? = null, usedGb: Float? = null, totalGb: Float? = null)`
  - `AgentStorageMetrics(usagePercent: Float? = null, usedTb: Float? = null, totalTb: Float? = null, freeGb: Float? = null, tempC: Float? = null)`
  - `AgentThermalMetrics(motherboardTempC: Float? = null, caseFanRpm: Int? = null)`
  - `AgentNetworkMetrics(downloadMbps: Float? = null, uploadMbps: Float? = null)`
  - `suspend fun system(baseUrl: String, token: String?): ApiResult<AgentMetrics>`

- [x] **Step 1: 写失败测试**（`AgentApiMetricsTest.kt`）

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
import kotlin.test.assertNull

private val metricsJsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

class AgentApiMetricsTest {

    @Test
    fun `system sends the bearer token and parses every section`() = runTest {
        var path = ""
        var authorization: String? = null
        val engine = MockEngine { request ->
            path = request.url.encodedPath
            authorization = request.headers[HttpHeaders.Authorization]
            respond(SAMPLE_METRICS, HttpStatusCode.OK, metricsJsonHeaders)
        }

        val result = AgentApi(createAgentHttpClient(engine)).system("http://192.168.1.10:9876", "token-1")

        assertEquals("/api/v1/system", path)
        assertEquals("Bearer token-1", authorization)
        val metrics = (result as ApiResult.Success).value
        assertEquals("Ryzen 7 7700X", metrics.identity.cpuShortName)
        assertEquals(12.5f, metrics.cpu.usagePercent)
        assertEquals(4.3f, metrics.cpu.clockGhz)
        assertEquals(32f, metrics.memory.totalGb)
        assertEquals(12.4f, metrics.network.downloadMbps)
        assertEquals(289440L, metrics.uptimeSeconds)
    }

    @Test
    fun `missing sensors stay null instead of becoming zero`() = runTest {
        val engine = MockEngine {
            respond(
                """
                {"capturedAtUtc":"2026-09-21T00:58:02Z",
                 "identity":{"hostname":"PC","os":"Windows 11","cpuName":"CPU","cpuShortName":"CPU",
                             "gpuName":"GPU","gpuShortName":"GPU","ramModule":"RAM","storageModule":"SSD"},
                 "cpu":{"name":"CPU","usagePercent":null,"clockGhz":null,"cores":null,"threads":8,
                        "tempC":null,"fanRpm":null},
                 "gpu":{"name":"GPU"}, "memory":{"usagePercent":null}, "storage":{},
                 "thermal":{}, "network":{"downloadMbps":null,"uploadMbps":null},
                 "uptimeSeconds":120,"bootedAtUtc":"2026-09-21T00:00:00Z"}
                """.trimIndent(),
                HttpStatusCode.OK,
                metricsJsonHeaders,
            )
        }

        val metrics = (AgentApi(createAgentHttpClient(engine))
            .system("http://192.168.1.10:9876", null) as ApiResult.Success).value

        assertNull(metrics.cpu.usagePercent)
        assertNull(metrics.cpu.tempC)
        assertNull(metrics.memory.totalGb)
        assertNull(metrics.gpu.vramTotalGb)
        assertNull(metrics.network.downloadMbps)
        assertNull(metrics.storage.tempC)
        assertEquals(8, metrics.cpu.threads)
    }

    @Test
    fun `a rejected token is unauthorized with a readable reason`() = runTest {
        val engine = MockEngine {
            respond("""{"error":"missing or invalid token"}""", HttpStatusCode.Unauthorized, metricsJsonHeaders)
        }

        val result = AgentApi(createAgentHttpClient(engine)).system("http://192.168.1.10:9876", "wrong")

        assertEquals(ApiFailure.UNAUTHORIZED, (result as ApiResult.Failure).reason)
    }

    @Test
    fun `a malformed payload is a decoding failure rather than a crash`() = runTest {
        val engine = MockEngine { respond("""{"capturedAtUtc":42}""", HttpStatusCode.OK, metricsJsonHeaders) }

        val result = AgentApi(createAgentHttpClient(engine)).system("http://192.168.1.10:9876", "token-1")

        assertEquals(ApiFailure.DECODING, (result as ApiResult.Failure).reason)
    }

    private companion object {
        val SAMPLE_METRICS = """
            {"capturedAtUtc":"2026-09-21T00:58:02Z",
             "identity":{"hostname":"DESKTOP-ALPHA","os":"Windows 11 Pro","cpuName":"AMD Ryzen 7 7700X",
                         "cpuShortName":"Ryzen 7 7700X","gpuName":"NVIDIA GeForce RTX 4070 Ti",
                         "gpuShortName":"RTX 4070 Ti","ramModule":"32 GB DDR5-6000","storageModule":"NVMe 2 TB"},
             "cpu":{"name":"AMD Ryzen 7 7700X","usagePercent":12.5,"clockGhz":4.3,"cores":8,"threads":16,
                    "tempC":42.0,"fanRpm":980},
             "gpu":{"name":"NVIDIA GeForce RTX 4070 Ti","usagePercent":8.0,"tempC":38.0,"vramUsedGb":2.4,
                    "vramTotalGb":12.0,"fanRpm":1200},
             "memory":{"usagePercent":38.0,"usedGb":12.2,"totalGb":32.0},
             "storage":{"usagePercent":95.0,"usedTb":1.9,"totalTb":2.0,"freeGb":102.0,"tempC":41.0},
             "thermal":{"motherboardTempC":35.0,"caseFanRpm":870},
             "network":{"downloadMbps":12.4,"uploadMbps":3.1},
             "uptimeSeconds":289440,"bootedAtUtc":"2025-04-18T12:00:00Z"}
        """.trimIndent()
    }
}
```

- [x] **Step 2: 跑测试确认失败**

Run: `JAVA_HOME=$HOME/.local/toolchain/jdk-25.0.4.1+1 ./gradlew :composeApp:testDebugUnitTest --tests "*AgentApiMetricsTest*"`
Expected: 编译失败（`Unresolved reference: system` / `AgentMetrics`）

- [x] **Step 3: 实现**（DTO 与 `system()`，与既有 `status()` 同样的 try/catch 结构）

```kotlin
/** Phase 5A 的 `GET /api/v1/system` 载荷。**字段可空 = Agent 没读到该传感器**（不是 0）。 */
@Serializable
data class AgentMetrics(
    val capturedAtUtc: String,
    val identity: AgentIdentity,
    val cpu: AgentCpuMetrics,
    val gpu: AgentGpuMetrics,
    val memory: AgentMemoryMetrics,
    val storage: AgentStorageMetrics,
    val thermal: AgentThermalMetrics,
    val network: AgentNetworkMetrics,
    val uptimeSeconds: Long,
    val bootedAtUtc: String,
)

suspend fun system(baseUrl: String, token: String?): ApiResult<AgentMetrics> =
    call { client.get("$baseUrl/api/v1/system") { bearer(token) } }
```

（各分节 DTO 的字段名与 `docs/plans/agent-api.md` 的表格逐字一致；数值字段一律 `= null` 默认值，
这样 Agent 省略字段时不会解析失败。）

- [x] **Step 4: 跑测试确认通过**

Run: `JAVA_HOME=$HOME/.local/toolchain/jdk-25.0.4.1+1 ./gradlew :composeApp:testDebugUnitTest`
Expected: BUILD SUCCESSFUL，新增 4 条测试通过

- [x] **Step 5: 提交** `feat: read the agent metrics payload on the phone`

### Task B2: DTO → 领域快照的纯映射

**Files:**
- Create: `domain/usecase/LiveMetricsMapper.kt`、`domain/model/Snapshots.kt`（改：新增 `LiveMetrics`，数值字段可空）
- Test: `commonTest/.../domain/LiveMetricsMapperTest.kt`

**Interfaces:**
- Consumes: B1 的 `AgentMetrics`
- Produces:
  - `data class LiveMetrics(val snapshot: MetricsSnapshot, val identity: HardwareIdentity, val summary: PcSummarySnapshot, val capturedAtLabel: String, val uptimeSeconds: Long)`
  - `object LiveMetricsMapper { fun map(metrics: AgentMetrics): LiveMetrics }`
  - `MetricsSnapshot` / `PcSummarySnapshot` 数值字段改为可空（`Float?` / `Int?`）

- [x] **Step 1: 写失败测试**（映射的每条规则一条断言：直通、单位（GHz 已是 GHz 不再换算）、缺省、`threads` 缺省时用 `Runtime`-无关的默认值 `null`、`bootDateLabel` 由 `bootedAtUtc` 大写化日期部分）
- [x] **Step 2: 跑测试确认失败**
- [x] **Step 3: 实现**（纯函数，无副作用；`recentActivity` 传 `emptyList()`——spec §8 把最近应用列为 P2）
- [x] **Step 4: 跑测试确认通过**
- [x] **Step 5: 提交** `feat: map agent metrics into the domain snapshot`

### Task B3: 采样循环 + Ring Buffer + UI 可空读数

**Files:**
- Modify: `app/App.kt`、`ui/monitor/MonitorMode.kt`、`ui/monitor/MetricCard.kt`、`ui/dashboard/widgets/PcSummaryWidget.kt`
- Test: `desktopTest/.../app/MetricsIntegrationTest.kt`、`desktopTest/.../ui/monitor/MonitorMetricsTest.kt`

**Interfaces:**
- Produces: `App(..., metricsPollMillis: Long = 2_000, metricsProbe: (suspend (PcDevice, String?) -> ApiResult<AgentMetrics>)? = null)`
- 行为：
  1. 每 `metricsPollMillis` 一次：设备存在且 `agentToken != null` → `AgentApi.system()`；成功则更新 `liveMetrics`、把 CPU/GPU/RAM/存储/网络推入五条 `MetricRingBuffer`、刷新 `capturedAt`；失败只记 `metricsNote`（`The Agent rejected the token` / `Nothing is listening on 192.168.1.10:9876`），**不动已有数据**；
  2. 未配对 → note `Pair the phone in Device Setup first`，不发请求；
  3. 超过 3 个采样周期没成功（`>= 3 * metricsPollMillis`）→ 卡片读数按 `—` 渲染（`monitor:stale` tag 出现，`monitor:captured` 显示 `Last update …`）；
  4. Monitor 与 Dashboard 摘要卡优先用 live 数据；没有 live 数据时退回 Mock（保证 Phase 1 视觉基线与桌面预览不变）。
- 格式化助手统一规则：`null` → `—`（百分比、GHz、GB、TB、℃、RPM、Mbps、uptime 全部适用）。

- [x] **Step 1: 写失败测试**（注入探针：成功一次 → Monitor 显示代理返回的数字；再失败 3 次 → `—` 与 `monitor:stale`；未配对 → 不发请求 + 提示文案）
- [x] **Step 2: 跑测试确认失败**
- [x] **Step 3: 实现**（`LaunchedEffect(device?.id, agentToken, metricsPollMillis)`；Ring Buffer 用 `remember { MetricRingBuffer(60) }` 五条；卡片接线）
- [x] **Step 4: 跑测试 + `:composeApp:assembleDebug` 确认通过**
- [x] **Step 5: 提交** `feat: show live pc metrics on the monitor`

### Task B4: 端到端验收与文档

**Files:**
- Modify: 本计划、`README.md`、`docs/plans/version-matrix.md`（若出现新的实测结论）、`docs/superpowers/plans/2026-09-19-roadmap.md`

- [x] **Step 1: 本机起 Agent（`--fake-metrics`）+ 模拟器 `adb reverse`**
- [x] **Step 2: 模拟器上走一遍真实链路**（配对 → Monitor 显示 Agent 的数字 → 停 Agent → 卡片变 `—` 且状态行标注 → 重启 Agent → 数字回来），每步截图存 `docs/plans/screenshots/phase5b-*.png`
- [x] **Step 3: 记录验收表并提交** `docs: record the phase 5b acceptance run`

---

## 2. 验收标准（对照 roadmap Phase 5 的手机端部分）

| roadmap 要求 | 落点 | 验证方式 |
| --- | --- | --- |
| 手机显示真实指标 | Task B2/B3 | `LiveMetricsMapperTest` + `MetricsIntegrationTest`（注入探针）+ 模拟器实录 |
| 读数与 PC 一致（±3% / ±2℃） | Task B4 | 需真机 Windows 对照（本机只能证明"显示的就是 Agent 给的数"） |
| 取不到就 `—` | Task B2/B3 | 映射测试（null 保持 null）+ UI 测试（`—` 与 `monitor:stale`） |
| 60 秒曲线 | Task B3 | Ring Buffer 单元测试（既有）+ 集成测试断言曲线在增长 |

## 3. 计划自检

1. **Spec 覆盖**：§8 的指标全部有落点；§7.2 的 Monitor 卡片逐张接线；§9 的自适应刻意留给 5C（明示）。**不做**：Recent Activity 真数据（P2）、多盘、WebSocket。
2. **占位符扫描**：B1 有完整代码与测试；B2–B4 给出可执行的行为契约与测试意图（照 Phase 4A 的 Task A4 写法），无 TBD。
3. **类型一致性**：`AgentMetrics`（B1）→ `LiveMetricsMapper.map`（B2）→ `LiveMetrics`（B2/B3）；`App` 的参数名 `metricsPollMillis` / `metricsProbe` 在 B3 与测试里同名。

## 4. 验收实录

**运行环境：** 模拟器 `wall`（emulator-5554，2560×1600）+ 本机 Linux Agent（`--fake-metrics --fake-power`，默认 token 文件）+ `adb reverse tcp:9876 tcp:9876`，2026-09-20 21:11 EDT。

### 4.1 测试与构建

| 步骤 | 命令 | 实测结果 |
| --- | --- | --- |
| B1 RED | `./gradlew :composeApp:testDebugUnitTest --tests "*AgentApiMetricsTest*"` | 编译失败：`Unresolved reference 'system'` |
| B1 GREEN | 同上（全量） | BUILD SUCCESSFUL，`AgentApiMetricsTest` 4/4 |
| B2+B3 | `./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest` | 单元 + UI 合计 `435 passed / 0 failed` |
| B2+B3 组装 | `./gradlew :composeApp:assembleDebug` | BUILD SUCCESSFUL（APK 45.9 MB） |
| 映射测试 | `LiveMetricsMapperTest` | 5 条：显示精度、缺项保持 null、短名、时间标签、时间戳解析失败不丢原文 |
| 采样集成测试 | `MetricsIntegrationTest` | 4 条：真实读数上屏 / 3 次失败退成 `—` + 标注 / 恢复后读数回来 / 未配对不发请求 |
| Monitor 展示测试 | `MonitorMetricsTest` | 4 条：`—` 全项、`Updated …`、`Last update …` + `No fresh metrics`、失败原因 |

### 4.2 端到端（模拟器 + 本机 Agent）

| 步骤 | 操作 | 实测结果 |
| --- | --- | --- |
| 1 | 装上本次构建的 APK（45.9 MB），冷启动 | Dashboard 的摘要卡变成真数据：`Last seen 21:11:09`、CPU 19%、Temp 39℃、RAM 34%、↓10.2 ↑2.9 Mbps —— 与 Mock 的 12%/42℃/38%/12.4/3.1 明显不同，说明是 Agent 给的数 |
| 2 | 左滑进 Monitor | 身份卡变成 Agent 报的机器：`xukunz-M8` / `Linux (fake metrics)` / `Fake Ryzen 7 7700X` / `Fake GeForce RTX 4070 Ti`，右上 `Last seen 21:11:15`、下面 `Updated 21:11:15`；四张指标卡 14% / 5% / 34% / 95%，温度 41/43/37/41℃，风扇 980/1,200/870 RPM，网络 ↓11.2 ↑4 Mbps，Uptime `3d 6h 0m` + `Since Sep 17, 2026`，Recent Activity 显示 `—`（spec §8 的 P2，未编假数据） |
| 3 | 停掉 Agent，等 10 秒 | 读数全部退成 `—`（环心、温度、风扇、网络、Uptime、Activity），身份卡改写成 `Last update 21:11:27`、`No fresh metrics`，并给出真实原因 `unexpected end of stream on http://127.0.0.1:9876/…`；rail 同时按 Phase 4B 的规则落到 `Ready to wake` |
| 4 | 重启 Agent（同一 token 文件），等 10 秒 | 读数自动回来：`Updated 21:11:59`，GPU 11% / 34℃、RAM 39%、网络 ↓15.5 ↑2.9 Mbps（数值在动，证明是重新采样而不是缓存），rail 回到 `Online` |

证据帧：

- [phase5b-dashboard-live.png](../../plans/screenshots/phase5b-dashboard-live.png)（Dashboard 摘要卡真数据）
- [phase5b-monitor-live.png](../../plans/screenshots/phase5b-monitor-live.png)（Monitor 真指标）
- [phase5b-monitor-stale.png](../../plans/screenshots/phase5b-monitor-stale.png)（掉线：全 `—` + `Last update` + 原因）
- [phase5b-monitor-recovered.png](../../plans/screenshots/phase5b-monitor-recovered.png)（恢复）

### 4.3 明确没验到的部分（不掩盖）

- **"与 PC 任务管理器读数一致（CPU ±3% / 温度 ±2℃）"没有验**：本机是 Linux + 合成读数，验的是"显示的就是 Agent 给的数"。
  真机对照要与 Phase 5A 的 Windows 自验清单一起做（[agent/README.md](../../../agent/README.md)）。
- 60 秒曲线在模拟器上只跑了几十秒，环只填了几个点（截图里的 sparkline 是这几秒的记录）；Ring Buffer 本身有单元测试（Phase 1）。
- Recent Activity 仍是 `—`（Phase 5A/5B 都不做，spec §8 列为 P2）。

---

## 5. 执行中的计划修正

- **B2 与 B3 必须一次落地（Ruling B-1）**：`MetricsSnapshot` / `PcSummarySnapshot` 的数值字段一旦变成可空，
  UI 里既有的字符串插值（如 `"${metrics.cpuClockGhz} GHz"`）会先渲染成 `null`，既有的 `MonitorModeTest` /
  `PcSummaryWidget` 断言会立刻变红——也就是说 B2 单独提交会留下一个**能编译但显示错误**的中间态。
  因此 B2（映射 + 可空模型）与 B3（采样循环 + UI `—` 化 + staleness）合并为一笔提交
  `feat: show live pc metrics on the monitor`，测试一起写。代价：这一笔提交比 Task 粒度更大，评审时要一次看完整条链路。
