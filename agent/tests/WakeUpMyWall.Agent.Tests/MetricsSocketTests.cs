using System.Diagnostics;
using System.Net;
using System.Net.WebSockets;
using System.Text;
using System.Text.Json;

namespace WakeUpMyWall.Agent.Tests;

/**
 * `/ws/v1/metrics`：握手要 Bearer、帧内容与 HTTP 端点同契约、推送间隔可配。
 * 这里用 TestServer 自带的 WebSocket 客户端，不需要真的起端口。
 */
public class MetricsSocketTests
{
    private static async Task<WebSocket> ConnectAsync(TestApp app, string? token)
    {
        var client = app.Server.CreateWebSocketClient();
        if (token is not null)
        {
            client.ConfigureRequest = request => request.Headers["Authorization"] = $"Bearer {token}";
        }

        return await client.ConnectAsync(new Uri("ws://localhost/ws/v1/metrics"), CancellationToken.None);
    }

    private static async Task<string> ReceiveAsync(WebSocket socket)
    {
        var buffer = new byte[16 * 1024];
        var result = await socket.ReceiveAsync(buffer, CancellationToken.None);
        return Encoding.UTF8.GetString(buffer, 0, result.Count);
    }

    [Fact]
    public async Task A_socket_with_a_token_receives_a_metrics_frame()
    {
        using var app = new TestApp();
        var token = await TestAuth.TokenAsync(app);
        using var socket = await ConnectAsync(app, token);

        var frame = await ReceiveAsync(socket);
        using var json = JsonDocument.Parse(frame);
        var root = json.RootElement;

        // 与 GET /api/v1/system 同一份契约：字段名逐字相同（否则手机端流式与轮询会分叉）。
        Assert.False(string.IsNullOrWhiteSpace(root.GetProperty("capturedAtUtc").GetString()));
        Assert.False(string.IsNullOrWhiteSpace(root.GetProperty("identity").GetProperty("hostname").GetString()));
        Assert.InRange(root.GetProperty("cpu").GetProperty("usagePercent").GetDouble(), 0, 100);
        Assert.True(root.GetProperty("uptimeSeconds").GetInt64() >= 0);

        socket.Abort();
    }

    [Fact]
    public async Task A_socket_without_a_token_is_rejected_before_the_upgrade()
    {
        using var app = new TestApp();

        var failure = await Assert.ThrowsAnyAsync<Exception>(() => ConnectAsync(app, token: null));
        Assert.Contains("401", failure.Message);

        // 用普通 HTTP 再看一眼：明确是 401，而不是别的失败。
        var response = await app.CreateClient().GetAsync("/ws/v1/metrics");
        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    [Fact]
    public async Task A_plain_http_request_with_a_token_is_told_to_use_a_websocket()
    {
        using var app = new TestApp();

        var response = await (await TestAuth.AuthorizedClientAsync(app)).GetAsync("/ws/v1/metrics");

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    [Fact]
    public async Task Frames_follow_the_configured_interval()
    {
        using var app = new TestApp(metricsIntervalMillis: 120);
        var token = await TestAuth.TokenAsync(app);
        using var socket = await ConnectAsync(app, token);

        var stopwatch = Stopwatch.StartNew();
        await ReceiveAsync(socket);
        var first = stopwatch.ElapsedMilliseconds;
        await ReceiveAsync(socket);
        var second = stopwatch.ElapsedMilliseconds;

        Assert.True(first < 300, $"第一帧应该在 300 ms 内到达，实测 {first} ms");
        Assert.InRange(second - first, 60, 900);   // 配置 120 ms；放宽容差避免 CI 抖动

        socket.Abort();
    }
}
