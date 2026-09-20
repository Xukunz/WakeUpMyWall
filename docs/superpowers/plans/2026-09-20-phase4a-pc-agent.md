# Phase 4A：PC Agent（C# / .NET）实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 PC 上跑一个常驻 HTTP 服务，让手机能（1）确定这台 PC 是不是开着，（2）真正执行睡眠 / 关机 / 重启 / 锁屏；没有配对 Token 的请求一律 401、未知动作一律 404 且不执行任何外部命令。

**Architecture:** `agent/` 下新建 .NET 10 Minimal API（SDK 10.0.401 已装到 `~/.dotnet-local`，实测 `dotnet --list-sdks`）。**平台专有部分只有一处** —— `IPowerController`：Windows 实现调用系统命令（`shutdown.exe`、`rundll32.exe powrprof.dll,SetSuspendState`、`rundll32.exe user32.dll,LockWorkStation`），非 Windows（含 CI 与我这台 Linux 开发机）用 `FakePowerController` 只记录不执行。因此**整个服务在本机就能跑契约测试**（401 / 404 / status / 动作白名单），真正的电源效果由用户在 Windows 上验收。指标（`/api/v1/system`）属于 Phase 5，本阶段返回 501 并说明原因。

**Tech Stack:** .NET 10（LTS，本机实测 SDK `10.0.401`）/ ASP.NET Core Minimal API / xUnit + `Microsoft.AspNetCore.Mvc.Testing`。**不引入数据库**：Token 与配对状态落 `%ProgramData%\WakeUpMyWall\agent.json`（测试注入临时目录）。

**Spec:** [docs/superpowers/specs/2026-09-19-desktop-companion-design.md](../specs/2026-09-19-desktop-companion-design.md) §5 Agent API 契约（V1 冻结）、§10.2、§11
**路标：** [2026-09-19-roadmap.md](2026-09-19-roadmap.md) Phase 4 段
**后续计划：** Phase 4B（手机端接入：配对 UI、Keystore 存 Token、把睡眠/关机/重启接到真实 API、在线检测）在 4A 契约实现且用户能在 Windows 上跑起来之后单独写。

---

## 0. 计划期定案

| 决策 | 选定 | 理由与代价 |
| --- | --- | --- |
| 技术栈 | **C# / .NET 10 Minimal API** | roadmap 决策记录里 Q1 已裁决（2026-09-19，已采纳）：V1 要 CPU/GPU 风扇转速与温度，Kotlin/OSHI 在 Windows 上拿不到风扇转速。代价：仓库里多一门语言与一套工具链。 |
| 目标框架 | `net10.0`（跨平台），Windows 专有能力藏在接口后 | 这样我能在这台机器上跑全部契约测试，而不是"写完交给用户编译"。代价：真正生效的电源命令只能在 Windows 上验。 |
| 指标 | `/api/v1/system` 本阶段返回 **501** | 指标是 Phase 5（LibreHardwareMonitor）。现在给假数据比 501 更糟。代价：Phase 4B 不接指标。 |
| 鉴权 | 除 `/api/v1/status`（只回最小信息）与配对端点外，全部要求 `Authorization: Bearer <token>` | spec §5 鉴权条款。`status` 免鉴权是为了让"PC 开着但还没配对"这一状态可达。代价：未配对时对外暴露主机名与版本（仅局域网）。 |
| 配对 | PC 端启动时生成 **6 位一次性配对码**（控制台打印 + 写日志，5 分钟有效）；手机 `POST /api/v1/pairing` 用码换 Token | spec §5 只冻结了 status/system/actions/power，配对端点由本计划补齐并写进 `docs/plans/agent-api.md`。代价：V1 的配对要求"看得见 PC 的人"才能配。 |
| 动作白名单 | `GET /api/v1/actions` 回 `id → 显示名`；`POST /api/v1/actions/{id}` 只允许注册表内 id | spec 安全红线：**永不提供任意命令执行接口**；验收要求"未知 id 返回 404 且不执行任何外部命令"。 |
| 开机自启 | 首选 Windows 服务（`sc.exe create`，需一次管理员权限）；备选启动目录快捷方式 | 服务能在无人登录时运行，符合"常驻"。 |
| Token 存储 | Agent 侧 `%ProgramData%\WakeUpMyWall\agent.json`；手机侧 Keystore 属 Phase 4B | spec：Token 不进模型明文序列化路径、手机端进 Keystore。 |

## Global Constraints

- **没有任意命令执行接口**：不接受命令字符串、不接受脚本路径、不接受参数拼接（spec §5 安全红线）。
- **未携带有效 Token 的请求返回 401**，且不得产生任何副作用（不调用电源控制器）。
- **未知动作 id 返回 404**，且不得执行任何外部命令。
- **单请求超时 2–3 秒**（手机侧）；Agent 侧命令执行不阻塞请求线程超过 3 秒。
- 只监听局域网：默认 `http://0.0.0.0:9876`，不做端口转发、不做公网暴露。
- 外部命令必须走**参数数组**（`ProcessStartInfo.ArgumentList`），禁止 `cmd /c` 字符串拼接。
- 每个任务先写失败测试再实现；`dotnet test` 全绿才算完成；提交信息用任务里的原文。

---

## 1. 文件结构

| 文件 | 职责 |
| --- | --- |
| `agent/WakeUpMyWall.Agent.sln` | 解决方案 |
| `agent/src/WakeUpMyWall.Agent/WakeUpMyWall.Agent.csproj` | `Microsoft.NET.Sdk.Web` + `net10.0` + `Nullable` + `TreatWarningsAsErrors` |
| `agent/src/WakeUpMyWall.Agent/Program.cs` | 组装：配置、鉴权过滤器、端点映射；末尾 `public partial class Program;` 供测试工厂使用 |
| `agent/src/WakeUpMyWall.Agent/Api/StatusEndpoints.cs` | `GET /api/v1/status`（免鉴权，最小信息） |
| `agent/src/WakeUpMyWall.Agent/Api/PowerEndpoints.cs` | `POST /api/v1/power/{sleep\|shutdown\|restart\|lock}`（鉴权） |
| `agent/src/WakeUpMyWall.Agent/Api/ActionEndpoints.cs` | `GET /api/v1/actions`、`POST /api/v1/actions/{id}`（鉴权 + 白名单） |
| `agent/src/WakeUpMyWall.Agent/Auth/TokenStore.cs` | `ITokenStore` + `InMemoryTokenStore`（A1）+ `FileTokenStore`（A2） |
| `agent/src/WakeUpMyWall.Agent/Auth/PairingService.cs` | 生成/校验一次性配对码（5 分钟） |
| `agent/src/WakeUpMyWall.Agent/Auth/BearerAuthFilter.cs` | 端点过滤器：校验 Bearer Token，失败 401 |
| `agent/src/WakeUpMyWall.Agent/Power/IPowerController.cs` | `Task<PowerResult> SleepAsync/ShutdownAsync/RestartAsync/LockAsync(CancellationToken)` |
| `agent/src/WakeUpMyWall.Agent/Power/WindowsPowerController.cs` | Windows 实现（参数数组调系统命令） |
| `agent/src/WakeUpMyWall.Agent/Power/FakePowerController.cs` | 记录调用、不执行任何外部命令 |
| `agent/src/WakeUpMyWall.Agent/Actions/ActionRegistry.cs` | id → 显示名 + 处理器；未知 id 查不到 |
| `agent/tests/WakeUpMyWall.Agent.Tests/` | xUnit + `WebApplicationFactory<Program>` |
| `docs/plans/agent-api.md` | 本阶段补的配对端点与状态码约定（spec §5 之外的增量） |
| `docs/plans/version-matrix.md` | 新增 .NET SDK 10.0.401 行与验证命令 |

---

### Task A1: Agent 骨架 + `GET /api/v1/status`

**Files:**
- Create: `agent/WakeUpMyWall.Agent.sln`、`agent/src/WakeUpMyWall.Agent/Program.cs`、`agent/src/WakeUpMyWall.Agent/Api/StatusEndpoints.cs`、`agent/src/WakeUpMyWall.Agent/Auth/TokenStore.cs`（只放接口 + 内存实现）
- Create: `agent/tests/WakeUpMyWall.Agent.Tests/StatusEndpointTests.cs` + 测试工程文件
- Modify: `.gitignore`（`agent/**/bin/`、`agent/**/obj/`）

**Interfaces:**
- Produces: `GET /api/v1/status` → `200 {"hostname":…,"agentVersion":"0.1.0","uptimeSeconds":…,"paired":false}`；`public partial class Program;`；`ITokenStore`（`HasToken` / `Token` / `Matches` / `Save`）

- [x] **Step 1: 建工程骨架**

```bash
export PATH="$HOME/.dotnet-local:$PATH"
cd /home/xukunz/桌面/WakeUpMyWall
mkdir -p agent/src agent/tests
cd agent
dotnet new sln -n WakeUpMyWall.Agent
dotnet new web -n WakeUpMyWall.Agent -o src/WakeUpMyWall.Agent
dotnet new xunit -n WakeUpMyWall.Agent.Tests -o tests/WakeUpMyWall.Agent.Tests
dotnet sln add src/WakeUpMyWall.Agent tests/WakeUpMyWall.Agent.Tests
dotnet add tests/WakeUpMyWall.Agent.Tests reference src/WakeUpMyWall.Agent
dotnet add tests/WakeUpMyWall.Agent.Tests package Microsoft.AspNetCore.Mvc.Testing
printf 'agent/**/bin/\nagent/**/obj/\n' >> ../.gitignore
```

在生成的 `WakeUpMyWall.Agent.csproj` 里加两行（其余生成内容保持不动）：

```xml
<Nullable>enable</Nullable>
<TreatWarningsAsErrors>true</TreatWarningsAsErrors>
```

- [x] **Step 2: 写失败测试**

```csharp
using System.Net;
using System.Net.Http.Json;
using Microsoft.AspNetCore.Mvc.Testing;
using Xunit;

namespace WakeUpMyWall.Agent.Tests;

public class StatusEndpointTests(WebApplicationFactory<Program> factory)
    : IClassFixture<WebApplicationFactory<Program>>
{
    [Fact]
    public async Task Status_reports_hostname_version_uptime_and_pairing_without_a_token()
    {
        var client = factory.CreateClient();

        var response = await client.GetAsync("/api/v1/status");
        var payload = await response.Content.ReadFromJsonAsync<StatusPayload>();

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        Assert.False(string.IsNullOrWhiteSpace(payload!.Hostname));
        Assert.Equal("0.1.0", payload.AgentVersion);
        Assert.True(payload.UptimeSeconds >= 0);
        Assert.False(payload.Paired);
    }

    private sealed record StatusPayload(string Hostname, string AgentVersion, long UptimeSeconds, bool Paired);
}
```

- [x] **Step 3: 跑测试确认失败**

Run: `export PATH="$HOME/.dotnet-local:$PATH"; cd agent && dotnet test`
Expected: FAIL —— `/api/v1/status` 返回 404

- [x] **Step 4: 实现**

```csharp
// src/WakeUpMyWall.Agent/Program.cs
using WakeUpMyWall.Agent.Api;
using WakeUpMyWall.Agent.Auth;

var builder = WebApplication.CreateBuilder(args);
builder.Services.AddSingleton(TimeProvider.System);
builder.Services.AddSingleton<ITokenStore>(new InMemoryTokenStore());

var app = builder.Build();
app.MapStatusEndpoints();
app.Run();

/** WebApplicationFactory<Program> 需要一个可引用的 Program 类型。 */
public partial class Program;
```

```csharp
// src/WakeUpMyWall.Agent/Api/StatusEndpoints.cs
using WakeUpMyWall.Agent.Auth;

namespace WakeUpMyWall.Agent.Api;

public static class StatusEndpoints
{
    private static readonly DateTimeOffset StartedAt = DateTimeOffset.UtcNow;

    public static void MapStatusEndpoints(this WebApplication app) =>
        // spec §5：status 配对前可用，只回最小信息。
        app.MapGet("/api/v1/status", (ITokenStore tokens) => Results.Ok(new StatusPayload(
            Hostname: Environment.MachineName,
            AgentVersion: "0.1.0",
            UptimeSeconds: (long)(DateTimeOffset.UtcNow - StartedAt).TotalSeconds,
            Paired: tokens.HasToken)));
}

public sealed record StatusPayload(string Hostname, string AgentVersion, long UptimeSeconds, bool Paired);
```

```csharp
// src/WakeUpMyWall.Agent/Auth/TokenStore.cs（A1 只放接口 + 内存实现，A2 补文件实现）
using System.Security.Cryptography;
using System.Text;

namespace WakeUpMyWall.Agent.Auth;

public interface ITokenStore
{
    bool HasToken { get; }
    string? Token { get; }
    bool Matches(string candidate);
    void Save(string token);
}

public sealed class InMemoryTokenStore(string? token = null) : ITokenStore
{
    private string? _token = token;

    public bool HasToken => _token is not null;
    public string? Token => _token;

    public bool Matches(string candidate) =>
        _token is not null && CryptographicOperations.FixedTimeEquals(
            Encoding.UTF8.GetBytes(_token), Encoding.UTF8.GetBytes(candidate));

    public void Save(string token) => _token = token;
}
```

- [x] **Step 5: 跑测试确认通过并提交**

Run: `export PATH="$HOME/.dotnet-local:$PATH"; cd agent && dotnet test`
Expected: PASS（1 条）

```bash
cd /home/xukunz/桌面/WakeUpMyWall
git add agent .gitignore
git commit -m "feat: add the pc agent skeleton with a status endpoint"
```

---

### Task A2: Token、配对码与 401

**Files:**
- Create: `agent/src/WakeUpMyWall.Agent/Auth/PairingService.cs`、`Auth/BearerAuthFilter.cs`
- Modify: `agent/src/WakeUpMyWall.Agent/Auth/TokenStore.cs`（补 `FileTokenStore`）、`Program.cs`（注册 + 配对端点 + 受保护端点分组）
- Create: `agent/tests/WakeUpMyWall.Agent.Tests/AuthTests.cs`、`docs/plans/agent-api.md`

**Interfaces:**
- Consumes: A1 的 `ITokenStore`
- Produces:
  - `POST /api/v1/pairing` body `{"code":"123456"}` → `200 {"token":"…"}`；码错/过期 → `403`；已配对 → `409`
  - `FileTokenStore(string path)`：Token 落盘，进程重启后保持
  - `BearerAuthFilter`：无 token / token 错 → `401`
  - `PairingService.CreateCode()`（6 位、5 分钟、一次性）、`TryRedeem(code, out token)`、`IsPaired`

- [ ] **Step 1: 写失败测试**

```csharp
using System.Net;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.Extensions.DependencyInjection;
using WakeUpMyWall.Agent.Auth;
using Xunit;

namespace WakeUpMyWall.Agent.Tests;

public class AuthTests(WebApplicationFactory<Program> factory) : IClassFixture<WebApplicationFactory<Program>>
{
    [Fact]
    public async Task Power_endpoint_without_a_token_is_unauthorized()
    {
        var response = await factory.CreateClient().PostAsync("/api/v1/power/lock", content: null);

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    [Fact]
    public async Task Pairing_with_the_printed_code_returns_a_token_that_then_works()
    {
        var client = factory.CreateClient();
        var code = factory.Services.GetRequiredService<PairingService>().CreateCode();

        var pair = await client.PostAsJsonAsync("/api/v1/pairing", new { code });
        pair.EnsureSuccessStatusCode();
        var token = (await pair.Content.ReadFromJsonAsync<TokenPayload>())!.Token;

        var authorized = factory.CreateClient();
        authorized.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token);

        Assert.Equal(HttpStatusCode.OK, (await authorized.PostAsync("/api/v1/power/lock", null)).StatusCode);
    }

    [Fact]
    public async Task A_wrong_code_is_rejected_and_a_used_code_cannot_be_replayed()
    {
        var client = factory.CreateClient();
        var code = factory.Services.GetRequiredService<PairingService>().CreateCode();

        Assert.Equal(HttpStatusCode.Forbidden,
            (await client.PostAsJsonAsync("/api/v1/pairing", new { code = "000000" })).StatusCode);
        Assert.Equal(HttpStatusCode.OK,
            (await client.PostAsJsonAsync("/api/v1/pairing", new { code })).StatusCode);
        Assert.Equal(HttpStatusCode.Conflict,
            (await client.PostAsJsonAsync("/api/v1/pairing", new { code })).StatusCode);
    }

    private sealed record TokenPayload(string Token);
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `export PATH="$HOME/.dotnet-local:$PATH"; cd agent && dotnet test`
Expected: FAIL —— 401 用例拿到 404（端点还没映射）、`PairingService` 不存在

- [ ] **Step 3: 实现**

```csharp
// src/WakeUpMyWall.Agent/Auth/PairingService.cs
using System.Security.Cryptography;
using System.Text;

namespace WakeUpMyWall.Agent.Auth;

public sealed class PairingService(ITokenStore store, TimeProvider clock)
{
    private static readonly TimeSpan Lifetime = TimeSpan.FromMinutes(5);
    private string? _code;
    private DateTimeOffset _expiresAt;

    public bool IsPaired => store.HasToken;

    /** 6 位数字、5 分钟有效、一次性；控制台与日志里打印给坐在 PC 前的人看。 */
    public string CreateCode()
    {
        _code = RandomNumberGenerator.GetInt32(0, 1_000_000).ToString("D6");
        _expiresAt = clock.GetUtcNow() + Lifetime;
        return _code;
    }

    public bool TryRedeem(string candidate, out string? token)
    {
        token = null;
        if (_code is null || clock.GetUtcNow() > _expiresAt) return false;
        if (!CryptographicOperations.FixedTimeEquals(
                Encoding.UTF8.GetBytes(_code), Encoding.UTF8.GetBytes(candidate))) return false;

        token = Convert.ToBase64String(RandomNumberGenerator.GetBytes(32));
        store.Save(token);
        _code = null;                 // 一次性
        return true;
    }
}
```

```csharp
// src/WakeUpMyWall.Agent/Auth/TokenStore.cs（追加文件实现）
using System.Text.Json;

public sealed class FileTokenStore(string path) : ITokenStore
{
    private string? _token = Load(path);

    public bool HasToken => _token is not null;
    public string? Token => _token;

    public bool Matches(string candidate) =>
        _token is not null && CryptographicOperations.FixedTimeEquals(
            Encoding.UTF8.GetBytes(_token), Encoding.UTF8.GetBytes(candidate));

    public void Save(string token)
    {
        Directory.CreateDirectory(Path.GetDirectoryName(path)!);
        File.WriteAllText(path, JsonSerializer.Serialize(new Persisted(token)));
        _token = token;
    }

    private static string? Load(string path) =>
        File.Exists(path) ? JsonSerializer.Deserialize<Persisted>(File.ReadAllText(path))?.Token : null;

    private sealed record Persisted(string Token);
}
```

```csharp
// src/WakeUpMyWall.Agent/Auth/BearerAuthFilter.cs
namespace WakeUpMyWall.Agent.Auth;

public sealed class BearerAuthFilter(ITokenStore tokens) : IEndpointFilter
{
    public async ValueTask<object?> InvokeAsync(EndpointFilterInvocationContext context, EndpointFilterDelegate next)
    {
        var header = context.HttpContext.Request.Headers.Authorization.ToString();
        if (!header.StartsWith("Bearer ", StringComparison.Ordinal) ||
            !tokens.Matches(header["Bearer ".Length..].Trim()))
        {
            return Results.Json(new { error = "missing or invalid token" }, statusCode: StatusCodes.Status401Unauthorized);
        }
        return await next(context);
    }
}
```

`Program.cs` 接上配对端点与受保护分组：

```csharp
builder.Services.AddSingleton<ITokenStore>(_ => new FileTokenStore(AgentPaths.TokenFile));
builder.Services.AddSingleton(TimeProvider.System);
builder.Services.AddSingleton<PairingService>();
builder.Services.AddSingleton<BearerAuthFilter>();

var protectedEndpoints = app.MapGroup("").AddEndpointFilter<BearerAuthFilter>();
// 后续 power / actions / system 端点都挂到 protectedEndpoints 上

app.MapPost("/api/v1/pairing", (PairingRequest request, PairingService pairing) =>
    pairing.IsPaired
        ? Results.Json(new { error = "already paired" }, statusCode: StatusCodes.Status409Conflict)
        : pairing.TryRedeem(request.Code, out var token)
            ? Results.Ok(new { token })
            : Results.Json(new { error = "invalid or expired pairing code" }, statusCode: StatusCodes.Status403Forbidden));

var pairingCode = app.Services.GetRequiredService<PairingService>().CreateCode();
app.Logger.LogInformation("配对码 {Code}（5 分钟有效，一次性）", pairingCode);
Console.WriteLine($"WakeUpMyWall 配对码：{pairingCode}");

public sealed record PairingRequest(string Code);
```

`AgentPaths.TokenFile` 的实现（`Auth/AgentPaths.cs`）：Windows 用 `%ProgramData%\WakeUpMyWall\agent.json`，其它平台用 `~/.wakeupmywall/agent.json`（这样本机测试与 Linux 冒烟也能跑）。

- [ ] **Step 4: 跑测试确认通过**

Run: `export PATH="$HOME/.dotnet-local:$PATH"; cd agent && dotnet test`
Expected: PASS（A1 1 条 + A2 3 条）

- [ ] **Step 5: 写 `docs/plans/agent-api.md` 并提交**

文档写清：配对端点（本计划新增，spec §5 未冻结）、`/status` 免鉴权但只回最小信息、401/403/404/409 的语义、`/api/v1/system` 在 Phase 4 返回 501 的原因。

```bash
cd /home/xukunz/桌面/WakeUpMyWall
git add agent docs/plans/agent-api.md
git commit -m "feat: pair the phone with a one-time code and guard the api with a bearer token"
```

---

### Task A3: 电源端点与动作白名单

**Files:**
- Create: `agent/src/WakeUpMyWall.Agent/Power/IPowerController.cs`、`Power/WindowsPowerController.cs`、`Power/FakePowerController.cs`、`Actions/ActionRegistry.cs`、`Api/PowerEndpoints.cs`、`Api/ActionEndpoints.cs`
- Create: `agent/tests/WakeUpMyWall.Agent.Tests/PowerEndpointTests.cs`、`ActionEndpointTests.cs`
- Modify: `Program.cs`（按平台选择控制器、把端点挂到 `protectedEndpoints`）

**Interfaces:**
- Consumes: A2 的 `protectedEndpoints` / `BearerAuthFilter`
- Produces:
  - `POST /api/v1/power/{sleep|shutdown|restart|lock}`（鉴权）→ `200 {"action":…,"accepted":true}`；控制器失败 → `500 {"error":…}`；动作名不认识 → `404`
  - `GET /api/v1/actions` → `200 [{"id":"browser","name":"Open Browser"}, …]`
  - `POST /api/v1/actions/{id}` → 白名单内 `200`；未知 id `404` **且执行列表保持为空**
  - `IPowerController`（四个 `Task<PowerResult> …Async(CancellationToken)`）、`PowerResult(bool Accepted, string? Error)`
  - `ActionRegistry`：`All`、`TryExecute(id, power)`、`Executed`、`ResetExecutions()`

- [ ] **Step 1: 写失败测试**

```csharp
// PowerEndpointTests.cs
public class PowerEndpointTests(WebApplicationFactory<Program> factory) : IClassFixture<WebApplicationFactory<Program>>
{
    [Theory]
    [InlineData("sleep")]
    [InlineData("shutdown")]
    [InlineData("restart")]
    [InlineData("lock")]
    public async Task Every_power_endpoint_reaches_the_controller(string action)
    {
        var fake = (FakePowerController)factory.Services.GetRequiredService<IPowerController>();
        fake.Clear();

        var response = await TestAuth.AuthorizedClient(factory).PostAsync($"/api/v1/power/{action}", null);

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        Assert.Equal(action, fake.LastAction);   // 真的走到了控制器，而不是只回 200
    }

    [Fact]
    public async Task An_unknown_power_action_is_404_and_touches_nothing()
    {
        var fake = (FakePowerController)factory.Services.GetRequiredService<IPowerController>();
        fake.Clear();

        var response = await TestAuth.AuthorizedClient(factory).PostAsync("/api/v1/power/explode", null);

        Assert.Equal(HttpStatusCode.NotFound, response.StatusCode);
        Assert.Null(fake.LastAction);
    }
}
```

```csharp
// ActionEndpointTests.cs
public class ActionEndpointTests(WebApplicationFactory<Program> factory) : IClassFixture<WebApplicationFactory<Program>>
{
    [Fact]
    public async Task Unknown_action_returns_404_and_executes_nothing()
    {
        var registry = factory.Services.GetRequiredService<ActionRegistry>();
        registry.ResetExecutions();

        var response = await TestAuth.AuthorizedClient(factory).PostAsync("/api/v1/actions/not-a-real-action", null);

        Assert.Equal(HttpStatusCode.NotFound, response.StatusCode);
        Assert.Empty(registry.Executed);          // 验收要求：未知 id 不得执行任何外部命令
    }

    [Fact]
    public async Task Known_action_runs_and_is_reported()
    {
        var registry = factory.Services.GetRequiredService<ActionRegistry>();
        registry.ResetExecutions();

        var response = await TestAuth.AuthorizedClient(factory).PostAsync("/api/v1/actions/browser", null);

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        Assert.Equal(["browser"], registry.Executed);
    }
}
```

`TestAuth` 是一个小 helper（测试工程里）：拿 `PairingService.CreateCode()` → `POST /api/v1/pairing` → 返回带 `Authorization` 的 `HttpClient`；`AuthTests` 也可以改用它减少重复。

- [ ] **Step 2: 跑测试确认失败**

Run: `export PATH="$HOME/.dotnet-local:$PATH"; cd agent && dotnet test`
Expected: FAIL —— 端点是 404、`FakePowerController` / `ActionRegistry` / `TestAuth` 不存在

- [ ] **Step 3: 实现**

```csharp
// Power/IPowerController.cs
namespace WakeUpMyWall.Agent.Power;

public sealed record PowerResult(bool Accepted, string? Error = null);

public interface IPowerController
{
    Task<PowerResult> SleepAsync(CancellationToken ct);
    Task<PowerResult> ShutdownAsync(CancellationToken ct);
    Task<PowerResult> RestartAsync(CancellationToken ct);
    Task<PowerResult> LockAsync(CancellationToken ct);
}
```

```csharp
// Power/FakePowerController.cs —— CI 与非 Windows 开发机使用；绝不执行外部命令
namespace WakeUpMyWall.Agent.Power;

public sealed class FakePowerController : IPowerController
{
    private readonly List<string> _calls = [];

    public string? LastAction { get { lock (_calls) return _calls.LastOrDefault(); } }
    public void Clear() { lock (_calls) _calls.Clear(); }

    public Task<PowerResult> SleepAsync(CancellationToken ct) => Record("sleep");
    public Task<PowerResult> ShutdownAsync(CancellationToken ct) => Record("shutdown");
    public Task<PowerResult> RestartAsync(CancellationToken ct) => Record("restart");
    public Task<PowerResult> LockAsync(CancellationToken ct) => Record("lock");

    private Task<PowerResult> Record(string action)
    {
        lock (_calls) _calls.Add(action);
        return Task.FromResult(new PowerResult(true));
    }
}
```

```csharp
// Power/WindowsPowerController.cs
using System.Diagnostics;

namespace WakeUpMyWall.Agent.Power;

public sealed class WindowsPowerController(ILogger<WindowsPowerController> logger) : IPowerController
{
    public Task<PowerResult> SleepAsync(CancellationToken ct) =>
        Run("rundll32.exe", ["powrprof.dll,SetSuspendState", "0,1,0"], ct);

    public Task<PowerResult> ShutdownAsync(CancellationToken ct) =>
        Run("shutdown.exe", ["/s", "/t", "0"], ct);

    public Task<PowerResult> RestartAsync(CancellationToken ct) =>
        Run("shutdown.exe", ["/r", "/t", "0"], ct);

    public Task<PowerResult> LockAsync(CancellationToken ct) =>
        Run("rundll32.exe", ["user32.dll,LockWorkStation"], ct);

    private async Task<PowerResult> Run(string file, string[] arguments, CancellationToken ct)
    {
        // 参数数组，绝不拼字符串；也不经过 cmd /c。
        var startInfo = new ProcessStartInfo(file) { UseShellExecute = false, CreateNoWindow = true };
        foreach (var argument in arguments) startInfo.ArgumentList.Add(argument);

        using var process = Process.Start(startInfo);
        if (process is null) return new PowerResult(false, $"could not start {file}");

        await process.WaitForExitAsync(ct);
        logger.LogInformation("{File} {Args} exited with {Code}", file, string.Join(' ', arguments), process.ExitCode);
        return process.ExitCode == 0
            ? new PowerResult(true)
            : new PowerResult(false, $"{file} exited with {process.ExitCode}");
    }
}
```

```csharp
// Actions/ActionRegistry.cs —— id → 显示名 + 处理器；白名单之外一律查不到
namespace WakeUpMyWall.Agent.Actions;

public sealed class ActionRegistry
{
    private static readonly Dictionary<string, string> Names = new()
    {
        ["browser"] = "Open Browser",
        ["steam"] = "Launch Steam",
        ["spotify"] = "Open Spotify",
        ["discord"] = "Open Discord",
    };

    private readonly List<string> _executed = [];

    public IReadOnlyDictionary<string, string> All => Names;
    public IReadOnlyList<string> Executed { get { lock (_executed) return [.. _executed]; } }
    public void ResetExecutions() { lock (_executed) _executed.Clear(); }

    /** 只有注册表内的 id 会被记录；未知 id 返回 false 且不产生任何副作用。 */
    public bool TryExecute(string id)
    {
        if (!Names.ContainsKey(id)) return false;
        lock (_executed) _executed.Add(id);
        return true;
    }
}
```

```csharp
// Api/PowerEndpoints.cs
using WakeUpMyWall.Agent.Power;

namespace WakeUpMyWall.Agent.Api;

public static class PowerEndpoints
{
    public static void MapPowerEndpoints(this RouteGroupBuilder group) =>
        group.MapPost("/api/v1/power/{action}", async (string action, IPowerController power, CancellationToken ct) =>
        {
            var result = action switch
            {
                "sleep" => await power.SleepAsync(ct),
                "shutdown" => await power.ShutdownAsync(ct),
                "restart" => await power.RestartAsync(ct),
                "lock" => await power.LockAsync(ct),
                _ => null,
            };

            return result is null
                ? Results.NotFound(new { error = "unknown power action" })
                : result.Accepted
                    ? Results.Ok(new { action, accepted = true })
                    : Results.Json(new { error = result.Error }, statusCode: StatusCodes.Status500InternalServerError);
        });
}
```

```csharp
// Api/ActionEndpoints.cs
using WakeUpMyWall.Agent.Actions;

namespace WakeUpMyWall.Agent.Api;

public static class ActionEndpoints
{
    public static void MapActionEndpoints(this RouteGroupBuilder group)
    {
        group.MapGet("/api/v1/actions", (ActionRegistry registry) =>
            Results.Ok(registry.All.Select(pair => new { id = pair.Key, name = pair.Value })));

        group.MapPost("/api/v1/actions/{id}", (string id, ActionRegistry registry) =>
            registry.TryExecute(id)
                ? Results.Ok(new { id, executed = true })
                : Results.NotFound(new { error = "unknown action" }));
    }
}
```

`Program.cs` 里选控制器并挂端点：

```csharp
var useFakePower = args.Contains("--fake-power") || !OperatingSystem.IsWindows();
if (useFakePower) builder.Services.AddSingleton<IPowerController, FakePowerController>();
else builder.Services.AddSingleton<IPowerController, WindowsPowerController>();
builder.Services.AddSingleton<ActionRegistry>();
...
protectedEndpoints.MapPowerEndpoints();
protectedEndpoints.MapActionEndpoints();
protectedEndpoints.MapGet("/api/v1/system", () =>
    Results.Json(new { error = "metrics land in Phase 5 (LibreHardwareMonitor)" }, statusCode: 501));
```

- [ ] **Step 4: 跑测试确认通过**

Run: `export PATH="$HOME/.dotnet-local:$PATH"; cd agent && dotnet test`
Expected: PASS（A1 1 + A2 3 + A3 7）

- [ ] **Step 5: 提交**

```bash
cd /home/xukunz/桌面/WakeUpMyWall
git add agent
git commit -m "feat: expose power endpoints and a whitelisted action registry"
```

---

### Task A4: Windows 服务自启、打包说明与用户验收

**Files:**
- Create: `agent/README.md`
- Modify: `docs/plans/version-matrix.md`、`README.md`（`agent/` 从"Phase 4 引入"改成已存在）

- [ ] **Step 1: 本机（Linux）发布冒烟**

```bash
export PATH="$HOME/.dotnet-local:$PATH"
cd /home/xukunz/桌面/WakeUpMyWall/agent
dotnet publish src/WakeUpMyWall.Agent -c Release -o /tmp/agent-publish
/tmp/agent-publish/WakeUpMyWall.Agent --fake-power --urls http://127.0.0.1:9876 &
sleep 2
curl -s http://127.0.0.1:9876/api/v1/status; echo
curl -s -o /dev/null -w 'power without token -> %{http_code}\n' -X POST http://127.0.0.1:9876/api/v1/power/lock
curl -s -o /dev/null -w 'system -> %{http_code}\n' http://127.0.0.1:9876/api/v1/system
kill %1
```
Expected: status JSON、`power without token -> 401`、`system -> 501`

- [ ] **Step 2: 写 `agent/README.md`（Windows 安装步骤）**

```powershell
# 1) 发布（可在开发机交叉发布，也可在 Windows 上直接 publish）
dotnet publish agent/src/WakeUpMyWall.Agent -c Release -r win-x64 --self-contained true -o C:\WakeUpMyWall
# 2) 放行防火墙（只对专用网络）
New-NetFirewallRule -DisplayName "WakeUpMyWall Agent" -Direction Inbound -Protocol TCP -LocalPort 9876 -Action Allow -Profile Private
# 3) 装成服务（管理员）——服务能在无人登录时运行
sc.exe create WakeUpMyWallAgent binPath= "C:\WakeUpMyWall\WakeUpMyWall.Agent.exe" start= auto
sc.exe start WakeUpMyWallAgent
# 4) 取配对码（服务日志）
Get-Content C:\ProgramData\WakeUpMyWall\agent.log -Tail 20
```

README 里同时写：卸载（`sc.exe delete WakeUpMyWallAgent`）、Token 存放位置、如何重新配对（删 `agent.json` 后重启服务）。

- [ ] **Step 3: 用户验收清单（Windows 那侧执行）**

| 步骤 | 期望 |
| --- | --- |
| `curl http://<PC-IP>:9876/api/v1/status` | 200 + 主机名/版本/`paired:false` |
| 不带 Token `POST /api/v1/power/shutdown` | **401**，且 PC 不关机 |
| 用日志里的配对码换 Token，再发同一个请求 | 200，且 PC 真的关机 |
| 重启 Agent 服务后重发带 Token 的请求 | 仍然 200（Token 落盘了） |
| `POST /api/v1/actions/not-a-real-action` | **404**，什么都不发生 |
| 服务设为 `start= auto` 后重启 Windows | 服务自动起来，`sc.exe query WakeUpMyWallAgent` 显示 RUNNING |

- [ ] **Step 4: 更新文档并提交**

```bash
cd /home/xukunz/桌面/WakeUpMyWall
git add agent docs README.md
git commit -m "docs: document installing the agent as a windows service"
```

---

## 2. 验收标准（对照 roadmap Phase 4 段）

| roadmap 要求 | 本计划的落点 | 验证方式 |
| --- | --- | --- |
| 项目骨架 + HTTP 服务 + `/status` | Task A1 | `StatusEndpointTests` |
| 心跳 | Task A1 的 `uptimeSeconds`；4B 的手机侧轮询打这个端点 | 服务端如实回运行时长 |
| `/power/{sleep,shutdown,restart,lock}` | Task A3 | `Every_power_endpoint_reaches_the_controller` 四个动作都断言走到了控制器 |
| 开机自启（Windows 服务） | Task A4 | 用户按 README 安装，`sc.exe query` 看到 RUNNING，重启 Windows 后仍在 |
| 配对 + Token 鉴权 | Task A2 | 401 / 403 / 409 三条测试 + 用户侧 `curl` |
| **未携带 Token 返回 401** | Task A2 | `Power_endpoint_without_a_token_is_unauthorized` |
| **未知 action 返回 404 且不执行任何外部命令** | Task A3 | `Unknown_action_returns_404_and_executes_nothing`（断言执行列表为空） |
| 手机端四种电源操作全部生效 | **Phase 4B** | 4B 计划负责；本计划只保证服务端可用 |

## 3. 计划自检（writing-plans 要求的三项）

1. **Spec 覆盖**：§5 的七个端点里，本计划实现 `GET /status`、四个 `POST /power/*`、`GET/POST /actions*`；`GET /api/v1/system` 与 `WS /ws/v1/metrics` 明确留给 Phase 5（501 + 文档说明）。安全红线（无任意命令执行、动作白名单）→ Task A3；鉴权 → Task A2；手机侧超时与 Token 存 Keystore → Phase 4B；`%ProgramData%` 落 Token → Task A2/A4。
2. **占位符扫描**：无 TBD；每步要么是可直接跑的命令，要么是完整代码。唯一"看情况"的地方是 `AgentPaths.TokenFile` 的平台分支，已写明两条具体路径。
3. **类型一致性**：`ITokenStore`（HasToken/Token/Matches/Save）→ `PairingService`（CreateCode/TryRedeem/IsPaired）→ `BearerAuthFilter` → `IPowerController`（四个 `…Async`）→ `PowerResult(Accepted, Error)` → `ActionRegistry`（All/TryExecute/Executed）。A1 的 `InMemoryTokenStore` 在 A2 被 `FileTokenStore` 替换时，A1 的测试不用改（两者同接口）。
