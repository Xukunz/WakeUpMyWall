using WakeUpMyWall.Agent.Metrics;

namespace WakeUpMyWall.Agent.Tests;

public class FakeSystemMetricsProviderTests
{
    private sealed class FixedClock(DateTimeOffset now) : TimeProvider
    {
        public override DateTimeOffset GetUtcNow() => now;
    }

    [Fact]
    public void Fake_readings_are_deterministic_for_a_fixed_clock_and_stay_in_range()
    {
        var provider = new FakeSystemMetricsProvider(
            new FixedClock(new DateTimeOffset(2026, 9, 20, 21, 4, 0, TimeSpan.Zero)));

        var first = provider.Read();
        var second = provider.Read();

        // 同一时刻两次读数一致。注意：载荷里的 Disks 是集合，record 的自动相等只看引用，
        // 所以这里逐段比较（集合用 xunit 的结构比较）。
        Assert.Equal(first.Cpu, second.Cpu);
        Assert.Equal(first.Memory, second.Memory);
        Assert.Equal(first.Network, second.Network);
        Assert.Equal(first.Disks, second.Disks);
        Assert.InRange(first.Cpu.UsagePercent!.Value, 0, 100);
        Assert.InRange(first.Memory.UsagePercent!.Value, 0, 100);
        Assert.InRange(first.Storage.UsagePercent!.Value, 0, 100);
        Assert.NotNull(first.Cpu.TempC);
        Assert.NotNull(first.Network.DownloadMbps);
        Assert.NotNull(first.Gpu.VramTotalGb);
    }

    [Fact]
    public void Fake_readings_move_when_time_moves()
    {
        var atStart = new FakeSystemMetricsProvider(
            new FixedClock(new DateTimeOffset(2026, 9, 20, 21, 4, 0, TimeSpan.Zero)));
        var later = new FakeSystemMetricsProvider(
            new FixedClock(new DateTimeOffset(2026, 9, 20, 21, 4, 30, TimeSpan.Zero)));

        Assert.NotEqual(atStart.Read().Cpu.UsagePercent, later.Read().Cpu.UsagePercent);
    }
}
