using System.Net;
using Microsoft.Extensions.DependencyInjection;
using WakeUpMyWall.Agent.Metrics;

namespace WakeUpMyWall.Agent.Tests;

/**
 * 指标读取失败时的契约：**给一句可读的错误**，而不是空白 500。
 * 真机上的典型场景：LibreHardwareMonitor 的驱动加载不起来（权限/主板不支持）。
 */
public class MetricsFailureTests
{
    private sealed class ThrowingMetricsProvider : ISystemMetricsProvider
    {
        public SystemMetricsPayload Read() =>
            throw new InvalidOperationException("LibreHardwareMonitor could not open the device");
    }

    [Fact]
    public async Task A_provider_failure_is_a_500_with_the_reason_in_the_body()
    {
        using var app = new TestApp(configureServices: services =>
            services.AddSingleton<ISystemMetricsProvider>(new ThrowingMetricsProvider()));

        var response = await (await TestAuth.AuthorizedClientAsync(app)).GetAsync("/api/v1/system");

        Assert.Equal(HttpStatusCode.InternalServerError, response.StatusCode);
        var body = await response.Content.ReadAsStringAsync();
        Assert.Contains("could not read system metrics", body);
        Assert.Contains("LibreHardwareMonitor could not open the device", body);
    }
}
