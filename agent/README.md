# WakeUpMyWall PC Agent

跑在 PC 上的常驻小服务，负责两件事：让手机知道这台 PC 是不是开着，以及执行睡眠 / 关机 / 重启 / 锁屏。
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
| `GET /api/v1/system` | Bearer | Phase 4 返回 501（指标属 Phase 5） |

## 从源码构建

```bash
# 需要 .NET 10 SDK（本机装在 ~/.dotnet-local）
export PATH="$HOME/.dotnet-local:$PATH"
cd agent
dotnet test                                   # 14 条契约测试
dotnet run --project src/WakeUpMyWall.Agent -- --fake-power   # 非 Windows：只记录不执行
dotnet publish src/WakeUpMyWall.Agent -c Release -r win-x64 --self-contained true -o dist/win-x64
```

`--fake-power` 让电源动作只被记录、不真的执行（CI、开发机、演示用）。非 Windows 平台上默认就是这个模式，
不会出现"在 Linux 上误关机"的情况。

## 常见问题

- **9876 被占用**：`netstat -ano | findstr :9876` 找出占用进程；换端口用环境变量 `ASPNETCORE_URLS=http://0.0.0.0:9877` 或 `--urls`。
- **手机连不上**：确认服务在听 `0.0.0.0`、防火墙放行了 9876、手机与 PC 在同一网段；`curl http://<PC-IP>:9876/api/v1/status` 从另一台机器试一次。
- **想重新配对**：删掉 `C:\ProgramData\WakeUpMyWall\agent.json` 并重启服务，会打印新的配对码。
- **卸载**：`sc.exe stop WakeUpMyWallAgent; sc.exe delete WakeUpMyWallAgent`。
