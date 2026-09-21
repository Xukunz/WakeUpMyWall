# Phase 5A：Agent 指标端点（`GET /api/v1/system`）实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 roadmap Phase 5 的服务端那一半做完：Agent 真的能给出 spec §8 的指标（CPU/GPU/RAM/存储/网络/温度/风扇/Uptime），手机端 Phase 5B 才有一个可信的数据源。Phase 4A 现在对 `/api/v1/system` 如实回 **501**，这一步把它换成真实载荷。

**Architecture:** 新增 `Metrics/` 一层：`SystemMetricsPayload`（线上契约）→ `MetricsMapper`（**纯函数**：把提供者中立的 `SensorReading` 列表 + `MachineFacts` 映射成载荷）→ `ISystemMetricsProvider`（Windows 侧用 LibreHardwareMonitor 适配成读数，其它平台用 `FakeSystemMetricsProvider` 合成读数）。映射逻辑只有一份，因此**在 Linux/CI 上就能单测**；Windows 专属的部分只剩"把 LHM 的传感器搬成读数"这一层薄适配。

**Tech Stack:** .NET 10 Minimal API、xunit、LibreHardwareMonitorLib 0.9.6（仅 `net10.0-windows`）、`System.IO.DriveInfo`、`Microsoft.Win32.Registry`（仅 Windows TFM）。

**Spec:** [spec §8 Agent 指标范围](../specs/2026-09-19-desktop-companion-design.md)、§9（ACTIVE 1s / IDLE 5s 刷新节奏）
**前置：** [Phase 4A](2026-09-20-phase4a-pc-agent.md)（Agent 骨架、Bearer 鉴权、`/api/v1/status` 与 501 占位）、[docs/plans/agent-api.md](../../plans/agent-api.md)（端点契约）
**后续：** Phase 5B（手机端 2 秒轮询 + Monitor 实时化）、Phase 5C（`/ws/v1/metrics` 流式通道）

---

## 0. 计划期定案

| 决策 | 选定 | 理由与代价 |
| --- | --- | --- |
| Phase 5 怎么切 | **5A = Agent 指标端点（本计划）**，5B = 手机端轮询 + Monitor 实时化，5C = WebSocket 流 | roadmap 的 Phase 5 一次说完三件事，但一次做完既无法单独验收、也无法单独回滚。与 Phase 4A/4B 的切法一致（服务端 / 手机端各一份计划）。代价：roadmap 里 Phase 5 要标注为"拆成三份计划" |
| 传输方式 | 5A 只做 `GET /api/v1/system`（HTTP），**不做** WebSocket | roadmap 的顺序就是"先 HTTP 轮询跑通全部指标，再切 `/ws/v1/metrics`"。代价：5C 之前刷新率受 HTTP 限制（2 秒一次足够） |
| 鉴权 | `/api/v1/system` **要 Bearer**（与 501 占位时挂的受保护组一致） | 载荷里有主机名/硬件型号等机器信息，免费开放没有理由。代价：手机端必须先配对（Phase 4B 已完成） |
| 读数不到的传感器 | 该字段给 **`null`**，绝不用 0 冒充 | 0 与"不知道"在 UI 上是两种结论（spec §8 明确温度/风扇依赖硬件监控库，取不到是常态）。代价：手机端要处理可空字段 |
| Windows 目标框架 | Agent 工程改成 **`net10.0;net10.0-windows`** 双目标；LibreHardwareMonitorLib 只挂在 `net10.0-windows` 上 | LHM 只带 `runtimes/win-*/` 资产（已实测 0.9.6 包内有 `ref/net10.0` 但运行时只有 win-*），单目标 `net10.0` 引用它在 Linux 上装不上/跑不了。代价：Windows 发布必须带 `-f net10.0-windows`，文档要写清 |
| 映射逻辑放哪 | 放**纯函数** `MetricsMapper` + 提供者中立的 `SensorReading`；LHM 只负责"搬读数" | 映射（含单位换算、缺失判定、聚合）全部能在 Linux/CI 上单测，Windows 侧只剩机械适配。代价：多一层间接，读数结构要跟着 LHM 的语义走（注释里写死） |
| LHM 传感器语义 | 已核对的真实约定：`Throughput` = **字节/秒**（`Network.cs` 的 `dBytesUploaded / dt`）、`Load` = %、`Temperature` = °C、`Clock` = MHz、`Fan` = RPM、`Data` = GB；内存是 `Memory`(Load) / `Memory Used` / `Memory Available`；存储是 `Total Space`(Data,GB) / `Used Space`(Load)；网络是 `Download Speed` / `Upload Speed` | 单位靠猜就会整页数字错一个量级。代价：LHM 改语义时要么跟着改，要么按注释回溯 |
| 存储用量从哪来 | 系统盘用 `DriveInfo`（精确、跨平台），温度从 LHM 取 | `DriveInfo` 的总量/剩余是权威值，LHM 的存储传感器命名在不同版本里改过。代价：只报系统盘，多盘要等后续 |
| 本机（Linux）怎么验收 | `--fake-metrics`（非 Windows 自动生效）合成读数，走真实 HTTP + Bearer 取一遍 | 与 `--fake-power` 同一思路：契约与服务链路能在 Linux 上端到端验；真机读数由用户在 Windows 上验（与 Phase 3 WOL、Phase 4A 电源同一约定）。代价：Windows 读数本身在本环境无法验证，必须显式标记 |

## Global Constraints

- 本计划只动 `agent/` 与文档。不新增除 LibreHardwareMonitorLib 0.9.6 之外的依赖。
- `/api/v1/system` 必须挂在 **Bearer 保护组**里：无 Token 一律 401，且不产生副作用。
- 所有传感器字段可空；取不到就是 `null`（不写 0、不写假字符串）。
- JSON 属性名沿用 Minimal API 的 `JsonSerializerDefaults.Web`（camelCase），字段名一旦定下就是**手机端的契约**，改动要同步 `docs/plans/agent-api.md`。
- `TreatWarningsAsErrors` 是开的：不能留未使用的字段/变量、不能有 nullable 警告。
- 每个 Task 结束前 `dotnet test agent/WakeUpMyWall.Agent.slnx` 全绿（14 条既有测试不许退化）。

---

## 1. 文件结构

| 文件 | 职责 |
| --- | --- |
| `agent/src/WakeUpMyWall.Agent/Metrics/SystemMetrics.cs`（新） | 线上契约：`SystemMetricsPayload` 与各分节 record（+ `MachineFacts`） |
| `agent/src/WakeUpMyWall.Agent/Metrics/SensorReading.cs`（新） | 提供者中立的读数 record + `SensorHardware` / `SensorKind` 常量 |
| `agent/src/WakeUpMyWall.Agent/Metrics/MetricsMapper.cs`（新） | 纯函数：读数 + 机器事实 → 载荷（单位换算、缺失、聚合） |
| `agent/src/WakeUpMyWall.Agent/Metrics/ISystemMetricsProvider.cs`（新） | `SystemMetricsPayload Read()` |
| `agent/src/WakeUpMyWall.Agent/Metrics/FakeSystemMetricsProvider.cs`（新） | 用时间合成读数（Linux/CI/演示），走同一条映射路径 |
| `agent/src/WakeUpMyWall.Agent/Metrics/Windows/WindowsMetricsProvider.cs`（新，仅 windows TFM） | LibreHardwareMonitor + DriveInfo + 注册表 → 读数/事实 |
| `agent/src/WakeUpMyWall.Agent/Api/SystemEndpoints.cs`（新） | `GET /api/v1/system` |
| `agent/src/WakeUpMyWall.Agent/Program.cs`（改） | 删 501 占位；按平台/`--fake-metrics` 注册提供者；挂 `MapSystemEndpoints()` |
| `agent/src/WakeUpMyWall.Agent/WakeUpMyWall.Agent.csproj`（改） | 双目标 TFM + 条件 PackageReference（Task A3） |
| 测试 | `agent/tests/WakeUpMyWall.Agent.Tests/MetricsMapperTests.cs`、`SystemEndpointTests.cs`、`FakeSystemMetricsProviderTests.cs` |

---

### Task A1: 契约模型 + 纯映射函数

**Files:**
- Create: `agent/src/WakeUpMyWall.Agent/Metrics/SystemMetrics.cs`、`Metrics/SensorReading.cs`、`Metrics/MetricsMapper.cs`
- Test: `agent/tests/WakeUpMyWall.Agent.Tests/MetricsMapperTests.cs`

**Interfaces:**
- Produces:
  - `SystemMetricsPayload(string CapturedAtUtc, IdentityPayload Identity, CpuMetricsPayload Cpu, GpuMetricsPayload Gpu, MemoryMetricsPayload Memory, StorageMetricsPayload Storage, ThermalMetricsPayload Thermal, NetworkMetricsPayload Network, long UptimeSeconds, string BootedAtUtc)`
  - `MachineFacts(string Hostname, string Os, string CpuName, string CpuShortName, string GpuName, string GpuShortName, string RamModule, string StorageModule, double? VramTotalGb, double? StorageTotalGb, double? StorageFreeGb, long UptimeSeconds, string BootedAtUtc)`
  - `SensorReading(string Hardware, string Sensor, string Name, double Value)`；`SensorHardware.{Cpu,Gpu,Memory,Storage,Motherboard,Network}`；`SensorKind.{Load,Temperature,ClockMhz,FanRpm,DataGb,ThroughputBps}`
  - `MetricsMapper.Map(readings: IReadOnlyList<SensorReading>, facts: MachineFacts, now: DateTimeOffset): SystemMetricsPayload`

- [ ] **Step 1: 写失败测试**（`MetricsMapperTests.cs`；每条测试只断言一条规则）

```csharp
using WakeUpMyWall.Agent.Metrics;

namespace WakeUpMyWall.Agent.Tests;

public class MetricsMapperTests
{
    private static readonly DateTimeOffset Now = new(2026, 9, 20, 21, 4, 0, TimeSpan.Zero);
    private static readonly MachineFacts Facts = new(
        Hostname: "DESKTOP-ALPHA", Os: "Windows 11 Pro",
        CpuName: "AMD Ryzen 7 7700X 8-Core Processor", CpuShortName: "Ryzen 7 7700X",
        GpuName: "NVIDIA GeForce RTX 4070 Ti", GpuShortName: "RTX 4070 Ti",
        RamModule: "32 GB DDR5-6000", StorageModule: "NVMe 2 TB",
        VramTotalGb: 12, StorageTotalGb: 2048, StorageFreeGb: 102,
        UptimeSeconds: 289440, BootedAtUtc: "2025-04-18T12:00:00Z");

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
            Facts, Now);

        Assert.Equal(12.5, payload.Cpu.UsagePercent);
        Assert.Equal(42.0, payload.Cpu.TempC);
        Assert.Equal(4.3, payload.Cpu.ClockGhz);           // 平均 4300 MHz → 4.3 GHz
        Assert.Equal(2, payload.Cpu.Cores);                // 由 "CPU Core #n" 计数得出
        Assert.Equal(38.0, payload.Memory.UsagePercent);
        Assert.Equal(12.2, payload.Memory.UsedGb);
        Assert.Equal(32.0, payload.Memory.TotalGb);        // Used + Available
        Assert.Equal(12.4, payload.Network.DownloadMbps);   // 1_550_000 B/s → 12.4 Mbps
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

        Assert.Equal(95.0, payload.Storage.UsagePercent);   // (2048-102)/2048
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
            Facts, Now);

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
            Facts, Now);

        Assert.Equal(55.0, payload.Cpu.UsagePercent);
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `cd agent && DOTNET_ROOT=$HOME/.dotnet-local PATH=$HOME/.dotnet-local:$PATH dotnet test WakeUpMyWall.Agent.slnx --filter MetricsMapperTests`
Expected: 编译失败，`The type or namespace name 'Metrics' does not exist`（RED）

- [ ] **Step 3: 实现**（三个文件；映射规则写在注释里，别让下一个人靠猜）

`Metrics/SystemMetrics.cs`：

```csharp
namespace WakeUpMyWall.Agent.Metrics;

/**
 * 手机端 MetricsSnapshot 的线上契约（spec §8）。字段名就是 JSON 属性名（camelCase），
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
    string Name, double? UsagePercent, double? ClockGhz, int? Cores, int? Threads, double? TempC, int? FanRpm);

public sealed record GpuMetricsPayload(
    string Name, double? UsagePercent, double? TempC, double? VramUsedGb, double? VramTotalGb, int? FanRpm);

public sealed record MemoryMetricsPayload(double? UsagePercent, double? UsedGb, double? TotalGb);

public sealed record StorageMetricsPayload(double? UsagePercent, double? UsedTb, double? TotalTb, double? FreeGb, double? TempC);

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
    long UptimeSeconds,
    string BootedAtUtc);
```

`Metrics/SensorReading.cs`：

```csharp
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
```

`Metrics/MetricsMapper.cs`：

```csharp
namespace WakeUpMyWall.Agent.Metrics;

/**
 * 读数 → 载荷的**纯函数**。规则：
 *  - 同名传感器出现多次时取**最后一个**（LHM 的枚举顺序里后面的更接近当前值）；
 *  - 缺失一律 null，不补 0；
 *  - 网络吞吐按 8 bit/byte 换算成 Mbps（1e6 进制，与 UI 的 Mbps 一致）；
 *  - CPU 核心数由 `CPU Core #n` 的时钟传感器计数得出；频率取它们的平均值（GHz，1 位小数）；
 *  - CPU 风扇取主板/CPU 硬件里名字含 "CPU" 的风扇；机箱风扇取主板硬件里其余风扇的最大值；
 *  - 存储用量来自 MachineFacts 的系统盘容量（DriveInfo），温度来自存储硬件的温度传感器。
 */
public static class MetricsMapper
{
    public static SystemMetricsPayload Map(
        IReadOnlyList<SensorReading> readings, MachineFacts facts, DateTimeOffset now)
    {
        var cpu = Hardware(readings, SensorHardware.Cpu);
        var gpu = Hardware(readings, SensorHardware.Gpu);
        var memory = Hardware(readings, SensorHardware.Memory);
        var storage = Hardware(readings, SensorHardware.Storage);
        var motherboard = Hardware(readings, SensorHardware.Motherboard);
        var network = Hardware(readings, SensorHardware.Network);

        var cores = cpu
            .Where(r => r.Sensor == SensorKind.ClockMhz && r.Name.StartsWith("CPU Core #", StringComparison.Ordinal))
            .Select(r => r.Name)
            .Distinct()
            .Count();
        var clockMhz = cpu
            .Where(r => r.Sensor == SensorKind.ClockMhz && r.Name.StartsWith("CPU Core #", StringComparison.Ordinal))
            .Select(r => r.Value)
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
                facts.Hostname, facts.Os, facts.CpuName, facts.CpuShortName,
                facts.GpuName, facts.GpuShortName, facts.RamModule, facts.StorageModule),
            Cpu: new CpuMetricsPayload(
                Name: facts.CpuName,
                UsagePercent: Round(Value(cpu, SensorKind.Load, "CPU Total"), 1),
                ClockGhz: clockMhz.Count == 0 ? null : Round(clockMhz.Average() / 1000.0, 1),
                Cores: cores == 0 ? null : cores,
                Threads: Environment.ProcessorCount,
                TempC: Round(
                    Value(cpu, SensorKind.Temperature, "CPU Package")
                    ?? Value(cpu, SensorKind.Temperature, "Core #1"),
                    1),
                FanRpm: Rpm(Last(CpuFans(motherboard, cpu)))),
            Gpu: new GpuMetricsPayload(
                Name: facts.GpuName,
                UsagePercent: Round(Max(gpu, SensorKind.Load, "GPU Core"), 1),
                TempC: Round(Max(gpu, SensorKind.Temperature, "GPU Core"), 1),
                VramUsedGb: Round(
                    Max(gpu, SensorKind.DataGb, "D3D Dedicated Memory Used")
                    ?? Max(gpu, SensorKind.DataGb, "GPU Memory Used"),
                    1),
                VramTotalGb: facts.VramTotalGb,
                FanRpm: Rpm(Max(gpu, SensorKind.FanRpm, "GPU Fan"))),
            Memory: new MemoryMetricsPayload(
                UsagePercent: Round(Value(memory, SensorKind.Load, "Memory"), 1),
                UsedGb: Round(memoryUsedGb, 1),
                TotalGb: Round(memoryUsedGb + memoryFreeGb, 1)),
            Storage: new StorageMetricsPayload(
                UsagePercent: Round(Percent(storageUsedGb, storageTotalGb), 1),
                UsedTb: Round(storageUsedGb / 1024.0, 1),
                TotalTb: Round(storageTotalGb / 1024.0, 1),
                FreeGb: Round(storageFreeGb, 1),
                TempC: Round(Value(storage, SensorKind.Temperature, "Temperature"), 1)),
            Thermal: new ThermalMetricsPayload(
                MotherboardTempC: Round(Value(motherboard, SensorKind.Temperature, "Temperature #1"), 1),
                CaseFanRpm: Rpm(CaseFans(motherboard))),
            Network: new NetworkMetricsPayload(
                DownloadMbps: Round(ToMbps(Value(network, SensorKind.ThroughputBps, "Download Speed")), 1),
                UploadMbps: Round(ToMbps(Value(network, SensorKind.ThroughputBps, "Upload Speed")), 1)),
            UptimeSeconds: facts.UptimeSeconds,
            BootedAtUtc: facts.BootedAtUtc);
    }

    private static List<SensorReading> Hardware(IReadOnlyList<SensorReading> readings, string hardware) =>
        readings.Where(r => r.Hardware == hardware).ToList();

    /** 同名传感器取最后一条（LHM 枚举里靠后的更接近当前时刻）。 */
    private static double? Value(List<SensorReading> readings, string sensor, string name) =>
        readings.Where(r => r.Sensor == sensor && r.Name == name).Select(r => (double?)r.Value).LastOrDefault();

    private static double? Max(List<SensorReading> readings, string sensor, string name) =>
        readings.Where(r => r.Sensor == sensor && r.Name == name).Select(r => (double?)r.Value).LastOrDefault();

    private static IEnumerable<double> CpuFans(List<SensorReading> motherboard, List<SensorReading> cpu) =>
        motherboard.Concat(cpu)
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
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `cd agent && DOTNET_ROOT=$HOME/.dotnet-local PATH=$HOME/.dotnet-local:$PATH dotnet test WakeUpMyWall.Agent.slnx`
Expected: `Passed: 19`（14 既有 + 5 新增），0 失败

- [ ] **Step 5: 提交** `feat: map raw sensor readings into the agent metric payload`

### Task A2: `GET /api/v1/system` + 假数据提供者

**Files:**
- Create: `Metrics/ISystemMetricsProvider.cs`、`Metrics/FakeSystemMetricsProvider.cs`、`Api/SystemEndpoints.cs`
- Modify: `agent/src/WakeUpMyWall.Agent/Program.cs`（删 501 占位，按平台注册提供者）
- Test: `agent/tests/WakeUpMyWall.Agent.Tests/SystemEndpointTests.cs`、`FakeSystemMetricsProviderTests.cs`

**Interfaces:**
- Consumes: Task A1 的 `SystemMetricsPayload` / `MachineFacts` / `MetricsMapper.Map`
- Produces: `interface ISystemMetricsProvider { SystemMetricsPayload Read(); }`；`FakeSystemMetricsProvider(TimeProvider clock)`；`MapSystemEndpoints(this RouteGroupBuilder group)`

- [ ] **Step 1: 写失败测试**

```csharp
using System.Net;
using System.Text.Json;
using WakeUpMyWall.Agent.Metrics;

namespace WakeUpMyWall.Agent.Tests;

public class SystemEndpointTests
{
    [Fact]
    public async Task Metrics_require_a_token()
    {
        using var app = new TestApp();
        var response = await app.CreateClient().GetAsync("/api/v1/system");

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    [Fact]
    public async Task Metrics_answer_with_the_contract_fields_when_authorized()
    {
        using var app = new TestApp();
        var response = await (await TestAuth.AuthorizedClientAsync(app)).GetAsync("/api/v1/system");

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        using var json = JsonDocument.Parse(await response.Content.ReadAsStringAsync());
        var root = json.RootElement;

        Assert.False(string.IsNullOrWhiteSpace(root.GetProperty("capturedAtUtc").GetString()));
        Assert.False(string.IsNullOrWhiteSpace(root.GetProperty("identity").GetProperty("hostname").GetString()));
        var cpu = root.GetProperty("cpu");
        Assert.InRange(cpu.GetProperty("usagePercent").GetDouble(), 0, 100);
        Assert.True(cpu.GetProperty("threads").GetInt32() >= 1);
        Assert.InRange(root.GetProperty("memory").GetProperty("usagePercent").GetDouble(), 0, 100);
        Assert.True(root.GetProperty("network").GetProperty("downloadMbps").GetDouble() >= 0);
        Assert.True(root.GetProperty("uptimeSeconds").GetInt64() >= 0);
    }
}

public class FakeSystemMetricsProviderTests
{
    private sealed class FixedClock(DateTimeOffset now) : TimeProvider
    {
        public override DateTimeOffset GetUtcNow() => now;
    }

    [Fact]
    public void Fake_readings_are_deterministic_for_a_fixed_clock_and_stay_in_range()
    {
        var clock = new FixedClock(new DateTimeOffset(2026, 9, 20, 21, 4, 0, TimeSpan.Zero));
        var provider = new FakeSystemMetricsProvider(clock);

        var first = provider.Read();
        var second = provider.Read();

        Assert.Equal(first, second);                        // 同一时刻两次读数一致（record 值相等）
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
```

- [ ] **Step 2: 跑测试确认失败**

Run: `cd agent && DOTNET_ROOT=$HOME/.dotnet-local PATH=$HOME/.dotnet-local:$PATH dotnet test WakeUpMyWall.Agent.slnx --filter SystemEndpointTests`
Expected: `/api/v1/system` 回 501 → `Assert.Equal() Failure: Expected Unauthorized, Actual NotImplemented`（RED）

- [ ] **Step 3: 实现**

`Metrics/ISystemMetricsProvider.cs`：

```csharp
namespace WakeUpMyWall.Agent.Metrics;

/**
 * 指标来源的唯一抽象。Windows 实现读 LibreHardwareMonitor，其它平台用假数据提供者，
 * 这样契约（端点 + JSON 形状 + 鉴权）在任何平台上都能端到端验证（与 IPowerController 同一思路）。
 */
public interface ISystemMetricsProvider
{
    SystemMetricsPayload Read();
}
```

`Metrics/FakeSystemMetricsProvider.cs`（合成读数 → **走同一条映射路径**，单位换算与聚合也被覆盖到）：

```csharp
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
```

`Api/SystemEndpoints.cs`：

```csharp
using WakeUpMyWall.Agent.Metrics;

namespace WakeUpMyWall.Agent.Api;

public static class SystemEndpoints
{
    // spec §8：指标是受保护端点（Bearer），载荷里有主机名与硬件型号。
    public static void MapSystemEndpoints(this RouteGroupBuilder group) =>
        group.MapGet("/api/v1/system", (ISystemMetricsProvider metrics) => Results.Ok(metrics.Read()));
}
```

`Program.cs` 两处改动（删掉 501 那段，改注册 + 挂端点）：

```csharp
builder.Services.AddSingleton<ISystemMetricsProvider>(services =>
    args.Contains("--fake-metrics") || !OperatingSystem.IsWindows()
        ? new FakeSystemMetricsProvider(services.GetRequiredService<TimeProvider>())
        : new WindowsMetricsProvider());

protectedEndpoints.MapSystemEndpoints();   // 取代原来的 501 占位
```

- [ ] **Step 4: 跑测试确认通过**

Run: `cd agent && DOTNET_ROOT=$HOME/.dotnet-local PATH=$HOME/.dotnet-local:$PATH dotnet test WakeUpMyWall.Agent.slnx`
Expected: `Passed: 23`，0 失败

- [ ] **Step 5: 冒烟一次真实 HTTP（Linux + 假数据）**

```bash
cd agent && DOTNET_ROOT=$HOME/.dotnet-local PATH=$HOME/.dotnet-local:$PATH \
  timeout 60 dotnet run --project src/WakeUpMyWall.Agent -- --fake-power --fake-metrics --urls http://127.0.0.1:9877
# 另开一个 shell：
curl -s http://127.0.0.1:9877/api/v1/status
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:9877/api/v1/system   # 期望 401
```

- [ ] **Step 6: 提交** `feat: serve the agent metric payload on /api/v1/system`

### Task A3: Windows 提供者（LibreHardwareMonitor）+ 双目标 TFM

**Files:**
- Modify: `agent/src/WakeUpMyWall.Agent/WakeUpMyWall.Agent.csproj`
- Create: `Metrics/Windows/WindowsMetricsProvider.cs`（仅 `net10.0-windows` 编译）
- Modify: `Program.cs`（`using WakeUpMyWall.Agent.Metrics.Windows;` 仅 windows TFM 需要）

**Interfaces:**
- Consumes: `ISystemMetricsProvider`、`SensorReading`、`MetricsMapper`、`MachineFacts`
- Produces: `WindowsMetricsProvider : ISystemMetricsProvider`（`Computer` + `DriveInfo` + 注册表）

- [ ] **Step 1: 改 csproj：双目标 + 条件依赖**

```xml
<PropertyGroup>
  <!-- 开发机/CI 走 net10.0（假数据），真实指标走 net10.0-windows（LHM 只带 win-* 运行时资产）。 -->
  <TargetFrameworks>net10.0;net10.0-windows</TargetFrameworks>
</PropertyGroup>

<ItemGroup Condition="'$(TargetFramework)' == 'net10.0-windows'">
  <PackageReference Include="LibreHardwareMonitorLib" Version="0.9.6" />
</ItemGroup>

<ItemGroup Condition="'$(TargetFramework)' != 'net10.0-windows'">
  <Compile Remove="Metrics/Windows/**/*.cs" />
</ItemGroup>
```

`Program.cs` 里那行 Windows 分支用 `#if WINDOWS` 包住：

```csharp
var useFakeMetrics = args.Contains("--fake-metrics") || !OperatingSystem.IsWindows();
#if WINDOWS
builder.Services.AddSingleton<ISystemMetricsProvider>(services =>
    useFakeMetrics
        ? new FakeSystemMetricsProvider(services.GetRequiredService<TimeProvider>())
        : new WindowsMetricsProvider());
#else
builder.Services.AddSingleton<ISystemMetricsProvider>(
    new FakeSystemMetricsProvider(builder.Services.BuildServiceProvider().GetRequiredService<TimeProvider>()));
#endif
```

（非 Windows 那一支不要为了取 `TimeProvider` 去建一个临时 provider —— 直接 `new FakeSystemMetricsProvider(TimeProvider.System)` 即可。）

- [ ] **Step 2: 实现 `WindowsMetricsProvider`**（薄适配：LHM 传感器 → `SensorReading`；`DriveInfo` 与注册表 → `MachineFacts`）

实现要点（照此写，不要自由发挥）：

1. 构造时建 `Computer { IsCpuEnabled = true, IsGpuEnabled = true, IsMemoryEnabled = true, IsStorageEnabled = true, IsMotherboardEnabled = true, IsNetworkEnabled = true }` 并 `Open()`；实现 `IDisposable`，`Dispose()` 里 `Close()`。
2. `Read()` 里对每个 hardware 调 `Update()`，再把 `hardware.Sensors`（`Value` 为 `null` 的跳过）按 `SensorType` 映射成读数：
   `Load → SensorKind.Load`、`Temperature → SensorKind.Temperature`、`Clock → SensorKind.ClockMhz`、`Fan → SensorKind.FanRpm`、`Data → SensorKind.DataGb`（**`SmallData` 是 MB → `/1024` 变成 GB**）、`Throughput → SensorKind.ThroughputBps`。
3. `HardwareType` 映射：`Cpu → SensorHardware.Cpu`、`GpuNvidia/GpuAmd/GpuIntel → SensorHardware.Gpu`、`Memory → SensorHardware.Memory`、`Storage → SensorHardware.Storage`、`Motherboard → SensorHardware.Motherboard`、`Network → SensorHardware.Network`；其余（含 `SuperIO`、`Battery`）跳过。
4. `MachineFacts`：`Hostname = Environment.MachineName`；`Os = RuntimeInformation.OSDescription`；CPU 名优先取注册表 `HKLM\HARDWARE\DESCRIPTION\System\CentralProcessor\0` 的 `ProcessorNameString`，读不到退回 LHM 的 Cpu 硬件名；GPU 名取第一个 Gpu 硬件名；`VramTotalGb` 读 `HKLM\SYSTEM\CurrentControlSet\Control\Class\{4d36e968-e325-11ce-bfc1-08002be10318}\0000\HardwareInformation.qwMemorySize`（REG_QWORD，字节 → GB）；任何一项读不到就是 `null`，绝不编。
5. 短名去掉厂商前缀（`AMD `、`Intel `、`NVIDIA GeForce `）——卡片宽度只放得下短名（spec 的 C2 规格）。
6. 存储容量：`new DriveInfo(Path.GetPathRoot(Environment.SystemDirectory)!)` 的 `TotalSize` / `AvailableFreeSpace`（字节 → GB，`/1024^3`）；`StorageModule` 取 LHM 存储硬件名，取不到就用盘符。
7. `UptimeSeconds = Environment.TickCount64 / 1000`；`BootedAtUtc = DateTimeOffset.UtcNow.AddSeconds(-uptime)`。

- [ ] **Step 3: 交叉编译 + 发布冒烟（Linux 上就能做）**

```bash
cd agent && DOTNET_ROOT=$HOME/.dotnet-local PATH=$HOME/.dotnet-local:$PATH \
  dotnet build src/WakeUpMyWall.Agent -f net10.0-windows
cd agent && DOTNET_ROOT=$HOME/.dotnet-local PATH=$HOME/.dotnet-local:$PATH \
  dotnet publish src/WakeUpMyWall.Agent -f net10.0-windows -r win-x64 --self-contained -c Release -o /tmp/agent-win
```
Expected: 两条都成功，`/tmp/agent-win/WakeUpMyWall.Agent.exe` 存在（1 个 native 依赖都不会漏）

- [ ] **Step 4: 双目标下跑全量测试，且 `net10.0` 目标仍可构建**

```bash
cd agent && DOTNET_ROOT=$HOME/.dotnet-local PATH=$HOME/.dotnet-local:$PATH \
  dotnet test WakeUpMyWall.Agent.slnx && dotnet build src/WakeUpMyWall.Agent -f net10.0
```
Expected: 23 条测试全绿；`net10.0` 构建成功（Windows 提供者被 `Compile Remove` 排除，不参与编译）

- [ ] **Step 5: 提交** `feat: read real windows metrics through librehardwaremonitor`

### Task A4: 文档、契约与验收

**Files:**
- Modify: `docs/plans/agent-api.md`、`agent/README.md`、`docs/plans/version-matrix.md`、`README.md`、`docs/superpowers/plans/2026-09-19-roadmap.md`、本计划

- [ ] **Step 1: 写 `agent-api.md` 的 `/api/v1/system` 契约**（把 501 那行换成真实端点 + 字段表：JSON 路径、单位、可空性、来源）
- [ ] **Step 2: 本机验收（Linux + 假数据，真实 HTTP + Bearer + 401 对照）**

起服务 → 从日志取配对码 → `POST /api/v1/pairing` 拿 Token → `GET /api/v1/system`（带 Token 与不带 Token 各一次），把关键字段贴进本计划 §4。

- [ ] **Step 3: 更新 roadmap / version-matrix / README / agent README**（Phase 5 拆成 5A/5B/5C；LHM 0.9.6 + 双 TFM 行；`--fake-metrics` 与 Windows 自验清单）
- [ ] **Step 4: 提交** `docs: record the phase 5a metric endpoint`

---

## 2. 验收标准（对照 roadmap Phase 5 的服务端部分）

| roadmap 要求 | 落点 | 验证方式 |
| --- | --- | --- |
| `/api/v1/system` 提供 spec §8 的全部指标 | Task A1/A2/A3 | `MetricsMapperTests` + 端点契约测试 + Linux 端到端实录 |
| 温度/风扇依赖硬件监控库 | Task A3 | LHM 0.9.6（win-x64 交叉发布通过）；真机读数由用户在 Windows 上验 |
| 取不到的传感器不撒谎 | Task A1 | `Missing_sensors_stay_null_instead_of_zero` |
| 受保护端点无 Token 401 | Task A2 | `Metrics_require_a_token` |
| 单位正确（网络 Mbps） | Task A1 | `1_550_000 B/s → 12.4 Mbps` 断言（单位来自 LHM 源码实读） |

## 3. 计划自检

1. **Spec 覆盖**：§8 的 9 项指标（CPU 使用率/温度/频率、GPU 使用率/温度/VRAM、RAM、存储、上/下行、Uptime、CPU/GPU 风扇、SSD/主板温度）逐项落在 `SystemMetricsPayload` 字段上。**刻意不做**：多盘逐个存储、每核心负载数组、历史曲线（手机端 Ring Buffer 负责）、刷新节奏（5B/5C）。
2. **占位符扫描**：无 TBD。A3 的 Windows 提供者以"要点 1–7"给出可执行规格（LHM 的 API 与传感器名/单位均已核对源码），不留"自行发挥"。
3. **类型一致性**：`SensorReading` / `MachineFacts` / `SystemMetricsPayload` 在 A1 定义，A2（假数据）与 A3（Windows）只消费；`ISystemMetricsProvider.Read()` 在 A2 定义、A3 实现；`MapSystemEndpoints` 在 A2 定义并被 `Program.cs` 调用。

## 4. 验收实录

（Task A4 完成后填写：Linux + `--fake-metrics` 的真实 HTTP 结果、双 TFM 构建/发布结果，以及"真机 Windows 读数待用户验证"的显式标记。）
