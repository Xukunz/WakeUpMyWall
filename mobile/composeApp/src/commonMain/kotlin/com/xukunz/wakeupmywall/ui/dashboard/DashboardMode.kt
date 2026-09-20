package com.xukunz.wakeupmywall.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.xukunz.wakeupmywall.ui.components.Breakpoints
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

    BoxWithConstraints(modifier = modifier.fillMaxSize().testTag("dashboard")) {
        val available = maxWidth
        val columns = Breakpoints.columns(available)
        val scrollable = Breakpoints.requiresVerticalScroll(available)

        // 概念图的三行排布：Compact（2 列）放不下，此时把三行摊平再按 2 列排并允许滚动。
        val greetingRow = buildList {
            if (WidgetType.Greeting in visible) add(Slot(1.4f) { m -> GreetingWidget(data.greeting, style, m) })
            if (decorations) add(Slot(1f) { m -> PerspectiveMark(m) })
        }
        val infoRow = buildList {
            if (WidgetType.Weather in visible) add(Slot(1f) { m -> WeatherWidget(data.weather, style, m) })
            if (WidgetType.Calendar in visible) add(Slot(1f) { m -> CalendarWidget(data.date, data.events, style, m) })
            if (WidgetType.Todo in visible) add(Slot(1f) { m -> TodoWidget(data.todos, style, m) })
            if (WidgetType.Clock in visible) add(Slot(1f) { m -> ClockWidget(data.time, data.date, style, m) })
        }
        val summaryRow = buildList {
            if (WidgetType.PcSummary in visible) {
                add(Slot(1.4f) { m -> PcSummaryWidget(data.pc, data.pcSummary, style, onPcSummaryClick, m) })
            }
            if (WidgetType.Decorative in visible) {
                add(Slot(1f) { m -> DecorativeWidget(style, m) })
            } else if (decorations) {
                add(Slot(1f) { m -> QuoteCard(m) })
            }
        }

        val rows = if (columns >= 3) {
            listOf(greetingRow, infoRow, summaryRow)
        } else {
            listOf(greetingRow + infoRow + summaryRow)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            rows.forEach { row ->
                row.chunked(columns).forEach { chunk ->
                    // 整行未被拆开时沿用概念图的权重，被拆开的部分均分。
                    val keepWeights = chunk.size == row.size
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        chunk.forEach { slot ->
                            slot.content(Modifier.weight(if (keepWeights) slot.weight else 1f))
                        }
                        repeat(columns - chunk.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            if (decorations) BrandStrip(Modifier.fillMaxWidth())
        }
    }
}

private data class Slot(
    val weight: Float,
    val content: @Composable (Modifier) -> Unit,
)
