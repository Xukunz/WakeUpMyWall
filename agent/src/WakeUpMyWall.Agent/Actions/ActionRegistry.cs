namespace WakeUpMyWall.Agent.Actions;

/**
 * 快捷动作白名单（spec §5）：手机只能按 id 触发这里注册过的动作。
 * 未知 id 一律查不到、**不产生任何副作用** —— 永不提供"传命令字符串"的接口。
 */
public sealed class ActionRegistry
{
    private static readonly Dictionary<string, string> Names = new()
    {
        ["browser"] = "Open Browser",
        ["steam"] = "Launch Steam",
        ["spotify"] = "Open Spotify",
        ["discord"] = "Open Discord",
    };

    private readonly List<string> _executed = [];

    public IReadOnlyDictionary<string, string> All => Names;

    public IReadOnlyList<string> Executed
    {
        get { lock (_executed) return [.. _executed]; }
    }

    public void ResetExecutions()
    {
        lock (_executed) _executed.Clear();
    }

    public bool TryExecute(string id)
    {
        if (!Names.ContainsKey(id)) return false;

        lock (_executed) _executed.Add(id);
        return true;
    }
}
