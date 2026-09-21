using System.Security.Cryptography;
using System.Text;

namespace WakeUpMyWall.Agent.Auth;

/**
 * 配对（spec §5 没冻结配对端点，这里是本阶段补的设计）：
 * PC 端生成 6 位数字配对码，打印在控制台与日志里；只有能看见 PC 的人才能用它换 Token。
 * 配对码 5 分钟有效、一次性；Token 换到后落盘，服务重启仍然有效。
 */
public sealed class PairingService(ITokenStore store, TimeProvider clock, Action<string>? onCodeCreated = null)
{
    private static readonly TimeSpan Lifetime = TimeSpan.FromMinutes(5);

    private string? _code;
    private DateTimeOffset _expiresAt;

    public bool IsPaired => store.HasToken;

    /** 当前有效的配对码；配对成功后置空（一次性），托盘菜单靠它显示"配对码：123456"。 */
    public string? CurrentCode => _code;

    public string CreateCode()
    {
        _code = RandomNumberGenerator.GetInt32(0, 1_000_000).ToString("D6");
        _expiresAt = clock.GetUtcNow() + Lifetime;
        onCodeCreated?.Invoke(_code);
        return _code;
    }

    /**
     * 解除配对并立刻生成新码（`POST /api/v1/unpair`）。
     *
     * 为什么必须有它：以前 Unpair 只清手机端的 Token，PC 端 `agent.json` 还在，
     * 重新配对就永远拿到 409 `already paired`，用户只能去删文件重启（真机实测踩到）。
     * 调用者必须持有当前 Token（端点在 Bearer 保护组里），因此这不是"谁都能踢人"的口子。
     */
    public string Unpair()
    {
        store.Clear();
        return CreateCode();
    }

    public bool TryRedeem(string candidate, out string? token)
    {
        token = null;
        if (_code is null || clock.GetUtcNow() > _expiresAt) return false;
        if (!CryptographicOperations.FixedTimeEquals(
                Encoding.UTF8.GetBytes(_code),
                Encoding.UTF8.GetBytes(candidate)))
        {
            return false;
        }

        token = Convert.ToBase64String(RandomNumberGenerator.GetBytes(32));
        store.Save(token);
        _code = null; // 一次性
        return true;
    }
}
