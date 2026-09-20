package com.xukunz.wakeupmywall.ui.monitor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.xukunz.wakeupmywall.core.theme.AppSizes
import com.xukunz.wakeupmywall.core.theme.AppShapes
import com.xukunz.wakeupmywall.core.theme.AppTypography
import com.xukunz.wakeupmywall.core.theme.DarkSurface
import com.xukunz.wakeupmywall.core.theme.LocalAccentPalette
import com.xukunz.wakeupmywall.core.theme.Spacing
import com.xukunz.wakeupmywall.domain.model.MetricsSnapshot
import com.xukunz.wakeupmywall.ui.components.Breakpoints
import com.xukunz.wakeupmywall.ui.components.WidgetStyle
import com.xukunz.wakeupmywall.ui.components.WidgetSurface
import com.xukunz.wakeupmywall.ui.dashboard.widgets.QuoteCard
import kotlin.math.sin

object MetricKeys {
    const val Cpu = "cpu"
    const val Gpu = "gpu"
    const val Ram = "ram"
    const val Storage = "storage"
    const val Network = "network"
}

/** Monitor 身份卡顶部的状态行（权威规格 C1：绿点 + 名称 + 在线状态 + 上次可见）。 */
data class PcStatusLine(
    val name: String,
    val stateLabel: String,
    val lastSeenLabel: String,
)

/** Quick Actions 的动作名（权威规格 C1）。**不内置任何第三方商标图标**，用几何字形 + 文字。 */
private val quickActions = listOf("Browser", "Discord", "Steam", "Spotify")

/**
 * Monitor 形态（权威规格 C）。第一行身份卡 + 快捷动作，第二行四张指标卡 + 温度/风扇，
 * 第三行网络/运行时长/最近活动/引用卡。列数由 [Breakpoints] 决定，窄屏允许纵向滚动。
 */
@Composable
fun MonitorMode(
    metrics: MetricsSnapshot,
    history: Map<String, List<Float>>,
    style: WidgetStyle,
    modifier: Modifier = Modifier,
    identity: com.xukunz.wakeupmywall.domain.model.HardwareIdentity? = null,
    onOpenDevice: (() -> Unit)? = null,
    status: PcStatusLine? = null,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().testTag("monitor")) {
        // maxWidth 只能在 BoxWithConstraints 作用域直接读，进入 Column 的 content lambda 后就不是这个 receiver 了。
        val available: Dp = maxWidth
        val scrollable = Breakpoints.requiresVerticalScroll(available)
        val columns = Breakpoints.columns(available)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                DeviceIdentityCard(identity, status, style, onOpenDevice, Modifier.weight(1.4f))
                QuickActionsCard(style, Modifier.weight(1f))
            }

            // 列数由断点决定：主区 ≥900dp 按概念图排 5 列（墙面屏 1280dp 的主区是 921dp），否则 3 列 / 2 列（风险 R7）。
            val metricSlots: List<@Composable (Modifier) -> Unit> = listOf(
                { slot ->
                    MetricCard(
                        key = MetricKeys.Cpu,
                        label = "CPU",
                        percent = metrics.cpuPercent,
                        modelLine = identity?.cpuShortName ?: "CPU",
                        values = history[MetricKeys.Cpu].orEmpty(),
                        footerPrimary = "${metrics.cpuClockGhz} GHz",
                        footerSecondary = "${metrics.cpuCores} cores ${metrics.cpuThreads} threads",
                        style = style,
                        modifier = slot,
                    )
                },
                { slot ->
                    MetricCard(
                        key = MetricKeys.Gpu,
                        label = "GPU",
                        percent = metrics.gpuPercent,
                        modelLine = identity?.gpuShortName ?: "GPU",
                        values = history[MetricKeys.Gpu].orEmpty(),
                        footerPrimary = "${metrics.gpuTempC} °C",
                        footerSecondary = "${metrics.vramUsedGb} / ${metrics.vramTotalGb} GB VRAM",
                        style = style,
                        modifier = slot,
                    )
                },
                { slot ->
                    MetricCard(
                        key = MetricKeys.Ram,
                        label = "RAM",
                        percent = metrics.ramPercent,
                        modelLine = identity?.ramModule ?: "RAM",
                        values = history[MetricKeys.Ram].orEmpty(),
                        footerPrimary = "${metrics.ramUsedGb} / ${metrics.ramTotalGb} GB",
                        footerSecondary = "Working set",
                        style = style,
                        modifier = slot,
                    )
                },
                { slot ->
                    MetricCard(
                        key = MetricKeys.Storage,
                        label = "Storage",
                        percent = metrics.storagePercent,
                        modelLine = identity?.storageModule ?: "Storage",
                        values = history[MetricKeys.Storage].orEmpty(),
                        footerPrimary = "${metrics.storageUsedTb} / ${metrics.storageTotalTb} TB",
                        footerSecondary = "${metrics.storageFreeGb} GB free",
                        style = style,
                        modifier = slot,
                    )
                },
                { slot -> TempsAndFansCard(metrics, style, slot) },
            )
            metricSlots.chunked(columns).forEach { metricRow ->
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    metricRow.forEach { slot -> slot(Modifier.weight(1f)) }
                    repeat(columns - metricRow.size) { Spacer(Modifier.weight(1f)) }
                }
            }

            // 第三行同样按断点排：576dp 下强排 4 列会把网络数值和引用卡挤到无法阅读（实测踩到）。
            val bottomSlots: List<@Composable (Modifier) -> Unit> = listOf(
                { slot -> NetworkCard(metrics, history[MetricKeys.Network].orEmpty(), style, slot) },
                { slot -> UptimeCard(metrics, style, slot) },
                { slot -> ActivityCard(metrics, style, slot) },
                { slot -> QuoteCard(slot.testTag("monitor:quote")) },
            )
            bottomSlots.chunked(columns).forEach { bottomRow ->
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    bottomRow.forEach { slot -> slot(Modifier.weight(1f)) }
                    repeat(columns - bottomRow.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun DeviceIdentityCard(
    identity: com.xukunz.wakeupmywall.domain.model.HardwareIdentity?,
    status: PcStatusLine?,
    style: WidgetStyle,
    onOpen: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    // 身份卡右上角的 `>` 是可点返回入口（HomeSurface 用它回 Dashboard）。
    WidgetSurface(
        style = style,
        modifier = modifier
            .testTag("monitor:identity")
            .then(if (onOpen != null) Modifier.clickable(onClick = onOpen) else Modifier),
    ) {
        if (status != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(AppSizes.statusDot)
                            .clip(CircleShape)
                            .background(LocalAccentPalette.current.onlineColor),
                    )
                    Text(
                        text = status.name,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.testTag("monitor:identity-name"),
                    )
                    Text(
                        text = status.stateLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalAccentPalette.current.onlineColor,
                        modifier = Modifier.testTag("monitor:identity-state"),
                    )
                }
                Text(
                    text = "Last seen ${status.lastSeenLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("monitor:identity-lastseen"),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(AppSizes.thumbnail)
                        .clip(AppShapes.button)
                        .background(DarkSurface.outline),
                )
                Column {
                    Text(
                        text = identity?.hostname ?: "PC",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.testTag("monitor:identity-hostname"),
                    )
                    Text(
                        text = identity?.os ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("monitor:identity-os"),
                    )
                    Text(
                        text = identity?.cpuName ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("monitor:identity-cpu"),
                    )
                    Text(
                        text = identity?.gpuName ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("monitor:identity-gpu"),
                    )
                }
            }
            Text(">", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun QuickActionsCard(style: WidgetStyle, modifier: Modifier = Modifier) {
    WidgetSurface(style = style, modifier = modifier.testTag("monitor:quick-actions")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Quick Actions", style = MaterialTheme.typography.bodyMedium)
            Text("+", style = MaterialTheme.typography.bodyMedium)
        }
        quickActions.forEach { action ->
            Row(
                modifier = Modifier.fillMaxWidth().testTag("action:$action"),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 几何字形占位：概念图里的第三方商标一律不内置（风险 R9）。
                Text("▦", style = MaterialTheme.typography.bodyMedium)
                Text("Open $action", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun TempsAndFansCard(metrics: MetricsSnapshot, style: WidgetStyle, modifier: Modifier = Modifier) {
    WidgetSurface(style = style, modifier = modifier.testTag("metric:temps")) {
        Text("System Temps", style = MaterialTheme.typography.bodyMedium)
        MetricRow("CPU", "${metrics.cpuTempC}°C", metrics.cpuTempC / 100f, "metric:temps-cpu")
        MetricRow("GPU", "${metrics.gpuTempC}°C", metrics.gpuTempC / 100f, "metric:temps-gpu")
        MetricRow(
            "Motherboard",
            "${metrics.motherboardTempC}°C",
            metrics.motherboardTempC / 100f,
            "metric:temps-motherboard",
        )
        MetricRow("SSD", "${metrics.ssdTempC}°C", metrics.ssdTempC / 100f, "metric:temps-ssd")

        Column(
            modifier = Modifier.fillMaxWidth().testTag("metric:fans"),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text("Fans", style = MaterialTheme.typography.bodyMedium)
            Text("CPU Fan ${formatRpm(metrics.cpuFanRpm)} RPM", style = MaterialTheme.typography.labelSmall, modifier = Modifier.testTag("metric:fans-cpu"))
            Text("GPU Fan ${formatRpm(metrics.gpuFanRpm)} RPM", style = MaterialTheme.typography.labelSmall, modifier = Modifier.testTag("metric:fans-gpu"))
            Text("Case Fans ${formatRpm(metrics.caseFanRpm)} RPM", style = MaterialTheme.typography.labelSmall, modifier = Modifier.testTag("metric:fans-case"))
        }
    }
}

@Composable
private fun NetworkCard(metrics: MetricsSnapshot, values: List<Float>, style: WidgetStyle, modifier: Modifier = Modifier) {
    WidgetSurface(style = style, modifier = modifier.testTag("metric:network")) {
        Text("Network", style = MaterialTheme.typography.bodyMedium)
        Text(
            text = "↓${metrics.downloadMbps} Mbps",
            style = AppTypography.metricReadout,
            modifier = Modifier.testTag("metric:network-down"),
        )
        Text(
            text = "↑${metrics.uploadMbps} Mbps",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("metric:network-up"),
        )
        MetricSparkline(values = values, modifier = Modifier.fillMaxWidth().size(AppSizes.thumbnail))
    }
}

@Composable
private fun UptimeCard(metrics: MetricsSnapshot, style: WidgetStyle, modifier: Modifier = Modifier) {
    WidgetSurface(style = style, modifier = modifier.testTag("metric:uptime")) {
        Text("Uptime", style = MaterialTheme.typography.bodyMedium)
        Text(
            text = formatUptime(metrics.uptimeSeconds),
            style = AppTypography.metricReadout,
            modifier = Modifier.testTag("metric:uptime-value"),
        )
        Text(
            text = metrics.bootDateLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ActivityCard(metrics: MetricsSnapshot, style: WidgetStyle, modifier: Modifier = Modifier) {
    WidgetSurface(style = style, modifier = modifier.testTag("monitor:activity")) {
        Text("Recent Activity", style = MaterialTheme.typography.bodyMedium)
        metrics.recentActivity.forEach { entry ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = entry.app,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(
                    text = entry.whenLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

/** 千分位：1,240 RPM 这种写法在概念图里是带分隔符的。 */
private fun formatRpm(rpm: Int): String {
    val text = rpm.toString()
    if (text.length <= 3) return text
    return text.dropLast(3) + "," + text.takeLast(3)
}

private fun formatUptime(seconds: Long): String {
    val days = seconds / 86_400
    val hours = (seconds % 86_400) / 3_600
    val minutes = (seconds % 3_600) / 60
    return "${days}d ${hours}h ${minutes}m"
}

/**
 * 由当前读数生成的确定性 60 秒 Mock 曲线（1 Hz）。Phase 5 接入真实轮询后，
 * 调用方改为传 `MetricRingBuffer.values()`，本函数随之删除。
 */
fun mockMetricHistory(metrics: MetricsSnapshot, samples: Int = 60): Map<String, List<Float>> {
    fun wave(base: Float, amplitude: Float): List<Float> =
        List(samples) { index -> base + amplitude * sin(index / 6f) }

    return mapOf(
        MetricKeys.Cpu to wave(metrics.cpuPercent, 4f),
        MetricKeys.Gpu to wave(metrics.gpuPercent, 6f),
        MetricKeys.Ram to wave(metrics.ramPercent, 2f),
        MetricKeys.Storage to List(samples) { metrics.storagePercent },
        MetricKeys.Network to wave(metrics.downloadMbps, 12f),
    )
}
