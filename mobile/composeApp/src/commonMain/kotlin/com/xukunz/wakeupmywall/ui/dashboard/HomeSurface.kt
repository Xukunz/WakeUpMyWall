package com.xukunz.wakeupmywall.ui.dashboard

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import com.xukunz.wakeupmywall.core.theme.AppSizes
import com.xukunz.wakeupmywall.domain.model.HardwareIdentity
import com.xukunz.wakeupmywall.domain.model.MetricsSnapshot
import com.xukunz.wakeupmywall.ui.components.WidgetStyle
import com.xukunz.wakeupmywall.ui.monitor.MonitorMode

enum class HomeMode { Dashboard, Monitor }

/** 主页形态控制器。Task 13 会扩成三态（加 StandBy）。 */
class HomeModeController {
    private val state = mutableStateOf(HomeMode.Dashboard)
    val current: State<HomeMode> get() = state

    fun show(mode: HomeMode) {
        state.value = mode
    }

    fun toggle() {
        state.value = if (state.value == HomeMode.Dashboard) HomeMode.Monitor else HomeMode.Dashboard
    }

    fun onSwipeLeft() {
        state.value = HomeMode.Monitor
    }

    fun onSwipeRight() {
        state.value = HomeMode.Dashboard
    }
}

/**
 * 左滑进 Monitor、右滑回 Dashboard（阈值 60dp，累计位移判定——单次拖拽事件远小于阈值）。
 */
@Composable
fun HomeSurface(
    mode: HomeMode,
    dashboard: DashboardData,
    metrics: MetricsSnapshot,
    history: Map<String, List<Float>>,
    style: WidgetStyle,
    onModeChange: (HomeMode) -> Unit,
    modifier: Modifier = Modifier,
    identity: HardwareIdentity? = null,
) {
    val threshold = with(LocalDensity.current) { AppSizes.swipeThreshold.toPx() }
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(threshold) {
                var travelled = 0f
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (travelled <= -threshold) onModeChange(HomeMode.Monitor)
                        if (travelled >= threshold) onModeChange(HomeMode.Dashboard)
                        travelled = 0f
                    },
                    onDragCancel = { travelled = 0f },
                    onHorizontalDrag = { _, dragAmount -> travelled += dragAmount },
                )
            }
            .testTag("home:surface"),
    ) {
        when (mode) {
            HomeMode.Dashboard -> DashboardMode(
                data = dashboard,
                style = style,
                onPcSummaryClick = { onModeChange(HomeMode.Monitor) },
            )
            HomeMode.Monitor -> MonitorMode(
                metrics = metrics,
                history = history,
                style = style,
                identity = identity,
                onOpenDevice = { onModeChange(HomeMode.Dashboard) },
            )
        }
    }
}
