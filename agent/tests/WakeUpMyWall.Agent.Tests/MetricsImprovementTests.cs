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
        VramTotalGb: 12, StorageTotalGb: 2048, StorageFreeGb: 102, NominalClockMhz: null,
        UptimeSeconds: 100, BootedAtUtc: "2025-04-18T12:00:00Z");
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
