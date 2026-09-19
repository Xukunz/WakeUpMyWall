package com.xukunz.wakeupmywall.domain.model

enum class WidgetType { Greeting, Clock, Weather, Calendar, Todo, PcSummary, Decorative }

data class DashboardWidget(
    val id: String,
    val type: WidgetType,
    val enabled: Boolean = true,
    val order: Int = 0,
)

object DashboardLayout {
    val default: List<DashboardWidget> = listOf(
        DashboardWidget("greeting", WidgetType.Greeting, order = 0),
        DashboardWidget("clock", WidgetType.Clock, order = 1),
        DashboardWidget("weather", WidgetType.Weather, order = 2),
        DashboardWidget("calendar", WidgetType.Calendar, order = 3),
        DashboardWidget("todo", WidgetType.Todo, order = 4),
        DashboardWidget("pc-summary", WidgetType.PcSummary, order = 5),
        DashboardWidget("decorative", WidgetType.Decorative, enabled = false, order = 6),
    )

    fun visible(widgets: List<DashboardWidget>): List<DashboardWidget> =
        widgets.filter { it.enabled }.sortedBy { it.order }

    fun toggle(widgets: List<DashboardWidget>, id: String): List<DashboardWidget> =
        widgets.map { if (it.id == id) it.copy(enabled = it.enabled.not()) else it }
}
