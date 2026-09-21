using System.Net;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using Microsoft.Extensions.DependencyInjection;
using WakeUpMyWall.Agent.Auth;

namespace WakeUpMyWall.Agent.Tests;

public class AuthTests
{
    [Fact]
    public async Task A_protected_endpoint_without_a_token_is_unauthorized()
    {
        using var app = new TestApp();

        var response = await app.CreateClient().GetAsync("/api/v1/system");

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    [Fact]
    public async Task A_token_that_does_not_match_is_also_unauthorized()
    {
        using var app = new TestApp();
        var client = app.CreateClient();
        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", "not-the-token");

        var response = await client.GetAsync("/api/v1/system");

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    [Fact]
    public async Task Pairing_with_the_printed_code_returns_a_token_that_then_works()
    {
        using var app = new TestApp();
        var code = app.Services.GetRequiredService<PairingService>().CreateCode();
        var client = app.CreateClient();

        var pair = await client.PostAsJsonAsync("/api/v1/pairing", new { code });
        pair.EnsureSuccessStatusCode();
        var token = (await pair.Content.ReadFromJsonAsync<TokenPayload>())!.Token;

        var authorized = app.CreateClient();
        authorized.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token);

        // 过滤器放行后处理器才跑：带正确 Token 的 /system 回 200（Phase 5A 起是真指标，不再是 501 占位）
        Assert.Equal(HttpStatusCode.OK, (await authorized.GetAsync("/api/v1/system")).StatusCode);
        // 配对成功后 /status 会如实说已经配过对
        var status = await (await app.CreateClient().GetAsync("/api/v1/status")).Content.ReadFromJsonAsync<StatusPayload>();
        Assert.True(status!.Paired);
    }

    [Fact]
    public async Task A_wrong_code_is_rejected_and_a_used_code_cannot_be_replayed()
    {
        using var app = new TestApp();
        var code = app.Services.GetRequiredService<PairingService>().CreateCode();
        var client = app.CreateClient();

        Assert.Equal(
            HttpStatusCode.Forbidden,
            (await client.PostAsJsonAsync("/api/v1/pairing", new { code = "000000" })).StatusCode);

        Assert.Equal(
            HttpStatusCode.OK,
            (await client.PostAsJsonAsync("/api/v1/pairing", new { code })).StatusCode);

        // 用过的码不再有效；而且此时已经配过对，端点是 409
        Assert.Equal(
            HttpStatusCode.Conflict,
            (await client.PostAsJsonAsync("/api/v1/pairing", new { code })).StatusCode);
    }

    private sealed record TokenPayload(string Token);

    private sealed record StatusPayload(string Hostname, string AgentVersion, long UptimeSeconds, bool Paired);
}
