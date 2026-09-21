# WakeUpMyWall Agent API（V1）

权威契约是 [spec §5](../superpowers/specs/2026-09-19-desktop-companion-design.md)：`GET /status`、`GET /system`、`GET /actions`、`POST /power/*`、`POST /actions/{id}`、`WS /ws/v1/metrics`。

这份文档只补 spec 没写死、但 Phase 4A 必须定下来的两件事：**配对端点**与**状态码语义**。

## 端点与鉴权

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/v1/status` | **免鉴权** | 只回最小信息：`hostname` / `agentVersion` / `uptimeSeconds` / `paired`。免鉴权是为了让"PC 开着但还没配对"这一状态可达（spec §4 的 `ONLINE` 判定也需要它）。 |
| POST | `/api/v1/pairing` | **免鉴权** | body `{"code":"123456"}`；配对码由 PC 端启动时生成并打印在控制台与日志里，6 位数字、5 分钟有效、一次性。成功回 `{"token":"…"}`。 |
| POST | `/api/v1/unpair` | Bearer | 忘掉当前 Token 并回一个新配对码 `{"pairingCode":"123456"}`。手机端 Unpair 必须调用它：否则 PC 端还留着旧 Token，重新配对会一直 409 `already paired`。 |
| GET | `/api/v1/system` | Bearer | 指标（Phase 5A 起为真实载荷，见 §指标载荷）。取不到的传感器一律 `null`，绝不用 0 冒充。 |
| GET | `/api/v1/actions` | Bearer | `id → 显示名` 白名单。 |
| POST | `/api/v1/actions/{id}` | Bearer | 只允许白名单内 id。 |
| POST | `/api/v1/power/sleep` `/shutdown` `/restart` `/lock` | Bearer | 四个电源动作；命令走参数数组，绝不拼字符串。 |
| WS | `/ws/v1/metrics` | Bearer（**握手时**校验） | Phase 5C 起可用：连上后按 1 秒推一帧，帧内容与 `GET /api/v1/system` **逐字相同**；间隔可用 `Agent:MetricsIntervalMs` 覆盖（默认 1000）。没有 Token 的握手直接 401，连接不会建立；用普通 HTTP 请求这个路径会得到 400。 |

## 状态码语义

| 码 | 出现场景 | 语义 |
| --- | --- | --- |
| 200 | 动作已受理 | 服务端**已经把命令交给系统执行**（不是"PC 已完成睡眠/关机"——那要等 Agent 掉线来间接判断）。 |
| 401 | 受保护端点缺少或携带错误 Bearer Token | 不产生任何副作用：过滤器在处理器之前返回。 |
| 403 | 配对码错误或过期 | 不签发 Token。 |
| 404 | 未知电源动作 / 未知 action id | **不执行任何外部命令**（验收要求，测试断言执行列表为空）。 |
| 409 | 已经配对过，又调 `/pairing` | 想重新配对需要删掉 `agent.json` 并重启服务。 |
| 500 | 系统命令返回非 0 退出码 | 回 `{"error":"…"}`，附上命令与退出码。 |
| 501 | — | 已不再使用：Phase 5A 起 `/api/v1/system` 返回真实指标（Phase 4 的 501 占位已删除）。 |

## 指标载荷（`GET /api/v1/system`，Phase 5A）

JSON 属性名沿用 Minimal API 的 camelCase；**除身份与时间字段外全部可空**——读不到就是 `null`（例如没有 LibreHardwareMonitor 支持的传感器）。单位写死如下，手机端不再做换算：

| JSON 路径 | 单位 | 来源 |
| --- | --- | --- |
| `capturedAtUtc` | ISO-8601（UTC） | 采样时刻 |
| `identity.hostname` / `identity.os` | 文本 | `Environment.MachineName` / `RuntimeInformation.OSDescription` |
| `identity.cpuName` / `identity.cpuShortName` | 文本 | 注册表 `ProcessorNameString`（短名去掉 `AMD ` / `Intel ` / `NVIDIA GeForce ` 前缀） |
| `identity.gpuName` / `identity.gpuShortName` | 文本 | LibreHardwareMonitor 的 GPU 硬件名 |
| `identity.ramModule` / `identity.storageModule` | 文本 | LHM 的 DIMM / 存储硬件名 |
| `cpu.usagePercent` / `cpu.tempC` | % / °C | LHM `CPU Total` / `CPU Package` |
| `cpu.clockGhz` | GHz | LHM `CPU Core #n` 的平均值（源单位 MHz） |
| `cpu.cores` / `cpu.threads` | 个 | 时钟传感器计数 / `Environment.ProcessorCount` |
| `cpu.fanRpm` | RPM | 主板/CPU 硬件里名字含 `CPU` 的风扇 |
| `gpu.usagePercent` / `gpu.tempC` / `gpu.vramUsedGb` / `gpu.vramTotalGb` / `gpu.fanRpm` | % / °C / GB / GB / RPM | LHM `GPU Core`、`D3D Dedicated Memory Used`；显存总量来自注册表 |
| `memory.usagePercent` / `memory.usedGb` / `memory.totalGb` | % / GB / GB | LHM `Memory` / `Memory Used` + `Memory Available` |
| `storage.usagePercent` / `storage.usedTb` / `storage.totalTb` / `storage.freeGb` / `storage.tempC` | % / TB / TB / GB / °C | 系统盘容量来自 `DriveInfo`，温度来自 LHM |
| `thermal.motherboardTempC` / `thermal.caseFanRpm` | °C / RPM | LHM 主板硬件 |
| `network.downloadMbps` / `network.uploadMbps` | Mbps | LHM 吞吐（**源单位是字节/秒**，按 `×8/1e6` 换算） |
| `uptimeSeconds` / `bootedAtUtc` | 秒 / ISO-8601 | `Environment.TickCount64` |

非 Windows 平台（开发机 / CI）与 `--fake-metrics` 走合成读数：数值由时间决定（可复现、随时间变化），
契约与真实载荷完全一致，但 `identity.os` 会写成 `Linux (fake metrics)`，一眼看得出是假的。

## 配对流程（V1）

1. 服务启动（或首次运行）生成 6 位配对码 → 控制台 + 日志：`WakeUpMyWall 配对码 123456`。
2. 手机（Phase 4B）`POST /api/v1/pairing {"code":"123456"}` → 拿到 Token。
3. Token 由 Agent 落到 `agent.json`（Windows：`%ProgramData%\WakeUpMyWall\agent.json`），手机存进 Android Keystore。
4. 之后所有受保护请求带 `Authorization: Bearer <token>`；服务重启后 Token 仍然有效。

安全边界：只监听局域网；**没有任意命令执行接口**；动作只能是白名单 id（spec §5 安全红线）。
