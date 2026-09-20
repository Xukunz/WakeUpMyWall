namespace WakeUpMyWall.Agent.Auth;

/** Agent 的落盘位置：Windows 走 ProgramData，其它平台（开发机/CI）走用户目录。 */
public static class AgentPaths
{
    public static string DefaultTokenFile => OperatingSystem.IsWindows()
        ? Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.CommonApplicationData),
            "WakeUpMyWall",
            "agent.json")
        : Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.UserProfile),
            ".wakeupmywall",
            "agent.json");
}
