using WakeUpMyWall.Agent.Auth;

namespace WakeUpMyWall.Agent.Api;

public static class StatusEndpoints
{
    private static readonly DateTimeOffset StartedAt = DateTimeOffset.UtcNow;

    /**
     * Agent 自报的版本号**从程序集读**（csproj 的 `<Version>`），不再手写字符串：
     * 手写版本号在 Phase 5A 时坑过一次——PC 上装的是旧包、`/status` 却报同样的 0.1.0，
     * 手机端与用户都分不清。
     */
    public static string AgentVersion =>
        typeof(Program).Assembly.GetName().Version?.ToString(3) ?? "0.0.0";

    public static void MapStatusEndpoints(this WebApplication app) =>
        // spec §5：status 在配对前可用，只回最小信息（主机名 / 版本 / 运行时长 / 是否已配对）。
        app.MapGet("/api/v1/status", (ITokenStore tokens) => Results.Ok(new StatusPayload(
            Hostname: Environment.MachineName,
            AgentVersion: AgentVersion,
            UptimeSeconds: (long)(DateTimeOffset.UtcNow - StartedAt).TotalSeconds,
            Paired: tokens.HasToken)));
}

public sealed record StatusPayload(string Hostname, string AgentVersion, long UptimeSeconds, bool Paired);
