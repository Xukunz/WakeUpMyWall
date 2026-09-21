using System.Diagnostics;

namespace WakeUpMyWall.Agent.Actions;

/**
 * 快捷动作白名单（spec §5）：手机只能按 id 触发这里注册过的动作。
 * 未知 id 一律查不到、**不产生任何副作用** —— 永不提供"传命令字符串"的接口。
 *
 * 0.4.0 起这些动作真的会启动程序（此前只把 id 记进列表，所以手机上点了没反应）。
 * 一律走 ShellExecute 打开协议/默认程序，不拼命令行；服务模式下没有交互桌面，会失败并如实回报。
 */
public sealed class ActionRegistry(IAppLauncher launcher, ILogger<ActionRegistry> logger)
{
    private static readonly Dictionary<string, string> Names = new()
    {
        ["browser"] = "Open Browser",
        ["steam"] = "Launch Steam",
        ["spotify"] = "Open Spotify",
        ["discord"] = "Open Discord",
    };

    private readonly List<string> _executed = [];

    /** id → 打开目标（协议或 URL）。改这里就等于改白名单，手机端不需要动。 */
    private static readonly Dictionary<string, string> Targets = new()
    {
        ["browser"] = "https://www.google.com",
        ["steam"] = "steam://open/main",
        ["spotify"] = "spotify:",
        ["discord"] = "discord://",
    };

    public IReadOnlyDictionary<string, string> All => Names;

    public IReadOnlyList<string> Executed
    {
        get { lock (_executed) return [.. _executed]; }
    }

    public void ResetExecutions()
    {
        lock (_executed) _executed.Clear();
    }

    /** 执行白名单动作。未知 id 与"已知但启动失败"必须分开——前者 404，后者 500 + 原因。 */
    public (bool Known, bool Executed, string? Error) Execute(string id)
    {
        if (!Targets.TryGetValue(id, out var target)) return (false, false, null);

        lock (_executed) _executed.Add(id);
        var launched = launcher.TryLaunch(target);
        logger.LogInformation("action {Id} -> {Target} ({Outcome})", id, target, launched ? "launched" : "failed");
        return (true, launched, launched ? null : $"could not open {target}");
    }
}

/** 启动外部程序/协议的边界：抽出来是为了让契约测试不真的把 Steam 拉起来。 */
public interface IAppLauncher
{
    bool TryLaunch(string target);
}

/** 真实实现：交给 shell 关联（浏览器开 URL、Steam 开 steam://）。 */
public sealed class ShellAppLauncher(ILogger<ShellAppLauncher> logger) : IAppLauncher
{
    public bool TryLaunch(string target)
    {
        try
        {
            using var process = Process.Start(new ProcessStartInfo(target) { UseShellExecute = true });
            return process is not null;
        }
        catch (Exception exception) when (exception is InvalidOperationException or System.ComponentModel.Win32Exception)
        {
            // 服务模式（Session 0）没有交互桌面，打开协议会失败——如实回报，不假装成功。
            logger.LogWarning(exception, "打开 {Target} 失败", target);
            return false;
        }
    }
}
