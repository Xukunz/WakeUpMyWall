package com.xukunz.wakeupmywall.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.xukunz.wakeupmywall.ui.settings.SettingsSection
import com.xukunz.wakeupmywall.ui.settings.SettingsWorkspace
import com.xukunz.wakeupmywall.ui.settings.DeviceSetupScreen
import com.xukunz.wakeupmywall.domain.usecase.DeviceSetupInput
import com.xukunz.wakeupmywall.domain.usecase.DeviceSetupValidator
import com.xukunz.wakeupmywall.ui.dashboard.DashboardData
import com.xukunz.wakeupmywall.ui.dashboard.HomeMode
import com.xukunz.wakeupmywall.ui.dashboard.HomeSurface
import com.xukunz.wakeupmywall.ui.monitor.mockMetricHistory
import com.xukunz.wakeupmywall.ui.powerrail.powerRailModel

@Composable
fun App(
    navigator: AppNavigator = remember { AppNavigator() },
    wallpaperId: String = BuiltInWallpapers.DefaultId,
    initialPcState: PcState = PcState.ONLINE,
) {
    val workspace by navigator.current.collectAsState()
    var pcState by remember { mutableStateOf(initialPcState) }
    var homeMode by remember { mutableStateOf(HomeMode.Dashboard) }
    var settingsSection by remember { mutableStateOf(SettingsSection.DeviceSetup) }
    val device = MockData.defaultDevice
    var deviceInput by remember {
        mutableStateOf(
            DeviceSetupInput(
                name = device.name,
                mac = device.macAddress?.normalized.orEmpty(),
                ip = device.ipAddress.orEmpty(),
                broadcast = device.broadcastAddress,
                wolPort = device.wolPort.toString(),
                agentPort = device.agentPort.toString(),
                agentHost = device.agentHost.orEmpty(),
            ),
        )
    }
    val railModel = powerRailModel(pcState, device)

    // 工作空间是入口，主页形态在 HomeSurface 内部切换：导航到 Monitor 时同步过去形态。
    LaunchedEffect(workspace) {
        when (workspace) {
            Workspace.Monitor -> homeMode = HomeMode.Monitor
            Workspace.Dashboard -> homeMode = HomeMode.Dashboard
            Workspace.Settings -> Unit
        }
    }

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
                        HomeSurface(
                            mode = homeMode,
                            dashboard = DashboardData(
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
                            metrics = MockData.metrics,
                            history = mockMetricHistory(MockData.metrics),
                            style = WidgetStyle.Glass,
                            onModeChange = { homeMode = it },
                            identity = MockData.hardware,
                        )
                    }
                    Workspace.Settings -> AppShell(
                        rail = railModel,
                        onRailEvent = { event ->
                            when (event) {
                                RailEvent.Settings -> Unit
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
                        SettingsWorkspace(
                            section = settingsSection,
                            onSectionChange = { settingsSection = it },
                            onBackHome = { navigator.goTo(Workspace.Dashboard) },
                        ) { current ->
                            when (current) {
                                SettingsSection.DeviceSetup -> DeviceSetupScreen(
                                    input = deviceInput,
                                    result = DeviceSetupValidator.validate(deviceInput),
                                    devices = MockData.devices,
                                    onInputChange = { deviceInput = it },
                                    onSave = {},
                                    onTestConnection = {},
                                )
                                // Task 12 会替换为 Appearance 与 Live Preview。
                                else -> PlaceholderScreen(current.title)
                            }
                        }
                    }
                }
            }
        }
    }
}
