package com.xukunz.wakeupmywall.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.xukunz.wakeupmywall.core.connectivity.ConnectionFailure
import com.xukunz.wakeupmywall.core.connectivity.ConnectionReport
import com.xukunz.wakeupmywall.core.connectivity.ConnectivityTester
import com.xukunz.wakeupmywall.core.connectivity.TcpProbe
import com.xukunz.wakeupmywall.core.connectivity.UnsupportedTcpProbe
import com.xukunz.wakeupmywall.core.agent.AgentTokenStore
import com.xukunz.wakeupmywall.core.agent.InMemoryAgentTokenStore
import com.xukunz.wakeupmywall.core.platform.PlatformBackHandler
import com.xukunz.wakeupmywall.core.i18n.AppLanguage
import com.xukunz.wakeupmywall.core.i18n.ChineseSimplifiedStrings
import com.xukunz.wakeupmywall.core.i18n.EnglishStrings
import com.xukunz.wakeupmywall.core.i18n.LocalStrings
import androidx.compose.runtime.CompositionLocalProvider
import com.xukunz.wakeupmywall.core.metrics.MetricRingBuffer
import com.xukunz.wakeupmywall.core.metrics.ActivityClock
import com.xukunz.wakeupmywall.core.metrics.KtorMetricsStream
import com.xukunz.wakeupmywall.core.metrics.MetricsCadence
import com.xukunz.wakeupmywall.core.metrics.MetricsGate
import com.xukunz.wakeupmywall.core.metrics.MetricsStream
import com.xukunz.wakeupmywall.core.metrics.withIdleTimeout
import com.xukunz.wakeupmywall.core.network.AgentApi
import com.xukunz.wakeupmywall.core.network.AgentMetrics
import com.xukunz.wakeupmywall.core.network.ApiResult
import com.xukunz.wakeupmywall.core.network.PowerAction
import com.xukunz.wakeupmywall.core.network.createAgentHttpClient
import com.xukunz.wakeupmywall.core.theme.WakeUpMyWallTheme
import com.xukunz.wakeupmywall.core.theme.ThemeAccent
import com.xukunz.wakeupmywall.core.storage.SettingsStorage
import com.xukunz.wakeupmywall.core.wol.UnsupportedWakeOnLanSender
import com.xukunz.wakeupmywall.core.wol.WakeOnLanSender
import com.xukunz.wakeupmywall.core.wol.WakeOutcome
import com.xukunz.wakeupmywall.core.wol.WakeSequence
import com.xukunz.wakeupmywall.core.wallpaper.BuiltInWallpapers
import com.xukunz.wakeupmywall.data.device.DeviceRepository
import com.xukunz.wakeupmywall.data.settings.InMemorySettingsStorage
import com.xukunz.wakeupmywall.data.mock.MockData
import com.xukunz.wakeupmywall.domain.model.DashboardLayout
import com.xukunz.wakeupmywall.domain.model.LiveMetrics
import com.xukunz.wakeupmywall.domain.model.MetricsSnapshot
import com.xukunz.wakeupmywall.domain.model.PcDevice
import com.xukunz.wakeupmywall.domain.model.PcEvent
import com.xukunz.wakeupmywall.domain.model.PcState
import com.xukunz.wakeupmywall.domain.model.PcSummarySnapshot
import com.xukunz.wakeupmywall.domain.usecase.LiveMetricsMapper
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
import com.xukunz.wakeupmywall.ui.settings.AgentPairingUi
import com.xukunz.wakeupmywall.domain.usecase.DeviceSetupInput
import com.xukunz.wakeupmywall.domain.usecase.DeviceSetupValidator
import com.xukunz.wakeupmywall.ui.dashboard.DashboardData
import com.xukunz.wakeupmywall.ui.dashboard.DashboardMode
import com.xukunz.wakeupmywall.ui.dashboard.HomeMode
import com.xukunz.wakeupmywall.ui.dashboard.HomeModeController
import com.xukunz.wakeupmywall.ui.dashboard.HomeSurface
import com.xukunz.wakeupmywall.ui.monitor.mockMetricHistory
import com.xukunz.wakeupmywall.ui.monitor.MetricKeys
import com.xukunz.wakeupmywall.ui.powerrail.powerRailModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

@Composable
fun App(
    navigator: AppNavigator = remember { AppNavigator() },
    wallpaperId: String = BuiltInWallpapers.DefaultId,
    initialPcState: PcState = PcState.ONLINE,
    // 平台实现在 MainActivity 注入（DataStore）；测试与桌面预览默认走内存实现。
    storage: SettingsStorage = remember { InMemorySettingsStorage() },
    // 平台实现在 MainActivity 注入（java.net.Socket）；桌面没有 TCP 探测能力，如实报不可用。
    probe: TcpProbe = UnsupportedTcpProbe,
    // 平台实现在 MainActivity 注入（java.net.DatagramSocket）；桌面如实报不可用。
    wakeSender: WakeOnLanSender = UnsupportedWakeOnLanSender,
    wakePollMillis: Long = 2_000,
    wakeBudgetMillis: Long = 60_000,
    /** 测试注入用：是否认为 Agent 已经起来。默认 null = 真去问 `GET /api/v1/status`。 */
    wakeAgentResponds: (suspend (PcDevice) -> Boolean)? = null,
    // 平台实现在 MainActivity 注入（Keystore + DataStore）；测试与预览用内存实现。
    agentTokens: AgentTokenStore = remember { InMemoryAgentTokenStore() },
    /**
     * Agent 客户端工厂。**工厂而不是实例**：桌面 target 没有 Ktor 引擎，
     * 提前构造会让每一屏都起不来（Phase 2 实测）；只有真的要发请求时才建。
     */
    agentApiFactory: () -> AgentApi = { AgentApi(createAgentHttpClient()) },
    /** 存在性轮询节奏：每 N 毫秒问一次 Agent"你在不在"。 */
    agentPollMillis: Long = 5_000,
    /** 测试注入用：直接给连接结论；默认 null = 用真实 TCP + HTTP 探测。 */
    agentReportProbe: (suspend (PcDevice) -> ConnectionReport)? = null,
    /** 指标采样节奏。spec §9 的 ACTIVE 1s / IDLE 5s 自适应属 Phase 5C，这里先固定 2 秒。 */
    metricsPollMillis: Long = 2_000,
    /** 测试注入用：直接给指标结果；默认 null = 真去问 `GET /api/v1/system`。 */
    metricsProbe: (suspend (PcDevice, String?) -> ApiResult<AgentMetrics>)? = null,
    /** 指标流（`/ws/v1/metrics`）。**工厂而不是实例**：桌面 target 没有 Ktor 引擎。 */
    metricsStreamFactory: () -> MetricsStream = { KtorMetricsStream(createAgentHttpClient()) },
    /** spec §9 的节奏：ACTIVE 1 秒 / IDLE 5 秒。 */
    cadence: MetricsCadence = MetricsCadence(),
    /** 多久没交互算 Idle（spec §9 默认 60 秒）。 */
    idleTimeoutMillis: Long = 60_000,
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
    val scope = rememberCoroutineScope()
    // HTTP 客户端在真正要发请求时才建（`ConnectivityTester` 收的是工厂）：桌面 target
    // 没有装引擎，提前构造会让每一屏都起不来（Phase 2 实测）。
    val connectivity = remember(probe) {
        ConnectivityTester(probe = probe, api = { AgentApi(createAgentHttpClient()) })
    }
    val agentApi by remember(agentApiFactory) { lazy(agentApiFactory) }
    var connectionReport by remember { mutableStateOf<ConnectionReport?>(null) }
    var isTestingConnection by remember { mutableStateOf(false) }
    // 唤醒失败/超时的解释行；成功或被重新触发时清空。
    var wakeNote by remember { mutableStateOf<String?>(null) }
    // Agent 配对状态：Token 本体只存在 Keystore/内存 store 里，这里只留一份用于发请求的副本。
    var agentToken by remember { mutableStateOf<String?>(null) }
    var isPairing by remember { mutableStateOf(false) }
    var pairingNote by remember { mutableStateOf<String?>(null) }
    // 指标采样的状态：最近一次成功的采样、连续失败次数、失败原因。
    var liveMetrics by remember { mutableStateOf<LiveMetrics?>(null) }
    // 电源动作的二次确认：Sleep/Shutdown/Restart 都是"点错就出事"的操作，必须先确认。
    var pendingPowerAction by remember { mutableStateOf<Pair<PowerAction, PcEvent>?>(null) }
    var metricsFailures by remember { mutableStateOf(0) }
    var metricsNote by remember { mutableStateOf<String?>(null) }
    // 换设备就换一组 60 点环形缓冲（曲线是"这台机器"的，不该跨设备混）。
    val metricBuffers = remember(device?.id) {
        listOf(MetricKeys.Cpu, MetricKeys.Gpu, MetricKeys.Ram, MetricKeys.Storage, MetricKeys.Network)
            .associateWith { MetricRingBuffer(capacity = 60) }
    }
    var metricHistory by remember(device?.id) { mutableStateOf<Map<String, List<Float>>>(emptyMap()) }
    // 指标流（Phase 5C）：`streamLive` 为真时 HTTP 轮询让位；Idle 判定与帧门控都是纯类。
    var streamLive by remember(device?.id) { mutableStateOf(false) }
    var isIdle by remember { mutableStateOf(false) }
    val monotonicStart = remember { TimeSource.Monotonic.markNow() }
    val activityClock = remember(idleTimeoutMillis) { ActivityClock(idleTimeoutMillis) }
    val metricsGate = remember(cadence) { MetricsGate(cadence) }
    fun elapsedMillis(): Long = monotonicStart.elapsedNow().inWholeMilliseconds
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
    var deviceInput by remember { mutableStateOf(device?.toSetupInput() ?: emptySetupInput()) }
    val wakeSequence = remember(wakeSender, wakePollMillis, wakeBudgetMillis, wakeAgentResponds, device) {
        WakeSequence(
            sender = wakeSender,
            agentResponded = wakeAgentResponds ?: { target ->
                // HTTP 客户端**在真正轮询时才建**：桌面 target 没有装引擎，提前建会崩。
                val host = target.agentHost ?: target.ipAddress
                // 探测本身失败也算"还没起来"：轮询期间不该因为一次请求异常把唤醒流程打断。
                host != null && runCatching {
                    AgentApi(createAgentHttpClient())
                        .status("http://$host:${target.agentPort}", null) is ApiResult.Success
                }.getOrDefault(false)
            },
            pollIntervalMillis = wakePollMillis,
            waitBudgetMillis = wakeBudgetMillis,
            onEvent = { event -> pcState = PcStateMachine.reduce(pcState, event) },
        )
    }
    val railModel = powerRailModel(pcState, device, wakeNote = wakeNote)

    /**
     * 探测结论 → 状态机事件（spec §4：在线与否一律由 Agent 判定）。
     * Agent 答得上就 `AgentResponded`；否则算 `AgentLost`，具体落 `WOL_READY` 还是 `OFFLINE`
     * 由状态机按"这台设备有没有 MAC/广播"决定。**只在用户点 Test Connection 时触发**：
     * 启动即探测会把 Phase 1 视觉基线的 ONLINE 形态全部改掉，那属于 Phase 5 的自动轮询。
     */
    fun applyConnectionReport(report: ConnectionReport) {
        connectionReport = report
        val event = if (report is ConnectionReport.AgentOnline) PcEvent.AgentResponded else PcEvent.AgentLost
        pcState = PcStateMachine.reduce(pcState, event, device)
    }

    /**
     * 电源命令：先确认真有可用的 Agent 与 Token，再发请求；成功后只把状态推进到瞬态
     * （`SLEEPING` / `SHUTTING_DOWN` / `RESTARTING`），**不假装 PC 已经睡着了** ——
     * 之后由存在性轮询按 Agent 在不在来收口。失败一律给出一句可读的原因。
     */
    fun runPowerAction(action: PowerAction, event: PcEvent) {
        val target = device
        scope.launch {
            wakeNote = null
            if (target == null) {
                wakeNote = "Add a device first"
                return@launch
            }
            val token = agentToken
            if (token == null) {
                wakeNote = "Pair the phone in Device Setup first (Agent section)"
                return@launch
            }

            when (val result = runCatching { agentApi.power(baseUrl(target), token, action) }.getOrNull()) {
                null -> wakeNote = "Could not reach the Agent"
                is ApiResult.Success -> pcState = PcStateMachine.reduce(pcState, event, target)
                is ApiResult.Failure -> wakeNote = result.message
            }
        }
    }

    /**
     * Quick Actions（Phase 5.5）：在 PC 上真的打开对应程序。Agent 只认白名单里的四个 id，
     * 未知 id 回 404、启动失败回 500 + 原因 —— 两种失败都在 rail 上给出可读提示（"点了没反应"是最糟的体验）。
     */
    fun runQuickAction(action: String) {
        val target = device
        scope.launch {
            wakeNote = null
            if (target == null) {
                wakeNote = "Add a device first"
                return@launch
            }
            val token = agentToken
            if (token == null) {
                wakeNote = "Pair the phone in Device Setup first (Agent section)"
                return@launch
            }

            when (val result = runCatching { agentApi.runAction(baseUrl(target), token, action) }.getOrNull()) {
                null -> wakeNote = "Could not reach the Agent"
                is ApiResult.Success -> wakeNote = "${action.replaceFirstChar { it.uppercase() }} launched on ${target.name}"
                is ApiResult.Failure -> wakeNote = result.message
            }
        }
    }

    /**
     * 启动/换设备时自己探测一次：**这是唤醒能不能用的关键**。以前必须手动点一次
     * `Test Connection` 才能从 Mock 的 `ONLINE` 走到 `WOL_READY`，用户按不动电源环就会
     * 以为 WOL 没生效。桌面没有 TCP 能力（`PROBE_UNAVAILABLE`）时保持初始状态不动，
     * 这样设计预览与截图基线也不受影响。
     */
    // 首帧 `devices` 还是空的：必须等仓库读完再判断"是不是真的没有设备"，
    // 否则会在加载完成前把状态判成 UNCONFIGURED（并把 Mock 的 ONLINE 覆盖掉）。
    var devicesLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(device?.id, probe, devicesLoaded) {
        if (!devicesLoaded) return@LaunchedEffect
        // 换设备/设备被删时，上一条唤醒解释行不再适用。
        wakeNote = null
        if (device == null) {
            pcState = PcStateMachine.reduce(pcState, PcEvent.DeviceRemoved)
            return@LaunchedEffect
        }
        val report = connectivity.test(device)
        if (report.isProbeUnavailable()) return@LaunchedEffect
        // 先"承认有设备了"（把 UNCONFIGURED 解开），再让真实探测结论决定最终状态。
        val configured = PcStateMachine.reduce(pcState, PcEvent.DeviceConfigured, device)
        connectionReport = report
        val event = if (report is ConnectionReport.AgentOnline) PcEvent.AgentResponded else PcEvent.AgentLost
        pcState = PcStateMachine.reduce(configured, event, device)
    }

    LaunchedEffect(repository) {
        repository.load()
        devicesLoaded = true
    }
    // 表单跟着仓库里那台设备走。键必须是**整个 `device`**，不能只用 `device.id`：
    // 首次播种的设备与存储里的设备共用同一个 id（`desktop-alpha`），只比 id 就漏掉了
    // "同一台设备、值不一样"这一种，表单会一直停在 Mock 值上而 Test Connection 用的是仓库值。
    LaunchedEffect(device) { device?.let { deviceInput = it.toSetupInput() } }

    // 换设备时把该设备的配对 Token 读回来（Keystore 里是密文）。
    LaunchedEffect(device?.id, agentTokens) {
        pairingNote = null
        agentToken = device?.let { agentTokens.read(it.id) }
    }

    /**
     * 存在性轮询：**这是"这台 PC 现在开着吗"的唯一来源**。
     * 每 [agentPollMillis] 探一次：Agent 应答 → `ONLINE`；主机通但 Agent 不在 → `AGENT_UNAVAILABLE`；
     * 连不上 → `AgentLost`（设备可唤醒则落 `WOL_READY`，否则 `OFFLINE`）。
     * `WAKING` 期间跳过：那段等待由 `WakeSequence` 自己的预算负责，不能被这里的 5 秒节奏打断。
     */
    LaunchedEffect(device?.id, agentPollMillis, agentReportProbe, agentToken) {
        val target = device ?: return@LaunchedEffect
        while (true) {
            if (pcState != PcState.WAKING) {
                val report = runCatching {
                    agentReportProbe?.invoke(target) ?: connectivity.test(target)
                }.getOrNull()

                val event = when {
                    report == null -> null                       // 拿不到结论（异常）→ 不动状态
                    report.isProbeUnavailable() -> null          // 桌面没有探测能力 → 不动状态
                    report is ConnectionReport.AgentOnline -> PcEvent.AgentResponded
                    report is ConnectionReport.WolOnly -> null   // 没配 Agent 主机，问不了
                    report is ConnectionReport.Failed &&
                        report.reason in unavailableReasons -> PcEvent.AgentUnavailable
                    else -> PcEvent.AgentLost
                }
                if (event != null) pcState = PcStateMachine.reduce(pcState, event, device)
            }
            delay(agentPollMillis)
        }
    }

    // 工作空间是入口，主页形态在 HomeSurface 内部切换：导航到 Monitor 时同步过去形态。
    LaunchedEffect(workspace) {
        when (workspace) {
            Workspace.Monitor -> homeModeController.show(HomeMode.Monitor)
            Workspace.Dashboard -> homeModeController.show(HomeMode.Dashboard)
            Workspace.Settings -> Unit
        }
    }

    /**
     * 指标采样（Phase 5B）：每 [metricsPollMillis] 问一次 `GET /api/v1/system`（Bearer）。
     * 成功 → 更新读数并把 CPU/GPU/RAM/存储/网络推入环形缓冲（60 点曲线）；
     * 失败 → 只记原因与连续失败次数，**不清空已有读数**；连续 [metricsStaleAfter] 次失败才算掉线，
     * 此时 UI 才把读数退成 `—` 并标注 `Last update …`（不留一张看不出真假的旧数字）。
     * 没配对不发请求：载荷要 Bearer，盲发只会白拿 401。
     */
    /** 一次采样落地的唯一入口：流式与轮询共用（两条路喂进去的东西必须一模一样）。 */
    fun applyMetricsSample(metrics: AgentMetrics) {
        val sample = LiveMetricsMapper.map(metrics)
        liveMetrics = sample
        metricsFailures = 0
        metricsNote = null
        sample.snapshot.cpuPercent?.let { metricBuffers.getValue(MetricKeys.Cpu).add(it) }
        sample.snapshot.gpuPercent?.let { metricBuffers.getValue(MetricKeys.Gpu).add(it) }
        sample.snapshot.ramPercent?.let { metricBuffers.getValue(MetricKeys.Ram).add(it) }
        sample.snapshot.storagePercent?.let { metricBuffers.getValue(MetricKeys.Storage).add(it) }
        sample.snapshot.downloadMbps?.let { metricBuffers.getValue(MetricKeys.Network).add(it) }
        metricHistory = metricBuffers.mapValues { (_, buffer) -> buffer.values() }
    }

    LaunchedEffect(device?.id, agentToken, metricsPollMillis, metricsProbe) {
        val target = device
        liveMetrics = null
        metricsFailures = 0
        metricsNote = null
        if (target == null) return@LaunchedEffect

        while (true) {
            // 流式通道活着的时候不采样：同一份数据不需要两条路同时拿。
            if (streamLive) {
                delay(metricsPollMillis)
                continue
            }
            val token = agentToken
            if (token == null) {
                metricsFailures = metricsStaleAfter
                metricsNote = "Pair the phone in Device Setup first (Agent section)"
            } else {
                when (
                    val result = runCatching {
                        metricsProbe?.invoke(target, token)
                            ?: agentApi.system(baseUrl(target), token)
                    }.getOrNull()
                ) {
                    null -> {
                        metricsFailures += 1
                        metricsNote = "Could not reach the Agent"
                    }
                    is ApiResult.Success -> applyMetricsSample(result.value)
                    is ApiResult.Failure -> {
                        metricsFailures += 1
                        metricsNote = result.message
                    }
                }
            }
            delay(metricsPollMillis)
        }
    }

    /** Idle 判定的心跳：每秒看一眼"最后一次触摸到现在多久了"（spec §9 默认 60 秒）。 */
    LaunchedEffect(activityClock) {
        while (true) {
            isIdle = activityClock.isIdle(elapsedMillis())
            delay(idleTickMillis)
        }
    }

    /**
     * 指标流（Phase 5C）：连上 `/ws/v1/metrics` 后按 1 Hz 收帧，按 [cadence] 决定哪些帧进 UI。
     * 断开 → 指数退避重连（1/2/4/8 秒封顶），期间 HTTP 轮询接手，读数不断供。
     * 只有**从来没收到过帧**（老 Agent 没有这个端点）才降级成"隔 60 秒再试"——
     * 已经证明能推的流只会按退避重连，不会把用户扔在 HTTP 上等一分钟（实测踩到过）。
     */
    LaunchedEffect(device?.id, agentToken, metricsStreamFactory, cadence) {
        val target = device ?: return@LaunchedEffect
        val token = agentToken ?: return@LaunchedEffect

        var backoff = streamBackoffStartMillis
        var failuresWithoutFrames = 0
        var everStreamed = false
        while (true) {
            var gotFrame = false
            runCatching {
                metricsStreamFactory().frames(baseUrl(target), token)
                    .withIdleTimeout(streamIdleTimeoutMillis)
                    .collect { frame ->
                        gotFrame = true
                        streamLive = true
                        if (metricsGate.accept(elapsedMillis(), isIdle)) applyMetricsSample(frame)
                    }
            }
            streamLive = false

            if (gotFrame) {
                everStreamed = true
                failuresWithoutFrames = 0
                backoff = streamBackoffStartMillis
            } else {
                failuresWithoutFrames += 1
                backoff = (backoff * 2).coerceAtMost(streamBackoffMaxMillis)
            }

            val streamingUnsupported = !everStreamed && failuresWithoutFrames >= streamFallbackAfter
            delay(if (streamingUnsupported) streamRetryMillis else backoff)
        }
    }

    // 指标只有三个来源，且**不互相冒充**：
    //   1. 有设备 + 采样新鲜 → Agent 的真实读数；
    //   2. 有设备 + 还没采到/已掉线 → 全部 `—`（拿 Mock 冒充真实机器会误导人）；
    //   3. 一台设备都没配 → 设计预览路径，继续用 Mock（Phase 1 的视觉基线）。
    val live = liveMetrics
    val hasDevice = device != null
    val metricsStale = metricsFailures >= metricsStaleAfter
    val shownMetrics = when {
        !hasDevice -> MockData.metrics
        live != null && !metricsStale -> live.snapshot
        else -> MetricsSnapshot.Unknown
    }
    val shownSummary = when {
        !hasDevice -> MockData.summaryMetrics
        live != null && !metricsStale -> live.summary
        else -> PcSummarySnapshot.Unknown
    }
    val shownIdentity = if (hasDevice) live?.identity else MockData.hardware
    val shownHistory = if (hasDevice) metricHistory else mockMetricHistory(MockData.metrics)

    // 系统返回：Settings → Dashboard；Monitor → Dashboard；StandBy → Monitor。已经在家且没别的可退时不拦。
    PlatformBackHandler(
        enabled = workspace != Workspace.Dashboard || homeMode != HomeMode.Dashboard,
    ) {
        if (workspace == Workspace.Settings) {
            navigator.goTo(Workspace.Dashboard)
        } else {
            homeModeController.onSwipeRight()
        }
    }

    WakeUpMyWallTheme(accent = appearance.accent) {
        // 语言切换只影响文案：所有用户可见字符串都从 LocalStrings 取（新增文案必须先加进表里）。
        CompositionLocalProvider(
            LocalStrings provides when (appearance.language) {
                AppLanguage.English -> EnglishStrings
                AppLanguage.ChineseSimplified -> ChineseSimplifiedStrings
            },
        ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // 任何触摸都把 ActivityClock 打回 ACTIVE（Initial 阶段只看不吃，不影响滑动手势）。
                .pointerInput(activityClock) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial)
                            activityClock.touch(elapsedMillis())
                        }
                    }
                },
        ) {
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
                                // 唤醒是"发 3 次魔包 + 轮询 Agent"的多步流程，交给 WakeSequence；
                                // 状态依旧只由状态机决定（它收到 WakeRequested/AgentResponded/WakeTimedOut）。
                                RailEvent.Primary -> {
                                    if (device == null) {
                                        // 一台设备都没配：主按钮是 `Setup PC`，直接送到 Device Setup。
                                        navigator.goTo(Workspace.Settings)
                                    } else {
                                        wakeNote = null
                                        scope.launch { wakeNote = wakeSequence.wake(device).toNote() }
                                    }
                                }
                                // 睡眠/关机/重启由 PC 上的 Agent 执行；成功后只进瞬态，
                                // 真正的结果交给存在性轮询（Agent 掉线 = 真的关机/睡眠了）。
                                RailEvent.Sleep -> pendingPowerAction = PowerAction.SLEEP to PcEvent.SleepRequested
                                RailEvent.Shutdown -> pendingPowerAction = PowerAction.SHUTDOWN to PcEvent.ShutdownRequested
                                RailEvent.Restart -> pendingPowerAction = PowerAction.RESTART to PcEvent.RestartRequested
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
                                pcSummary = shownSummary,
                                widgets = DashboardLayout.default,
                            ),
                            metrics = shownMetrics,
                            history = shownHistory,
                            style = appearance.widgetStyle,
                            onModeChange = homeModeController::show,
                            identity = shownIdentity,
                            capturedLabel = live?.capturedAtLabel,
                            metricsStale = metricsStale,
                            metricsNote = metricsNote,
                            onQuickAction = ::runQuickAction,
                        )
                    }
                    Workspace.Settings -> AppShell(
                        rail = railModel,
                        onRailEvent = { event ->
                            when (event) {
                                RailEvent.Settings -> Unit
                                RailEvent.Primary -> {
                                    if (device == null) {
                                        navigator.goTo(Workspace.Settings)
                                    } else {
                                        wakeNote = null
                                        scope.launch { wakeNote = wakeSequence.wake(device).toNote() }
                                    }
                                }
                                RailEvent.Sleep -> pendingPowerAction = PowerAction.SLEEP to PcEvent.SleepRequested
                                RailEvent.Shutdown -> pendingPowerAction = PowerAction.SHUTDOWN to PcEvent.ShutdownRequested
                                RailEvent.Restart -> pendingPowerAction = PowerAction.RESTART to PcEvent.RestartRequested
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
                                        // 没有设备时 Save 不管用（表单是空的、校验也过不了）；新增走 `+ Add Device`。
                                        val target = device
                                        DeviceSetupValidator.validate(deviceInput).device?.let { edited ->
                                            if (target != null) {
                                                scope.launch {
                                                    repository.update(target.id, edited.copy(isDefault = target.isDefault))
                                                }
                                            }
                                        }
                                    },
                                    onTestConnection = {
                                        val target = device
                                        scope.launch {
                                            isTestingConnection = true
                                            if (target != null) applyConnectionReport(connectivity.test(target))
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
                                    agent = AgentPairingUi(
                                        paired = agentToken != null,
                                        isPairing = isPairing,
                                        note = pairingNote,
                                        onPair = { code ->
                                            val target = device
                                            scope.launch {
                                                isPairing = true
                                                pairingNote = null
                                                if (target == null) {
                                                    pairingNote = "Add a device first"
                                                } else {
                                                    when (val result = agentApi.pair(baseUrl(target), code)) {
                                                        is ApiResult.Success -> {
                                                            agentTokens.write(target.id, result.value.token)
                                                            agentToken = result.value.token
                                                        }
                                                        is ApiResult.Failure -> pairingNote = result.message
                                                    }
                                                }
                                                isPairing = false
                                            }
                                        },
                                        onUnpair = {
                                            val target = device
                                            scope.launch {
                                                pairingNote = null
                                                if (target != null) agentTokens.clear(target.id)
                                                agentToken = null
                                            }
                                        },
                                    ),
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
            // 电源动作的二次确认弹窗（点错就关机/重启，代价太大）。
            pendingPowerAction?.let { (action, event) ->
                AlertDialog(
                    onDismissRequest = { pendingPowerAction = null },
                    modifier = Modifier.testTag("dialog:power"),
                    title = { Text("${action.title()} ${device?.name ?: "the PC"}?") },
                    text = { Text("This sends a ${action.title().lowercase()} command to the PC right away.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                pendingPowerAction = null
                                runPowerAction(action, event)
                            },
                            modifier = Modifier.testTag("dialog:power-confirm"),
                        ) { Text(action.title()) }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { pendingPowerAction = null },
                            modifier = Modifier.testTag("dialog:power-cancel"),
                        ) { Text("Cancel") }
                    },
                )
            }
        }
    }
        }
}

/** 二次确认弹窗里的动作名（与 spec §7.2 的 Power Rail 文案一致）。 */
private fun PowerAction.title(): String = when (this) {
    PowerAction.SLEEP -> "Sleep"
    PowerAction.SHUTDOWN -> "Shut down"
    PowerAction.RESTART -> "Restart"
    PowerAction.LOCK -> "Lock"
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

/**
 * 只在"没成功"时给用户一句话，成功时 rail 的状态行已经说明一切。
 * 失败文案带 `host:port` 或具体原因，避免"只显示 Failed"。
 */
private fun WakeOutcome.toNote(): String? = when (this) {
    is WakeOutcome.AgentOnline -> null
    is WakeOutcome.NoAnswer -> "Sent $packets wake packets — no answer from the Agent within ${formatWaited(waitedMillis)}"
    is WakeOutcome.Failed -> message
}

private fun formatWaited(millis: Long): String {
    val seconds = millis / 1_000
    return if (seconds < 1) "<1 s" else "$seconds s"
}

/** 桌面 `UnsupportedTcpProbe` 之类"这条平台没有探测能力"的结论：不要拿它改状态。 */
private fun ConnectionReport.isProbeUnavailable(): Boolean =
    this is ConnectionReport.Failed && reason == ConnectionFailure.PROBE_UNAVAILABLE

/** Agent 的基址：优先 `agentHost`，没有再退回设备 IP。 */
private fun baseUrl(device: PcDevice): String {
    val host = device.agentHost ?: device.ipAddress ?: "127.0.0.1"
    return "http://$host:${device.agentPort}"
}

/** 一台设备都没配时的空表单（字段留空，保存按钮会被校验挡住）。 */
private fun emptySetupInput() = DeviceSetupInput(
    name = "",
    mac = "",
    ip = "",
    broadcast = "192.168.1.255",
    wolPort = "9",
    agentPort = "9876",
    agentHost = "",
)

/** 主机答了、但答话的不是我们的 Agent（或它拒绝了 Token）：算 `AGENT_UNAVAILABLE` 而不是"关机"。 */
private val unavailableReasons = setOf(
    ConnectionFailure.NOT_AN_AGENT,
    ConnectionFailure.UNAUTHORIZED,
    ConnectionFailure.AGENT_ERROR,
)

/**
 * 连续多少次指标采样失败，才算"这台机器现在读不到数"。
 * 3 次 ≈ 6 秒（2 秒采样），能熬过一次抖动，又不会让用户对着过期数字发呆太久。
 */
private const val metricsStaleAfter = 3

/** 指标流的重连退避：1 → 2 → 4 → 8 秒封顶。 */
private const val streamBackoffStartMillis = 1_000L
private const val streamBackoffMaxMillis = 8_000L

/** 连续这么多次"连上但一帧都没收到"就判定流式不可用，交给 HTTP 兜底。 */
private const val streamFallbackAfter = 2

/** 兜底之后每隔这么久再试一次流（等对方升级 Agent）。 */
private const val streamRetryMillis = 60_000L

/** 这么久没收到帧就认为这条流已经死了（服务端 1 Hz 推，3 秒足够判死）。 */
private const val streamIdleTimeoutMillis = 3_000L

/** Idle 判定的心跳间隔。 */
private const val idleTickMillis = 1_000L
