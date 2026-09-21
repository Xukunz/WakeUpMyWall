using WakeUpMyWall.Agent.Actions;
using WakeUpMyWall.Agent.Metrics;
using Microsoft.Extensions.Logging.Abstractions;

namespace WakeUpMyWall.Agent.Tests;

/** 真机上报过的三类指标问题：挑错显卡、频率读不到、网速恒为 0。规则做成纯函数就是为了在这里盯住。 */
public class MetricsImprovementTests
{
    [Fact]
    public void The_discrete_gpu_wins_over_the_integrated_one()
    {
        // 带核显的机器：LHM 常常先把 Intel 核显枚举出来（用户实测就是这么错的）。
        var candidates = new List<(string Vendor, string Name)>
        {
            ("intel", "Intel UHD Graphics 770"),
            ("nvidia", "NVIDIA GeForce RTX 4070 Ti"),
        };

        Assert.Equal(1, GpuSelection.PickPrimary(candidates));
    }

    [Fact]
    public void Same_vendor_prefers_the_card_that_is_not_integrated()
    {
        var candidates = new List<(string Vendor, string Name)>
        {
            ("amd", "AMD Radeon Graphics"),            // APU 核显
            ("amd", "AMD Radeon RX 7900 XT"),          // 独显
        };

        Assert.Equal(1, GpuSelection.PickPrimary(candidates));
    }

    [Fact]
    public void A_single_gpu_is_always_picked()
    {
        Assert.Equal(0, GpuSelection.PickPrimary([("intel", "Intel Iris Xe Graphics")]));
        Assert.Equal(-1, GpuSelection.PickPrimary([]));
    }

    [Fact]
    public void Zero_valued_clock_sensors_fall_back_to_the_nominal_frequency()
    {
        // 没有管理员权限时 LHM 拿不到 MSR：时钟传感器存在但值为 0。
        var payload = MetricsMapper.Map(
            [new SensorReading(SensorHardware.Cpu, SensorKind.ClockMhz, "CPU Core #1", 0)],
            Facts with { NominalClockMhz = 4200 },
            DateTimeOffset.UnixEpoch);

        Assert.Equal(4.2, payload.Cpu.ClockGhz);
    }

    [Fact]
    public void Without_clock_sensors_the_core_count_comes_from_the_provider()
    {
        // 非管理员时 LHM 连时钟传感器都没有：核心数改由 WMI 提供（否则卡片只剩 `— cores`）。
        var payload = MetricsMapper.Map(
            [],
            Facts with { PhysicalCores = 8 },
            DateTimeOffset.UnixEpoch);

        Assert.Equal(8, payload.Cpu.Cores);
        Assert.Equal(Environment.ProcessorCount, payload.Cpu.Threads);
    }

    [Fact]
    public void Zero_valued_temperatures_and_fans_are_reported_as_unknown()
    {
        var payload = MetricsMapper.Map(
            [
                new SensorReading(SensorHardware.Cpu, SensorKind.Temperature, "CPU Package", 0),
                new SensorReading(SensorHardware.Motherboard, SensorKind.FanRpm, "CPU Fan", 0),
            ],
            Facts,
            DateTimeOffset.UnixEpoch);

        Assert.Null(payload.Cpu.TempC);      // 不是 0 ℃
        Assert.Null(payload.Cpu.FanRpm);     // 不是 0 RPM
    }

    [Fact]
    public void Amd_wraps_the_cpu_temperature_under_its_own_sensor_name()
    {
        // LHM 在 AMD 上把封装温度叫 `Core (Tctl/Tdie)`（`CPU Package` 是 Intel 的名字）——
        // 这两个字符串都能在 LibreHardwareMonitorLib 0.9.6 的字符串表里查到（见 SensorDump 的输出）。
        // 之前只认 Intel 的名字，所以 Ryzen 上的 CPU 温度永远是 `—`（用户实测：9800X3D）。
        var zen = MetricsMapper.Map(
            [new SensorReading(SensorHardware.Cpu, SensorKind.Temperature, "Core (Tctl/Tdie)", 55.4)],
            Facts,
            DateTimeOffset.UnixEpoch);
        Assert.Equal(55.4, zen.Cpu.TempC);

        // 早期 Zen 只有 Tctl 或只有 Tdie，同样要接住。
        var earlyZen = MetricsMapper.Map(
            [new SensorReading(SensorHardware.Cpu, SensorKind.Temperature, "Core (Tdie)", 61.0)],
            Facts,
            DateTimeOffset.UnixEpoch);
        Assert.Equal(61.0, earlyZen.Cpu.TempC);
    }

    [Fact]
    public void Without_a_package_sensor_the_hottest_core_is_the_cpu_temperature()
    {
        var payload = MetricsMapper.Map(
            [
                new SensorReading(SensorHardware.Cpu, SensorKind.Temperature, "CPU Core #1", 48.0),
                new SensorReading(SensorHardware.Cpu, SensorKind.Temperature, "CPU Core #2", 63.5),
            ],
            Facts,
            DateTimeOffset.UnixEpoch);

        Assert.Equal(63.5, payload.Cpu.TempC);
    }

    [Fact]
    public void A_distance_to_tjmax_reading_is_not_a_cpu_temperature()
    {
        // `Distance to TjMax` 数值看着像温度，含义正相反（离 TjMax 还有多少度）——
        // 它不能拿来当 CPU 温度，所以兜底取最大值时要把它排除掉。
        var payload = MetricsMapper.Map(
            [new SensorReading(SensorHardware.Cpu, SensorKind.Temperature, "Distance to TjMax", 31.0)],
            Facts,
            DateTimeOffset.UnixEpoch);

        Assert.Null(payload.Cpu.TempC);
    }

    [Fact]
    public void Nvme_composite_temperature_is_the_storage_temperature()
    {
        // 存储温度同样是两套名字：SATA 是 SMART 属性 194 的 `Temperature`，
        // NVMe 是 SMART/Health 里的 `Composite Temperature`（LHM 0.9.6 的盘温度传感器名）。
        // 用户那块 Samsung SSD 990 EVO Plus 是 NVMe，所以之前只认 `Temperature` 的查法一直是空。
        var payload = MetricsMapper.Map(
            [new SensorReading(SensorHardware.Storage, SensorKind.Temperature, "Composite Temperature", 41.0)],
            Facts,
            DateTimeOffset.UnixEpoch);

        Assert.Equal(41.0, payload.Storage.TempC);
        Assert.Equal(41.0, payload.Disks.Single().TempC);   // 系统盘那一页也要带上温度
    }

    [Fact]
    public void Numbered_storage_temperatures_fall_back_to_the_lowest_sensor()
    {
        // 多传感器的盘（990 EVO Plus 报 composite + sensor 1/2）LHM 会起名 `Temperature #2`、`#3`……
        var payload = MetricsMapper.Map(
            [
                new SensorReading(SensorHardware.Storage, SensorKind.Temperature, "Temperature #2", 44.0),
                new SensorReading(SensorHardware.Storage, SensorKind.Temperature, "Temperature #3", 52.0),
            ],
            Facts,
            DateTimeOffset.UnixEpoch);

        Assert.Equal(44.0, payload.Storage.TempC);
    }

    [Fact]
    public void Network_rates_come_from_counter_deltas_and_never_from_resets()
    {
        var sampler = new NetworkRateSampler();

        Assert.Equal((null, null), sampler.Sample(new NetworkCounters(1_000, 500), 10_000));   // 建立基线
        var (down, up) = sampler.Sample(new NetworkCounters(1_000_000 + 1_000, 500 + 1_550_000), 11_000);

        // 采样器给的是**字节/秒**（与 LHM 的吞吐传感器同单位），乘 8 换成 Mbps 由 MetricsMapper 负责。
        Assert.Equal(1_000_000.0, down!.Value, 3);
        Assert.Equal(1_550_000.0, up!.Value, 3);
        // 计数器重置（网卡重启）：给 null，而不是负数。
        Assert.Equal((null, null), sampler.Sample(new NetworkCounters(10, 5), 12_000));
    }

    private static readonly MachineFacts Facts = new(
        Hostname: "DESKTOP-ALPHA", Os: "Windows 11 Pro",
        CpuName: "AMD Ryzen 7 7700X", CpuShortName: "Ryzen 7 7700X",
        GpuName: "NVIDIA GeForce RTX 4070 Ti", GpuShortName: "RTX 4070 Ti",
        RamModule: "32 GB", StorageModule: "NVMe 2 TB",
        VramTotalGb: 12, StorageTotalGb: 2048, StorageFreeGb: 102, SystemDiskMount: "C:\\", Disks: [new DiskFact("NVMe 2 TB", "C:\\", 2048, 102)], NominalClockMhz: null, PhysicalCores: null,
        UptimeSeconds: 100, BootedAtUtc: "2025-04-18T12:00:00Z");
}

/**
 * `--dump-sensors` 的结论段：真机上三种"温度是 `—`"的原因看起来一模一样，
 * 但处理办法完全不同，所以文案必须分得清（这个函数就是那句结论）。
 */
public class TemperatureDiagnosisTests
{
    [Fact]
    public void A_matched_sensor_is_named_in_the_conclusion()
    {
        var text = TemperatureDiagnosis.Describe(
            "CPU 温度",
            [new SensorReading(SensorHardware.Cpu, SensorKind.Temperature, "Core (Tctl/Tdie)", 55.4)],
            MetricsMapper.CpuTemperatureNames,
            MetricsMapper.CpuTemperature);

        Assert.Equal("CPU 温度：55.4 ℃（传感器名 Core (Tctl/Tdie)）", text);
    }

    [Fact]
    public void A_fallback_reading_says_the_candidate_names_did_not_match()
    {
        var text = TemperatureDiagnosis.Describe(
            "CPU 温度",
            [
                new SensorReading(SensorHardware.Cpu, SensorKind.Temperature, "Some Vendor Sensor", 48.0),
                new SensorReading(SensorHardware.Cpu, SensorKind.Temperature, "Another Sensor", 63.5),
            ],
            MetricsMapper.CpuTemperatureNames,
            MetricsMapper.CpuTemperature);

        Assert.Equal("CPU 温度：63.5 ℃（候选名都没命中，兜底取了最高的温度传感器）", text);
    }

    [Fact]
    public void No_sensor_at_all_points_at_the_driver_rather_than_a_name_mismatch()
    {
        // 这块硬件上连温度传感器都没有 —— 只有这一种情况与权限/驱动有关。
        var text = TemperatureDiagnosis.Describe(
            "SSD 温度",
            [new SensorReading(SensorHardware.Storage, SensorKind.Load, "Used Space", 70.0)],
            MetricsMapper.StorageTemperatureNames,
            MetricsMapper.StorageTemperature);

        Assert.Equal("SSD 温度：这块硬件上一个温度传感器都没有", text);
    }

    [Fact]
    public void Sensors_that_read_nothing_are_listed_by_name()
    {
        var text = TemperatureDiagnosis.Describe(
            "SSD 温度",
            [
                new SensorReading(SensorHardware.Storage, SensorKind.Temperature, "Temperature", 0),
                new SensorReading(SensorHardware.Storage, SensorKind.Temperature, "Composite Temperature", 0),
            ],
            MetricsMapper.StorageTemperatureNames,
            MetricsMapper.StorageTemperature);

        Assert.Equal("SSD 温度：有温度传感器但读数全空或为 0（Temperature, Composite Temperature）", text);
    }
}

/** Quick Actions 之前只是"记录 id"，手机点了没反应；现在必须真的调用启动器。 */
public class ActionRegistryTests
{
    private sealed class RecordingLauncher : IAppLauncher
    {
        public List<string> Launched { get; } = [];
        public bool Result { get; set; } = true;
        public bool TryLaunch(string target)
        {
            Launched.Add(target);
            return Result;
        }
    }

    [Fact]
    public void A_known_action_launches_its_target()
    {
        var launcher = new RecordingLauncher();
        var registry = new ActionRegistry(launcher, NullLogger<ActionRegistry>.Instance);

        var result = registry.Execute("steam");

        Assert.True(result is { Known: true, Executed: true });
        Assert.Equal(["steam://open/main"], launcher.Launched);
    }

    [Fact]
    public void An_unknown_action_launches_nothing()
    {
        var launcher = new RecordingLauncher();
        var registry = new ActionRegistry(launcher, NullLogger<ActionRegistry>.Instance);

        var result = registry.Execute("explode");

        Assert.True(result is { Known: false, Executed: false });
        Assert.Empty(launcher.Launched);
    }

    [Fact]
    public void A_failed_launch_is_reported_with_the_reason()
    {
        var launcher = new RecordingLauncher { Result = false };
        var registry = new ActionRegistry(launcher, NullLogger<ActionRegistry>.Instance);

        var result = registry.Execute("spotify");

        Assert.True(result is { Known: true, Executed: false });
        Assert.Contains("spotify:", result.Error);
    }
}
