package com.xukunz.wakeupmywall.ui.standby

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import com.xukunz.wakeupmywall.core.theme.AppSizes
import com.xukunz.wakeupmywall.core.theme.DarkSurface
import com.xukunz.wakeupmywall.core.theme.LocalAccentPalette
import com.xukunz.wakeupmywall.core.theme.Spacing
import com.xukunz.wakeupmywall.domain.model.NextEvent
import com.xukunz.wakeupmywall.ui.components.WidgetStyle
import com.xukunz.wakeupmywall.ui.components.WidgetSurface
import com.xukunz.wakeupmywall.ui.dashboard.DashboardData
import com.xukunz.wakeupmywall.ui.dashboard.widgets.PerspectiveMark
import com.xukunz.wakeupmywall.ui.powerrail.PowerRailModel
import com.xukunz.wakeupmywall.ui.powerrail.PowerRingButton
import com.xukunz.wakeupmywall.ui.RailEvent

/**
 * StandBy 形态（权威规格 D）：超大时钟 + 天气 + 下一场日程 + 右侧 PC 浮层卡 + 底部状态条。
 * **刻意不渲染常驻 Power Rail** —— 由 `StandByModeTest.pc card exposes night mode...` 守住。
 */
@Composable
fun StandByMode(
    data: DashboardData,
    rail: PowerRailModel,
    nextEvent: NextEvent,
    onRailEvent: (RailEvent) -> Unit,
    onOpenMonitor: () -> Unit,
    modifier: Modifier = Modifier,
    nightMode: Boolean = true,
    autoDim: Boolean = true,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.lg).testTag("standby"),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = "A MORE FOCUSED TOMORROW",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("standby:brand"),
            )
            PerspectiveMark()
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StandByClock(
                time = data.time,
                meridiem = "PM",
                date = data.date,
                modifier = Modifier.weight(1.6f),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                WidgetSurface(style = WidgetStyle.Glass, modifier = Modifier.testTag("standby:weather")) {
                    Text("Weather", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(data.weather.condition, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = data.weather.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("${data.weather.temperatureC}° · ${data.weather.location}", style = MaterialTheme.typography.bodyMedium)
                }

                WidgetSurface(style = WidgetStyle.Glass, modifier = Modifier.testTag("standby:next-event")) {
                    Text(
                        text = nextEvent.countdownLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("standby:next-event-countdown"),
                    )
                    Text(
                        text = nextEvent.title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.testTag("standby:next-event-title"),
                    )
                    Text(nextEvent.timeRange, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = nextEvent.source,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("standby:next-event-source"),
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            // 权威规格 D 是"右侧 PC 浮层卡"，因此只占右约三分之一宽度，而不是铺满整行。
            WidgetSurface(
                style = WidgetStyle.Glass,
                modifier = Modifier.fillMaxWidth(AppSizes.standbyCardWidthFraction).testTag("standby:pc-card"),
            ) {
                Text(rail.pcName, style = MaterialTheme.typography.titleLarge)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    PowerRingButton(
                        enabled = rail.primaryEnabled,
                        onClick = { onRailEvent(RailEvent.Primary) },
                        diameter = AppSizes.standbyRingDiameter,
                    )
                    Column {
                        Text(rail.primaryLabel, style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = rail.primaryCaption,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                ToggleLine(
                    label = "Night Mode",
                    value = if (nightMode) "ON" else "OFF",
                    tag = "standby:night-mode",
                )
                ToggleLine(
                    label = "Auto-Dim",
                    value = if (autoDim) "ACTIVE" else "IDLE",
                    tag = "standby:auto-dim",
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(com.xukunz.wakeupmywall.core.theme.AppShapes.card)
                .background(DarkSurface.card.copy(alpha = 0.5f))
                .clickable(onClick = onOpenMonitor)
                .padding(Spacing.md)
                .testTag("standby:status-bar"),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("▣", style = MaterialTheme.typography.bodyMedium)
            Box(
                Modifier
                    .size(AppSizes.statusDot)
                    .clip(CircleShape)
                    .background(LocalAccentPalette.current.onlineColor),
            )
            Text(rail.pcName, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = rail.stateLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Last seen 1 min ago",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = ">",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable(onClick = onOpenMonitor).testTag("standby:open-monitor"),
            )
        }
    }
}

@Composable
private fun ToggleLine(label: String, value: String, tag: String) {
    Row(
        modifier = Modifier.fillMaxWidth().testTag(tag),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
