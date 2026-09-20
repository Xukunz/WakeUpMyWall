package com.xukunz.wakeupmywall.ui.standby

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import com.xukunz.wakeupmywall.core.theme.AppShapes
import com.xukunz.wakeupmywall.core.theme.AppSizes
import com.xukunz.wakeupmywall.core.theme.AppTypography
import com.xukunz.wakeupmywall.core.theme.DarkSurface
import com.xukunz.wakeupmywall.core.theme.LocalAccentPalette
import com.xukunz.wakeupmywall.core.theme.Spacing
import com.xukunz.wakeupmywall.domain.model.NextEvent
import com.xukunz.wakeupmywall.ui.RailEvent
import com.xukunz.wakeupmywall.ui.components.WeatherIcon
import com.xukunz.wakeupmywall.ui.components.WidgetStyle
import com.xukunz.wakeupmywall.ui.components.WidgetSurface
import com.xukunz.wakeupmywall.ui.dashboard.DashboardData
import com.xukunz.wakeupmywall.ui.dashboard.widgets.PerspectiveMark
import com.xukunz.wakeupmywall.ui.icons.AppIcon
import com.xukunz.wakeupmywall.ui.icons.AppIconKind
import com.xukunz.wakeupmywall.ui.powerrail.PowerRailModel
import com.xukunz.wakeupmywall.ui.powerrail.PowerRingButton

/**
 * StandBy 形态（权威规格 D，逐项对齐概念图 `imgs/concept/image-gen-5.png`）：
 *
 * - 顶行：`A MORE FOCUSED TOMORROW` + 短分割线（左）、透视装饰（右）；
 * - 左：超大时钟 + 长日期；下面并排 Weather 卡与 Next Event 卡；
 * - 右：PC 浮层卡 —— `My PC` + `POWER CONTROL` + `>`，发光环，`Power On` / `WAKE YOUR PC`，
 *   以及 `Night Mode` / `Auto-Dim` 两枚带图标的开关瓦片；
 * - 底：通栏状态条（显示器图标 + 绿点 + 名称 + 状态 + 上次可见 + `>`）。
 *
 * **刻意不渲染常驻 Power Rail** —— 由 `StandByModeTest` 守住（StandBy 用浮层卡而不是侧栏）。
 * 竖屏（手机 20:9、折叠外屏 21.1:9、内屏竖放）改成单列纵向滚动：横排的 "时钟 + 两张卡 + 浮层卡"
 * 在 400dp 宽度里会把每张卡压到不可读。
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
    BoxWithConstraints(modifier = modifier.fillMaxSize().testTag("standby")) {
        val stacked = maxHeight > maxWidth
        if (stacked) {
            StandByStacked(data, rail, nextEvent, onRailEvent, onOpenMonitor, nightMode, autoDim)
        } else {
            StandByWide(data, rail, nextEvent, onRailEvent, onOpenMonitor, nightMode, autoDim)
        }
    }
}

@Composable
private fun StandByWide(
    data: DashboardData,
    rail: PowerRailModel,
    nextEvent: NextEvent,
    onRailEvent: (RailEvent) -> Unit,
    onOpenMonitor: () -> Unit,
    nightMode: Boolean,
    autoDim: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        StandByHeader()
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Column(
                modifier = Modifier.weight(1.9f),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                StandByClock(
                    time = data.time,
                    meridiem = "PM",
                    date = data.date,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    StandByWeatherCard(data, Modifier.weight(1f))
                    StandByNextEventCard(nextEvent, Modifier.weight(1f))
                }
                Spacer(Modifier.weight(1f))
                StandByStatusBar(rail, data.pcSummary.lastSeenLabel, onOpenMonitor)
            }
            PcPowerCard(
                rail = rail,
                onPrimary = { onRailEvent(RailEvent.Primary) },
                onOpenMonitor = onOpenMonitor,
                nightMode = nightMode,
                autoDim = autoDim,
                modifier = Modifier.fillMaxWidth(AppSizes.standbyCardWidthFraction),
            )
        }
    }
}

@Composable
private fun StandByStacked(
    data: DashboardData,
    rail: PowerRailModel,
    nextEvent: NextEvent,
    onRailEvent: (RailEvent) -> Unit,
    onOpenMonitor: () -> Unit,
    nightMode: Boolean,
    autoDim: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        StandByHeader()
        StandByClock(time = data.time, meridiem = "PM", date = data.date)
        StandByWeatherCard(data, Modifier.fillMaxWidth())
        StandByNextEventCard(nextEvent, Modifier.fillMaxWidth())
        PcPowerCard(
            rail = rail,
            onPrimary = { onRailEvent(RailEvent.Primary) },
            onOpenMonitor = onOpenMonitor,
            nightMode = nightMode,
            autoDim = autoDim,
            modifier = Modifier.fillMaxWidth(),
        )
        StandByStatusBar(rail, data.pcSummary.lastSeenLabel, onOpenMonitor)
    }
}

/** 顶行：品牌条 + 短分割线（左）、透视装饰（右）。 */
@Composable
private fun StandByHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.testTag("standby:brand"),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text = "A MORE FOCUSED TOMORROW",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // 概念图里品牌条下面有一段 60dp 左右的短横线。
            Box(
                modifier = Modifier
                    .size(width = AppSizes.brandDividerWidth, height = Spacing.hairline)
                    .background(DarkSurface.outline),
            )
        }
        PerspectiveMark()
    }
}

/** 天气卡（概念图左下的那张）：图标 + 大字温度 + 右侧上下两行高低温 + 天气 + 地点 + 一句话天气。 */
@Composable
private fun StandByWeatherCard(data: DashboardData, modifier: Modifier = Modifier) {
    WidgetSurface(style = WidgetStyle.Glass, modifier = modifier.testTag("standby:weather")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            WeatherIcon(
                condition = data.weather.condition,
                isNight = data.weather.isNight,
                size = AppSizes.weatherIcon,
                modifier = Modifier.testTag("standby:weather-icon"),
            )
            Text(
                text = "${data.weather.temperatureC}°",
                style = AppTypography.metricValue,
                modifier = Modifier.testTag("standby:weather-temp"),
            )
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text("↑${data.weather.highC}°", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "↓${data.weather.lowC}°",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(data.weather.condition, style = MaterialTheme.typography.bodyMedium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            AppIcon(
                kind = AppIconKind.LocationPin,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                size = AppSizes.iconSmall,
            )
            Text(
                text = data.weather.location,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("standby:weather-location"),
            )
        }
        Text(
            text = data.weather.summary,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("standby:weather-summary"),
        )
    }
}

/** Next Event 卡：日历图标 + 标题 + 右侧倒计时，然后是日程名 / 时间段 / 来源。 */
@Composable
private fun StandByNextEventCard(nextEvent: NextEvent, modifier: Modifier = Modifier) {
    WidgetSurface(style = WidgetStyle.Glass, modifier = modifier.testTag("standby:next-event")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            AppIcon(
                kind = AppIconKind.Calendar,
                tint = MaterialTheme.colorScheme.primary,
                size = AppSizes.iconMedium,
            )
            Text("Next Event", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                text = nextEvent.countdownLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("standby:next-event-countdown"),
            )
        }
        Text(
            text = nextEvent.title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.testTag("standby:next-event-title"),
        )
        Text(nextEvent.timeRange, style = MaterialTheme.typography.bodyMedium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            AppIcon(
                kind = AppIconKind.People,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                size = AppSizes.iconSmall,
            )
            Text(
                text = nextEvent.source,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("standby:next-event-source"),
            )
        }
    }
}

/**
 * PC 浮层卡（概念图右侧那张）：标题行（`My PC` + `POWER CONTROL` + `>`）→ 发光环 →
 * 动作标签 → 两枚开关瓦片。
 *
 * 环下方的文案取自 [PowerRailModel.primaryLabel] / [PowerRailModel.primaryCaption]：
 * 可唤醒时就是概念图的 `Power On` / `WAKE YOUR PC`，不可唤醒时显示当前状态
 * （`PC Online` / `AGENT CONNECTED`），不会在机器已经开机时假装还能"唤醒"。
 */
@Composable
private fun PcPowerCard(
    rail: PowerRailModel,
    onPrimary: () -> Unit,
    onOpenMonitor: () -> Unit,
    nightMode: Boolean,
    autoDim: Boolean,
    modifier: Modifier = Modifier,
) {
    WidgetSurface(style = WidgetStyle.Glass, modifier = modifier.testTag("standby:pc-card")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(rail.pcName, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = rail.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AppIcon(
                kind = AppIconKind.ChevronRight,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                size = AppSizes.iconMedium,
                modifier = Modifier.clickable(onClick = onOpenMonitor).testTag("standby:open-monitor"),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            PowerRingButton(
                enabled = rail.primaryEnabled,
                onClick = onPrimary,
                diameter = AppSizes.standbyRingDiameter,
            )
        }
        Text(
            text = rail.primaryLabel,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.fillMaxWidth().testTag("standby:power-label"),
            textAlign = TextAlign.Center,
        )
        Text(
            text = rail.primaryCaption,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().testTag("standby:power-caption"),
            textAlign = TextAlign.Center,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            ToggleTile(
                icon = AppIconKind.Moon,
                label = "Night Mode",
                value = if (nightMode) "ON" else "OFF",
                tag = "standby:night-mode",
                modifier = Modifier.weight(1f),
            )
            ToggleTile(
                icon = AppIconKind.Sun,
                label = "Auto-Dim",
                value = if (autoDim) "ACTIVE" else "IDLE",
                tag = "standby:auto-dim",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 概念图里 Night Mode / Auto-Dim 是两枚并排的描边瓦片：图标在上、名称在中、取值在下。 */
@Composable
private fun ToggleTile(
    icon: AppIconKind,
    label: String,
    value: String,
    tag: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(AppShapes.card)
            .border(Spacing.hairline, DarkSurface.outline, AppShapes.card)
            .padding(Spacing.sm)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        AppIcon(kind = icon, tint = MaterialTheme.colorScheme.onSurface, size = AppSizes.iconLarge)
        Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
    }
}

/** 底部通栏状态条：显示器图标 + 绿点 + 名称 + 状态（在线色）+ 上次可见 + `>`。 */
@Composable
private fun StandByStatusBar(rail: PowerRailModel, lastSeenLabel: String, onOpenMonitor: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(DarkSurface.card.copy(alpha = 0.5f))
            .clickable(onClick = onOpenMonitor)
            .padding(Spacing.md)
            .testTag("standby:status-bar"),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(
            kind = AppIconKind.Display,
            tint = MaterialTheme.colorScheme.primary,
            size = AppSizes.iconMedium,
        )
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
            color = LocalAccentPalette.current.onlineColor,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "Last seen $lastSeenLabel",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        AppIcon(
            kind = AppIconKind.ChevronRight,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            size = AppSizes.iconMedium,
        )
    }
}
