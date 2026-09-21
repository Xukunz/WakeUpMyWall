# WakeUpMyWall PC Agent

跑在 PC 上的常驻小服务，负责三件事：让手机知道这台 PC 是不是开着、执行睡眠 / 关机 / 重启 / 锁屏，
以及提供 CPU / GPU / 内存 / 存储 / 网络 / 温度 / 风扇的实时指标。
接口契约见 [docs/plans/agent-api.md](../docs/plans/agent-api.md)。

## 一键安装（Windows 10/11 x64，推荐）

1. 从 [Releases](https://github.com/Xukunz/WakeUpMyWall/releases)（当前最新是 [`v0.2.0`](https://github.com/Xukunz/WakeUpMyWall/releases/tag/v0.2.0)）下载 **`WakeUpMyWall-Agent-Setup-<版本>.exe`**（自包含，机器上不需要 .NET 运行时）。
2. 双击安装（会要管理员权限），安装程序会自动：

   - 铺文件到 `%ProgramFiles%\WakeUpMyWall`；
   - 注册并启动 Windows 服务 `WakeUpMyWallAgent`（**开机自启**，无需登录）；
   - 放行防火墙 TCP `9876`（仅专用/域网络）；
   - **把配对码显示给你**（服务没有控制台，码取自 `%ProgramData%\WakeUpMyWall\pairing.txt`）。

3. 手机端：Device Setup → `Agent Host` 填这台 PC 的局域网 IP、`Agent Port` 9876 → Save → 在 Agent 区输入安装程序给的配对码 → Pair。

想要别的安装方式：

- **便携版**：下载 `WakeUpMyWall-Agent-win-x64-<版本>.zip`，解压后手动跑 exe（控制台会直接打印配对码）。
- **脚本安装**：把 `install.ps1` 与 zip 放一起，管理员 PowerShell 执行
  `powershell -ExecutionPolicy Bypass -File .\install.ps1 -ZipPath .\WakeUpMyWall-Agent-win-x64-<版本>.zip`，效果与安装包一致。

> 版本号已经与发布产物对齐：`GET /api/v1/status` 里的 `agentVersion` 就来自这一版的构建（`0.2.0` 起），
> 不会再出现"PC 上装的旧包和 `/status` 报的一样"的情况。

## 手动运行（排查用）

1. 解压 zip 到例如 `C:\WakeUpMyWall`，跑一次：

   ```powershell
   C:\WakeUpMyWall\WakeUpMyWall.Agent.exe
   ```

   控制台会打印：`WakeUpMyWall 配对码：123456`（5 分钟有效、一次性）。
   同时这份码也会写到 `%ProgramData%\WakeUpMyWall\pairing.txt`，服务模式下用它取码。
2. 同一台 PC 上验证：

   ```powershell
   curl.exe http://127.0.0.1:9876/api/v1/status
   curl.exe -o NUL -w "%{http_code}`n" -X POST http://127.0.0.1:9876/api/v1/power/lock   # 期望 401
   curl.exe -o NUL -w "%{http_code}`n" http://127.0.0.1:9876/api/v1/system               # 期望 401（指标要 Bearer）
   ```
4. 从别的机器验证（手机要连的就是这个地址）：

   ```powershell
   curl.exe http://<这台PC的IP>:9876/api/v1/status
   ```

## 手动装成 Windows 服务（一键安装包已替你做了这些）

管理员 PowerShell：

```powershell
# 只对专用网络放行 9876
New-NetFirewallRule -DisplayName "WakeUpMyWall Agent" -Direction Inbound -Protocol TCP -LocalPort 9876 -Action Allow -Profile Private

sc.exe create WakeUpMyWallAgent binPath= "C:\WakeUpMyWall\WakeUpMyWall.Agent.exe" start= auto
sc.exe start WakeUpMyWallAgent

# 取配对码（服务模式下控制台不可见）
Get-Content C:\ProgramData\WakeUpMyWall\pairing.txt
```

服务模式下的落盘位置：`C:\ProgramData\WakeUpMyWall\`

| 文件 | 内容 |
| --- | --- |
| `pairing.txt` | 当前配对码（重启服务会换新的） |
| `agent.json` | 配对后的 Token |
| `agent.log` | 服务日志：启动、配对码、**每次电源动作的结果**、指标读取失败的原因（0.2.1 起） |

点手机上的电源按钮没反应时，先看 `agent.log`：里面会写明动作有没有到、命令的退出码是多少；
如果日志里根本没有那一行，说明请求没到 Agent（多半是没配对或网络不通）。

## 端点速查

| 端点 | 鉴权 | 说明 |
| --- | --- | --- |
| `GET /api/v1/status` | 免 | `{"hostname","agentVersion","uptimeSeconds","paired"}` |
| `POST /api/v1/pairing` | 免 | body `{"code":"123456"}` → `{"token":"…"}` |
| `POST /api/v1/power/{sleep\|shutdown\|restart\|lock}` | Bearer | 200 = 命令已交给系统执行 |
| `GET /api/v1/actions` | Bearer | 白名单 `id → 显示名` |
| `POST /api/v1/actions/{id}` | Bearer | 未知 id → 404，且不执行任何命令 |
| `GET /api/v1/system` | Bearer | 实时指标；字段与单位见 [agent-api.md](../docs/plans/agent-api.md) 的「指标载荷」 |
| `WS /ws/v1/metrics` | Bearer（握手） | 每秒推一帧指标，帧内容与 `/api/v1/system` 相同（Phase 5C） |

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
这两个开关也有配置形式：`Agent:UseFakePower` / `Agent:UseFakeMetrics`（契约测试用它保证在任何平台
都拿到假实现——Windows runner 上曾经因为拿到真实控制器而红过 6 条）。

### 发布一个 Windows 包（维护者）

推一个 tag 即可，流水线 [.github/workflows/release.yml](../.github/workflows/release.yml) 会：
跑契约测试 → `dotnet publish -f net10.0-windows -r win-x64 --self-contained` → 打 zip →
用 Inno Setup 编译一键安装包 → 建 Release 并挂上三个产物（zip / Setup exe / install.ps1）。

```bash
# 版本号在 agent/src/WakeUpMyWall.Agent/WakeUpMyWall.Agent.csproj 的 <Version>
git tag v0.2.0 && git push origin v0.2.0
```

流水线里有一步"版本号对齐检查"：tag 与 csproj 的 `<Version>` 不一致就直接失败，避免再次出现"包和版本对不上"。

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
