using System.Net.Http.Headers;
using System.Net.Http.Json;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.AspNetCore.TestHost;
using Microsoft.Extensions.DependencyInjection;
using WakeUpMyWall.Agent.Auth;

namespace WakeUpMyWall.Agent.Tests;

/**
 * 每个测试一个实例：Token 文件指到临时目录，配对状态互不干扰
 * （真实路径在 Windows 上是 %ProgramData%\WakeUpMyWall\agent.json，测试绝不能碰它）。
 */
public sealed class TestApp : WebApplicationFactory<Program>
{
    private readonly string _tokenFile =
        Path.Combine(Path.GetTempPath(), $"wumw-agent-test-{Guid.NewGuid():N}.json");

    private readonly string _pairingCodeFile =
        Path.Combine(Path.GetTempPath(), $"wumw-agent-test-{Guid.NewGuid():N}.pairing");

    private readonly string _logFile =
        Path.Combine(Path.GetTempPath(), $"wumw-agent-test-{Guid.NewGuid():N}.log");

    private readonly int? _metricsIntervalMillis;

    private readonly Action<IServiceCollection>? _configureServices;

    /**
     * [metricsIntervalMillis] 只给"推送间隔"的测试用（默认走 Agent 的 1 秒）；
     * [configureServices] 只给"要替换某个服务实现"的测试用（例如让指标读取抛异常）。
     */
    public TestApp(int? metricsIntervalMillis = null, Action<IServiceCollection>? configureServices = null)
    {
        _metricsIntervalMillis = metricsIntervalMillis;
        _configureServices = configureServices;
    }

    protected override void ConfigureWebHost(IWebHostBuilder builder)
    {
        builder.UseSetting("Agent:TokenFile", _tokenFile);
        builder.UseSetting("Agent:PairingCodeFile", _pairingCodeFile);
        builder.UseSetting("Agent:LogFile", _logFile);
        // 契约测试永远用假控制器/假指标：不然在 Windows runner 上会拿到真实控制器与 LibreHardwareMonitor，
        // 测试要么强转失败，要么去开硬件驱动（CI 实测踩过）。
        builder.UseSetting("Agent:UseFakePower", "true");
        builder.UseSetting("Agent:UseFakeMetrics", "true");
        if (_metricsIntervalMillis is not null)
        {
            builder.UseSetting("Agent:MetricsIntervalMs", _metricsIntervalMillis.Value.ToString());
        }

        // 替换服务实现的钩子（例如让指标读取抛异常）：走 TestHost 的 ConfigureTestServices。
        if (_configureServices is not null) builder.ConfigureTestServices(_configureServices);
    }

    protected override void Dispose(bool disposing)
    {
        base.Dispose(disposing);
        if (File.Exists(_tokenFile)) File.Delete(_tokenFile);
        if (File.Exists(_pairingCodeFile)) File.Delete(_pairingCodeFile);
        if (File.Exists(_logFile)) File.Delete(_logFile);
    }

    /** 启动时写给"没有控制台的服务模式"看的配对码文件（安装包就靠它把码显示给用户）。 */
    public string PairingCodeFile => _pairingCodeFile;
}

public static class TestAuth
{
    /** 走一遍真实配对流程，返回 Token 本体（WebSocket 要自己塞进握手头）。 */
    public static async Task<string> TokenAsync(TestApp app)
    {
        var code = app.Services.GetRequiredService<PairingService>().CreateCode();
        var response = await app.CreateClient().PostAsJsonAsync("/api/v1/pairing", new { code });
        response.EnsureSuccessStatusCode();
        return (await response.Content.ReadFromJsonAsync<TokenPayload>())!.Token;
    }

    /** 走一遍真实配对流程，返回带 Token 的 client（不是伪造 header）。 */
    public static async Task<HttpClient> AuthorizedClientAsync(TestApp app)
    {
        var token = await TokenAsync(app);
        var client = app.CreateClient();
        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token);
        return client;
    }

    private sealed record TokenPayload(string Token);
}
