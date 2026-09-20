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

    protected override void ConfigureWebHost(IWebHostBuilder builder) =>
        builder.UseSetting("Agent:TokenFile", _tokenFile);

    protected override void Dispose(bool disposing)
    {
        base.Dispose(disposing);
        if (File.Exists(_tokenFile)) File.Delete(_tokenFile);
    }
}

public static class TestAuth
{
    /** 走一遍真实配对流程，返回带 Token 的 client（不是伪造 header）。 */
    public static async Task<HttpClient> AuthorizedClientAsync(TestApp app)
    {
        var code = app.Services.GetRequiredService<PairingService>().CreateCode();
        var client = app.CreateClient();

        var response = await client.PostAsJsonAsync("/api/v1/pairing", new { code });
        response.EnsureSuccessStatusCode();
        var token = (await response.Content.ReadFromJsonAsync<TokenPayload>())!.Token;

        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token);
        return client;
    }

    private sealed record TokenPayload(string Token);
}
