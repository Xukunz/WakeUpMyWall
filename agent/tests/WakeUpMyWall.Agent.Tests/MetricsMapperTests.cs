using WakeUpMyWall.Agent.Metrics;

namespace WakeUpMyWall.Agent.Tests;

public class MetricsMapperTests
{
    private static readonly DateTimeOffset Now = new(2026, 9, 20, 21, 4, 0, TimeSpan.Zero);

    private static readonly MachineFacts Facts = new(
        Hostname: "DESKTOP-ALPHA",
        Os: "Windows 11 Pro",
        CpuName: "AMD Ryzen 7 7700X 8-Core Processor",
        CpuShortName: "Ryzen 7 7700X",
        GpuName: "NVIDIA GeForce RTX 4070 Ti",
        GpuShortName: "RTX 4070 Ti",
        RamModule: "32 GB DDR5-6000",
        StorageModule: "NVMe 2 TB",
        VramTotalGb: 12,
        StorageTotalGb: 2048,
        StorageFreeGb: 102,
        NominalClockMhz: null,
        UptimeSeconds: 289440,
        BootedAtUtc: "2025-04-18T12:00:00Z");

    [Fact]
    public void Cpu_memory_and_network_readings_land_in_the_right_fields()
    {
        var payload = MetricsMapper.Map(
            new List<SensorReading>
            {
                new(SensorHardware.Cpu, SensorKind.Load, "CPU Total", 12.5),
                new(SensorHardware.Cpu, SensorKind.Temperature, "CPU Package", 42.0),
                new(SensorHardware.Cpu, SensorKind.ClockMhz, "CPU Core #1", 4350),
                new(SensorHardware.Cpu, SensorKind.ClockMhz, "CPU Core #2", 4250),
                new(SensorHardware.Memory, SensorKind.Load, "Memory", 38.0),
                new(SensorHardware.Memory, SensorKind.DataGb, "Memory Used", 12.2),
                new(SensorHardware.Memory, SensorKind.DataGb, "Memory Available", 19.8),
                new(SensorHardware.Network, SensorKind.ThroughputBps, "Download Speed", 1_550_000),
                new(SensorHardware.Network, SensorKind.ThroughputBps, "Upload Speed", 387_500),
                new(SensorHardware.Motherboard, SensorKind.Temperature, "Temperature #1", 35.0),
                new(SensorHardware.Motherboard, SensorKind.FanRpm, "CPU Fan", 980),
                new(SensorHardware.Motherboard, SensorKind.FanRpm, "Fan #3", 870),
            },
            Facts,
            Now);

        Assert.Equal(12.5, payload.Cpu.UsagePercent);
        Assert.Equal(42.0, payload.Cpu.TempC);
        Assert.Equal(4.3, payload.Cpu.ClockGhz);            // 平均 4300 MHz → 4.3 GHz
        Assert.Equal(2, payload.Cpu.Cores);                 // 由 "CPU Core #n" 计数得出
        Assert.Equal(38.0, payload.Memory.UsagePercent);
        Assert.Equal(12.2, payload.Memory.UsedGb);
        Assert.Equal(32.0, payload.Memory.TotalGb);         // Used + Available
        Assert.Equal(12.4, payload.Network.DownloadMbps);    // 1_550_000 B/s → 12.4 Mbps
        Assert.Equal(3.1, payload.Network.UploadMbps);
        Assert.Equal(35.0, payload.Thermal.MotherboardTempC);
        Assert.Equal(980, payload.Cpu.FanRpm);
        Assert.Equal(870, payload.Thermal.CaseFanRpm);
        Assert.Equal(289440, payload.UptimeSeconds);
        Assert.Equal("DESKTOP-ALPHA", payload.Identity.Hostname);
    }

    [Fact]
    public void Storage_usage_comes_from_the_drive_facts_not_from_sensors()
    {
        var payload = MetricsMapper.Map(new List<SensorReading>(), Facts, Now);

        Assert.Equal(95.0, payload.Storage.UsagePercent);    // (2048-102)/2048
        Assert.Equal(1.9, payload.Storage.UsedTb);
        Assert.Equal(2.0, payload.Storage.TotalTb);
        Assert.Equal(102, payload.Storage.FreeGb);
    }

    [Fact]
    public void Gpu_readings_map_usage_temperature_and_vram()
    {
        var payload = MetricsMapper.Map(
            new List<SensorReading>
            {
                new(SensorHardware.Gpu, SensorKind.Load, "GPU Core", 8.0),
                new(SensorHardware.Gpu, SensorKind.Temperature, "GPU Core", 38.0),
                new(SensorHardware.Gpu, SensorKind.FanRpm, "GPU Fan", 1200),
                new(SensorHardware.Gpu, SensorKind.DataGb, "D3D Dedicated Memory Used", 2.4),
            },
            Facts,
            Now);

        Assert.Equal(8.0, payload.Gpu.UsagePercent);
        Assert.Equal(38.0, payload.Gpu.TempC);
        Assert.Equal(2.4, payload.Gpu.VramUsedGb);
        Assert.Equal(12, payload.Gpu.VramTotalGb);
        Assert.Equal(1200, payload.Gpu.FanRpm);
    }

    [Fact]
    public void Missing_sensors_stay_null_instead_of_zero()
    {
        var payload = MetricsMapper.Map(
            new List<SensorReading>(),
            Facts with { StorageTotalGb = null, StorageFreeGb = null, VramTotalGb = null },
            Now);

        Assert.Null(payload.Cpu.UsagePercent);
        Assert.Null(payload.Cpu.TempC);
        Assert.Null(payload.Cpu.FanRpm);
        Assert.Null(payload.Memory.UsagePercent);
        Assert.Null(payload.Memory.TotalGb);
        Assert.Null(payload.Network.DownloadMbps);
        Assert.Null(payload.Thermal.MotherboardTempC);
        Assert.Null(payload.Storage.UsagePercent);
        Assert.Null(payload.Storage.TempC);
        Assert.Null(payload.Gpu.VramTotalGb);
    }

    [Fact]
    public void The_newest_reading_wins_when_a_sensor_name_repeats()
    {
        var payload = MetricsMapper.Map(
            new List<SensorReading>
            {
                new(SensorHardware.Cpu, SensorKind.Load, "CPU Total", 10.0),
                new(SensorHardware.Cpu, SensorKind.Load, "CPU Total", 55.0),
            },
            Facts,
            Now);

        Assert.Equal(55.0, payload.Cpu.UsagePercent);
    }
}
