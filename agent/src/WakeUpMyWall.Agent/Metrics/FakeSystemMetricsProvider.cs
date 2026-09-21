namespace WakeUpMyWall.Agent.Metrics;

/**
 * 开发机 / CI / 演示用的假指标：数值由时间决定（不是随机数），所以可复现、也会随时间动。
 * 真实 PC 上由 WindowsMetricsProvider 取代（`--fake-metrics` 可强制用假的）。
 */
public sealed class FakeSystemMetricsProvider(TimeProvider clock) : ISystemMetricsProvider
{
    private const double TotalRamGb = 32.0;

    private static readonly DateTimeOffset BootedAt = DateTimeOffset.UtcNow.AddDays(-3).AddHours(-6);

    public SystemMetricsPayload Read()
    {
        var now = clock.GetUtcNow();
        var t = now.ToUnixTimeMilliseconds() / 1000.0;

        var memoryLoad = 38 + 4 * Math.Sin(t / 23);
        var memoryUsedGb = TotalRamGb * memoryLoad / 100.0;

        // 合成读数走的是和真实提供者一样的那条路（MetricsMapper），所以单位换算/聚合也被覆盖到。
        var readings = new List<SensorReading>
        {
            new(SensorHardware.Cpu, SensorKind.Load, "CPU Total", 22 + 9 * Math.Sin(t / 7)),
            new(SensorHardware.Cpu, SensorKind.Temperature, "CPU Package", 45 + 6 * Math.Sin(t / 11)),
            new(SensorHardware.Cpu, SensorKind.ClockMhz, "CPU Core #1", 4200 + 300 * Math.Sin(t / 5)),
            new(SensorHardware.Cpu, SensorKind.ClockMhz, "CPU Core #2", 4150 + 280 * Math.Sin(t / 6)),
            new(SensorHardware.Cpu, SensorKind.ClockMhz, "CPU Core #3", 4300 + 250 * Math.Cos(t / 5)),
            new(SensorHardware.Cpu, SensorKind.ClockMhz, "CPU Core #4", 4250 + 260 * Math.Cos(t / 6)),
            new(SensorHardware.Gpu, SensorKind.Load, "GPU Core", 9 + 7 * Math.Sin(t / 13)),
            new(SensorHardware.Gpu, SensorKind.Temperature, "GPU Core", 38 + 5 * Math.Cos(t / 17)),
            new(SensorHardware.Gpu, SensorKind.FanRpm, "GPU Fan", 1200),
            new(SensorHardware.Gpu, SensorKind.DataGb, "D3D Dedicated Memory Used", 2.4),
            new(SensorHardware.Memory, SensorKind.Load, "Memory", memoryLoad),
            new(SensorHardware.Memory, SensorKind.DataGb, "Memory Used", memoryUsedGb),
            new(SensorHardware.Memory, SensorKind.DataGb, "Memory Available", TotalRamGb - memoryUsedGb),
            new(SensorHardware.Motherboard, SensorKind.Temperature, "Temperature #1", 35 + 2 * Math.Sin(t / 29)),
            new(SensorHardware.Motherboard, SensorKind.FanRpm, "CPU Fan", 980),
            new(SensorHardware.Motherboard, SensorKind.FanRpm, "Fan #3", 870),
            new(SensorHardware.Storage, SensorKind.Temperature, "Temperature", 41 + 2 * Math.Sin(t / 31)),
            new(SensorHardware.Network, SensorKind.ThroughputBps, "Download Speed", 1_550_000 + 400_000 * Math.Sin(t / 3)),
            new(SensorHardware.Network, SensorKind.ThroughputBps, "Upload Speed", 387_500 + 120_000 * Math.Cos(t / 4)),
        };

        var facts = new MachineFacts(
            Hostname: Environment.MachineName,
            Os: "Linux (fake metrics)",
            CpuName: "Fake Ryzen 7 7700X 8-Core Processor",
            CpuShortName: "Ryzen 7 7700X",
            GpuName: "Fake GeForce RTX 4070 Ti",
            GpuShortName: "RTX 4070 Ti",
            RamModule: "32 GB DDR5-6000",
            StorageModule: "NVMe 2 TB",
            VramTotalGb: 12,
            StorageTotalGb: 2048,
            StorageFreeGb: 102,
            UptimeSeconds: (long)(now - BootedAt).TotalSeconds,
            BootedAtUtc: BootedAt.ToString("o"));

        return MetricsMapper.Map(readings, facts, now);
    }
}
