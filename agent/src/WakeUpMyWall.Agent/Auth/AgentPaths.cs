namespace WakeUpMyWall.Agent.Auth;

/** Agent 的落盘位置：Windows 走 ProgramData，其它平台（开发机/CI）走用户目录。 */
public static class AgentPaths
{
    /** Agent 的落盘目录。Windows 是 `%ProgramData%\WakeUpMyWall`，其它平台是 `~/.wakeupmywall`。 */
    public static string BaseDirectory => OperatingSystem.IsWindows()
        ? Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.CommonApplicationData), "WakeUpMyWall")
        : Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), ".wakeupmywall");

    public static string DefaultTokenFile => Path.Combine(BaseDirectory, "agent.json");

    /**
     * 当前配对码的落点。装成服务后没有控制台可看，安装包与用户都从这里取码
     * （写失败不影响 Agent 运行，只是回到"只能看控制台"）。
     */
    public static string PairingCodeFile => Path.Combine(BaseDirectory, "pairing.txt");
}
