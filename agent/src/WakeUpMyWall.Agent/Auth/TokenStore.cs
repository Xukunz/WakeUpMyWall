using System.Security.Cryptography;
using System.Text;
using System.Text.Json;

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

    /** 忘掉 Token（手机端点 Unpair 时由 `/api/v1/unpair` 调用）：落盘文件一并删掉，回到"未配对"。 */
    void Clear();
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

    public void Clear() => _token = null;
}

/**
 * 落盘实现：配对成功后写入 AgentPaths（Windows 是 %ProgramData%\WakeUpMyWall\agent.json），
 * 服务重启后 Token 仍然有效。文件只在配对时写一次。
 */
public sealed class FileTokenStore(string path) : ITokenStore
{
    private string? _token = Load(path);

    public bool HasToken => _token is not null;

    public string? Token => _token;

    public bool Matches(string candidate) =>
        _token is not null && CryptographicOperations.FixedTimeEquals(
            Encoding.UTF8.GetBytes(_token),
            Encoding.UTF8.GetBytes(candidate));

    public void Save(string token)
    {
        var directory = Path.GetDirectoryName(path);
        if (!string.IsNullOrEmpty(directory)) Directory.CreateDirectory(directory);
        File.WriteAllText(path, JsonSerializer.Serialize(new Persisted(token)));
        _token = token;
    }

    public void Clear()
    {
        _token = null;
        if (File.Exists(path)) File.Delete(path);
    }

    private static string? Load(string path) =>
        File.Exists(path)
            ? JsonSerializer.Deserialize<Persisted>(File.ReadAllText(path))?.Token
            : null;

    private sealed record Persisted(string Token);
}
