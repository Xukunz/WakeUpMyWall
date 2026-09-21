namespace WakeUpMyWall.Agent.Metrics.Windows;

/**
 * LHM 的驱动依赖：**0.9.5 起 LibreHardwareMonitor 不再用 WinRing0，改成用 PawnIO 这个内核驱动**
 * 读 MSR / SuperIO / NVMe SMART（它自带的 `LibreHardwareMonitor.Resources.PawnIo.*.bin` 模块要装进 PawnIO）。
 *
 * 为什么单独拎出来：没装 PawnIO 时——
 *  · CPU 温度（Ryzen 的 Tctl/Tdie）与 CPU 频率读不到（LHM 的时钟传感器值是 0）；
 *  · 主板温度、机箱风扇读不到；
 *  · SSD 温度（NVMe SMART）读不到；
 * 而 GPU 温度（走 NVAPI/NVML）、内存、磁盘容量**照常**，所以看起来像"只有温度坏了"。
 * 用户报的"CPU 温度和 SSD 温度读不出来"正是这一组症状——这是环境前提，不是 Agent 代码能绕过的。
 */
public static class PawnIoStatus
{
    /** 明确"没装"才返回 true；检测本身失败（LHM 换了接口）不算未安装，免得误报。 */
    public static bool IsMissing()
    {
        try
        {
            return !LibreHardwareMonitor.PawnIo.PawnIo.IsInstalled;
        }
        catch (Exception)
        {
            return false;
        }
    }

    /** 给 `--dump-sensors` 用的单行状态。 */
    public static string Describe()
    {
        try
        {
            if (!LibreHardwareMonitor.PawnIo.PawnIo.IsInstalled)
            {
                return "未安装 —— 温度/风扇/盘温都会是 `—`，装上 PawnIO 再重启 Agent";
            }

            var version = LibreHardwareMonitor.PawnIo.PawnIo.Version;
            return version is null ? "已安装（版本未知）" : $"已安装（{version}）";
        }
        catch (Exception exception)
        {
            return $"检测失败（{exception.GetType().Name}）—— 以传感器清单与日志为准";
        }
    }

    /** 可读的一句话提醒；没发现问题时返回 null。 */
    public static string? Warning => IsMissing()
        ? "PawnIO 未安装：CPU 温度与频率、主板温度、SSD 温度都读不到（GPU 温度不受影响）"
        : null;
}
