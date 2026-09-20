using WakeUpMyWall.Agent.Power;

namespace WakeUpMyWall.Agent.Api;

public static class PowerEndpoints
{
    public static void MapPowerEndpoints(this RouteGroupBuilder group) =>
        group.MapPost(
            "/api/v1/power/{action}",
            async (string action, IPowerController power, CancellationToken ct) =>
            {
                var result = action switch
                {
                    "sleep" => await power.SleepAsync(ct),
                    "shutdown" => await power.ShutdownAsync(ct),
                    "restart" => await power.RestartAsync(ct),
                    "lock" => await power.LockAsync(ct),
                    _ => null,
                };

                return result is null
                    ? Results.NotFound(new { error = "unknown power action" })
                    : result.Accepted
                        ? Results.Ok(new { action, accepted = true })
                        : Results.Json(
                            new { error = result.Error },
                            statusCode: StatusCodes.Status500InternalServerError);
            });
}
