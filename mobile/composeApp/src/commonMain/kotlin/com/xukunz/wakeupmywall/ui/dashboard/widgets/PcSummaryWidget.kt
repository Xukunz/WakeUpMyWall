package com.xukunz.wakeupmywall.ui.dashboard.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.platform.testTag
import com.xukunz.wakeupmywall.core.theme.AppShapes
import com.xukunz.wakeupmywall.core.theme.AppSizes
import com.xukunz.wakeupmywall.core.theme.DarkSurface
import com.xukunz.wakeupmywall.core.theme.LocalAccentPalette
import com.xukunz.wakeupmywall.core.theme.Spacing
import com.xukunz.wakeupmywall.domain.model.PcSummarySnapshot
import com.xukunz.wakeupmywall.ui.components.WidgetStyle
import com.xukunz.wakeupmywall.ui.components.WidgetSurface
import com.xukunz.wakeupmywall.ui.powerrail.PowerRailModel

/** 权威规格 B 行 3：绿点 + 名称 + 状态 + 上次可见；缩略图；四条指标带彩色细条；`>` 进入 Monitor。 */
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
            Text(">", style = MaterialTheme.typography.bodyMedium)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            // 缩略图：视觉打磨阶段替换为真实机箱图，这里先用中性占位块。
            Box(
                modifier = Modifier
                    .size(AppSizes.statusDot * 6)
                    .clip(AppShapes.button)
                    .background(DarkSurface.outline),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                MetricBar("CPU ${summary.cpuPercent}%", summary.cpuPercent / 100f, "dashboard:pc-cpu")
                MetricBar("Temp ${summary.cpuTempC}°C", summary.cpuTempC / 100f, "dashboard:pc-temp")
                MetricBar("RAM ${summary.ramPercent}%", summary.ramPercent / 100f, "dashboard:pc-ram")
                MetricBar(
                    label = "Network ↓${summary.downloadMbps} Mbps ↑${summary.uploadMbps} Mbps",
                    fraction = summary.downloadMbps / 200f,
                    tag = "dashboard:pc-network",
                )
            }
        }
    }
}

@Composable
private fun MetricBar(label: String, fraction: Float, tag: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.testTag(tag))
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(AppSizes.barHeight)
                .clip(AppShapes.badge)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}
