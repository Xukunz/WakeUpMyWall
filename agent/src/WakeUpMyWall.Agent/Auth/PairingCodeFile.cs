namespace WakeUpMyWall.Agent.Auth;

/**
 * 把当前配对码写到盘上，供"装成服务、没有控制台"的场景读取：
 * 安装包在装完后直接读这个文件把配对码显示给用户（见 agent/installer/WakeUpMyWall.Agent.iss）。
 *
 * 写失败不是错误：Agent 照常运行，用户仍可以在前台窗口看到配对码。
 */
public static class PairingCodeFile
{
    public static bool TryWrite(string path, string code, out string? error)
    {
        try
        {
            var directory = Path.GetDirectoryName(path);
            if (!string.IsNullOrEmpty(directory))
            {
                Directory.CreateDirectory(directory);
            }

            File.WriteAllText(path, code);
            error = null;
            return true;
        }
        catch (Exception exception) when (exception is IOException or UnauthorizedAccessException or NotSupportedException)
        {
            error = exception.Message;
            return false;
        }
    }
}
