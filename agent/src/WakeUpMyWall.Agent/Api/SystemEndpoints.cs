using WakeUpMyWall.Agent.Metrics;

namespace WakeUpMyWall.Agent.Api;

public static class SystemEndpoints
{
    // spec §8：指标是受保护端点（Bearer）——载荷里有主机名与硬件型号。
    public static void MapSystemEndpoints(this RouteGroupBuilder group) =>
        group.MapGet("/api/v1/system", (ISystemMetricsProvider metrics) => Results.Ok(metrics.Read()));
}
