package com.xukunz.wakeupmywall.ui.powerrail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import com.xukunz.wakeupmywall.core.theme.AppSizes
import com.xukunz.wakeupmywall.core.theme.AppShapes
import com.xukunz.wakeupmywall.core.theme.DarkSurface
import com.xukunz.wakeupmywall.core.theme.LocalAccentPalette
import com.xukunz.wakeupmywall.core.theme.Spacing

/**
 * 常驻 PC 控制栏（权威规格 A1–A6）。所有可用性都来自 [PowerRailModel]，
 * 组件本身不做任何状态判断。
 */
@Composable
fun PowerRail(
    model: PowerRailModel,
    onPrimary: () -> Unit,
    onSleep: () -> Unit,
    onShutdown: () -> Unit,
    onRestart: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    quoteCard: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(Spacing.lg)
            .testTag("powerrail"),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // A1 标题区 + 齿轮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = model.pcName,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.testTag("powerrail:name"),
                )
                Text(
                    text = model.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("powerrail:subtitle"),
                )
            }
            IconButton(onClick = onSettings, modifier = Modifier.testTag("powerrail:settings")) {
                Text("⚙", style = MaterialTheme.typography.titleLarge)
            }
        }

        // A2 状态行：状态色点 + 短文案
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            val dotColor = if (model.agentReachable) {
                LocalAccentPalette.current.onlineColor
            } else {
                MaterialTheme.colorScheme.primary
            }
            Box(Modifier.size(AppSizes.statusDot).clip(CircleShape).background(dotColor))
            Text(
                text = model.stateLabel,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("powerrail:state"),
            )
        }

        // A3 主电源环 + 标签 + 副标
        PowerRingButton(enabled = model.primaryEnabled, onClick = onPrimary)
        Text(
            text = model.primaryLabel,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("powerrail:primary-label"),
        )
        Text(
            text = model.primaryCaption,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("powerrail:primary-caption"),
        )

        // A4 次级动作（等宽三枚）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            RailAction("Sleep", "SNAP MODE", model.canSleep, onSleep, "powerrail:sleep", Modifier.weight(1f))
            RailAction("Shut Down", "POWER OFF", model.canShutdown, onShutdown, "powerrail:shutdown", Modifier.weight(1f))
            RailAction("Restart", "FRESH START", model.canRestart, onRestart, "powerrail:restart", Modifier.weight(1f))
        }

        // A5 连接条：通道文案 + 能力描述 + 箭头
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.button)
                .background(DarkSurface.card.copy(alpha = 0.5f))
                .padding(Spacing.md)
                .testTag("powerrail:connection-bar"),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("⇄", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = model.connectionLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f).testTag("powerrail:connection"),
                )
                Text("→", style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = model.statusLine,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("powerrail:status"),
            )
        }

        // A6 引用卡（可开关的装饰层，默认关闭）
        if (quoteCard) {
            Text(
                text = "SAME PROGRESS A BRIGHTER TOMORROW",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().testTag("powerrail:quote"),
            )
        }
    }
}

@Composable
private fun RailAction(
    label: String,
    caption: String,
    enabled: Boolean,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier.testTag(tag)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            Text(
                text = caption,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}
