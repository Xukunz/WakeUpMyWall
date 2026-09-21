using WakeUpMyWall.Agent.Tray;

namespace WakeUpMyWall.Agent.Tests;

/**
 * 托盘图标是"用户唯一能看到的东西"（Agent 没有窗口），所以选图规则与图标文件本身都要有测试盯着：
 * 选错就是浅底浅图糊成一片，少尺寸就是托盘里糊。
 */
public class TrayIconTests
{
    [Fact]
    public void The_taskbar_theme_decides_which_tray_icon_is_used()
    {
        // 亮色任务栏放暗色图，深色任务栏放亮色图。
        Assert.Equal("tray_dark.ico", TrayIconAssets.ForTaskbar(lightTaskbar: true));
        Assert.Equal("tray_bright.ico", TrayIconAssets.ForTaskbar(lightTaskbar: false));
    }

    [Theory]
    [InlineData("app.ico", 256)]
    [InlineData("tray_bright.ico", 32)]
    [InlineData("tray_dark.ico", 32)]
    public void Every_icon_ships_multiple_sizes_so_windows_never_scales_a_single_bitmap(string name, int requiredSize)
    {
        var file = Path.Combine(AssetsDirectory(), name);
        Assert.True(File.Exists(file), $"缺少图标 {name}（跑一次 tools/prepare_ui_assets.py）");

        var bytes = File.ReadAllBytes(file);
        Assert.Equal(0, BitConverter.ToUInt16(bytes, 0));   // reserved
        Assert.Equal(1, BitConverter.ToUInt16(bytes, 2));   // type = icon
        var count = BitConverter.ToUInt16(bytes, 4);
        Assert.True(count >= 3, $"{name} 只有 {count} 档尺寸，托盘/桌面会糊");

        var sizes = new List<int>();
        for (var index = 0; index < count; index++)
        {
            // ICONDIRENTRY：宽高各一字节，0 表示 256。
            var width = bytes[6 + index * 16];
            sizes.Add(width == 0 ? 256 : width);
        }
        Assert.Contains(requiredSize, sizes);
    }

    /** 测试跑在 bin/ 下，向上找到仓库里的 Assets 目录（跨平台，与 Windows 无关）。 */
    private static string AssetsDirectory()
    {
        var directory = new DirectoryInfo(AppContext.BaseDirectory);
        while (directory is not null)
        {
            var candidate = Path.Combine(directory.FullName, "agent", "src", "WakeUpMyWall.Agent", "Assets");
            if (Directory.Exists(candidate)) return candidate;
            directory = directory.Parent;
        }
        throw new DirectoryNotFoundException("找不到 agent/src/WakeUpMyWall.Agent/Assets");
    }
}
