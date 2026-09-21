using WakeUpMyWall.Agent.Metrics;

namespace WakeUpMyWall.Agent.Api;

public static class SystemEndpoints
{
    // spec §8：指标是受保护端点（Bearer）——载荷里有主机名与硬件型号。
    // 读指标失败（例如 LibreHardwareMonitor 的驱动加载不起来）必须给一句**可读**的错误：
    // 空白 500 会让手机端只显示一句 "HTTP 500"，排查等于从零开始。
    public static void MapSystemEndpoints(this RouteGroupBuilder group) =>
        group.MapGet("/api/v1/system", (ISystemMetricsProvider metrics, ILoggerFactory loggerFactory) =>
        {
            try
            {
                return Results.Ok(metrics.Read());
            }
            catch (Exception exception)
            {
                loggerFactory.CreateLogger("Metrics").LogError(exception, "读取指标失败");
                return Results.Json(
                    new { error = $"could not read system metrics: {exception.GetType().Name}: {exception.Message}" },
                    statusCode: StatusCodes.Status500InternalServerError);
            }
        });
}
