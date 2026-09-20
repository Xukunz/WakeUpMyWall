package com.xukunz.wakeupmywall.ui.monitor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.xukunz.wakeupmywall.core.theme.AppSizes
import com.xukunz.wakeupmywall.core.theme.AppShapes
import com.xukunz.wakeupmywall.core.theme.AppTypography
import com.xukunz.wakeupmywall.core.theme.DarkSurface
import com.xukunz.wakeupmywall.core.theme.LocalAccentPalette
import com.xukunz.wakeupmywall.core.theme.MetricColors
import com.xukunz.wakeupmywall.core.theme.Spacing
import com.xukunz.wakeupmywall.domain.model.MetricsSnapshot
import com.xukunz.wakeupmywall.ui.components.Breakpoints
import com.xukunz.wakeupmywall.ui.components.PcCover
import com.xukunz.wakeupmywall.ui.components.WidgetStyle
import com.xukunz.wakeupmywall.ui.components.WidgetSurface
import com.xukunz.wakeupmywall.ui.dashboard.widgets.QuoteCard
import com.xukunz.wakeupmywall.ui.icons.AppIcon
import com.xukunz.wakeupmywall.ui.icons.AppIconKind
import kotlin.math.sin

object MetricKeys {
    const val Cpu = "cpu"
    const val Gpu = "gpu"
    const val Ram = "ram"
    const val Storage = "storage"
    const val Network = "network"
}

/** 行内槽位：卡片 + 它的横向权重（概念图里底行四张卡不是等宽的）。 */
private class WeightedSlot(
    val weight: Float,
    val content: @Composable (Modifier) -> Unit,
)

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
    deviceId: String? = null,
    onOpenDevice: (() -> Unit)? = null,
    status: PcStatusLine? = null,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().testTag("monitor")) {
        // maxWidth 只能在 BoxWithConstraints 作用域直接读，进入 Column 的 content lambda 后就不是这个 receiver 了。
        val available: Dp = maxWidth
        val scrollable = Breakpoints.requiresVerticalScroll(available) || maxHeight > maxWidth
        val columns = Breakpoints.columns(available)
        // 竖屏手机（20:9 / 21.1:9 的 ~412dp）里身份卡与快捷动作不再并排：并排会把四张动作瓦片压成竖条。
        val stacked = Breakpoints.stacksRows(available)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (stacked) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    DeviceIdentityCard(identity, deviceId, status, style, onOpenDevice, Modifier.fillMaxWidth())
                    QuickActionsCard(style, Modifier.fillMaxWidth())
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    DeviceIdentityCard(identity, deviceId, status, style, onOpenDevice, Modifier.weight(1.4f))
                    QuickActionsCard(style, Modifier.weight(1f))
                }
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
                        icon = AppIconKind.Cpu,
                        color = MetricColors.cpu,
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
                        icon = AppIconKind.Gpu,
                        color = MetricColors.gpu,
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
                        icon = AppIconKind.Ram,
                        color = MetricColors.ram,
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
                        icon = AppIconKind.Storage,
                        color = MetricColors.storage,
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
            // 权重也不等：概念图里 Recent Activity 明显比 Network / Uptime 宽（实测 295px vs 205px），
            // 等宽会把应用名截成 `Micros…`，等于丢掉这张卡的信息量。
            val bottomSlots = listOf(
                WeightedSlot(1f) { slot -> NetworkCard(metrics, history[MetricKeys.Network].orEmpty(), style, slot) },
                WeightedSlot(0.85f) { slot -> UptimeCard(metrics, style, slot) },
                WeightedSlot(1.35f) { slot -> ActivityCard(metrics, style, slot) },
                WeightedSlot(1f) { slot -> QuoteCard(slot.testTag("monitor:quote")) },
            )
            bottomSlots.chunked(columns).forEach { bottomRow ->
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    bottomRow.forEach { slot -> slot.content(Modifier.weight(slot.weight)) }
                    repeat(columns - bottomRow.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun DeviceIdentityCard(
    identity: com.xukunz.wakeupmywall.domain.model.HardwareIdentity?,
    deviceId: String?,
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
                // 概念图 C1：身份卡的缩略图是真实的机箱照片，不是占位块。
                PcCover(
                    deviceId = deviceId,
                    modifier = Modifier
                        .size(AppSizes.coverIdentityWidth, AppSizes.coverIdentityHeight)
                        .testTag("monitor:identity-cover"),
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
            AppIcon(
                kind = AppIconKind.ChevronRight,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                size = AppSizes.iconMedium,
            )
        }
    }
}

/**
 * 权威规格 C1 的 Quick Actions：四个等宽图标瓦片横排，标签在下。
 * 图标是代码绘制的通用图形——Discord / Steam / Spotify 是第三方商标，**不内置、不仿制**，
 * 用 [AppIconKind.AppTile] 表示"某个应用"，语义靠下方文字承担。
 */
@Composable
private fun QuickActionsCard(style: WidgetStyle, modifier: Modifier = Modifier) {
    WidgetSurface(style = style, modifier = modifier.testTag("monitor:quick-actions")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Quick Actions", style = MaterialTheme.typography.bodyMedium)
            AppIcon(
                kind = AppIconKind.Plus,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                size = AppSizes.iconMedium,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            quickActions.forEach { action ->
                Column(
                    modifier = Modifier.weight(1f).testTag("action:$action"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Box(
                        modifier = Modifier
                            .size(AppSizes.iconTile)
                            .clip(AppShapes.button)
                            .background(DarkSurface.card.copy(alpha = 0.6f))
                            .border(Spacing.hairline, DarkSurface.outline, AppShapes.button),
                        contentAlignment = Alignment.Center,
                    ) {
                        AppIcon(
                            kind = if (action == "Browser") AppIconKind.Display else AppIconKind.AppTile,
                            tint = MaterialTheme.colorScheme.onSurface,
                            size = AppSizes.iconLarge,
                        )
                    }
                    Text(
                        text = action,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun TempsAndFansCard(metrics: MetricsSnapshot, style: WidgetStyle, modifier: Modifier = Modifier) {
    WidgetSurface(style = style, modifier = modifier.testTag("metric:temps")) {
        SectionLine(icon = AppIconKind.Thermometer, tint = MetricColors.temperatureWarm, title = "System Temps")
        MetricRow("CPU", "${metrics.cpuTempC}°C", metrics.cpuTempC / 100f, "metric:temps-cpu", MetricColors.temperature)
        MetricRow("GPU", "${metrics.gpuTempC}°C", metrics.gpuTempC / 100f, "metric:temps-gpu", MetricColors.temperatureWarm)
        MetricRow(
            "Motherboard",
            "${metrics.motherboardTempC}°C",
            metrics.motherboardTempC / 100f,
            "metric:temps-motherboard",
            MetricColors.temperature,
        )
        MetricRow("SSD", "${metrics.ssdTempC}°C", metrics.ssdTempC / 100f, "metric:temps-ssd", MetricColors.temperature)

        Column(
            modifier = Modifier.fillMaxWidth().testTag("metric:fans"),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            SectionLine(icon = AppIconKind.Fan, tint = MetricColors.fan, title = "Fans")
            // 概念图里风扇也是"点 + 名称 + 数值 + 细条"，转速按 2000 RPM 满刻度换算。
            MetricRow("CPU Fan", "${formatRpm(metrics.cpuFanRpm)} RPM", metrics.cpuFanRpm / 2000f, "metric:fans-cpu", MetricColors.fan)
            MetricRow("GPU Fan", "${formatRpm(metrics.gpuFanRpm)} RPM", metrics.gpuFanRpm / 2000f, "metric:fans-gpu", MetricColors.fan)
            MetricRow("Case Fans", "${formatRpm(metrics.caseFanRpm)} RPM", metrics.caseFanRpm / 2000f, "metric:fans-case", MetricColors.fan)
        }
    }
}

/** 卡片内的小节标题：图标 + 文字（概念图里 System Temps / Fans 前面各有一个图形）。 */
@Composable
private fun SectionLine(icon: AppIconKind, tint: Color, title: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(kind = icon, tint = tint, size = AppSizes.iconMedium)
        Text(title, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun NetworkCard(metrics: MetricsSnapshot, values: List<Float>, style: WidgetStyle, modifier: Modifier = Modifier) {
    WidgetSurface(style = style, modifier = modifier.testTag("metric:network")) {
        SectionLine(icon = AppIconKind.Wifi, tint = MetricColors.networkIcon, title = "Network")
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
        SectionLine(icon = AppIconKind.Clock, tint = MetricColors.uptimeIcon, title = "Uptime")
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
        SectionLine(icon = AppIconKind.List, tint = MetricColors.activityIcon, title = "Recent Activity")
        metrics.recentActivity.forEachIndexed { index, entry ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(AppSizes.statusDot)
                        .clip(CircleShape)
                        .background(MetricColors.activityDots[index % MetricColors.activityDots.size]),
                )
                Text(
                    text = entry.app,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
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
