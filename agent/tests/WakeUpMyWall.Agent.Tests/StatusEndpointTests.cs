using System.Net;
using System.Net.Http.Json;

namespace WakeUpMyWall.Agent.Tests;

public class StatusEndpointTests
{
    [Fact]
    public async Task Status_reports_hostname_version_uptime_and_pairing_without_a_token()
    {
        using var app = new TestApp();
        var client = app.CreateClient();

        var response = await client.GetAsync("/api/v1/status");
        var payload = await response.Content.ReadFromJsonAsync<StatusPayload>();

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        Assert.False(string.IsNullOrWhiteSpace(payload!.Hostname));
        // 版本号从 csproj 的 <Version> 派生：装到 PC 上的包是哪一版，手机端一眼能看出。
        Assert.Equal("0.3.0", payload.AgentVersion);
        Assert.True(payload.UptimeSeconds >= 0);
        Assert.False(payload.Paired);
    }

    private sealed record StatusPayload(string Hostname, string AgentVersion, long UptimeSeconds, bool Paired);
}
