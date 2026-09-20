using System.Security.Cryptography;
using System.Text;

namespace WakeUpMyWall.Agent.Auth;

/**
 * Token 的读写边界。Agent 侧只存**配对成功后下发的那个 Token**，
 * 手机端把它放进 Android Keystore（Phase 4B）。
 */
public interface ITokenStore
{
    bool HasToken { get; }
    string? Token { get; }
    bool Matches(string candidate);
    void Save(string token);
}

/** 内存实现：单元测试与"还没配过对"的首次启动都能用。 */
public sealed class InMemoryTokenStore(string? token = null) : ITokenStore
{
    private string? _token = token;

    public bool HasToken => _token is not null;

    public string? Token => _token;

    public bool Matches(string candidate) =>
        _token is not null && CryptographicOperations.FixedTimeEquals(
            Encoding.UTF8.GetBytes(_token),
            Encoding.UTF8.GetBytes(candidate));

    public void Save(string token) => _token = token;
}
