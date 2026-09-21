namespace WakeUpMyWall.Agent.Metrics;

/**
 * 提供者中立的一条传感器读数。**名字与单位沿用 LibreHardwareMonitor 的约定**（已核对源码）：
 *   Load          → 百分比（0–100）
 *   Temperature   → 摄氏度
 *   ClockMhz      → MHz
 *   FanRpm        → RPM
 *   DataGb        → GB
 *   ThroughputBps → 字节/秒（LHM 的 Network.cs 存的就是 dBytes/dt）
 * 这样映射逻辑只有一份，Windows 适配层与假数据提供者都喂同一种读数，Linux/CI 也能单测。
 */
public sealed record SensorReading(string Hardware, string Sensor, string Name, double Value);

public static class SensorHardware
{
    public const string Cpu = "cpu";
    public const string Gpu = "gpu";
    public const string Memory = "memory";
    public const string Storage = "storage";
    public const string Motherboard = "motherboard";
    public const string Network = "network";
}

public static class SensorKind
{
    public const string Load = "load";
    public const string Temperature = "temperature";
    public const string ClockMhz = "clock";
    public const string FanRpm = "fan";
    public const string DataGb = "data";
    public const string ThroughputBps = "throughput";
}
