using System.Net;
using System.Net.Http.Json;
using Microsoft.AspNetCore.Mvc.Testing;

namespace WakeUpMyWall.Agent.Tests;

public class StatusEndpointTests(WebApplicationFactory<Program> factory)
    : IClassFixture<WebApplicationFactory<Program>>
{
    [Fact]
    public async Task Status_reports_hostname_version_uptime_and_pairing_without_a_token()
    {
        var client = factory.CreateClient();

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
