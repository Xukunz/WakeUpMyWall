using WakeUpMyWall.Agent.Api;
using WakeUpMyWall.Agent.Auth;
using WakeUpMyWall.Agent.Actions;
using WakeUpMyWall.Agent.Metrics;
using WakeUpMyWall.Agent.Power;
#if WINDOWS
using WakeUpMyWall.Agent.Metrics.Windows;
#endif

var builder = WebApplication.CreateBuilder(args);

#if WINDOWS
// 装成 Windows 服务时要真的向 SCM 报到（否则 sc.exe start 报 1053）；
// 在控制台里手动运行同样的 exe 时，这个方法什么都不做（IsWindowsService() 为 false）。
builder.Host.UseWindowsService(options => options.ServiceName = "WakeUpMyWallAgent");
#endif

// 端口默认就是 9876（spec §5 的 Agent 端口）；ASPNETCORE_URLS / --urls / Agent:Urls 都能覆盖。
// 显式写在这里，是因为只靠命令行参数时容易被 launchSettings 之外的默认值抢走（实测过）。
builder.WebHost.UseUrls(
    builder.Configuration["urls"]
    ?? builder.Configuration["ASPNETCORE_URLS"]
    ?? builder.Configuration["Agent:Urls"]
    ?? "http://0.0.0.0:9876");

builder.Services.AddSingleton(TimeProvider.System);
// Token 落盘位置可由配置覆盖（测试用临时文件，生产用 AgentPaths.DefaultTokenFile）。
builder.Services.AddSingleton<ITokenStore>(services => new FileTokenStore(
    services.GetRequiredService<IConfiguration>()["Agent:TokenFile"] ?? AgentPaths.DefaultTokenFile));
builder.Services.AddSingleton<PairingService>();
builder.Services.AddSingleton<BearerAuthFilter>();
builder.Services.AddSingleton<ActionRegistry>();

// 电源动作只有 Windows 能真做；其它平台（CI / 开发机 / 冒烟）用 --fake-power，
// 只记录不执行，避免把开发机真的关掉。
// `Agent:UseFakePower` 是给契约测试用的开关：测试要在**任何平台**上都拿到假控制器
// （否则在 Windows runner 上测试会拿到真实控制器，然后一强转就崩 —— CI 实测踩过）。
var useFakePower = args.Contains("--fake-power")
    || !OperatingSystem.IsWindows()
    || builder.Configuration.GetValue("Agent:UseFakePower", false);
if (useFakePower) builder.Services.AddSingleton<IPowerController, FakePowerController>();
else builder.Services.AddSingleton<IPowerController, WindowsPowerController>();

// 指标来源：Windows 上有 LibreHardwareMonitor 的真实读数；其它目标（CI / 开发机）不会带这个包
// （它只提供 win-* 运行时资产），照实喂合成读数。`--fake-metrics` 在 Windows 上也能强制造假，
// 这样端点契约（JSON 形状 + Bearer 鉴权）在任何平台上都能端到端验证。
#if WINDOWS
var useFakeMetrics = args.Contains("--fake-metrics")
    || builder.Configuration.GetValue("Agent:UseFakeMetrics", false);
builder.Services.AddSingleton<ISystemMetricsProvider>(services => useFakeMetrics
    ? new FakeSystemMetricsProvider(services.GetRequiredService<TimeProvider>())
    : new WindowsMetricsProvider());
#else
builder.Services.AddSingleton<ISystemMetricsProvider>(new FakeSystemMetricsProvider(TimeProvider.System));
#endif

var app = builder.Build();

// 指标流（/ws/v1/metrics）需要 WebSocket 中间件；HTTP 端点不受影响。
app.UseWebSockets();
app.MapStatusEndpoints();
app.MapMetricsSocket();

/** 受保护端点（power / actions / system）都挂在这一组上。 */
var protectedEndpoints = app.MapGroup("").AddEndpointFilter<BearerAuthFilter>();
protectedEndpoints.MapSystemEndpoints();
protectedEndpoints.MapPowerEndpoints();
protectedEndpoints.MapActionEndpoints();

app.MapPost("/api/v1/pairing", (PairingRequest request, PairingService pairing) =>
    pairing.IsPaired
        ? Results.Json(new { error = "already paired" }, statusCode: StatusCodes.Status409Conflict)
        : pairing.TryRedeem(request.Code, out var token)
            ? Results.Ok(new { token })
            : Results.Json(
                new { error = "invalid or expired pairing code" },
                statusCode: StatusCodes.Status403Forbidden));

var pairingCode = app.Services.GetRequiredService<PairingService>().CreateCode();
var pairingCodeFile = builder.Configuration["Agent:PairingCodeFile"] ?? AgentPaths.PairingCodeFile;
var pairingCodeWritten = PairingCodeFile.TryWrite(pairingCodeFile, pairingCode, out var pairingCodeError);

app.Logger.LogInformation(
    "WakeUpMyWall Agent {Version} 已启动，配对码 {Code}（5 分钟有效，一次性）",
    StatusEndpoints.AgentVersion,
    pairingCode);
Console.WriteLine($"WakeUpMyWall 配对码：{pairingCode}");

// 装成服务后没有控制台：把码留在盘上，安装包与用户都从这里取（写失败只是少一条便利路径）。
if (pairingCodeWritten)
{
    app.Logger.LogInformation("配对码已写入 {File}", pairingCodeFile);
}
else
{
    app.Logger.LogWarning("配对码写不进 {File}：{Reason}", pairingCodeFile, pairingCodeError);
}

app.Run();

/** WebApplicationFactory<Program> 需要一个可引用的 Program 类型。 */
public partial class Program;

public sealed record PairingRequest(string Code);
