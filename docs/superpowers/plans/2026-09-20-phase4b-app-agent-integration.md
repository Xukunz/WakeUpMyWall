# Phase 4B：手机端接入 Agent 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 手机端真正用上 Phase 4A 的 Agent：能配对拿 Token 并存进 Android Keystore，能用真实接口执行睡眠 / 关机 / 重启，并且**周期性探测 `/api/v1/status` 来判断 PC 是否开着**（在线 → `Online`；主机在但 Agent 不在 → `Agent unavailable`；都不在 → `Ready to wake`）。

**Architecture:** 沿用既有骨架：`core/network/AgentApi` 扩三个调用（配对 / 动作 / 电源），`AgentTokenStore` 是新的"按设备存 Token"边界（接口在 commonMain、Keystore 实现在 androidMain、测试用内存实现），`App` 里加一个**存在性轮询循环**（每 N 秒 `status()` → 喂给既有 `PcStateMachine`）。电源命令只负责"发命令 + 立刻进入瞬态"，之后的状态完全交给轮询与状态机现有的规则（`RESTARTING` 在 Agent 掉线时保持不变、重新应答才回 `ONLINE`）——这样不新增第二套状态推导。

**Tech Stack:** 既有 Ktor client（MockEngine 测试）、kotlinx-serialization、Android Keystore（AES/GCM/NoPadding，密文复用既有 `KeyValueStore`）。

**Spec:** [spec §5 鉴权与超时](../specs/2026-09-19-desktop-companion-design.md)、§7.5（Device Setup 的 `Advanced / Agent` 区）、§4（状态表）
**前置：** [Phase 4A](2026-09-20-phase4a-pc-agent.md)（Agent 服务端 + 契约，已实现并发布 Windows 包）、[Phase 2](2026-09-20-phase2-device-system.md)（设备仓库与 KeyValueStore）、[Phase 3](2026-09-20-phase3-wol.md)（WOL 与状态派生）

---

## 0. 计划期定案

| 决策 | 选定 | 理由与代价 |
| --- | --- | --- |
| Token 存哪 | 不在 `PcDevice` 里（spec 明确禁止进模型序列化路径）；新建 `AgentTokenStore`，Android 实现用 **Keystore AES-GCM** 加密后把密文写进既有 `KeyValueStore`（DataStore） | Token 明文不落盘。代价：Keystore 在 JVM 单测里不可用，那条实现只能靠模拟器验收（androidUnitTest 里跑不了 `AndroidKeyStore`）。 |
| 存在性轮询节奏 | 每 **5 秒**一次 `GET /api/v1/status`（可在 `App` 参数里改，测试注入更短的值） | 5 秒对"桌面面板"足够且不啰嗦；Phase 5 换成指标流后这个循环只保留"离线判定"。代价：不在前台时会耗一点电（Phase 8 的 Keep Screen On / 生命周期策略里收口）。 |
| 电源命令后的状态 | 只把状态推进到瞬态（`SLEEPING` / `SHUTTING_DOWN` / `RESTARTING`），**不假装已完成**；之后由轮询 + 状态机决定（Agent 掉线 → `WOL_READY`/`OFFLINE`；重启后重新应答 → `ONLINE`） | 复用状态机既有规则，不写第二套推导。代价：用户看到"命令已接受"到"真的关机"之间有几秒的瞬态。 |
| 锁屏 | 接口实现了（`lock`），但 **Phase 4B 不加 UI 按钮** | 概念图的 Power Rail 只有三个次级动作（Sleep / Shut Down / Restart），加锁屏按钮属于设计变更，留给 Phase 5/7 一起做。代价：lock 端点暂时只能被集成测试用。 |
| 未配对时的行为 | 电源动作给出明确提示（`Pair the phone in Device Setup first`），不发请求 | 与 Phase 3 的"失败必须可解释"一致。代价：用户要先配对（一次性）。 |

## Global Constraints

- commonMain 禁止 `android.*`；Keystore 实现在 androidMain，通过接口注入。
- 所有新 UI 元素要有 `testTag`（本阶段新增：`device:pairing-code`、`device:pair`、`device:pairing-state`、`device:unpair`）。
- 颜色/间距/字号只能取自 `core/theme`；不新增依赖（Keystore 与 Ktor 都是既有能力）。
- 每笔提交前 `./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest` 全绿；动 androidMain 时加 `:composeApp:assembleDebug`。
- Agent 请求超时沿用 `createAgentHttpClient`（2.5 s）；失败要有可读原因，不许静默。

---

## 1. 文件结构

| 文件 | 职责 |
| --- | --- |
| `core/network/AgentApi.kt`（改） | 新增 `pair` / `actions` / `power`；`PowerAction` 枚举（sleep/shutdown/restart/lock 的路径） |
| `core/agent/AgentTokenStore.kt`（新，commonMain） | `read(deviceId)` / `write(deviceId, token)` / `clear(deviceId)`；`InMemoryAgentTokenStore` |
| `androidMain/.../agent/KeystoreAgentTokenStore.kt`（新） | Keystore AES-GCM 加密 + `KeyValueStore` 存密文 |
| `ui/settings/AgentSection.kt`（新） | Device Setup 的 `Advanced / Agent` 区：配对码输入、Pair、状态、Unpair |
| `ui/settings/DeviceSetupScreen.kt`（改） | 挂上 `AgentSection` |
| `app/App.kt`（改） | 建 `AgentTokenStore`；存在性轮询；电源命令走真实接口；Rail 事件与提示 |
| `androidMain/.../MainActivity.kt`（改） | 注入 `KeystoreAgentTokenStore` |
| 测试 | `commonTest/.../core/network/AgentApiAgentTest.kt`、`commonTest/.../core/agent/AgentTokenStoreTest.kt`、`desktopTest/.../ui/settings/AgentSectionTest.kt`、`desktopTest/.../app/AgentIntegrationTest.kt` |

---

### Task B1: AgentApi 扩配对 / 动作 / 电源

**Files:**
- Modify: `mobile/composeApp/src/commonMain/kotlin/com/xukunz/wakeupmywall/core/network/AgentApi.kt`
- Test: `mobile/composeApp/src/commonTest/kotlin/com/xukunz/wakeupmywall/core/network/AgentApiAgentTest.kt`

**Interfaces:**
- Produces:
  - `enum class PowerAction(val path: String) { SLEEP("sleep"), SHUTDOWN("shutdown"), RESTART("restart"), LOCK("lock") }`
  - `suspend fun pair(baseUrl: String, code: String): ApiResult<PairingResponse>`（`PairingResponse(token)`）
  - `suspend fun actions(baseUrl: String, token: String?): ApiResult<List<ActionEntry>>`（`ActionEntry(id, name)`）
  - `suspend fun power(baseUrl: String, token: String?, action: PowerAction): ApiResult<PowerResponse>`（`PowerResponse(action, accepted)`）

- [x] **Step 1: 写失败测试**（MockEngine：配对成功 / 403 错误码 / 电源 200 / 电源 404 未知动作 / actions 解析）

```kotlin
@Test
fun `pairing failure surfaces the reason instead of throwing`() = runTest {
    val engine = MockEngine { respond("""{"error":"invalid or expired pairing code"}""", HttpStatusCode.Forbidden) }
    val api = AgentApi(createAgentHttpClient(engine))

    val result = api.pair("http://192.168.1.10:9876", "000000")

    assertEquals(ApiFailure.UNAUTHORIZED, (result as ApiResult.Failure).reason)  // 4xx 统一映射，消息带原文
    assertTrue(result.message.contains("invalid or expired"))
}
```

- [x] **Step 2: 跑测试确认失败** → `./gradlew :composeApp:desktopTest --tests "*AgentApiAgentTest*"`
- [x] **Step 3: 实现**（沿用 `status()` 的 try/catch 结构：CancellationException 重抛、超时→TIMEOUT、4xx→UNAUTHORIZED/NOT_FOUND、5xx→SERVER、解析失败→DECODING）
- [x] **Step 4: 跑全量测试确认通过**
- [x] **Step 5: 提交** `feat: talk to the agent for pairing, actions and power`

### Task B2: Token 存储（Keystore）与 Device Setup 的 Agent 区

**Files:**
- Create: `core/agent/AgentTokenStore.kt`、`androidMain/.../agent/KeystoreAgentTokenStore.kt`、`ui/settings/AgentSection.kt`
- Modify: `ui/settings/DeviceSetupScreen.kt`、`androidMain/.../MainActivity.kt`、`app/App.kt`（注入 store）
- Test: `commonTest/.../AgentTokenStoreTest.kt`、`desktopTest/.../AgentSectionTest.kt`

**Interfaces:**
- Produces: `interface AgentTokenStore { suspend fun read(deviceId: String): String?; suspend fun write(deviceId: String, token: String); suspend fun clear(deviceId: String) }`；`InMemoryAgentTokenStore`；`KeystoreAgentTokenStore(store: KeyValueStore)`；`AgentSection(host, port, paired: Boolean, isPairing: Boolean, onPair: (String) -> Unit, onUnpair: () -> Unit, modifier)`
- 行为约定：配对成功后 `device:pairing-state` 显示 `Paired · token stored in Keystore`；未配对显示 `Not paired yet`；`device:unpair` 清掉 Token（并可再次配对）。

- [x] **Step 1: 写失败测试**（内存 store 的读写/清除；AgentSection 的 Pair 回调与两态文案）
- [x] **Step 2: 跑测试确认失败**
- [x] **Step 3: 实现**（Keystore：`KeyGenParameterSpec` + `AES/GCM/NoPadding`；IV 与密文一起 base64 后写入 `KeyValueStore("agent-token:<deviceId>")`）
- [x] **Step 4: 跑全量测试 + `assembleDebug` 确认通过**
- [x] **Step 5: 提交** `feat: pair the phone and keep the agent token in the keystore`

### Task B3: 存在性轮询（PC 是否开着）

**Files:**
- Modify: `app/App.kt`
- Test: `desktopTest/.../app/AgentIntegrationTest.kt`

**Interfaces:**
- Produces: `App(..., agentPollMillis: Long = 5_000, agentStatusProbe: (suspend (PcDevice, String?) -> ApiResult<AgentStatus>)? = null)`
- 行为：每 `agentPollMillis` 探一次；成功 → `PcEvent.AgentResponded`，失败 → `PcEvent.AgentLost`；**瞬态期间照常轮询**，让状态机自己收敛（`RESTARTING` 掉线保持、重新应答回 `ONLINE`）。Agent 不可达且设备可唤醒 → `WOL_READY`（Phase 3 已实现），不可唤醒 → `OFFLINE`。

- [x] **Step 1: 写失败测试**（注入探针：先成功 → `Online`；后失败 → `Ready to wake`；重启过程中 Agent 回来 → `Online`）
- [x] **Step 2: 跑测试确认失败**
- [x] **Step 3: 实现**（`LaunchedEffect(device?.id) { while (true) { ...; delay(agentPollMillis) } }`，与 WakeSequence 的轮询互不干扰）
- [x] **Step 4: 跑全量测试确认通过**
- [x] **Step 5: 提交** `feat: detect whether the pc is on by polling the agent`

### Task B4: 电源动作接到真实接口

**Files:**
- Modify: `app/App.kt`
- Test: `desktopTest/.../app/AgentIntegrationTest.kt`（追加）

**Interfaces:**
- 行为：`RailEvent.Sleep/Shutdown/Restart` → 已配对且 Agent 可达时 `POST /api/v1/power/*` → 成功即进瞬态（`SLEEPING`/`SHUTTING_DOWN`/`RESTARTING`）；未配对 → note `Pair the phone in Device Setup first`；请求失败 → note 带上原因（`The Agent rejected the token` / `Nothing is listening on …`）。

- [ ] **Step 1: 写失败测试**（已配对 → POST 发出 + 状态进 `Sleeping…`；未配对 → 不发请求 + 出现配对提示；401 → 提示 token 问题）
- [ ] **Step 2: 跑测试确认失败**
- [ ] **Step 3: 实现**（把 Phase 3 的"needs the PC Agent"占位 note 换成真实调用；失败原因沿用 `ConnectionFailure` 文案）
- [ ] **Step 4: 跑全量测试 + `assembleDebug`**
- [ ] **Step 5: 提交** `feat: run sleep, shutdown and restart through the agent`

### Task B5: 端到端验收（模拟器 + 本机 Agent）与文档

**Files:**
- Modify: 本计划（勾选 + 验收实录）、`docs/plans/version-matrix.md`、`README.md`

- [ ] **Step 1: 本机起 Agent（`--fake-power`）并用 `adb reverse` 接到模拟器**

```bash
export PATH="$HOME/.dotnet-local:$PATH" DOTNET_ROOT="$HOME/.dotnet-local"
(setsid nohup /tmp/agent-linux/WakeUpMyWall.Agent --fake-power > /tmp/agent-e2e.log 2>&1 < /dev/null &)
ADB=/home/xukunz/.local/toolchain/android-sdk/platform-tools/adb
"$ADB" -s emulator-5554 reverse tcp:9876 tcp:9876
grep -o '配对码：[0-9]\{6\}' /tmp/agent-e2e.log | tail -1
```

- [ ] **Step 2: 模拟器上走一遍真实链路**
  1. Settings → Device Setup 填 Agent Host `127.0.0.1`、Agent Port `9876`、Save；
  2. 在 Agent 区输入日志里的配对码 → Pair → 状态变 `Paired`；
  3. 回主页：rail 应变 `Online`（存在性轮询打到本机 Agent）；
  4. 点 Sleep → 状态进 `Sleeping…`，并在 `/tmp/agent-e2e.log` 里看到 `sleep` 被记录（FakePowerController 只记录不执行）；
  5. 关掉 Agent → 5 秒内 rail 变 `Ready to wake`；重启 Agent → 变回 `Online`。
  每一步截图存 `docs/plans/screenshots/phase4b-*.png`。

- [ ] **Step 3: 记录验收表并提交** `docs: record the phase 4b acceptance run`

---

## 2. 验收标准（对照 roadmap Phase 4 段）

| roadmap 要求 | 落点 | 验证方式 |
| --- | --- | --- |
| 手机端四种电源操作全部生效 | Task B4（三种有 UI）+ Phase 4A 的四个端点 | 模拟器上 Sleep/Shutdown/Restart 各发一次，Agent 日志里能看到对应动作被记录；真机由用户在 Windows 上按 A4 清单验 |
| 未携带 Token 返回 401 | Phase 4A（服务端）+ Task B4（手机端把 401 显示成"token 有问题"） | 4A 测试 + B4 的 UI 测试 |
| 未知 action 404 且不执行 | Phase 4A | 4A 测试（执行列表为空） |
| 心跳 / 在线检测 | Task B3 | `AgentIntegrationTest` + B5 的模拟器实录 |

## 3. 计划自检

1. **Spec 覆盖**：§5 鉴权（Bearer + 超时 2–3 s）→ B1/B4；§7.5 `Advanced / Agent` 区 → B2；§4 状态表（ONLINE / AGENT_UNAVAILABLE 的区别）→ B3；Token 进 Keystore（§5 安全条款）→ B2。**刻意不做**：锁屏按钮（设计变更）、指标（Phase 5）。
2. **占位符扫描**：无 TBD。B1–B4 的代码在实现时按既有模式（`AgentApi.status` 的 try/catch、`DeviceSetupFormTest` 的 UI 测试写法）落地，测试意图与断言值已写死。
3. **类型一致性**：`PowerAction` → `AgentApi.power` → `ApiResult<PowerResponse>`；`AgentTokenStore`（read/write/clear）→ `App` 接线 → `AgentSection` 回调；状态事件沿用 Phase 3/4A 已定的 `PcEvent.AgentResponded / AgentLost`。
