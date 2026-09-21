using System.Runtime.InteropServices;
using LibreHardwareMonitor.Hardware;
using Microsoft.Win32;

namespace WakeUpMyWall.Agent.Metrics.Windows;

/**
 * 真实 PC 上的指标来源：LibreHardwareMonitor 提供 CPU/GPU/内存/存储/主板/网络的传感器，
 * DriveInfo 提供系统盘容量，注册表提供 CPU 名称与显存容量。
 *
 * 这一层只做"搬运"：把 LHM 的传感器变成 MetricsMapper 认识的 SensorReading，映射规则（单位换算、
 * 缺失判定、聚合）全在 MetricsMapper 里，因此那些规则能在 Linux/CI 上单测（见 SensorReading 的注释）。
 */
public sealed class WindowsMetricsProvider : ISystemMetricsProvider, IDisposable
{
    private readonly Computer _computer = new()
    {
        IsCpuEnabled = true,
        IsGpuEnabled = true,
        IsMemoryEnabled = true,
        IsStorageEnabled = true,
        IsMotherboardEnabled = true,
        IsNetworkEnabled = true,
    };

    public WindowsMetricsProvider() => _computer.Open();

    public SystemMetricsPayload Read()
    {
        var now = DateTimeOffset.UtcNow;
        var readings = new List<SensorReading>();
        var gpuTaken = false;

        foreach (var hardware in _computer.Hardware)
        {
            var target = MapHardware(hardware.HardwareType, ref gpuTaken);
            if (target is null) continue;

            Collect(hardware, target, readings);
        }

        return MetricsMapper.Map(readings, ReadFacts(now), now);
    }

    public void Dispose() => _computer.Close();

    /** 硬件（含主板的 SuperIO 子硬件）的传感器搬成读数；子硬件沿用父硬件的归类。 */
    private static void Collect(IHardware hardware, string target, List<SensorReading> readings)
    {
        hardware.Update();
        foreach (var sensor in hardware.Sensors)
        {
            var kind = MapSensor(sensor.SensorType);
            if (kind is null || sensor.Value is null) continue;

            // SmallData 是 MB，其余 Data 是 GB（LHM 的约定，见 SensorReading 的注释）。
            var value = sensor.SensorType == SensorType.SmallData ? sensor.Value.Value / 1024.0 : sensor.Value.Value;
            readings.Add(new SensorReading(target, kind, sensor.Name, value));
        }

        foreach (var sub in hardware.SubHardware) Collect(sub, target, readings);
    }

    /** GPU 只认第一块（多显卡时概念图上的读数也只有一个）；其余硬件按类型归类。 */
    private static string? MapHardware(HardwareType type, ref bool gpuTaken) => type switch
    {
        HardwareType.Cpu => SensorHardware.Cpu,
        HardwareType.GpuNvidia or HardwareType.GpuAmd or HardwareType.GpuIntel => gpuTaken ? null : Take(ref gpuTaken),
        HardwareType.Memory => SensorHardware.Memory,
        HardwareType.Storage => SensorHardware.Storage,
        HardwareType.Motherboard or HardwareType.SuperIO => SensorHardware.Motherboard,
        HardwareType.Network => SensorHardware.Network,
        _ => null,
    };

    private static string? Take(ref bool gpuTaken)
    {
        gpuTaken = true;
        return SensorHardware.Gpu;
    }

    private static string? MapSensor(SensorType type) => type switch
    {
        SensorType.Load => SensorKind.Load,
        SensorType.Temperature => SensorKind.Temperature,
        SensorType.Clock => SensorKind.ClockMhz,
        SensorType.Fan => SensorKind.FanRpm,
        SensorType.Data or SensorType.SmallData => SensorKind.DataGb,
        SensorType.Throughput => SensorKind.ThroughputBps,
        _ => null,
    };

    private MachineFacts ReadFacts(DateTimeOffset now)
    {
        var cpuHardware = _computer.Hardware.FirstOrDefault(h => h.HardwareType == HardwareType.Cpu);
        var gpuHardware = _computer.Hardware.FirstOrDefault(h =>
            h.HardwareType is HardwareType.GpuNvidia or HardwareType.GpuAmd or HardwareType.GpuIntel);
        var storageHardware = _computer.Hardware.FirstOrDefault(h => h.HardwareType == HardwareType.Storage);

        var (storageTotalGb, storageFreeGb, driveRoot) = ReadSystemDrive();

        return new MachineFacts(
            Hostname: Environment.MachineName,
            Os: RuntimeInformation.OSDescription,
            CpuName: RegistryString(@"HARDWARE\DESCRIPTION\System\CentralProcessor\0", "ProcessorNameString")
                     ?? cpuHardware?.Name
                     ?? "CPU",
            CpuShortName: Shorten(RegistryString(@"HARDWARE\DESCRIPTION\System\CentralProcessor\0", "ProcessorNameString")
                                  ?? cpuHardware?.Name
                                  ?? "CPU"),
            GpuName: gpuHardware?.Name ?? "GPU",
            GpuShortName: Shorten(gpuHardware?.Name ?? "GPU"),
            RamModule: RamModuleName(),
            StorageModule: storageHardware?.Name ?? driveRoot,
            VramTotalGb: ReadVramTotalGb(),
            StorageTotalGb: storageTotalGb,
            StorageFreeGb: storageFreeGb,
            UptimeSeconds: Environment.TickCount64 / 1000,
            BootedAtUtc: now.AddSeconds(-Environment.TickCount64 / 1000.0).ToString("o"));
    }

    /** 系统盘就是产品关心的那块：`C:\` 或者系统目录所在盘。 */
    private static (double? TotalGb, double? FreeGb, string Root) ReadSystemDrive()
    {
        var root = Path.GetPathRoot(Environment.SystemDirectory) ?? "C:\\";

        try
        {
            var drive = new DriveInfo(root);
            return (drive.TotalSize / 1024.0 / 1024.0 / 1024.0,
                    drive.AvailableFreeSpace / 1024.0 / 1024.0 / 1024.0,
                    root);
        }
        catch (Exception)
        {
            // 取不到容量不是错误：字段给 null（MetricsMapper 会如实留空）。
            return (null, null, root);
        }
    }

    /** 显存容量只在注册表里（LHM 不暴露总量）。读不到就是 null。 */
    private static double? ReadVramTotalGb()
    {
        const string adapterKey =
            @"SYSTEM\CurrentControlSet\Control\Class\{4d36e968-e325-11ce-bfc1-08002be10318}\0000";

        using var key = Registry.LocalMachine.OpenSubKey(adapterKey);
        if (key?.GetValue("HardwareInformation.qwMemorySize") is long bytes && bytes > 0)
        {
            return bytes / 1024.0 / 1024.0 / 1024.0;
        }

        return null;
    }

    /** 内存型号：LHM 的 DIMM 硬件名就是模块名（SPD），取不到就退回总量。 */
    private string RamModuleName()
    {
        var dimms = _computer.Hardware
            .Where(h => h.HardwareType == HardwareType.Memory)
            .Select(h => h.Name)
            .Where(name => !name.Contains("Total", StringComparison.OrdinalIgnoreCase) &&
                           !name.Contains("Virtual", StringComparison.OrdinalIgnoreCase))
            .Distinct()
            .ToList();

        return dimms.Count > 0 ? string.Join(" + ", dimms) : "RAM";
    }

    private static string? RegistryString(string subKey, string valueName)
    {
        using var key = Registry.LocalMachine.OpenSubKey(subKey);
        return (key?.GetValue(valueName) as string)?.Trim();
    }

    /** 卡片小字只放得下短名：去掉厂商前缀（概念图的 `Ryzen 7 7700X` / `RTX 4070 Ti`）。 */
    private static string Shorten(string name)
    {
        string[] prefixes =
        [
            "AMD ", "Intel ", "Intel(R) ", "NVIDIA GeForce ", "NVIDIA ", "Advanced Micro Devices, Inc. ",
        ];

        var shortened = name;
        foreach (var prefix in prefixes)
        {
            if (shortened.StartsWith(prefix, StringComparison.OrdinalIgnoreCase))
            {
                shortened = shortened[prefix.Length..];
            }
        }

        return shortened.Trim();
    }
}
