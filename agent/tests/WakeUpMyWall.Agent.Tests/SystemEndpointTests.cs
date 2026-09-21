using System.Net;
using System.Text.Json;

namespace WakeUpMyWall.Agent.Tests;

public class SystemEndpointTests
{
    [Fact]
    public async Task Metrics_require_a_token()
    {
        using var app = new TestApp();

        var response = await app.CreateClient().GetAsync("/api/v1/system");

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    [Fact]
    public async Task Metrics_answer_with_the_contract_fields_when_authorized()
    {
        using var app = new TestApp();

        var response = await (await TestAuth.AuthorizedClientAsync(app)).GetAsync("/api/v1/system");

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        using var json = JsonDocument.Parse(await response.Content.ReadAsStringAsync());
        var root = json.RootElement;

        Assert.False(string.IsNullOrWhiteSpace(root.GetProperty("capturedAtUtc").GetString()));
        Assert.False(string.IsNullOrWhiteSpace(root.GetProperty("identity").GetProperty("hostname").GetString()));

        var cpu = root.GetProperty("cpu");
        Assert.InRange(cpu.GetProperty("usagePercent").GetDouble(), 0, 100);
        Assert.True(cpu.GetProperty("threads").GetInt32() >= 1);
        Assert.InRange(root.GetProperty("memory").GetProperty("usagePercent").GetDouble(), 0, 100);
        Assert.True(root.GetProperty("network").GetProperty("downloadMbps").GetDouble() >= 0);
        Assert.True(root.GetProperty("uptimeSeconds").GetInt64() >= 0);
    }
}
