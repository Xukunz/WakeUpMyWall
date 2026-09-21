using System.Text.RegularExpressions;
using WakeUpMyWall.Agent.Auth;

namespace WakeUpMyWall.Agent.Tests;

/**
 * 服务模式没有控制台，配对码只能落在盘上——这是"一键安装"能走通的关键一环：
 * 安装包装完读这个文件，把码显示给用户。
 */
public class PairingCodeFileTests
{
    [Fact]
    public void Starting_the_agent_writes_the_pairing_code_to_disk()
    {
        using var app = new TestApp();
        _ = app.CreateClient();      // 触碰一次宿主：入口里的"生成并落盘配对码"随之执行

        Assert.True(File.Exists(app.PairingCodeFile), $"没有写出配对码文件：{app.PairingCodeFile}");
        var code = File.ReadAllText(app.PairingCodeFile).Trim();
        Assert.Matches(new Regex("^[0-9]{6}$"), code);
    }

    [Fact]
    public void An_unwritable_location_is_reported_instead_of_crashing()
    {
        // 目录名与已存在的文件冲突：CreateDirectory 会失败，但 Agent 不能因此起不来。
        var blocker = Path.Combine(Path.GetTempPath(), $"wumw-blocker-{Guid.NewGuid():N}");
        File.WriteAllText(blocker, "not a directory");

        var written = PairingCodeFile.TryWrite(Path.Combine(blocker, "pairing.txt"), "123456", out var error);

        Assert.False(written);
        Assert.False(string.IsNullOrWhiteSpace(error));
        File.Delete(blocker);
    }

    [Fact]
    public void The_pairing_file_lives_next_to_the_token_file()
    {
        // 两个文件同一个目录：卸载/排查时只需要看一个地方。
        Assert.Equal(
            Path.GetDirectoryName(AgentPaths.DefaultTokenFile),
            Path.GetDirectoryName(AgentPaths.PairingCodeFile));
    }
}
