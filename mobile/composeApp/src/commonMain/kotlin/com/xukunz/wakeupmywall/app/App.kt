package com.xukunz.wakeupmywall.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import com.xukunz.wakeupmywall.core.theme.WakeUpMyWallTheme
import com.xukunz.wakeupmywall.core.wallpaper.BuiltInWallpapers
import com.xukunz.wakeupmywall.data.mock.MockData
import com.xukunz.wakeupmywall.domain.model.DashboardLayout
import com.xukunz.wakeupmywall.domain.model.PcEvent
import com.xukunz.wakeupmywall.domain.model.PcState
import com.xukunz.wakeupmywall.domain.usecase.PcStateMachine
import com.xukunz.wakeupmywall.ui.AppShell
import com.xukunz.wakeupmywall.ui.RailEvent
import com.xukunz.wakeupmywall.ui.components.PlaceholderScreen
import com.xukunz.wakeupmywall.ui.components.WallpaperBackground
import com.xukunz.wakeupmywall.ui.components.WidgetStyle
import com.xukunz.wakeupmywall.ui.dashboard.DashboardData
import com.xukunz.wakeupmywall.ui.dashboard.DashboardMode
import com.xukunz.wakeupmywall.ui.powerrail.powerRailModel

@Composable
fun App(
    navigator: AppNavigator = remember { AppNavigator() },
    wallpaperId: String = BuiltInWallpapers.DefaultId,
    initialPcState: PcState = PcState.ONLINE,
) {
    val workspace by navigator.current.collectAsState()
    var pcState by remember { mutableStateOf(initialPcState) }
    val device = MockData.defaultDevice
    val railModel = powerRailModel(pcState, device)

    WakeUpMyWallTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            WallpaperBackground(wallpaperId)
            // Surface 保持透明，只借用 Material3 的 contentColor，让壁纸透出来。
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
            ) {
                when (workspace) {
                    // Phase 1 只驱动本地状态机；真实 WOL / Agent 调用分别在 Phase 3 与 Phase 4。
                    Workspace.Dashboard, Workspace.Monitor -> AppShell(
                        rail = railModel,
                        onRailEvent = { event ->
                            when (event) {
                                RailEvent.Settings -> navigator.goTo(Workspace.Settings)
                                RailEvent.Primary ->
                                    pcState = PcStateMachine.reduce(pcState, PcEvent.WakeRequested)
                                RailEvent.Sleep ->
                                    pcState = PcStateMachine.reduce(pcState, PcEvent.SleepRequested)
                                RailEvent.Shutdown ->
                                    pcState = PcStateMachine.reduce(pcState, PcEvent.ShutdownRequested)
                                RailEvent.Restart ->
                                    pcState = PcStateMachine.reduce(pcState, PcEvent.RestartRequested)
                            }
                        },
                    ) {
                        when (workspace) {
                            Workspace.Dashboard -> DashboardMode(
                                data = DashboardData(
                                    greeting = MockData.greetingText,
                                    time = MockData.clockTime,
                                    date = MockData.calendarDateLabel,
                                    weather = MockData.weather,
                                    events = MockData.calendarEvents,
                                    todos = MockData.todos,
                                    pc = railModel,
                                    pcSummary = MockData.summaryMetrics,
                                    widgets = DashboardLayout.default,
                                ),
                                style = WidgetStyle.Glass,
                                onPcSummaryClick = { navigator.goTo(Workspace.Monitor) },
                            )
                            else -> PlaceholderScreen("PC Monitor")
                        }
                    }
                    Workspace.Settings -> PlaceholderScreen("Settings")
                }
            }
        }
    }
}
