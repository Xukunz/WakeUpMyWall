using System.Security.Cryptography;
using System.Text;

namespace WakeUpMyWall.Agent.Auth;

/**
 * 配对（spec §5 没冻结配对端点，这里是本阶段补的设计）：
 * PC 端生成 6 位数字配对码，打印在控制台与日志里；只有能看见 PC 的人才能用它换 Token。
 * 配对码 5 分钟有效、一次性；Token 换到后落盘，服务重启仍然有效。
 */
public sealed class PairingService(ITokenStore store, TimeProvider clock)
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
        return _code;
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
