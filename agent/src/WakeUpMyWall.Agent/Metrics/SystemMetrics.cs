namespace WakeUpMyWall.Agent.Metrics;

/**
 * 手机端 MetricsSnapshot 的线上契约（spec §8）。属性名就是 JSON 属性名（camelCase），
 * 与 docs/plans/agent-api.md 的表格一一对应；任何读不到的传感器给 null，绝不用 0 冒充。
 */
public sealed record SystemMetricsPayload(
    string CapturedAtUtc,
    IdentityPayload Identity,
    CpuMetricsPayload Cpu,
    GpuMetricsPayload Gpu,
    MemoryMetricsPayload Memory,
    StorageMetricsPayload Storage,
    ThermalMetricsPayload Thermal,
    NetworkMetricsPayload Network,
    long UptimeSeconds,
    string BootedAtUtc);

/** 静态硬件身份（Monitor 身份卡与指标卡的型号小字）。 */
public sealed record IdentityPayload(
    string Hostname,
    string Os,
    string CpuName,
    string CpuShortName,
    string GpuName,
    string GpuShortName,
    string RamModule,
    string StorageModule);

public sealed record CpuMetricsPayload(
    string Name,
    double? UsagePercent,
    double? ClockGhz,
    int? Cores,
    int? Threads,
    double? TempC,
    int? FanRpm);

public sealed record GpuMetricsPayload(
    string Name,
    double? UsagePercent,
    double? TempC,
    double? VramUsedGb,
    double? VramTotalGb,
    int? FanRpm);

public sealed record MemoryMetricsPayload(double? UsagePercent, double? UsedGb, double? TotalGb);

public sealed record StorageMetricsPayload(
    double? UsagePercent, double? UsedTb, double? TotalTb, double? FreeGb, double? TempC);

public sealed record ThermalMetricsPayload(double? MotherboardTempC, int? CaseFanRpm);

public sealed record NetworkMetricsPayload(double? DownloadMbps, double? UploadMbps);

/**
 * 提供者能直接问到的"机器事实"：主机/型号这类静态信息，以及 DriveInfo / 注册表给出的容量。
 * 传感器读数（usage / temp / clock / fan / 吞吐）不在这里，它们在 SensorReading 列表里。
 */
public sealed record MachineFacts(
    string Hostname,
    string Os,
    string CpuName,
    string CpuShortName,
    string GpuName,
    string GpuShortName,
    string RamModule,
    string StorageModule,
    double? VramTotalGb,
    double? StorageTotalGb,
    double? StorageFreeGb,
    /** 处理器标称主频（MHz）：LHM 读不到实时频率时的兜底，来源是注册表 `~MHz`。 */
    double? NominalClockMhz,
    long UptimeSeconds,
    string BootedAtUtc);
