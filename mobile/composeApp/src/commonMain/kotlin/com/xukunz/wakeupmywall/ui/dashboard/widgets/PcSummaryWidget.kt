package com.xukunz.wakeupmywall.ui.dashboard.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import com.xukunz.wakeupmywall.core.theme.AppShapes
import com.xukunz.wakeupmywall.core.theme.AppSizes
import com.xukunz.wakeupmywall.core.theme.DarkSurface
import com.xukunz.wakeupmywall.core.theme.LocalAccentPalette
import com.xukunz.wakeupmywall.core.theme.MetricColors
import com.xukunz.wakeupmywall.core.theme.Spacing
import com.xukunz.wakeupmywall.domain.model.PcSummarySnapshot
import com.xukunz.wakeupmywall.ui.components.PcCover
import com.xukunz.wakeupmywall.ui.components.WidgetStyle
import com.xukunz.wakeupmywall.ui.components.WidgetSurface
import com.xukunz.wakeupmywall.ui.icons.AppIcon
import com.xukunz.wakeupmywall.ui.icons.AppIconKind
import com.xukunz.wakeupmywall.ui.powerrail.PowerRailModel

/**
 * 权威规格 B 行 3：绿点 + 名称 + 状态 + 上次可见；PC 封面缩略图；四条指标**横向四列**
 * （每列 = 名称 + 数值 + 彩色细条）；`>` 进入 Monitor。
 *
 * 缩略图用的是打包素材 `pc_default_cover.jpg`（由 `tools/prepare_ui_assets.py` 从
 * `imgs/ui/pc_default_cover.png` 派生），不再是中性占位块。
 */
@Composable
fun PcSummaryWidget(
    pc: PowerRailModel,
    summary: PcSummarySnapshot,
    style: WidgetStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WidgetSurface(
        style = style,
        modifier = modifier.testTag("dashboard:pc-summary").clickable(onClick = onClick),
    ) {
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
                Text(pc.pcName, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = pc.stateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Last seen ${summary.lastSeenLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("dashboard:pc-lastseen"),
                )
            }
            AppIcon(
                kind = AppIconKind.ChevronRight,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                size = AppSizes.iconMedium,
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val narrow = maxWidth < AppSizes.summaryMetricsMinWidth
            if (narrow) {
                // Compact 形态：封面单独一行，指标 2×2。四列硬排会把 `CPU` 压成竖排单字、`12%` 折行。
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    PcCover(
                        deviceId = pc.deviceId,
                        modifier = Modifier
                            .size(AppSizes.coverThumbnailWidth, AppSizes.coverThumbnailHeight)
                            .testTag("dashboard:pc-cover"),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        SummaryMetric("CPU", "${summary.cpuPercent}%", summary.cpuPercent / 100f, MetricColors.cpu, "cpu", Modifier.weight(1f))
                        SummaryMetric("Temp", "${summary.cpuTempC}°C", summary.cpuTempC / 100f, MetricColors.temperature, "temp", Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        SummaryMetric("RAM", "${summary.ramPercent}%", summary.ramPercent / 100f, MetricColors.ram, "ram", Modifier.weight(1f))
                        NetworkMetric(summary, Modifier.weight(1.4f))
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    // 四列指标 + 封面在 1280dp 下刚好排得下：间距用 sm，用 md 会把 Network 的两行数值挤成三行。
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PcCover(
                        deviceId = pc.deviceId,
                        modifier = Modifier
                            .size(AppSizes.coverThumbnailWidth, AppSizes.coverThumbnailHeight)
                            .testTag("dashboard:pc-cover"),
                    )
                    SummaryMetric("CPU", "${summary.cpuPercent}%", summary.cpuPercent / 100f, MetricColors.cpu, "cpu", Modifier.weight(1f))
                    SummaryMetric("Temp", "${summary.cpuTempC}°C", summary.cpuTempC / 100f, MetricColors.temperature, "temp", Modifier.weight(1f))
                    SummaryMetric("RAM", "${summary.ramPercent}%", summary.ramPercent / 100f, MetricColors.ram, "ram", Modifier.weight(1f))
                    NetworkMetric(summary, Modifier.weight(2.2f))
                }
            }
        }
    }
}

/** Network 是唯一有两行数值的一列（下行 ↓ 上行 ↑），任何排布下都要给它更宽的位置。 */
@Composable
private fun NetworkMetric(summary: PcSummarySnapshot, modifier: Modifier = Modifier) {
    SummaryMetric(
        label = "Network",
        value = "↓${summary.downloadMbps} Mbps\n↑${summary.uploadMbps} Mbps",
        fraction = summary.downloadMbps / 200f,
        color = MetricColors.network,
        key = "network",
        modifier = modifier,
    )
}

/** 摘要卡的一列指标：名称 + 数值 + 彩色细条（概念图 B3）。 */
@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    fraction: Float,
    color: Color,
    key: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("dashboard:pc-$key-label"),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("dashboard:pc-$key"),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppSizes.metricBarHeight)
                .clip(AppShapes.badge)
                .background(DarkSurface.outline),
        ) {
            // 概念图里是"灰底细轨 + 彩色填充段"，不是只有一个浮空的色条。
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(AppSizes.metricBarHeight)
                    .background(color),
            )
        }
    }
}
