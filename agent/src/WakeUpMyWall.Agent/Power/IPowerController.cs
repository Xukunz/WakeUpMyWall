namespace WakeUpMyWall.Agent.Power;

public sealed record PowerResult(bool Accepted, string? Error = null);

/**
 * 电源能力的唯一抽象。Windows 实现调用系统命令；其它平台用 FakePowerController
 * （只记录、不执行），这样契约测试与服务冒烟都能在 Linux/CI 上跑。
 */
public interface IPowerController
{
    Task<PowerResult> SleepAsync(CancellationToken ct);
    Task<PowerResult> ShutdownAsync(CancellationToken ct);
    Task<PowerResult> RestartAsync(CancellationToken ct);
    Task<PowerResult> LockAsync(CancellationToken ct);
}
