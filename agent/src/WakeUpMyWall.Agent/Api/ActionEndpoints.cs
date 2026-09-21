using WakeUpMyWall.Agent.Actions;

namespace WakeUpMyWall.Agent.Api;

public static class ActionEndpoints
{
    public static void MapActionEndpoints(this RouteGroupBuilder group)
    {
        group.MapGet("/api/v1/actions", (ActionRegistry registry) =>
            Results.Ok(registry.All.Select(pair => new { id = pair.Key, name = pair.Value })));

        group.MapPost("/api/v1/actions/{id}", (string id, ActionRegistry registry) =>
        {
            var result = registry.Execute(id);
            return result switch
            {
                { Known: false } => Results.NotFound(new { error = "unknown action" }),
                { Executed: true } => Results.Ok(new { id, executed = true }),
                _ => Results.Json(
                    new { error = result.Error ?? "could not execute action" },
                    statusCode: StatusCodes.Status500InternalServerError),
            };
        });
    }
}
