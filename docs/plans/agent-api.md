# WakeUpMyWall Agent API（V1）

权威契约是 [spec §5](../superpowers/specs/2026-09-19-desktop-companion-design.md)：`GET /status`、`GET /system`、`GET /actions`、`POST /power/*`、`POST /actions/{id}`、`WS /ws/v1/metrics`。

这份文档只补 spec 没写死、但 Phase 4A 必须定下来的两件事：**配对端点**与**状态码语义**。

## 端点与鉴权

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/v1/status` | **免鉴权** | 只回最小信息：`hostname` / `agentVersion` / `uptimeSeconds` / `paired`。免鉴权是为了让"PC 开着但还没配对"这一状态可达（spec §4 的 `ONLINE` 判定也需要它）。 |
| POST | `/api/v1/pairing` | **免鉴权** | body `{"code":"123456"}`；配对码由 PC 端启动时生成并打印在控制台与日志里，6 位数字、5 分钟有效、一次性。成功回 `{"token":"…"}`。 |
| GET | `/api/v1/system` | Bearer | Phase 4 返回 **501**：指标属于 Phase 5（LibreHardwareMonitor）。 |
| GET | `/api/v1/actions` | Bearer | `id → 显示名` 白名单。 |
| POST | `/api/v1/actions/{id}` | Bearer | 只允许白名单内 id。 |
| POST | `/api/v1/power/sleep` `/shutdown` `/restart` `/lock` | Bearer | 四个电源动作；命令走参数数组，绝不拼字符串。 |
| WS | `/ws/v1/metrics` | Bearer | Phase 5。 |

## 状态码语义

| 码 | 出现场景 | 语义 |
| --- | --- | --- |
| 200 | 动作已受理 | 服务端**已经把命令交给系统执行**（不是"PC 已完成睡眠/关机"——那要等 Agent 掉线来间接判断）。 |
| 401 | 受保护端点缺少或携带错误 Bearer Token | 不产生任何副作用：过滤器在处理器之前返回。 |
| 403 | 配对码错误或过期 | 不签发 Token。 |
| 404 | 未知电源动作 / 未知 action id | **不执行任何外部命令**（验收要求，测试断言执行列表为空）。 |
| 409 | 已经配对过，又调 `/pairing` | 想重新配对需要删掉 `agent.json` 并重启服务。 |
| 500 | 系统命令返回非 0 退出码 | 回 `{"error":"…"}`，附上命令与退出码。 |
| 501 | `/api/v1/system` | Phase 4 不提供指标。 |

## 配对流程（V1）

1. 服务启动（或首次运行）生成 6 位配对码 → 控制台 + 日志：`WakeUpMyWall 配对码 123456`。
2. 手机（Phase 4B）`POST /api/v1/pairing {"code":"123456"}` → 拿到 Token。
3. Token 由 Agent 落到 `agent.json`（Windows：`%ProgramData%\WakeUpMyWall\agent.json`），手机存进 Android Keystore。
4. 之后所有受保护请求带 `Authorization: Bearer <token>`；服务重启后 Token 仍然有效。

安全边界：只监听局域网；**没有任意命令执行接口**；动作只能是白名单 id（spec §5 安全红线）。
