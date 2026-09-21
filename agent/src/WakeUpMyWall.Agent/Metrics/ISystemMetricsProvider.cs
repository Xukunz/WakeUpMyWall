namespace WakeUpMyWall.Agent.Metrics;

/**
 * 指标来源的唯一抽象。Windows 实现读 LibreHardwareMonitor，其它平台用假数据提供者，
 * 这样契约（端点 + JSON 形状 + 鉴权）在任何平台上都能端到端验证（与 IPowerController 同一思路）。
 */
public interface ISystemMetricsProvider
{
    SystemMetricsPayload Read();
}
