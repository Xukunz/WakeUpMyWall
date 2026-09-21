namespace WakeUpMyWall.Agent.Tray;

/**
 * 托盘图标选哪一张。任务栏主题有亮/暗两种：
 * 亮色任务栏要放**暗色**图（浅底上的浅色图会糊成一片），深色任务栏放**亮色**图。
 * 母版是 `imgs/ui/icon_{bright,dark}.png`，由 `tools/prepare_ui_assets.py` 转成 .ico。
 */
public static class TrayIconAssets
{
    public static string ForTaskbar(bool lightTaskbar) => lightTaskbar ? "tray_dark.ico" : "tray_bright.ico";
}
