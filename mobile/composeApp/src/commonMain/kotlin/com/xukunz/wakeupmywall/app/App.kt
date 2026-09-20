package com.xukunz.wakeupmywall.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import com.xukunz.wakeupmywall.core.connectivity.ConnectionReport
import com.xukunz.wakeupmywall.core.connectivity.ConnectivityTester
import com.xukunz.wakeupmywall.core.connectivity.TcpProbe
import com.xukunz.wakeupmywall.core.connectivity.UnsupportedTcpProbe
import com.xukunz.wakeupmywall.core.network.AgentApi
import com.xukunz.wakeupmywall.core.network.createAgentHttpClient
import com.xukunz.wakeupmywall.core.theme.WakeUpMyWallTheme
import com.xukunz.wakeupmywall.core.theme.ThemeAccent
import com.xukunz.wakeupmywall.core.storage.SettingsStorage
import com.xukunz.wakeupmywall.core.wallpaper.BuiltInWallpapers
import com.xukunz.wakeupmywall.data.device.DeviceRepository
import com.xukunz.wakeupmywall.data.settings.InMemorySettingsStorage
import com.xukunz.wakeupmywall.data.mock.MockData
import com.xukunz.wakeupmywall.domain.model.DashboardLayout
import com.xukunz.wakeupmywall.domain.model.PcDevice
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
import com.xukunz.wakeupmywall.ui.settings.AppearanceScreen
import com.xukunz.wakeupmywall.ui.settings.AppearanceState
import com.xukunz.wakeupmywall.domain.usecase.DeviceSetupInput
import com.xukunz.wakeupmywall.domain.usecase.DeviceSetupValidator
import com.xukunz.wakeupmywall.ui.dashboard.DashboardData
import com.xukunz.wakeupmywall.ui.dashboard.DashboardMode
import com.xukunz.wakeupmywall.ui.dashboard.HomeMode
import com.xukunz.wakeupmywall.ui.dashboard.HomeModeController
import com.xukunz.wakeupmywall.ui.dashboard.HomeSurface
import com.xukunz.wakeupmywall.ui.monitor.mockMetricHistory
import com.xukunz.wakeupmywall.ui.powerrail.powerRailModel
import kotlinx.coroutines.launch

@Composable
fun App(
    navigator: AppNavigator = remember { AppNavigator() },
    wallpaperId: String = BuiltInWallpapers.DefaultId,
    initialPcState: PcState = PcState.ONLINE,
    // 平台实现在 MainActivity 注入（DataStore）；测试与桌面预览默认走内存实现。
    storage: SettingsStorage = remember { InMemorySettingsStorage() },
    // 平台实现在 MainActivity 注入（java.net.Socket）；桌面没有 TCP 探测能力，如实报不可用。
    probe: TcpProbe = UnsupportedTcpProbe,
) {
    val workspace by navigator.current.collectAsState()
    var pcState by remember { mutableStateOf(initialPcState) }
    // 主页三形态的唯一状态源。Task 13 的三态切换（含 StandBy）靠它驱动，
    // 所以 StandBy 必须是可到达的：左滑 Dashboard → Monitor → StandBy，右滑逐级退回。
    val homeModeController = remember { HomeModeController() }
    val homeMode by homeModeController.current
    var settingsSection by remember { mutableStateOf(SettingsSection.DeviceSetup) }
    // 设备列表的唯一来源。`seed` 只在存储里从来没写过设备时用一次（Phase 3 会移除播种）。
    val repository = remember(storage) { DeviceRepository(storage, seed = MockData.devices) }
    val devices by repository.devices.collectAsState()
    // 这里**必须**在首帧就真的读一次 `devices`：`collectAsState` 的订阅登记在"读取发生的
    // 那个重组作用域"上。只在 Settings 分支里读，Dashboard 首帧就不会因为它变化而重组，
    // 侧栏会一直停在兜底设备上（实测过）。
    val device = remember(devices) { repository.active }
    // 空列表兜底：Phase 2 的界面还没有"一台设备都没有"的形态（Phase 3 补空状态）。
        ?: MockData.defaultDevice
    val scope = rememberCoroutineScope()
    // HTTP 客户端**留到真正要测连接时再建**：`createAgentHttpClient()` 需要一个平台引擎，
    // 桌面 target（只用于 UI 测试与设计预览）没有装引擎，提前建会让每一屏都起不来。
    val connectivity by remember(probe) {
        lazy { ConnectivityTester(probe, AgentApi(createAgentHttpClient())) }
    }
    var connectionReport by remember { mutableStateOf<ConnectionReport?>(null) }
    var isTestingConnection by remember { mutableStateOf(false) }
    // Appearance 是全应用外观的唯一来源：壁纸、强调色、卡片风格都从这里流向真正渲染的界面。
    var appearance by remember {
        mutableStateOf(
            AppearanceState(
                accent = ThemeAccent.AuroraBlue,
                widgetStyle = WidgetStyle.Glass,
                wallpaperId = wallpaperId,
                transparency = 0.7f,
                fontScale = 1f,
                widgets = DashboardLayout.default,
            ),
        )
    }
    var deviceInput by remember {
        mutableStateOf(device.toSetupInput())
    }
    val railModel = powerRailModel(pcState, device)

    LaunchedEffect(repository) { repository.load() }
    // 激活设备换人时把表单切到新设备（否则表单还停在上一次的输入上）。
    LaunchedEffect(device.id) { deviceInput = device.toSetupInput() }

    // 工作空间是入口，主页形态在 HomeSurface 内部切换：导航到 Monitor 时同步过去形态。
    LaunchedEffect(workspace) {
        when (workspace) {
            Workspace.Monitor -> homeModeController.show(HomeMode.Monitor)
            Workspace.Dashboard -> homeModeController.show(HomeMode.Dashboard)
            Workspace.Settings -> Unit
        }
    }

    WakeUpMyWallTheme(accent = appearance.accent) {
        Box(modifier = Modifier.fillMaxSize()) {
            WallpaperBackground(appearance.wallpaperId)
            // Surface 保持透明，只借用 Material3 的 contentColor，让壁纸透出来。
            // 壁纸铺满整屏（含刘海与手势条区域），但**交互内容**要躲开系统栏：
            // Android 15+ 强制 edge-to-edge，真实手机上不躲就会出现"齿轮被状态栏压住"这类问题。
            Surface(
                modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
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
                            style = appearance.widgetStyle,
                            onModeChange = homeModeController::show,
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
                                    devices = devices,
                                    report = connectionReport,
                                    isTesting = isTestingConnection,
                                    onInputChange = { deviceInput = it },
                                    onSave = {
                                        DeviceSetupValidator.validate(deviceInput).device?.let { edited ->
                                            scope.launch {
                                                repository.update(device.id, edited.copy(isDefault = device.isDefault))
                                            }
                                        }
                                    },
                                    onTestConnection = {
                                        scope.launch {
                                            isTestingConnection = true
                                            connectionReport = connectivity.test(device)
                                            isTestingConnection = false
                                        }
                                    },
                                    onSelectDevice = { id -> scope.launch { repository.setDefault(id) } },
                                    onDeleteDevice = { id -> scope.launch { repository.delete(id) } },
                                    onAddDevice = {
                                        scope.launch {
                                            repository.add(
                                                PcDevice(
                                                    id = "new-pc-${devices.size + 1}",
                                                    name = "New PC",
                                                    macAddress = null,
                                                ),
                                            )
                                        }
                                    },
                                )
                                SettingsSection.Appearance -> AppearanceScreen(
                                    state = appearance,
                                    onStateChange = { appearance = it },
                                ) { previewState ->
                                    DashboardMode(
                                        data = DashboardData(
                                            greeting = MockData.greetingText,
                                            time = MockData.clockTime,
                                            date = MockData.calendarDateLabel,
                                            weather = MockData.weather,
                                            events = MockData.calendarEvents,
                                            todos = MockData.todos,
                                            pc = railModel,
                                            pcSummary = MockData.summaryMetrics,
                                            widgets = previewState.widgets,
                                        ),
                                        style = previewState.widgetStyle,
                                        onPcSummaryClick = {},
                                    )
                                }
                                // Task 13/14 会继续填充其余 section。
                                else -> PlaceholderScreen(current.title)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * `PcDevice` → 表单输入。放在 App 层，避免 `ui/settings` 的组件知道领域模型到表单的映射细节。
 */
private fun PcDevice.toSetupInput() = DeviceSetupInput(
    name = name,
    mac = macAddress?.normalized.orEmpty(),
    ip = ipAddress.orEmpty(),
    broadcast = broadcastAddress,
    wolPort = wolPort.toString(),
    agentPort = agentPort.toString(),
    agentHost = agentHost.orEmpty(),
)
