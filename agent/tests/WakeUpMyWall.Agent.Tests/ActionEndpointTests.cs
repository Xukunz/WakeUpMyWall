using System.Net;
using System.Net.Http.Json;
using Microsoft.Extensions.DependencyInjection;
using WakeUpMyWall.Agent.Actions;

namespace WakeUpMyWall.Agent.Tests;

public class ActionEndpointTests
{
    [Fact]
    public async Task The_registry_lists_whitelisted_actions_with_display_names()
    {
        using var app = new TestApp();

        var actions = await (await TestAuth.AuthorizedClientAsync(app))
            .GetFromJsonAsync<List<ActionPayload>>("/api/v1/actions");

        Assert.Contains(actions!, a => a.Id == "browser" && a.Name == "Open Browser");
        Assert.Contains(actions!, a => a.Id == "steam");
    }

    [Fact]
    public async Task Unknown_action_returns_404_and_executes_nothing()
    {
        using var app = new TestApp();
        var registry = app.Services.GetRequiredService<ActionRegistry>();
        registry.ResetExecutions();

        var response = await (await TestAuth.AuthorizedClientAsync(app))
            .PostAsync("/api/v1/actions/not-a-real-action", content: null);

        Assert.Equal(HttpStatusCode.NotFound, response.StatusCode);
        Assert.Empty(registry.Executed); // 验收要求：未知 id 不得执行任何外部命令
    }

    [Fact]
    public async Task Known_action_runs_and_is_reported()
    {
        using var app = new TestApp();
        var registry = app.Services.GetRequiredService<ActionRegistry>();
        registry.ResetExecutions();

        var response = await (await TestAuth.AuthorizedClientAsync(app))
            .PostAsync("/api/v1/actions/browser", content: null);

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        Assert.Equal(["browser"], registry.Executed);
    }

    private sealed record ActionPayload(string Id, string Name);
}
