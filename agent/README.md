# WakeUpMyWall PC Agent

跑在 PC 上的常驻小服务，负责三件事：让手机知道这台 PC 是不是开着、执行睡眠 / 关机 / 重启 / 锁屏，
以及提供 CPU / GPU / 内存 / 存储 / 网络 / 温度 / 风扇的实时指标。
接口契约见 [docs/plans/agent-api.md](../docs/plans/agent-api.md)。

## 快速开始（Windows，不用装 .NET）

1. 从 [Releases](https://github.com/Xukunz/WakeUpMyWall/releases) 下载 `agent-win-x64-v0.1.0.zip`（自包含，机器上不需要 .NET 运行时），解压到例如 `C:\WakeUpMyWall`。
2. 先手动跑一次，确认能起：

   ```powershell
   C:\WakeUpMyWall\WakeUpMyWall.Agent.exe
   ```

   控制台会打印：`WakeUpMyWall 配对码：123456`（5 分钟有效、一次性）。
3. 同一台 PC 上验证：

   ```powershell
   curl.exe http://127.0.0.1:9876/api/v1/status
   curl.exe -o NUL -w "%{http_code}`n" -X POST http://127.0.0.1:9876/api/v1/power/lock   # 期望 401
   curl.exe -o NUL -w "%{http_code}`n" http://127.0.0.1:9876/api/v1/system               # 期望 401（指标要 Bearer）
   ```
4. 从别的机器验证（手机要连的就是这个地址）：

   ```powershell
   curl.exe http://<这台PC的IP>:9876/api/v1/status
   ```

## 装成 Windows 服务（推荐：无人登录也能常驻）

管理员 PowerShell：

```powershell
# 只对专用网络放行 9876
New-NetFirewallRule -DisplayName "WakeUpMyWall Agent" -Direction Inbound -Protocol TCP -LocalPort 9876 -Action Allow -Profile Private

sc.exe create WakeUpMyWallAgent binPath= "C:\WakeUpMyWall\WakeUpMyWall.Agent.exe" start= auto
sc.exe start WakeUpMyWallAgent

# 取配对码（服务模式下控制台不可见，看日志）
Get-Content C:\ProgramData\WakeUpMyWall\agent.log -Tail 20
```

服务模式下的日志位置：`C:\ProgramData\WakeUpMyWall\`。Token 也在同一个目录（`agent.json`）。

## 端点速查

| 端点 | 鉴权 | 说明 |
| --- | --- | --- |
| `GET /api/v1/status` | 免 | `{"hostname","agentVersion","uptimeSeconds","paired"}` |
| `POST /api/v1/pairing` | 免 | body `{"code":"123456"}` → `{"token":"…"}` |
| `POST /api/v1/power/{sleep\|shutdown\|restart\|lock}` | Bearer | 200 = 命令已交给系统执行 |
| `GET /api/v1/actions` | Bearer | 白名单 `id → 显示名` |
| `POST /api/v1/actions/{id}` | Bearer | 未知 id → 404，且不执行任何命令 |
| `GET /api/v1/system` | Bearer | 实时指标；字段与单位见 [agent-api.md](../docs/plans/agent-api.md) 的「指标载荷」 |

## 从源码构建

```bash
# 需要 .NET 10 SDK（本机装在 ~/.dotnet-local）
export PATH="$HOME/.dotnet-local:$PATH"
cd agent
dotnet test                                   # 14 条契约测试
dotnet run --project src/WakeUpMyWall.Agent -- --fake-power   # 非 Windows：只记录不执行
dotnet publish src/WakeUpMyWall.Agent -f net10.0-windows -c Release -r win-x64 --self-contained true -o dist/win-x64
```

`--fake-power` 让电源动作只被记录、不真的执行（CI、开发机、演示用）。非 Windows 平台上默认就是这个模式，
不会出现"在 Linux 上误关机"的情况。

`--fake-metrics` 让 `/api/v1/system` 返回由时间合成的读数（同样只用于 CI / 开发机 / 演示），
非 Windows 平台上默认就是这个模式，`identity.os` 会写成 `Linux (fake metrics)` 以便一眼识别。

工程是**双目标**的：`net10.0`（开发机 / CI）与 `net10.0-windows`（真实指标）。
LibreHardwareMonitor 只提供 win-* 运行时资产，所以**给 Windows 打包时必须带 `-f net10.0-windows`**，
否则打出来的包里没有真实指标。

### Windows 上的指标自验清单（Phase 5A）

1. 起服务（普通窗口即可），从控制台取配对码，然后：

   ```powershell
   $code = "123456"   # 换成控制台里的配对码
   $token = (Invoke-RestMethod -Method Post -Uri http://127.0.0.1:9876/api/v1/pairing -ContentType application/json -Body "{`"code`":`"$code`"}").token
   Invoke-RestMethod -Headers @{ Authorization = "Bearer $token" } http://127.0.0.1:9876/api/v1/system
   ```

2. 对照任务管理器 / 硬件监控工具核对：
   - `cpu.usagePercent` 与任务管理器的 CPU 占用接近（±3%）；
   - `memory.usagePercent` 与任务管理器的内存占用接近（±2%）；
   - `cpu.tempC` / `gpu.tempC` 有读数（**需要管理员权限**：LibreHardwareMonitor 的内核驱动要管理员才能加载；
     普通权限下这两个字段可能是 `null`，这是如实反映，不是 bug）；
   - `network.downloadMbps` 与实际下载速度量级一致；
   - `storage.totalTb` / `storage.freeGb` 与系统盘一致。
3. 把结果（截图或文字）补进 [Phase 5A 计划](../docs/superpowers/plans/2026-09-20-phase5a-agent-metrics.md) 的 §4 验收实录。

## 常见问题

- **9876 被占用**：`netstat -ano | findstr :9876` 找出占用进程；换端口用环境变量 `ASPNETCORE_URLS=http://0.0.0.0:9877` 或 `--urls`。
- **手机连不上**：确认服务在听 `0.0.0.0`、防火墙放行了 9876、手机与 PC 在同一网段；`curl http://<PC-IP>:9876/api/v1/status` 从另一台机器试一次。
- **想重新配对**：删掉 `C:\ProgramData\WakeUpMyWall\agent.json` 并重启服务，会打印新的配对码。
- **卸载**：`sc.exe stop WakeUpMyWallAgent; sc.exe delete WakeUpMyWallAgent`。
