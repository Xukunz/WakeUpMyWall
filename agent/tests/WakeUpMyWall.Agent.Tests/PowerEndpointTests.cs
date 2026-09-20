using System.Net;
using Microsoft.Extensions.DependencyInjection;
using WakeUpMyWall.Agent.Power;

namespace WakeUpMyWall.Agent.Tests;

public class PowerEndpointTests
{
    [Theory]
    [InlineData("sleep")]
    [InlineData("shutdown")]
    [InlineData("restart")]
    [InlineData("lock")]
    public async Task Every_power_endpoint_reaches_the_controller(string action)
    {
        using var app = new TestApp();
        var controller = (FakePowerController)app.Services.GetRequiredService<IPowerController>();
        controller.Clear();

        var response = await (await TestAuth.AuthorizedClientAsync(app))
            .PostAsync($"/api/v1/power/{action}", content: null);

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        Assert.Equal(action, controller.LastAction); // 真的走到了控制器，而不是只回了 200
    }

    [Fact]
    public async Task An_unknown_power_action_is_404_and_touches_nothing()
    {
        using var app = new TestApp();
        var controller = (FakePowerController)app.Services.GetRequiredService<IPowerController>();
        controller.Clear();

        var response = await (await TestAuth.AuthorizedClientAsync(app))
            .PostAsync("/api/v1/power/explode", content: null);

        Assert.Equal(HttpStatusCode.NotFound, response.StatusCode);
        Assert.Null(controller.LastAction);
    }

    [Fact]
    public async Task A_controller_failure_is_reported_as_500_with_the_reason()
    {
        using var app = new TestApp();
        var controller = (FakePowerController)app.Services.GetRequiredService<IPowerController>();
        controller.FailNext("shutdown.exe exited with 5");

        var response = await (await TestAuth.AuthorizedClientAsync(app))
            .PostAsync("/api/v1/power/shutdown", content: null);

        Assert.Equal(HttpStatusCode.InternalServerError, response.StatusCode);
        Assert.Contains("exited with 5", await response.Content.ReadAsStringAsync());
    }
}
