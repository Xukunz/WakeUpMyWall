namespace WakeUpMyWall.Agent.Power;

/**
 * CI 与非 Windows 开发机使用（也用于本机的端到端冒烟）：记录调用但**不执行任何外部命令**，
 * 所以测试里可以断言"确实走到了控制器"，而不会真的把机器关掉。
 */
public sealed class FakePowerController : IPowerController
{
    private readonly List<string> _calls = [];
    private string? _nextFailure;

    public string? LastAction
    {
        get { lock (_calls) return _calls.LastOrDefault(); }
    }

    public void Clear()
    {
        lock (_calls) _calls.Clear();
    }

    /** 让下一次调用失败，用来验证 500 + 原因透传。 */
    public void FailNext(string reason) => _nextFailure = reason;

    public Task<PowerResult> SleepAsync(CancellationToken ct) => Record("sleep");

    public Task<PowerResult> ShutdownAsync(CancellationToken ct) => Record("shutdown");

    public Task<PowerResult> RestartAsync(CancellationToken ct) => Record("restart");

    public Task<PowerResult> LockAsync(CancellationToken ct) => Record("lock");

    private Task<PowerResult> Record(string action)
    {
        lock (_calls) _calls.Add(action);

        if (_nextFailure is { } failure)
        {
            _nextFailure = null;
            return Task.FromResult(new PowerResult(false, failure));
        }

        return Task.FromResult(new PowerResult(true));
    }
}
