using WakeUpMyWall.Agent.Actions;

namespace WakeUpMyWall.Agent.Api;

public static class ActionEndpoints
{
    public static void MapActionEndpoints(this RouteGroupBuilder group)
    {
        group.MapGet("/api/v1/actions", (ActionRegistry registry) =>
            Results.Ok(registry.All.Select(pair => new { id = pair.Key, name = pair.Value })));

        group.MapPost("/api/v1/actions/{id}", (string id, ActionRegistry registry) =>
            registry.TryExecute(id)
                ? Results.Ok(new { id, executed = true })
                : Results.NotFound(new { error = "unknown action" }));
    }
}
