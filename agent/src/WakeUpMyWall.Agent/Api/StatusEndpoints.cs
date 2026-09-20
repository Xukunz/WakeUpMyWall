using WakeUpMyWall.Agent.Auth;

namespace WakeUpMyWall.Agent.Api;

public static class StatusEndpoints
{
    private static readonly DateTimeOffset StartedAt = DateTimeOffset.UtcNow;

    public static void MapStatusEndpoints(this WebApplication app) =>
        // spec §5：status 在配对前可用，只回最小信息（主机名 / 版本 / 运行时长 / 是否已配对）。
        app.MapGet("/api/v1/status", (ITokenStore tokens) => Results.Ok(new StatusPayload(
            Hostname: Environment.MachineName,
            AgentVersion: "0.1.0",
            UptimeSeconds: (long)(DateTimeOffset.UtcNow - StartedAt).TotalSeconds,
            Paired: tokens.HasToken)));
}

public sealed record StatusPayload(string Hostname, string AgentVersion, long UptimeSeconds, bool Paired);
