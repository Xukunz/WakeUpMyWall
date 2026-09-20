package com.xukunz.wakeupmywall.ui.powerrail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.xukunz.wakeupmywall.core.theme.AppSizes
import com.xukunz.wakeupmywall.core.theme.AppShapes
import com.xukunz.wakeupmywall.core.theme.DarkSurface
import com.xukunz.wakeupmywall.core.theme.LocalAccentPalette
import com.xukunz.wakeupmywall.core.theme.Spacing
import com.xukunz.wakeupmywall.ui.icons.AppIcon
import com.xukunz.wakeupmywall.ui.icons.AppIconKind

/**
 * 竖屏形态的常驻控制栏（权威规格 A 的紧凑版）：手机 20:9、折叠外屏 21.1:9 这类窗口里，
 * 28% 宽的侧栏会把主内容挤到 200dp 以下，所以整条栏挪到底部，横向铺开：
 *
 * ```
 * [绿点 状态 · 通道文案 ............... 齿轮]
 * [ 环 80dp | Power On / WAKE YOUR PC ........ ]
 * [ Sleep ][ Shut Down ][ Restart ]            ← 三等分瓦片
 * ```
 *
 * 与侧栏共用同一份 [PowerRailModel] 与同一批 testTag，语义完全一致；
 * 可用性仍然只来自 `PcState.capabilities`。
 */
@Composable
fun PowerRailCompact(
    model: PowerRailModel,
    onPrimary: () -> Unit,
    onSleep: () -> Unit,
    onShutdown: () -> Unit,
    onRestart: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(DarkSurface.card.copy(alpha = 0.55f))
            .padding(Spacing.md)
            .testTag("powerrail"),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(AppSizes.statusDot)
                    .clip(CircleShape)
                    .background(
                        if (model.agentReachable) {
                            LocalAccentPalette.current.onlineColor
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    ),
            )
            Text(
                text = model.stateLabel,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("powerrail:state"),
            )
            Text(
                text = model.connectionLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).testTag("powerrail:connection"),
            )
            IconButton(onClick = onSettings, modifier = Modifier.testTag("powerrail:settings")) {
                AppIcon(
                    kind = AppIconKind.Gear,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    size = AppSizes.iconMedium,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PowerRingButton(
                enabled = model.primaryEnabled,
                onClick = onPrimary,
                diameter = AppSizes.compactRingDiameter,
            )
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = model.primaryLabel,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.testTag("powerrail:primary-label"),
                )
                Text(
                    text = model.primaryCaption,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("powerrail:primary-caption"),
                )
                Text(
                    text = model.pcName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("powerrail:name"),
                )
                Text(
                    text = model.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("powerrail:subtitle"),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            CompactAction(AppIconKind.Moon, "Sleep", "SNAP MODE", model.canSleep, onSleep, "powerrail:sleep", Modifier.weight(1f))
            CompactAction(AppIconKind.StopSquare, "Shut Down", "POWER OFF", model.canShutdown, onShutdown, "powerrail:shutdown", Modifier.weight(1f))
            CompactAction(AppIconKind.Restart, "Restart", "FRESH START", model.canRestart, onRestart, "powerrail:restart", Modifier.weight(1f))
        }

        // 顶行已经写着通道文案，相同时再排一行就是同一句话重复（见 PowerRailModel.showsStatusLine）。
        if (model.showsStatusLine) {
            Text(
                text = model.statusLine,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("powerrail:status"),
            )
        }
    }
}

/** 底栏里的次级动作：图标在上、标签在下，整块可点（比按钮控件更省高度）。 */
@Composable
private fun CompactAction(
    icon: AppIconKind,
    label: String,
    caption: String,
    enabled: Boolean,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }
    Column(
        modifier = modifier
            .clip(AppShapes.button)
            .background(DarkSurface.card.copy(alpha = 0.5f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(PaddingValues(horizontal = Spacing.xs, vertical = Spacing.sm))
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        AppIcon(kind = icon, tint = contentColor, size = AppSizes.iconMedium)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.5f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
