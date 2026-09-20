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
        Assert.Equal("0.1.0", payload.AgentVersion);
        Assert.True(payload.UptimeSeconds >= 0);
        Assert.False(payload.Paired);
    }

    private sealed record StatusPayload(string Hostname, string AgentVersion, long UptimeSeconds, bool Paired);
}
