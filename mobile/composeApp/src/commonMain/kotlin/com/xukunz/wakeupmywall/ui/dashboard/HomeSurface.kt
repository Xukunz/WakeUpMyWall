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
import com.xukunz.wakeupmywall.domain.model.NextEvent
import com.xukunz.wakeupmywall.data.mock.MockData
import com.xukunz.wakeupmywall.ui.RailEvent
import com.xukunz.wakeupmywall.ui.components.WidgetStyle
import com.xukunz.wakeupmywall.ui.monitor.MonitorMode
import com.xukunz.wakeupmywall.ui.monitor.PcStatusLine
import com.xukunz.wakeupmywall.ui.standby.StandByMode

enum class HomeMode { Dashboard, Monitor, StandBy }

/** 三形态的步行顺序：Dashboard → Monitor → StandBy，两端停住。 */
fun HomeMode.forward(): HomeMode =
    HomeMode.entries[(ordinal + 1).coerceAtMost(HomeMode.entries.lastIndex)]

fun HomeMode.backward(): HomeMode =
    HomeMode.entries[(ordinal - 1).coerceAtLeast(0)]

/** 主页三形态控制器：左滑前进、右滑后退，到边界停住。 */
class HomeModeController {
    private val state = mutableStateOf(HomeMode.Dashboard)
    val current: State<HomeMode> get() = state

    fun show(mode: HomeMode) {
        state.value = mode
    }

    /** `toggle` 与左滑同向：按 Dashboard → Monitor → StandBy → Dashboard 循环。 */
    fun toggle() {
        state.value = when (state.value) {
            HomeMode.Dashboard -> HomeMode.Monitor
            HomeMode.Monitor -> HomeMode.StandBy
            HomeMode.StandBy -> HomeMode.Dashboard
        }
    }

    fun onSwipeLeft() {
        state.value = state.value.forward()
    }

    fun onSwipeRight() {
        state.value = state.value.backward()
    }
}

/**
 * 左滑前进一态、右滑后退一态（阈值 60dp，累计位移判定——单次拖拽事件远小于阈值）。
 * 步子按 [forward] / [backward] 走，所以 Monitor 之下还能进 StandBy，而不是直接弹回 Dashboard。
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
    nextEvent: NextEvent = MockData.nextEvent,
) {
    val threshold = with(LocalDensity.current) { AppSizes.swipeThreshold.toPx() }
    Box(
        modifier = modifier
            .fillMaxSize()
            // mode 必须进 key：否则手势回调会一直捕获进入时的那个形态，第二次左滑就还在原地。
            .pointerInput(mode, threshold) {
                var travelled = 0f
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (travelled <= -threshold) onModeChange(mode.forward())
                        if (travelled >= threshold) onModeChange(mode.backward())
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
                deviceId = dashboard.pc.deviceId,
                onOpenDevice = { onModeChange(HomeMode.Dashboard) },
                status = PcStatusLine(
                    name = dashboard.pc.pcName,
                    stateLabel = dashboard.pc.stateLabel,
                    lastSeenLabel = dashboard.pcSummary.lastSeenLabel,
                ),
            )
            HomeMode.StandBy -> StandByMode(
                // 权威规格 D 的长日期与日历卡（B2）的短日期不是同一个字符串。
                data = dashboard.copy(date = MockData.standbyDateLabel),
                rail = dashboard.pc,
                nextEvent = nextEvent,
                onRailEvent = { event ->
                    if (event == RailEvent.Primary) onModeChange(HomeMode.Dashboard)
                },
                onOpenMonitor = { onModeChange(HomeMode.Monitor) },
            )
        }
    }
}
