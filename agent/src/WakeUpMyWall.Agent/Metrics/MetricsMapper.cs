namespace WakeUpMyWall.Agent.Metrics;

/**
 * 读数 → 载荷的**纯函数**。规则：
 *  - 同名传感器出现多次时取**最后一条**（LHM 的枚举顺序里靠后的更接近当前值）；
 *  - 缺失一律 null，不补 0；
 *  - 网络吞吐按 8 bit/byte 换算成 Mbps（1e6 进制，与 UI 的 Mbps 一致）；
 *  - CPU 核心数由 `CPU Core #n` 的时钟传感器计数得出；频率取它们的平均值（GHz，1 位小数）；
 *  - CPU 风扇取主板/CPU 硬件里名字含 "CPU" 的风扇；机箱风扇取主板硬件里其余风扇的最大值；
 *  - 存储用量来自 MachineFacts 的系统盘容量（DriveInfo），温度来自存储硬件的温度传感器。
 *
 * 温度传感器的**名字因厂商/接口而异**（LHM 自己起的名字），所以按候选名列表找，而不是只认一个名字：
 * Intel 的 CPU 封装温度叫 `CPU Package`，AMD Zen 叫 `Core (Tctl/Tdie)`；SATA 盘的温度叫 `Temperature`，
 * NVMe 盘叫 `Composite Temperature`。这些字符串都能在 LibreHardwareMonitorLib 0.9.6 的字符串表里查到，
 * `--dump-sensors` 的"结论"段用的也是同一份列表。
 */
public static class MetricsMapper
{
    /** CPU 温度传感器的候选名，按优先级排列（越靠前越接近"整颗 CPU 的温度"）。 */
    public static readonly string[] CpuTemperatureNames =
    [
        "CPU Package",              // Intel：封装温度
        "Core (Tctl/Tdie)",         // AMD Zen：Tctl/Tdie，最接近封装温度
        "Core (Tctl)",              // 早期 Zen
        "Core (Tdie)",              // 只有 Tdie 的机型
        "CCDs Max (Tdie)",          // 多 CCD 的 Zen
        "CCDs Average (Tdie)",
        "Core Max",                 // 少数机型给的是"核心最大值 / 平均值"
        "Core Average",
    ];

    /**
     * 存储温度传感器的候选名。SATA 盘的温度来自 SMART 属性 194（`Temperature`），
     * NVMe 盘来自 SMART/Health 的 `Composite Temperature`——用户那块 Samsung SSD 990 EVO Plus 是 NVMe，
     * 只认 `Temperature` 的查法在它身上永远是空（用户实测："SSD 温度读不出来"）。
     */
    public static readonly string[] StorageTemperatureNames = ["Temperature", "Composite Temperature"];

    public static SystemMetricsPayload Map(
        IReadOnlyList<SensorReading> readings,
        MachineFacts facts,
        DateTimeOffset now)
    {
        var cpu = Hardware(readings, SensorHardware.Cpu);
        var gpu = Hardware(readings, SensorHardware.Gpu);
        var memory = Hardware(readings, SensorHardware.Memory);
        var storage = Hardware(readings, SensorHardware.Storage);
        var motherboard = Hardware(readings, SensorHardware.Motherboard);
        var network = Hardware(readings, SensorHardware.Network);

        // 值为 0 的测量类传感器一律当"读不到"：没有 0 ℃ 的 CPU、也没有 0 MHz 的主频。
        // 这在没有管理员权限（LHM 装不了内核驱动）的机器上是常态——此时退回注册表的标称主频，
        // 而不是给用户看一个假的 0 GHz。
        var coreClocks = cpu
            .Where(r => r.Sensor == SensorKind.ClockMhz
                        && r.Value > 0
                        && r.Name.StartsWith("CPU Core #", StringComparison.Ordinal))
            .ToList();

        var memoryUsedGb = Value(memory, SensorKind.DataGb, "Memory Used");
        var memoryFreeGb = Value(memory, SensorKind.DataGb, "Memory Available");

        var storageTotalGb = facts.StorageTotalGb;
        var storageFreeGb = facts.StorageFreeGb;
        var storageUsedGb = storageTotalGb is not null && storageFreeGb is not null
            ? storageTotalGb - storageFreeGb
            : null;

        return new SystemMetricsPayload(
            CapturedAtUtc: now.ToString("o"),
            Identity: new IdentityPayload(
                facts.Hostname,
                facts.Os,
                facts.CpuName,
                facts.CpuShortName,
                facts.GpuName,
                facts.GpuShortName,
                facts.RamModule,
                facts.StorageModule),
            Cpu: new CpuMetricsPayload(
                Name: facts.CpuName,
                UsagePercent: Round(Value(cpu, SensorKind.Load, "CPU Total"), 1),
                ClockGhz: coreClocks.Count > 0
                    ? Round(coreClocks.Average(r => r.Value) / 1000.0, 1)
                    : Round(facts.NominalClockMhz / 1000.0, 1),
                // 优先用传感器数出来的核心数（有 MSR 权限时最准）；否则用提供者给的物理核心数（WMI）。
                Cores: coreClocks.Count == 0
                    ? facts.PhysicalCores
                    : coreClocks.Select(r => r.Name).Distinct().Count(),
                Threads: Environment.ProcessorCount,
                TempC: Round(CpuTemperature(cpu), 1),
                FanRpm: Rpm(Positive(Last(CpuFans(motherboard, cpu))))),
            Gpu: new GpuMetricsPayload(
                Name: facts.GpuName,
                UsagePercent: Round(Value(gpu, SensorKind.Load, "GPU Core"), 1),
                TempC: Round(Positive(Value(gpu, SensorKind.Temperature, "GPU Core")), 1),
                VramUsedGb: Round(
                    Value(gpu, SensorKind.DataGb, "D3D Dedicated Memory Used")
                    ?? Value(gpu, SensorKind.DataGb, "GPU Memory Used"),
                    1),
                VramTotalGb: facts.VramTotalGb,
                FanRpm: Rpm(Value(gpu, SensorKind.FanRpm, "GPU Fan"))),
            Memory: new MemoryMetricsPayload(
                UsagePercent: Round(Value(memory, SensorKind.Load, "Memory"), 1),
                UsedGb: Round(memoryUsedGb, 1),
                TotalGb: Round(memoryUsedGb + memoryFreeGb, 1)),
            Storage: new StorageMetricsPayload(
                UsagePercent: Round(Percent(storageUsedGb, storageTotalGb), 1),
                UsedTb: Round(storageUsedGb / 1024.0, 1),
                TotalTb: Round(storageTotalGb / 1024.0, 1),
                FreeGb: Round(storageFreeGb, 1),
                TempC: Round(StorageTemperature(storage), 1)),
            Disks: facts.Disks.Select(disk => ToDisk(disk, facts, storage)).ToList(),
            Thermal: new ThermalMetricsPayload(
                MotherboardTempC: Round(Positive(Value(motherboard, SensorKind.Temperature, "Temperature #1")), 1),
                CaseFanRpm: Rpm(Positive(CaseFans(motherboard)))),
            Network: new NetworkMetricsPayload(
                DownloadMbps: Round(ToMbps(Value(network, SensorKind.ThroughputBps, "Download Speed")), 1),
                UploadMbps: Round(ToMbps(Value(network, SensorKind.ThroughputBps, "Upload Speed")), 1)),
            UptimeSeconds: facts.UptimeSeconds,
            BootedAtUtc: facts.BootedAtUtc);
    }

    private static List<SensorReading> Hardware(IReadOnlyList<SensorReading> readings, string hardware) =>
        readings.Where(r => r.Hardware == hardware).ToList();

    /** 同名传感器取最后一条（LHM 枚举里靠后的更接近当前时刻）。 */
    private static double? Value(IReadOnlyList<SensorReading> readings, string sensor, string name) =>
        readings.Where(r => r.Sensor == sensor && r.Name == name).Select(r => (double?)r.Value).LastOrDefault();

    /**
     * CPU 温度：按候选名依次找；一个都没匹配上时，取这块 CPU 上最高的温度读数——
     * 逐核温度（`CPU Core #n` / `Core #n`）也在其中，最热的那颗核就是用户想看的 CPU 温度。
     * `Distance to TjMax` 必须排除：它数值看着像温度，含义正相反（离 TjMax 还有多少度）。
     */
    public static double? CpuTemperature(IReadOnlyList<SensorReading> cpu)
    {
        foreach (var name in CpuTemperatureNames)
        {
            if (Positive(Value(cpu, SensorKind.Temperature, name)) is { } value) return value;
        }

        var candidates = cpu
            .Where(r => r.Sensor == SensorKind.Temperature
                        && !r.Name.Contains("Distance", StringComparison.OrdinalIgnoreCase))
            .Select(r => r.Value)
            .Where(value => value > 0)
            .ToList();
        return candidates.Count == 0 ? null : candidates.Max();
    }

    /**
     * 存储温度：SATA（`Temperature`）与 NVMe（`Composite Temperature`）两套名字都要认；
     * 再兜底到多传感器盘的编号传感器（`Temperature #2`、`#3`……），编号最小的那个最接近整盘温度。
     */
    public static double? StorageTemperature(IReadOnlyList<SensorReading> storage)
    {
        foreach (var name in StorageTemperatureNames)
        {
            if (Positive(Value(storage, SensorKind.Temperature, name)) is { } value) return value;
        }

        return storage
            .Where(r => r.Sensor == SensorKind.Temperature
                        && r.Name.StartsWith("Temperature #", StringComparison.Ordinal))
            .OrderBy(r => SensorIndex(r.Name))
            .Select(r => (double?)r.Value)
            .FirstOrDefault(value => value is > 0);
    }

    /** `Temperature #3` → 3；解析不出编号就排到最后。 */
    private static int SensorIndex(string name) =>
        int.TryParse(name.AsSpan(name.LastIndexOf('#') + 1), out var index) ? index : int.MaxValue;

    private static IEnumerable<double> CpuFans(List<SensorReading> motherboard, List<SensorReading> cpu) =>
        motherboard
            .Concat(cpu)
            .Where(r => r.Sensor == SensorKind.FanRpm && r.Name.Contains("CPU", StringComparison.OrdinalIgnoreCase))
            .Select(r => r.Value);

    private static double? CaseFans(List<SensorReading> motherboard)
    {
        var values = motherboard
            .Where(r => r.Sensor == SensorKind.FanRpm && !r.Name.Contains("CPU", StringComparison.OrdinalIgnoreCase))
            .Select(r => (double?)r.Value)
            .ToList();
        return values.Count == 0 ? null : values.Max();
    }

    private static double? Last(IEnumerable<double> values)
    {
        var list = values.ToList();
        return list.Count == 0 ? null : list[^1];
    }

    private static double? ToMbps(double? bytesPerSecond) => bytesPerSecond * 8.0 / 1_000_000.0;

    private static double? Percent(double? used, double? total) =>
        used is null || total is null || total == 0 ? null : used / total * 100.0;

    private static double? Round(double? value, int digits) =>
        value is null ? null : Math.Round(value.Value, digits);

    private static int? Rpm(double? value) => value is null ? null : (int)Math.Round(value.Value);

    /** 单块磁盘：容量来自 DriveInfo；温度只有系统盘能确定归属（多盘与 LHM 存储硬件的对应关系不可靠）。 */
    private static DiskPayload ToDisk(DiskFact disk, MachineFacts facts, List<SensorReading> storage)
    {
        var usedGb = disk.TotalGb - disk.FreeGb;
        var isSystemDisk = string.Equals(disk.Mount, facts.SystemDiskMount, StringComparison.OrdinalIgnoreCase);
        return new DiskPayload(
            Name: disk.Name,
            Mount: disk.Mount,
            UsagePercent: Round(Percent(usedGb, disk.TotalGb), 1),
            UsedGb: Round(usedGb, 1),
            TotalGb: Round(disk.TotalGb, 1),
            FreeGb: Round(disk.FreeGb, 1),
            TempC: isSystemDisk ? Round(StorageTemperature(storage), 1) : null);
    }

    /** 0 值的温度/转速等于"读不到"（没有 0 ℃ 的 CPU）；用 0 冒充会被当成真实读数。 */
    private static double? Positive(double? value) => value is > 0 ? value : null;
}
