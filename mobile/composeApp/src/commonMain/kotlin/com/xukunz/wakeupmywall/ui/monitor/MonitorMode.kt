package com.xukunz.wakeupmywall.ui.monitor

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xukunz.wakeupmywall.core.theme.AppSizes
import com.xukunz.wakeupmywall.core.theme.AppShapes
import com.xukunz.wakeupmywall.core.theme.AppTypography
import com.xukunz.wakeupmywall.core.theme.DarkSurface
import com.xukunz.wakeupmywall.core.theme.LocalAccentPalette
import com.xukunz.wakeupmywall.core.theme.MetricColors
import com.xukunz.wakeupmywall.core.theme.Spacing
import com.xukunz.wakeupmywall.core.i18n.LocalStrings
import com.xukunz.wakeupmywall.domain.model.MonitorCardConfig
import com.xukunz.wakeupmywall.domain.model.MonitorCardDetail
import com.xukunz.wakeupmywall.domain.model.MonitorCardId
import com.xukunz.wakeupmywall.domain.model.MonitorLayout
import com.xukunz.wakeupmywall.domain.model.MetricsSnapshot
import com.xukunz.wakeupmywall.ui.components.Breakpoints
import com.xukunz.wakeupmywall.ui.components.PcCover
import com.xukunz.wakeupmywall.ui.components.WidgetStyle
import com.xukunz.wakeupmywall.ui.components.WidgetSurface
import com.xukunz.wakeupmywall.ui.dashboard.widgets.QuoteCard
import com.xukunz.wakeupmywall.ui.icons.AppIcon
import com.xukunz.wakeupmywall.ui.icons.AppIconKind
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object MetricKeys {
    const val Cpu = "cpu"
    const val Gpu = "gpu"
    const val Ram = "ram"
    const val Storage = "storage"
    const val Network = "network"
}

/** 行内槽位：卡片 + 它的横向权重（概念图里底行四张卡不是等宽的）。 */
private class WeightedSlot(
    val weight: Float,
    val content: @Composable (Modifier) -> Unit,
)

/** Monitor 身份卡顶部的状态行（权威规格 C1：绿点 + 名称 + 在线状态 + 上次可见）。 */
data class PcStatusLine(
    val name: String,
    val stateLabel: String,
    val lastSeenLabel: String,
)

/** Quick Actions 的动作名（权威规格 C1）。**不内置任何第三方商标图标**，用几何字形 + 文字。 */
private val quickActions = listOf("Browser", "Discord", "Steam", "Spotify")

/**
 * Monitor 形态（权威规格 C）。第一行身份卡 + 快捷动作，第二行四张指标卡 + 温度/风扇，
 * 第三行网络/运行时长/最近活动/引用卡。列数由 [Breakpoints] 决定，窄屏允许纵向滚动。
 */
@Composable
fun MonitorMode(
    metrics: MetricsSnapshot,
    history: Map<String, List<Float>>,
    style: WidgetStyle,
    modifier: Modifier = Modifier,
    identity: com.xukunz.wakeupmywall.domain.model.HardwareIdentity? = null,
    deviceId: String? = null,
    onOpenDevice: (() -> Unit)? = null,
    status: PcStatusLine? = null,
    /** 上一次成功采样的时刻（`HH:mm:ss`）；null = 还没有过成功采样。 */
    capturedLabel: String? = null,
    /** 连续多次没采到（Agent 掉线 / 未配对）：读数全部显示 `—`，并如实标注。 */
    isStale: Boolean = false,
    /** 失败原因（可读文案）；null = 不渲染。 */
    metricsNote: String? = null,
    /** Quick Actions 的点击回调（Phase 5.5）：带上动作 id（`browser` / `steam` / `spotify` / `discord`）。 */
    onQuickAction: ((String) -> Unit)? = null,
    /** 卡片布局（顺序 / 显隐 / 每卡信息类别）；默认就是 Phase 1 定下的顺序。 */
    cards: List<MonitorCardConfig> = MonitorLayout.Default,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().testTag("monitor")) {
        // maxWidth 只能在 BoxWithConstraints 作用域直接读，进入 Column 的 content lambda 后就不是这个 receiver 了。
        val available: Dp = maxWidth
        val scrollable = Breakpoints.requiresVerticalScroll(available) || maxHeight > maxWidth
        val columns = Breakpoints.columns(available)
        // 竖屏手机（20:9 / 21.1:9 的 ~412dp）里身份卡与快捷动作不再并排：并排会把四张动作瓦片压成竖条。
        val stacked = Breakpoints.stacksRows(available)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (stacked) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    DeviceIdentityCard(
                        identity, deviceId, status, style, onOpenDevice, capturedLabel, isStale, metricsNote,
                        Modifier.fillMaxWidth(),
                    )
                    QuickActionsCard(style, onQuickAction, Modifier.fillMaxWidth())
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    DeviceIdentityCard(
                        identity, deviceId, status, style, onOpenDevice, capturedLabel, isStale, metricsNote,
                        Modifier.weight(1.4f),
                    )
                    QuickActionsCard(style, onQuickAction, Modifier.weight(1f))
                }
            }

            // 列数由断点决定：主区 ≥900dp 按概念图排 5 列（墙面屏 1280dp 的主区是 921dp），否则 3 列 / 2 列（风险 R7）。
            // 卡片顺序 / 显隐 / 信息类别全部来自配置（默认与 Phase 1 布局一致）。
            val visibleCards = MonitorLayout.visible(cards)
            val metricSlots: List<@Composable (Modifier) -> Unit> = visibleCards.mapNotNull { config ->
                val slot: (@Composable (Modifier) -> Unit)? = when (config.id) {
                    MonitorCardId.Cpu -> ({ slot: Modifier ->
                        MetricCard(
                            key = MetricKeys.Cpu,
                            label = LocalStrings.current.cpu,
                            percent = metrics.cpuPercent,
                            modelLine = identity?.cpuShortName ?: "CPU",
                            values = history[MetricKeys.Cpu].orEmpty(),
                            footerPrimary = when (MonitorLayout.effectiveDetail(config)) {
                                MonitorCardDetail.Cores ->
                                    "${metrics.cpuCores.number()} ${LocalStrings.current.cores} ${metrics.cpuThreads.number()} ${LocalStrings.current.threads}"
                                MonitorCardDetail.Temp -> metrics.cpuTempC.celsiusText()
                                else -> "${metrics.cpuClockGhz.number()} GHz"
                            },
                            // 次行永远给"另一类信息"：默认（频率）时次行是核心数，选核心数时次行回到频率。
                            footerSecondary = if (MonitorLayout.effectiveDetail(config) == MonitorCardDetail.Cores) {
                                "${metrics.cpuClockGhz.number()} GHz"
                            } else {
                                "${metrics.cpuCores.number()} ${LocalStrings.current.cores} ${metrics.cpuThreads.number()} ${LocalStrings.current.threads}"
                            },
                            style = style,
                            modifier = slot,
                            icon = AppIconKind.Cpu,
                            color = MetricColors.cpu,
                        )
                    })
                    MonitorCardId.Gpu -> ({ slot: Modifier ->
                        MetricCard(
                            key = MetricKeys.Gpu,
                            label = LocalStrings.current.gpu,
                            percent = metrics.gpuPercent,
                            modelLine = identity?.gpuShortName ?: "GPU",
                            values = history[MetricKeys.Gpu].orEmpty(),
                            footerPrimary = when (MonitorLayout.effectiveDetail(config)) {
                                MonitorCardDetail.Vram ->
                                    "${metrics.vramUsedGb.number()} / ${metrics.vramTotalGb.number()} GB VRAM"
                                MonitorCardDetail.Usage -> "${metrics.gpuPercent.number()}% load"
                                else -> metrics.gpuTempC.celsiusText()
                            },
                            footerSecondary = "${metrics.vramUsedGb.number()} / ${metrics.vramTotalGb.number()} GB VRAM",
                            style = style,
                            modifier = slot,
                            icon = AppIconKind.Gpu,
                            color = MetricColors.gpu,
                        )
                    })
                    MonitorCardId.Ram -> ({ slot: Modifier ->
                        MetricCard(
                            key = MetricKeys.Ram,
                            label = LocalStrings.current.ram,
                            percent = metrics.ramPercent,
                            modelLine = identity?.ramModule ?: "RAM",
                            values = history[MetricKeys.Ram].orEmpty(),
                            footerPrimary = when (MonitorLayout.effectiveDetail(config)) {
                                MonitorCardDetail.Free -> "${metrics.ramTotalGb?.let { total -> metrics.ramUsedGb?.let { total - it } }.number()} GB ${LocalStrings.current.free}"
                                else -> "${metrics.ramUsedGb.number()} / ${metrics.ramTotalGb.number()} GB"
                            },
                            footerSecondary = LocalStrings.current.workingSet,
                            style = style,
                            modifier = slot,
                            icon = AppIconKind.Ram,
                            color = MetricColors.ram,
                        )
                    })
                    MonitorCardId.Storage -> ({ slot: Modifier ->
                        StorageCard(
                            disks = metrics.disks,
                            metrics = metrics,
                            modelLine = identity?.storageModule ?: "Storage",
                            values = history[MetricKeys.Storage].orEmpty(),
                            style = style,
                            modifier = slot,
                        )
                    })
                    MonitorCardId.Temps -> ({ slot: Modifier -> TempsAndFansCard(metrics, style, slot) })
                    else -> null
                }
                slot
            }
            metricSlots.chunked(columns).forEach { metricRow ->
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    metricRow.forEach { slot -> slot(Modifier.weight(1f)) }
                    repeat(columns - metricRow.size) { Spacer(Modifier.weight(1f)) }
                }
            }

            // 第三行同样按断点排：576dp 下强排 4 列会把网络数值和引用卡挤到无法阅读（实测踩到）。
            // 权重也不等：概念图里 Recent Activity 明显比 Network / Uptime 宽（实测 295px vs 205px），
            // 等宽会把应用名截成 `Micros…`，等于丢掉这张卡的信息量。
            val bottomSlots = visibleCards.mapNotNull { config ->
                val slot: WeightedSlot? = when (config.id) {
                    MonitorCardId.Network -> WeightedSlot(1f) { slot ->
                        NetworkCard(metrics, history[MetricKeys.Network].orEmpty(), style, slot)
                    }
                    MonitorCardId.Uptime -> WeightedSlot(0.85f) { slot -> UptimeCard(metrics, style, slot) }
                    MonitorCardId.Activity -> WeightedSlot(1.35f) { slot -> ActivityCard(metrics, style, slot) }
                    MonitorCardId.Quote -> WeightedSlot(1f) { slot -> QuoteCard(slot.testTag("monitor:quote")) }
                    else -> null
                }
                slot
            }
            bottomSlots.chunked(columns).forEach { bottomRow ->
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    bottomRow.forEach { slot -> slot.content(Modifier.weight(slot.weight)) }
                    repeat(columns - bottomRow.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun DeviceIdentityCard(
    identity: com.xukunz.wakeupmywall.domain.model.HardwareIdentity?,
    deviceId: String?,
    status: PcStatusLine?,
    style: WidgetStyle,
    onOpen: (() -> Unit)?,
    capturedLabel: String?,
    isStale: Boolean,
    metricsNote: String?,
    modifier: Modifier = Modifier,
) {
    // 身份卡右上角的 `>` 是可点返回入口（HomeSurface 用它回 Dashboard）。
    WidgetSurface(
        style = style,
        modifier = modifier
            .testTag("monitor:identity")
            .then(if (onOpen != null) Modifier.clickable(onClick = onOpen) else Modifier),
    ) {
        if (status != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 左上角返回：进 PC 卡片后用户第一反应是"返回"，而不是去找卡片右上的 `>`。
                    if (onOpen != null) {
                        Box(
                            modifier = Modifier
                                .size(AppSizes.iconLarge)
                                .clip(AppShapes.button)
                                .clickable(onClick = onOpen)
                                .testTag("monitor:back"),
                            contentAlignment = Alignment.Center,
                        ) {
                            AppIcon(
                                kind = AppIconKind.ChevronLeft,
                                tint = MaterialTheme.colorScheme.onSurface,
                                size = AppSizes.iconMedium,
                            )
                        }
                    }
                    Box(
                        Modifier
                            .size(AppSizes.statusDot)
                            .clip(CircleShape)
                            .background(LocalAccentPalette.current.onlineColor),
                    )
                    Text(
                        text = status.name,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.testTag("monitor:identity-name"),
                    )
                    Text(
                        text = status.stateLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalAccentPalette.current.onlineColor,
                        modifier = Modifier.testTag("monitor:identity-state"),
                    )
                }
                Text(
                    text = "${LocalStrings.current.lastSeen} ${status.lastSeenLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("monitor:identity-lastseen"),
                )
            }
        }
        // 指标的"新鲜度"如实写在身份卡里：掉线时它比任何数字都重要（Phase 5B）。
        // 刻意放在 status 判断之外——没有状态行时（设计预览）同样需要说明数据是什么时候采的。
        if (capturedLabel != null) {
            Text(
                text = if (isStale) "${LocalStrings.current.lastUpdate} $capturedLabel" else "${LocalStrings.current.updated} $capturedLabel",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("monitor:captured"),
            )
        }
        if (isStale) {
            Text(
                text = LocalStrings.current.noFreshMetrics,
                style = MaterialTheme.typography.labelSmall,
                color = LocalAccentPalette.current.onlineColor,
                modifier = Modifier.testTag("monitor:stale"),
            )
        }
        if (metricsNote != null) {
            Text(
                text = metricsNote,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("monitor:metrics-note"),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 概念图 C1：身份卡的缩略图是真实的机箱照片，不是占位块。
                PcCover(
                    deviceId = deviceId,
                    modifier = Modifier
                        .size(AppSizes.coverIdentityWidth, AppSizes.coverIdentityHeight)
                        .testTag("monitor:identity-cover"),
                )
                Column {
                    Text(
                        text = identity?.hostname ?: "PC",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.testTag("monitor:identity-hostname"),
                    )
                    Text(
                        text = identity?.os ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("monitor:identity-os"),
                    )
                    Text(
                        text = identity?.cpuName ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("monitor:identity-cpu"),
                    )
                    Text(
                        text = identity?.gpuName ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("monitor:identity-gpu"),
                    )
                }
            }
            AppIcon(
                kind = AppIconKind.ChevronRight,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                size = AppSizes.iconMedium,
            )
        }
    }
}

/**
 * 权威规格 C1 的 Quick Actions：四个等宽图标瓦片横排，标签在下。
 * 图标是代码绘制的通用图形——Discord / Steam / Spotify 是第三方商标，**不内置、不仿制**，
 * 用 [AppIconKind.AppTile] 表示"某个应用"，语义靠下方文字承担。
 */
@Composable
private fun QuickActionsCard(
    style: WidgetStyle,
    onAction: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    WidgetSurface(style = style, modifier = modifier.testTag("monitor:quick-actions")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(LocalStrings.current.quickActions, style = MaterialTheme.typography.bodyMedium)
            AppIcon(
                kind = AppIconKind.Plus,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                size = AppSizes.iconMedium,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            quickActions.forEach { action ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action:$action")
                        // Phase 5.5：点一下就真的在 PC 上打开对应程序（Agent 白名单里只有这四个 id）。
                        .then(
                            if (onAction == null) Modifier
                            else Modifier.clickable { onAction(action.lowercase()) },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Box(
                        modifier = Modifier
                            .size(AppSizes.iconTile)
                            .clip(AppShapes.button)
                            .background(DarkSurface.card.copy(alpha = 0.6f))
                            .border(Spacing.hairline, DarkSurface.outline, AppShapes.button),
                        contentAlignment = Alignment.Center,
                    ) {
                        AppIcon(
                            kind = if (action == "Browser") AppIconKind.Display else AppIconKind.AppTile,
                            tint = MaterialTheme.colorScheme.onSurface,
                            size = AppSizes.iconLarge,
                        )
                    }
                    Text(
                        text = action,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * Storage 卡片：**每块磁盘一页**，横向拖动或按页脚的 `‹ ●─● ›` 翻页（用户反馈"只能看 C 盘"）。
 *
 * 页数 = 磁盘数（Agent 载荷**顶层**的 `disks`）；老 Agent 没有这个字段、或机器上只有一块盘时就是单页，
 * 与原来完全一致。页脚给这一页的盘符/名称、已用/总量、剩余空间与温度，
 * 标题下的小字 `C:\ · 1/2` 说清这是第几块盘。
 */
@Composable
private fun StorageCard(
    disks: List<com.xukunz.wakeupmywall.domain.model.DiskSnapshot>,
    metrics: MetricsSnapshot,
    modelLine: String,
    values: List<Float>,
    style: WidgetStyle,
    modifier: Modifier = Modifier,
) {
    val pages = disks.ifEmpty {
        listOf(
            com.xukunz.wakeupmywall.domain.model.DiskSnapshot(
                name = modelLine,
                mount = "—",
                usagePercent = metrics.storagePercent,
                usedGb = metrics.storageUsedTb?.times(1024f),
                totalGb = metrics.storageTotalTb?.times(1024f),
                freeGb = metrics.storageFreeGb?.toFloat(),
                tempC = metrics.ssdTempC,
            )
        )
    }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val pagerScope = rememberCoroutineScope()

    WidgetSurface(style = style, modifier = modifier.testTag("metric:storage")) {
        // 标题行只放"这是哪张卡、哪块盘"：翻页控件搬到页脚（见下）。
        // 之前 ◀ ▶ 和型号文字挤在同一行，型号先把宽度吃光，按钮被量成 0 宽、在真机上直接看不见。
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(kind = AppIconKind.Storage, tint = MetricColors.storage, size = AppSizes.iconLarge)
            Column(modifier = Modifier.weight(1f)) {
                Text(LocalStrings.current.storage, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = modelLine,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("metric:storage-model"),
                )
                if (pages.size > 1) {
                    // 翻页提示（`C:\ · 1/2`）：说清这一页是哪块盘、一共有几块。
                    Text(
                        text = "${pages[pagerState.currentPage].mount} · ${pagerState.currentPage + 1}/${pages.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MetricColors.storage,
                        modifier = Modifier.testTag("metric:storage-page"),
                    )
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().testTag("metric:storage-pager"),
        ) { page ->
            val disk = pages[page]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    ProgressRing(
                        percent = disk.usagePercent,
                        tag = "metric:storage-ring-$page",
                        color = MetricColors.storage,
                        modifier = Modifier.size(AppSizes.progressRing),
                    )
                }
                MetricSparkline(values = if (page == 0) values else emptyList(), modifier = Modifier.fillMaxWidth().height(Spacing.lg))
                Text(
                    text = disk.name,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("metric:storage-disk-$page"),
                )
                Text(
                    text = "${gb(disk.usedGb)} / ${gb(disk.totalGb)} GB",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.testTag("metric:storage-used-$page"),
                )
                Text(
                    text = buildString {
                        append("${gb(disk.freeGb)} GB ${LocalStrings.current.free}")
                        if (disk.tempC != null) append(" · ${disk.tempC}°C")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("metric:storage-footer-$page"),
                )
            }
        }

        if (pages.size > 1) StoragePagerControls(pageCount = pages.size, state = pagerState, scope = pagerScope)
    }
}

/**
 * 存储卡片的翻页行：`◀ ●─● ▶`。
 *
 * 左/右按钮是手势的兜底（横向拖动在某些系统或外层容器里会被抢走，按钮永远可用），
 * 中间是页数指示：**点 + 当前页胶囊**（参考图 Apple Music 小部件下面那排；当前页拉长成一段胶囊，
 * 其余是圆点）。两边按钮先量、圆点行吃剩下的空间，所以卡片再窄也不会把按钮挤没。
 */
@Composable
private fun StoragePagerControls(
    pageCount: Int,
    state: PagerState,
    scope: CoroutineScope,
) {
    val currentPage = state.currentPage
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs).testTag("metric:storage-pager-controls"),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StoragePageButton(AppIconKind.ChevronLeft, "metric:storage-prev", currentPage > 0) {
            scope.launch { state.animateScrollToPage(currentPage - 1) }
        }
        Row(
            // `fill = false`：圆点只占自己需要的宽度，整组控件居中；卡片变窄时先挤圆点，按钮不动。
            modifier = Modifier.weight(1f, fill = false),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(pageCount) { index ->
                StoragePageDot(active = index == currentPage, index = index)
            }
        }
        StoragePageButton(AppIconKind.ChevronRight, "metric:storage-next", currentPage < pageCount - 1) {
            scope.launch { state.animateScrollToPage(currentPage + 1) }
        }
    }
}

/** 存储卡片的翻页按钮（上一块 / 下一块盘）：28dp 的圆形可按区域。 */
@Composable
private fun StoragePageButton(
    kind: AppIconKind,
    tag: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(AppSizes.iconLarge)
            .clip(AppShapes.badge)
            .background(DarkSurface.card.copy(alpha = if (enabled) 0.5f else 0.25f))
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(
            kind = kind,
            tint = if (enabled) MetricColors.storage else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            size = AppSizes.iconSmall,
        )
    }
}

/**
 * 页数指示的一格：当前页是胶囊（更宽），其余是圆点。
 * 每个点单独打标签，测试才能数出"一共几页、现在是第几页"。
 */
@Composable
private fun StoragePageDot(active: Boolean, index: Int) {
    Box(
        Modifier
            .padding(horizontal = Spacing.xs)
            .size(width = if (active) AppSizes.pageDotActiveWidth else AppSizes.pageDot, height = AppSizes.pageDot)
            .clip(CircleShape)
            .background(if (active) MetricColors.storage else DarkSurface.textSecondary.copy(alpha = 0.45f))
            .testTag("metric:storage-dot-$index"),
    )
}

/** 磁盘容量：GB 保留一位小数，整数不拖 `.0`。 */
private fun gb(value: Float?): String = value?.number() ?: UnknownReading

@Composable
private fun TempsAndFansCard(metrics: MetricsSnapshot, style: WidgetStyle, modifier: Modifier = Modifier) {
    WidgetSurface(style = style, modifier = modifier.testTag("metric:temps")) {
        SectionLine(icon = AppIconKind.Thermometer, tint = MetricColors.temperatureWarm, title = LocalStrings.current.systemTemps)
        MetricRow("CPU", metrics.cpuTempC.celsiusText(), temperatureFraction(metrics.cpuTempC), "metric:temps-cpu", MetricColors.temperature)
        MetricRow("GPU", metrics.gpuTempC.celsiusText(), temperatureFraction(metrics.gpuTempC), "metric:temps-gpu", MetricColors.temperatureWarm)
        MetricRow(
            "Motherboard",
            metrics.motherboardTempC.celsiusText(),
            temperatureFraction(metrics.motherboardTempC),
            "metric:temps-motherboard",
            MetricColors.temperature,
        )
        MetricRow("SSD", metrics.ssdTempC.celsiusText(), temperatureFraction(metrics.ssdTempC), "metric:temps-ssd", MetricColors.temperature)

        Column(
            modifier = Modifier.fillMaxWidth().testTag("metric:fans"),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            SectionLine(icon = AppIconKind.Fan, tint = MetricColors.fan, title = LocalStrings.current.fans)
            // 概念图里风扇也是"点 + 名称 + 数值 + 细条"，转速按 2000 RPM 满刻度换算。
            MetricRow("CPU Fan", "${metrics.cpuFanRpm.rpmText()} RPM", fanFraction(metrics.cpuFanRpm), "metric:fans-cpu", MetricColors.fan)
            MetricRow("GPU Fan", "${metrics.gpuFanRpm.rpmText()} RPM", fanFraction(metrics.gpuFanRpm), "metric:fans-gpu", MetricColors.fan)
            MetricRow("Case Fans", "${metrics.caseFanRpm.rpmText()} RPM", fanFraction(metrics.caseFanRpm), "metric:fans-case", MetricColors.fan)
        }
    }
}

/** 没读到温度/转速时进度条给 0（条是空的），但数值显示 `—`：两者表达的是同一件事。 */
private fun temperatureFraction(value: Int?): Float = (value ?: 0) / 100f

private fun fanFraction(rpm: Int?): Float = (rpm ?: 0) / 2000f

/** 卡片内的小节标题：图标 + 文字（概念图里 System Temps / Fans 前面各有一个图形）。 */
@Composable
private fun SectionLine(icon: AppIconKind, tint: Color, title: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(kind = icon, tint = tint, size = AppSizes.iconMedium)
        Text(title, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun NetworkCard(metrics: MetricsSnapshot, values: List<Float>, style: WidgetStyle, modifier: Modifier = Modifier) {
    WidgetSurface(style = style, modifier = modifier.testTag("metric:network")) {
        SectionLine(icon = AppIconKind.Wifi, tint = MetricColors.networkIcon, title = LocalStrings.current.network)
        Text(
            text = "↓${metrics.downloadMbps.number()} Mbps",
            style = AppTypography.metricReadout,
            modifier = Modifier.testTag("metric:network-down"),
        )
        Text(
            text = "↑${metrics.uploadMbps.number()} Mbps",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("metric:network-up"),
        )
        MetricSparkline(values = values, modifier = Modifier.fillMaxWidth().size(AppSizes.thumbnail))
    }
}

@Composable
private fun UptimeCard(metrics: MetricsSnapshot, style: WidgetStyle, modifier: Modifier = Modifier) {
    WidgetSurface(style = style, modifier = modifier.testTag("metric:uptime")) {
        SectionLine(icon = AppIconKind.Clock, tint = MetricColors.uptimeIcon, title = LocalStrings.current.uptime)
        Text(
            text = metrics.uptimeSeconds.uptimeText(),
            style = AppTypography.metricReadout,
            modifier = Modifier.testTag("metric:uptime-value"),
        )
        Text(
            text = metrics.bootDateLabel.orUnknown(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ActivityCard(metrics: MetricsSnapshot, style: WidgetStyle, modifier: Modifier = Modifier) {
    WidgetSurface(style = style, modifier = modifier.testTag("monitor:activity")) {
        SectionLine(icon = AppIconKind.List, tint = MetricColors.activityIcon, title = LocalStrings.current.recentActivity)
        if (metrics.recentActivity.isEmpty()) {
            // spec §8 把"最近应用"列为 P2：还没有真实数据时如实显示 —，不编四条假记录。
            Text(
                text = UnknownReading,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("monitor:activity-empty"),
            )
        }
        metrics.recentActivity.forEachIndexed { index, entry ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(AppSizes.statusDot)
                        .clip(CircleShape)
                        .background(MetricColors.activityDots[index % MetricColors.activityDots.size]),
                )
                Text(
                    text = entry.app,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = entry.whenLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * 由 Mock 读数生成的确定性曲线。**只在没有真实设备的预览路径上用**（Phase 5B 起，
 * 配了设备但没有采样时走 `MetricsSnapshot.Unknown`，不再用 Mock 数字冒充）。
 * 有真实采样时调用方传 `MetricRingBuffer.values()`。
 */
fun mockMetricHistory(metrics: MetricsSnapshot, samples: Int = 60): Map<String, List<Float>> {
    fun wave(base: Float, amplitude: Float): List<Float> =
        List(samples) { index -> base + amplitude * sin(index / 6f) }

    return mapOf(
        MetricKeys.Cpu to wave(metrics.cpuPercent ?: 0f, 4f),
        MetricKeys.Gpu to wave(metrics.gpuPercent ?: 0f, 6f),
        MetricKeys.Ram to wave(metrics.ramPercent ?: 0f, 2f),
        MetricKeys.Storage to List(samples) { metrics.storagePercent ?: 0f },
        MetricKeys.Network to wave(metrics.downloadMbps ?: 0f, 12f),
    )
}
