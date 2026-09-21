using System.Net.Http.Headers;
using System.Net.Http.Json;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
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

    private readonly int? _metricsIntervalMillis;

    /** [metricsIntervalMillis] 只给"推送间隔"的测试用：默认走 Agent 的 1 秒。 */
    public TestApp(int? metricsIntervalMillis = null) => _metricsIntervalMillis = metricsIntervalMillis;

    protected override void ConfigureWebHost(IWebHostBuilder builder)
    {
        builder.UseSetting("Agent:TokenFile", _tokenFile);
        if (_metricsIntervalMillis is not null)
        {
            builder.UseSetting("Agent:MetricsIntervalMs", _metricsIntervalMillis.Value.ToString());
        }
    }

    protected override void Dispose(bool disposing)
    {
        base.Dispose(disposing);
        if (File.Exists(_tokenFile)) File.Delete(_tokenFile);
    }
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
