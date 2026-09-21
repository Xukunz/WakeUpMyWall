using System.Text;
using LibreHardwareMonitor.Hardware;

namespace WakeUpMyWall.Agent.Metrics.Windows;

/**
 * `--dump-sensors`：把 LibreHardwareMonitor 在这台机器上**实际枚举到的**硬件与传感器
 * 逐条写到 `%ProgramData%\WakeUpMyWall\sensors.txt` 然后退出。
 *
 * 为什么需要它：温度/风扇/频率/网络这些读数依赖硬件与驱动，光看"手机上是 `—`"没法判断
 * 是"驱动没加载"、"这块主板没有该传感器"，还是"我们的名字没匹配上"。
 * 这份清单能一次性回答这三个问题。
 */
public static class SensorDump
{
    public static string Write(string path)
    {
        var report = new StringBuilder();
        report.AppendLine($"WakeUpMyWall Agent 传感器清单  {DateTimeOffset.Now:yyyy-MM-dd HH:mm:ss zzz}");
        report.AppendLine($"进程是否以管理员身份运行：{IsElevated()}");
        report.AppendLine();

        var computer = new Computer
        {
            IsCpuEnabled = true,
            IsGpuEnabled = true,
            IsMemoryEnabled = true,
            IsStorageEnabled = true,
            IsMotherboardEnabled = true,
            IsNetworkEnabled = true,
        };
        computer.Open();

        foreach (var hardware in computer.Hardware)
        {
            Dump(hardware, report, depth: 0);
        }
        computer.Close();

        var directory = Path.GetDirectoryName(path);
        if (!string.IsNullOrEmpty(directory)) Directory.CreateDirectory(directory);
        File.WriteAllText(path, report.ToString(), Encoding.UTF8);
        return path;
    }

    private static void Dump(IHardware hardware, StringBuilder report, int depth)
    {
        var indent = new string(' ', depth * 2);
        hardware.Update();
        report.AppendLine($"{indent}[{hardware.HardwareType}] {hardware.Name}");

        foreach (var sensor in hardware.Sensors)
        {
            report.AppendLine(
                $"{indent}  - {sensor.SensorType} \"{sensor.Name}\" = {sensor.Value?.ToString() ?? "(null)"}");
        }

        foreach (var sub in hardware.SubHardware) Dump(sub, report, depth + 1);
    }

    private static bool IsElevated()
    {
        using var identity = System.Security.Principal.WindowsIdentity.GetCurrent();
        return new System.Security.Principal.WindowsPrincipal(identity)
            .IsInRole(System.Security.Principal.WindowsBuiltInRole.Administrator);
    }
}
