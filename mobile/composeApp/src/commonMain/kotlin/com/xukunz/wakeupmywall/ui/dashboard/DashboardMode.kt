package com.xukunz.wakeupmywall.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.xukunz.wakeupmywall.core.theme.Spacing
import com.xukunz.wakeupmywall.domain.model.CalendarEvent
import com.xukunz.wakeupmywall.domain.model.DashboardLayout
import com.xukunz.wakeupmywall.domain.model.DashboardWidget
import com.xukunz.wakeupmywall.domain.model.PcSummarySnapshot
import com.xukunz.wakeupmywall.domain.model.TodoItem
import com.xukunz.wakeupmywall.domain.model.WeatherSnapshot
import com.xukunz.wakeupmywall.domain.model.WidgetType
import com.xukunz.wakeupmywall.ui.components.WidgetStyle
import com.xukunz.wakeupmywall.ui.dashboard.widgets.BrandStrip
import com.xukunz.wakeupmywall.ui.dashboard.widgets.CalendarWidget
import com.xukunz.wakeupmywall.ui.dashboard.widgets.ClockWidget
import com.xukunz.wakeupmywall.ui.dashboard.widgets.DecorativeWidget
import com.xukunz.wakeupmywall.ui.dashboard.widgets.GreetingWidget
import com.xukunz.wakeupmywall.ui.dashboard.widgets.PcSummaryWidget
import com.xukunz.wakeupmywall.ui.dashboard.widgets.PerspectiveMark
import com.xukunz.wakeupmywall.ui.dashboard.widgets.QuoteCard
import com.xukunz.wakeupmywall.ui.dashboard.widgets.TodoWidget
import com.xukunz.wakeupmywall.ui.dashboard.widgets.WeatherWidget
import com.xukunz.wakeupmywall.ui.powerrail.PowerRailModel

data class DashboardData(
    val greeting: String,
    val time: String,
    val date: String,
    val weather: WeatherSnapshot,
    val events: List<CalendarEvent>,
    val todos: List<TodoItem>,
    val pc: PowerRailModel,
    val pcSummary: PcSummarySnapshot,
    val widgets: List<DashboardWidget>,
)

/**
 * Dashboard 形态（权威规格 B）。显隐完全由 [DashboardData.widgets] 决定，
 * 装饰层（Perspective / Quote / BrandStrip）按规范 D10 默认开启，开关接入 Appearance 后由上层控制。
 */
@Composable
fun DashboardMode(
    data: DashboardData,
    style: WidgetStyle,
    onPcSummaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    decorations: Boolean = true,
) {
    val visible = DashboardLayout.visible(data.widgets).map { it.type }.toSet()

    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.lg).testTag("dashboard"),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        // B 行 1：问候 + 透视装饰
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            if (WidgetType.Greeting in visible) {
                GreetingWidget(data.greeting, style, Modifier.weight(1.4f))
            }
            if (decorations) {
                PerspectiveMark(Modifier.weight(1f))
            }
        }

        // B 行 2：天气 · 日历 · 任务
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            if (WidgetType.Weather in visible) WeatherWidget(data.weather, style, Modifier.weight(1f))
            if (WidgetType.Calendar in visible) CalendarWidget(data.date, data.events, style, Modifier.weight(1f))
            if (WidgetType.Todo in visible) TodoWidget(data.todos, style, Modifier.weight(1f))
            if (WidgetType.Clock in visible) ClockWidget(data.time, data.date, style, Modifier.weight(1f))
        }

        // B 行 3：PC 摘要（宽）+ 引用装饰 / 几何装饰
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            if (WidgetType.PcSummary in visible) {
                PcSummaryWidget(data.pc, data.pcSummary, style, onPcSummaryClick, Modifier.weight(1.4f))
            }
            if (WidgetType.Decorative in visible) {
                DecorativeWidget(style, Modifier.weight(1f))
            } else if (decorations) {
                QuoteCard(Modifier.weight(1f))
            }
        }

        if (decorations) BrandStrip(Modifier.fillMaxWidth())
    }
}
