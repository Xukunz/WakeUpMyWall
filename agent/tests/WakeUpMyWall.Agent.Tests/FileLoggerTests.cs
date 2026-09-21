using Microsoft.Extensions.Logging;
using WakeUpMyWall.Agent.Logging;

namespace WakeUpMyWall.Agent.Tests;

/**
 * 服务模式没有控制台：日志必须真的落到盘上，否则用户报"点了没反应"时无从查起。
 */
public class FileLoggerTests
{
    [Fact]
    public void Log_lines_are_appended_to_the_file()
    {
        var path = Path.Combine(Path.GetTempPath(), $"wumw-log-{Guid.NewGuid():N}.log");
        using var provider = new FileLoggerProvider(path);
        var logger = provider.CreateLogger("Power");

        logger.LogInformation("power action {Action} -> {Outcome}", "sleep", "accepted");

        var content = File.ReadAllText(path);
        Assert.Contains("power action sleep -> accepted", content);
        Assert.Contains("[Information]", content);
        File.Delete(path);
    }

    [Fact]
    public void An_unwritable_log_path_never_breaks_the_caller()
    {
        // 用一个"目录名被文件占住"的路径：写入失败必须被吞掉。
        var blocker = Path.Combine(Path.GetTempPath(), $"wumw-log-blocker-{Guid.NewGuid():N}");
        File.WriteAllText(blocker, "not a directory");

        using var provider = new FileLoggerProvider(Path.Combine(blocker, "agent.log"));
        var exception = Record.Exception(() => provider.CreateLogger("Power").LogInformation("hello"));

        Assert.Null(exception);
        File.Delete(blocker);
    }
}
