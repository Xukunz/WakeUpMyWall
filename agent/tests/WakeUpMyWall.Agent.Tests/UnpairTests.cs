using System.Net;
using System.Net.Http.Json;
using System.Text.RegularExpressions;

namespace WakeUpMyWall.Agent.Tests;

/**
 * 真机踩到的坑：手机端点 Unpair 只清了本地 Token，PC 端 `agent.json` 还在，
 * 于是重新配对永远 409 `already paired`。`POST /api/v1/unpair` 就是补这个洞。
 */
public class UnpairTests
{
    [Fact]
    public async Task Unpair_forgets_the_token_and_hands_out_a_fresh_code()
    {
        using var app = new TestApp();
        var client = await TestAuth.AuthorizedClientAsync(app);

        var response = await client.PostAsync("/api/v1/unpair", content: null);

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        var payload = await response.Content.ReadFromJsonAsync<UnpairPayload>();
        Assert.Matches(new Regex("^[0-9]{6}$"), payload!.PairingCode);

        // 旧 Token 立刻失效，且 PC 回到"未配对"。
        Assert.Equal(HttpStatusCode.Unauthorized, (await client.GetAsync("/api/v1/system")).StatusCode);
        Assert.Contains("\"paired\":false", await app.CreateClient().GetStringAsync("/api/v1/status"));

        // 新码能立刻用来重新配对。
        var rePair = await app.CreateClient().PostAsJsonAsync("/api/v1/pairing", new { code = payload.PairingCode });
        Assert.Equal(HttpStatusCode.OK, rePair.StatusCode);
    }

    [Fact]
    public async Task Unpair_without_a_token_is_rejected()
    {
        using var app = new TestApp();

        var response = await app.CreateClient().PostAsync("/api/v1/unpair", content: null);

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    private sealed record UnpairPayload(string PairingCode);
}
