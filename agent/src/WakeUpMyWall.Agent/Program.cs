using WakeUpMyWall.Agent.Api;
using WakeUpMyWall.Agent.Auth;

var builder = WebApplication.CreateBuilder(args);
builder.Services.AddSingleton(TimeProvider.System);
// Token 落盘位置可由配置覆盖（测试用临时文件，生产用 AgentPaths.DefaultTokenFile）。
builder.Services.AddSingleton<ITokenStore>(services => new FileTokenStore(
    services.GetRequiredService<IConfiguration>()["Agent:TokenFile"] ?? AgentPaths.DefaultTokenFile));
builder.Services.AddSingleton<PairingService>();
builder.Services.AddSingleton<BearerAuthFilter>();

var app = builder.Build();

app.MapStatusEndpoints();

/** 受保护端点（power / actions / system）都挂在这一组上。 */
var protectedEndpoints = app.MapGroup("").AddEndpointFilter<BearerAuthFilter>();
// 指标端点属于 Phase 5（LibreHardwareMonitor）：现在如实回 501，而不是给假数据。
protectedEndpoints.MapGet("/api/v1/system", () =>
    Results.Json(
        new { error = "metrics land in Phase 5 (LibreHardwareMonitor)" },
        statusCode: StatusCodes.Status501NotImplemented));

app.MapPost("/api/v1/pairing", (PairingRequest request, PairingService pairing) =>
    pairing.IsPaired
        ? Results.Json(new { error = "already paired" }, statusCode: StatusCodes.Status409Conflict)
        : pairing.TryRedeem(request.Code, out var token)
            ? Results.Ok(new { token })
            : Results.Json(
                new { error = "invalid or expired pairing code" },
                statusCode: StatusCodes.Status403Forbidden));

var pairingCode = app.Services.GetRequiredService<PairingService>().CreateCode();
app.Logger.LogInformation("WakeUpMyWall 配对码 {Code}（5 分钟有效，一次性）", pairingCode);
Console.WriteLine($"WakeUpMyWall 配对码：{pairingCode}");

app.Run();

/** WebApplicationFactory<Program> 需要一个可引用的 Program 类型。 */
public partial class Program;

public sealed record PairingRequest(string Code);
