using WakeUpMyWall.Agent.Power;

namespace WakeUpMyWall.Agent.Api;

public static class PowerEndpoints
{
    public static void MapPowerEndpoints(this RouteGroupBuilder group) =>
        group.MapPost(
            "/api/v1/power/{action}",
            async (string action, IPowerController power, ILoggerFactory loggerFactory, CancellationToken ct) =>
            {
                var logger = loggerFactory.CreateLogger("Power");
                var result = action switch
                {
                    "sleep" => await power.SleepAsync(ct),
                    "shutdown" => await power.ShutdownAsync(ct),
                    "restart" => await power.RestartAsync(ct),
                    "lock" => await power.LockAsync(ct),
                    _ => null,
                };

                // 每次电源请求都留痕：手机点了没反应时，这是唯一能分清"没发到"和"发了但没生效"的地方。
                logger.LogInformation(
                    "power action {Action} -> {Outcome}",
                    action,
                    result is null ? "unknown action" : result.Accepted ? "accepted" : result.Error);

                return result is null
                    ? Results.NotFound(new { error = "unknown power action" })
                    : result.Accepted
                        ? Results.Ok(new { action, accepted = true })
                        : Results.Json(
                            new { error = result.Error },
                            statusCode: StatusCodes.Status500InternalServerError);
            });
}
