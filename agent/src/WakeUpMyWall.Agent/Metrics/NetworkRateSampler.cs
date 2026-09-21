namespace WakeUpMyWall.Agent.Metrics;

/** 网卡累计字节数（来自 .NET 的 IP 统计，不依赖 LibreHardwareMonitor）。 */
public readonly record struct NetworkCounters(long BytesReceived, long BytesSent);

/**
 * 把"累计字节数"变成"实时速率"。为什么不用 LHM 的吞吐传感器：
 * 它在服务/Session 0 或某些驱动环境下会一直是 0；网卡的累计计数是系统自带的，永远有值。
 *
 * 规则：第一次采样只建立基线（速率为 null）；计数器回绕/重置（差值为负）时同样给 null，
 * 绝不把负数或跳变当成真实速率。
 */
public sealed class NetworkRateSampler
{
    private NetworkCounters? previous;
    private long previousMillis;

    public (double? DownloadBytesPerSecond, double? UploadBytesPerSecond) Sample(
        NetworkCounters counters, long nowMillis)
    {
        var last = previous;
        var lastMillis = previousMillis;
        previous = counters;
        previousMillis = nowMillis;

        if (last is not { } baseline) return (null, null);

        var elapsed = (nowMillis - lastMillis) / 1000.0;
        if (elapsed <= 0) return (null, null);

        var received = counters.BytesReceived - baseline.BytesReceived;
        var sent = counters.BytesSent - baseline.BytesSent;
        if (received < 0 || sent < 0) return (null, null);   // 计数器重置

        return (received / elapsed, sent / elapsed);
    }
}
