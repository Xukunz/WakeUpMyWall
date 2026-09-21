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
    /** 每块**固定磁盘**一条（系统盘是其中一条）。手机端的 Storage 卡片按这个列表左右翻页。 */
    IReadOnlyList<DiskPayload> Disks,
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

/** 单块磁盘。`mount` 是盘符（`C:\`），`tempC` 只在能确定归属时给（当前只有系统盘）。 */
public sealed record DiskPayload(
    string Name, string Mount, double? UsagePercent, double? UsedGb, double? TotalGb, double? FreeGb, double? TempC);

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
    /** 系统盘盘符（`C:\`）：只有它能确定地对应 LHM 的存储温度传感器。 */
    string SystemDiskMount,
    /** 固定磁盘清单（盘符 / 名称 / 容量），由提供者从 DriveInfo 枚举。 */
    IReadOnlyList<DiskFact> Disks,
    /** 处理器标称主频（MHz）：LHM 读不到实时频率时的兜底，来源是注册表 `~MHz`。 */
    double? NominalClockMhz,
    /** 物理核心数（WMI `Win32_Processor.NumberOfCores`）：LHM 的核心数要 MSR 权限，没有时用这个兜底。 */
    int? PhysicalCores,
    long UptimeSeconds,
    string BootedAtUtc);

/** 提供者能直接问到的磁盘事实（容量来自 DriveInfo）。 */
public sealed record DiskFact(string Name, string Mount, double TotalGb, double FreeGb);
