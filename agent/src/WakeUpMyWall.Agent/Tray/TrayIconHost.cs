using System.Drawing;
using System.Windows.Forms;
using Microsoft.Win32;

namespace WakeUpMyWall.Agent.Tray;

/**
 * 托盘宿主：**没有窗口**，只在通知区域放一个图标；右键菜单两项——当前配对码与退出。
 *
 * 用 WinForms 的 `NotifyIcon`（而不是手写 `Shell_NotifyIcon`）：它替我们处理了消息窗口、
 * 图标句柄生命周期、任务栏重启、DPI 这些坑，代价是发布体积大一些。
 *
 * 三点约定：
 *  1. 消息循环跑在**专用 STA 线程**上，与 ASP.NET 的线程互不干扰；
 *  2. 只有在"交互式运行"时才启动——服务模式（Session 0）装不了托盘图标，由调用方判断；
 *  3. **启不来不算错**：返回 null 并记日志，Agent 本体照常提供接口（用户还能用日志/配置文件排查）。
 */
public sealed class TrayIconHost : IDisposable
{
    private readonly NotifyIcon _notifyIcon;
    private readonly ContextMenuStrip _menu;
    private readonly ToolStripMenuItem _pairingItem;

    private TrayIconHost(Func<string> menuLabel, string? notice, Func<string> resetPairing, Action exit, ILogger logger)
    {
        // 先建图标对象：下面的菜单项回调要用它弹气泡，字段是 readonly，不能等菜单做完再赋值。
        _notifyIcon = new NotifyIcon
        {
            Icon = LoadIcon(IsLightTaskbar()),
            // 悬停提示有 63 字符上限；此处只是名字，详细信息走右键菜单。
            Text = "WakeUpMyWall Agent",
        };
        _pairingItem = new ToolStripMenuItem(menuLabel()) { Enabled = false };

        // 重置配对必须在 PC 侧提供：手机端一旦丢了 Token，就无法让 PC 忘掉旧配对，
        // 重新配对会永远 409 already paired（真机踩到）。
        var resetItem = new ToolStripMenuItem("重新生成配对码（解除配对）");
        resetItem.Click += (_, _) =>
        {
            var code = resetPairing();
            _pairingItem.Text = menuLabel();
            _notifyIcon.BalloonTipTitle = "配对已重置";
            _notifyIcon.BalloonTipText = $"新的配对码：{code}（5 分钟有效）";
            _notifyIcon.ShowBalloonTip(10_000);
        };

        var exitItem = new ToolStripMenuItem("退出 WakeUpMyWall Agent");
        exitItem.Click += (_, _) =>
        {
            logger.LogInformation("用户从托盘退出 Agent");
            Application.Exit();
            exit();
        };

        _menu = new ContextMenuStrip();
        _menu.Items.Add(_pairingItem);
        // 环境前提缺了（例如 PawnIO 没装）时挂一条不可点的说明：温度读不到的原因不该只在日志里。
        if (notice is not null) _menu.Items.Add(new ToolStripMenuItem($"⚠ {notice}") { Enabled = false });
        _menu.Items.Add(resetItem);
        _menu.Items.Add(new ToolStripSeparator());
        _menu.Items.Add(exitItem);
        // 每次弹出前刷新：配对码是会变的（重启换新码，配对成功后失效）。
        _menu.Opening += (_, _) => _pairingItem.Text = menuLabel();

        _notifyIcon.ContextMenuStrip = _menu;
        _notifyIcon.Visible = true;

        // 首次运行给一次气泡，省得用户找不到图标（很多人不知道要去右下角找）。
        _notifyIcon.BalloonTipTitle = "WakeUpMyWall Agent 已在运行";
        _notifyIcon.BalloonTipText = notice is null
            ? menuLabel() + "（右键图标可查看或退出）"
            : $"{notice}。右键图标查看配对码。";
        _notifyIcon.ShowBalloonTip(8_000);
    }

    /**
     * 在专用 STA 线程上启动托盘。**失败返回 null**（不抛）：调用方只记日志，不影响 Agent。
     */
    public static TrayIconHost? Start(
        Func<string> menuLabel,
        string? notice,
        Func<string> resetPairing,
        Action exit,
        ILogger logger)
    {
        TrayIconHost? host = null;
        using var ready = new ManualResetEventSlim(false);

        var thread = new Thread(() =>
        {
            try
            {
                host = new TrayIconHost(menuLabel, notice, resetPairing, exit, logger);
                ready.Set();
                Application.Run(new ApplicationContext());
            }
            catch (Exception exception)
            {
                logger.LogWarning(exception, "托盘图标启动失败，Agent 仍在后台运行（可从 agent.log 查配对码）");
                ready.Set();
            }
        })
        {
            IsBackground = true,
            Name = "WakeUpMyWallAgent.Tray",
        };
        thread.SetApartmentState(ApartmentState.STA);
        thread.Start();

        // 等图标创建完成：5 秒足够，超时也不阻塞 Agent 启动。
        return ready.Wait(TimeSpan.FromSeconds(5)) ? host : null;
    }

    public void Dispose()
    {
        _notifyIcon.Visible = false;
        _notifyIcon.Dispose();
        _menu.Dispose();
    }

    /** 托盘图标来自程序集内嵌资源，不依赖安装目录里的相对路径。 */
    private static Icon LoadIcon(bool lightTaskbar)
    {
        var resource = $"WakeUpMyWall.Agent.Assets.{TrayIconAssets.ForTaskbar(lightTaskbar)}";
        using var stream = typeof(TrayIconHost).Assembly.GetManifestResourceStream(resource)
            ?? throw new InvalidOperationException($"缺少托盘图标资源：{resource}");
        return new Icon(stream);
    }

    /** 任务栏亮暗跟随"Windows 模式"（`SystemUsesLightTheme` = 1 表示浅色）。 */
    private static bool IsLightTaskbar()
    {
        using var key = Registry.CurrentUser.OpenSubKey(
            @"Software\Microsoft\Windows\CurrentVersion\Themes\Personalize");
        return key?.GetValue("SystemUsesLightTheme") is int light && light == 1;
    }
}
