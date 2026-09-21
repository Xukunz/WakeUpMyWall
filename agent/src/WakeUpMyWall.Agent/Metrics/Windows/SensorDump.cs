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
        report.AppendLine($"PawnIO 驱动（LHM 读温度/风扇/盘温要它）：{PawnIoStatus.Describe()}");
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

        // 结论段与真实采样走**同一条**搬运路径（WindowsMetricsProvider 的归类 + 搬运），
        // 用的也是 MetricsMapper 的同一份候选名列表，所以"清单里的结论"就是"手机上会看到什么"。
        var readings = new List<SensorReading>();
        foreach (var hardware in computer.Hardware)
        {
            var target = WindowsMetricsProvider.MapHardware(hardware, primaryGpu: null);
            if (target is not null) WindowsMetricsProvider.Collect(hardware, target, readings);
        }
        AppendVerdicts(report, readings);

        computer.Close();

        var directory = Path.GetDirectoryName(path);
        if (!string.IsNullOrEmpty(directory)) Directory.CreateDirectory(directory);
        File.WriteAllText(path, report.ToString(), Encoding.UTF8);
        return path;
    }

    /**
     * 结论段：直接回答"手机端为什么是 `—`"。三种可能分得清清楚楚——
     * ① 名字没匹配上（这里会写出命中的传感器名）；② 有传感器但读数为空/0；③ 这块硬件上根本没有温度传感器。
     * 只有第 ③ 种和权限/驱动有关：LHM 读 MSR/SuperIO/NVMe SMART 都要它的内核驱动。
     */
    private static void AppendVerdicts(StringBuilder report, List<SensorReading> readings)
    {
        report.AppendLine();
        report.AppendLine("结论（手机端这些读数是 `—` 时看这里）：");
        report.AppendLine("  · " + TemperatureDiagnosis.Describe(
            "CPU 温度", Group(readings, SensorHardware.Cpu), MetricsMapper.CpuTemperatureNames, MetricsMapper.CpuTemperature));
        report.AppendLine("  · " + TemperatureDiagnosis.Describe(
            "SSD 温度", Group(readings, SensorHardware.Storage), MetricsMapper.StorageTemperatureNames, MetricsMapper.StorageTemperature));
        report.AppendLine("  · " + TemperatureDiagnosis.Describe(
            "主板温度", Group(readings, SensorHardware.Motherboard), ["Temperature #1"]));
        report.AppendLine();
        report.AppendLine("  出现“这块硬件上一个温度传感器都没有”时，按这个顺序查（都不是 Agent 代码问题）：");
        report.AppendLine("  1. 最上面的 PawnIO 一行：未安装 → 装上 PawnIO 再重启 Agent（LHM 0.9.5 起全靠它读温度/风扇/盘温）；");
        report.AppendLine("  2. 进程是否以管理员身份运行：否 → 用管理员身份运行，或用安装包把它装成 Windows 服务；");
        report.AppendLine("  3. 两条都正常却还是没有传感器：把这份清单整个发出来。");
    }

    private static List<SensorReading> Group(List<SensorReading> readings, string hardware) =>
        readings.Where(r => r.Hardware == hardware).ToList();

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
