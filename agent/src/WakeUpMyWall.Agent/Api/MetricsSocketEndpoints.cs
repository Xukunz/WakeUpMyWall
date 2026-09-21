using System.Net.WebSockets;
using System.Text.Json;
using WakeUpMyWall.Agent.Auth;
using WakeUpMyWall.Agent.Metrics;

namespace WakeUpMyWall.Agent.Api;

public static class MetricsSocketEndpoints
{
    private const string BearerScheme = "Bearer ";
    private const int DefaultIntervalMillis = 1_000;

    // 与 HTTP 端点用同一套 Web 默认（camelCase），两边的字段名不许分叉。
    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web);

    /**
     * spec §5/§8 的流式通道：手机连上后按间隔收帧，帧内容与 `GET /api/v1/system` **逐字相同**。
     *
     * 鉴权放在**握手阶段**（AcceptWebSocketAsync 之前）：没 Token 就 401，连接根本不会建立，
     * 也就不会产生任何副作用。过滤器（BearerAuthFilter）不覆盖 WebSocket 升级请求，所以这里自查。
     */
    public static void MapMetricsSocket(this WebApplication app) =>
        app.MapGet(
            "/ws/v1/metrics",
            async (
                HttpContext context,
                ITokenStore tokens,
                ISystemMetricsProvider metrics,
                IConfiguration configuration,
                CancellationToken ct) =>
            {
                var header = context.Request.Headers.Authorization.ToString();
                if (!header.StartsWith(BearerScheme, StringComparison.Ordinal) ||
                    !tokens.Matches(header[BearerScheme.Length..].Trim()))
                {
                    context.Response.StatusCode = StatusCodes.Status401Unauthorized;
                    await context.Response.WriteAsJsonAsync(new { error = "missing or invalid token" }, ct);
                    return;
                }

                if (!context.WebSockets.IsWebSocketRequest)
                {
                    context.Response.StatusCode = StatusCodes.Status400BadRequest;
                    await context.Response.WriteAsJsonAsync(
                        new { error = "this endpoint speaks websocket" },
                        ct);
                    return;
                }

                var interval = ReadInterval(configuration);
                using var socket = await context.WebSockets.AcceptWebSocketAsync();
                await PushFramesAsync(socket, metrics, interval, ct);
            });

    /**
     * 推帧循环：一直推到客户端断开或服务停止。取消/断开都不算错误，安静收尾。
     *
     * 本协议是"只推不收"：客户端没有任何上行消息。因此这里**不并发读**——客户端的断开
     * 由 `SendAsync` 抛错或 `State` 变化暴露（实测在 TestHost 里并发 Receive 会把连接打断）。
     */
    private static async Task PushFramesAsync(
        WebSocket socket,
        ISystemMetricsProvider metrics,
        TimeSpan interval,
        CancellationToken ct)
    {
        while (!ct.IsCancellationRequested && socket.State == WebSocketState.Open)
        {
            try
            {
                var frame = JsonSerializer.SerializeToUtf8Bytes(metrics.Read(), JsonOptions);
                await socket.SendAsync(frame, WebSocketMessageType.Text, endOfMessage: true, ct);
                await Task.Delay(interval, ct);
            }
            catch (OperationCanceledException)
            {
                break;
            }
            catch (WebSocketException)
            {
                break;
            }
        }

        if (socket.State == WebSocketState.Open)
        {
            // CloseOutput：对端先发 Close 时只需把确认帧发出去（这回礼正是客户端 CloseAsync 在等的）。
            await socket.CloseOutputAsync(WebSocketCloseStatus.NormalClosure, "bye", CancellationToken.None);
        }
    }

    /** 推送间隔可由 `Agent:MetricsIntervalMs` 覆盖（测试与演示用短值）。 */
    private static TimeSpan ReadInterval(IConfiguration configuration) =>
        int.TryParse(configuration["Agent:MetricsIntervalMs"], out var millis) && millis > 0
            ? TimeSpan.FromMilliseconds(millis)
            : TimeSpan.FromMilliseconds(DefaultIntervalMillis);
}
