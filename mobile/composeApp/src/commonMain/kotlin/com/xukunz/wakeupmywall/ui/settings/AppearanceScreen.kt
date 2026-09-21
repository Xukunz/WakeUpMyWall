package com.xukunz.wakeupmywall.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import com.xukunz.wakeupmywall.core.i18n.LocalStrings
import com.xukunz.wakeupmywall.core.i18n.localize
import com.xukunz.wakeupmywall.domain.model.MonitorLayout
import com.xukunz.wakeupmywall.core.i18n.AppLanguage
import com.xukunz.wakeupmywall.core.theme.AppSizes
import com.xukunz.wakeupmywall.core.theme.AppShapes
import com.xukunz.wakeupmywall.core.theme.DarkSurface
import com.xukunz.wakeupmywall.core.theme.Spacing
import com.xukunz.wakeupmywall.core.theme.ThemeAccent
import com.xukunz.wakeupmywall.core.theme.palette
import com.xukunz.wakeupmywall.core.wallpaper.BuiltInWallpapers
import com.xukunz.wakeupmywall.domain.model.DashboardLayout
import com.xukunz.wakeupmywall.domain.model.DashboardWidget
import com.xukunz.wakeupmywall.ui.components.WidgetStyle
import com.xukunz.wakeupmywall.ui.components.Breakpoints
import com.xukunz.wakeupmywall.ui.components.thumbnail
import com.xukunz.wakeupmywall.ui.icons.AppIcon
import com.xukunz.wakeupmywall.ui.icons.AppIconKind
import org.jetbrains.compose.resources.painterResource

data class AppearanceState(
    val accent: ThemeAccent,
    val widgetStyle: WidgetStyle,
    val wallpaperId: String,
    val transparency: Float,
    val fontScale: Float,
    val widgets: List<DashboardWidget>,
    /** Monitor（硬件界面）的卡片布局：顺序 / 显隐 / 每卡信息类别。 */
    val monitorCards: List<com.xukunz.wakeupmywall.domain.model.MonitorCardConfig> =
        com.xukunz.wakeupmywall.domain.model.MonitorLayout.Default,
    /** 界面语言（spec：多语言兼容，V1 提供英文与简体中文）。 */
    val language: com.xukunz.wakeupmywall.core.i18n.AppLanguage =
        com.xukunz.wakeupmywall.core.i18n.AppLanguage.English,
)

/** Appearance 的纯函数归约器：全部返回新状态，非法输入保持原值。 */
object AppearanceReducer {

    fun toggleWidget(state: AppearanceState, id: String): AppearanceState =
        state.copy(widgets = DashboardLayout.toggle(state.widgets, id))

    fun setAccent(state: AppearanceState, accent: ThemeAccent): AppearanceState =
        state.copy(accent = accent)

    fun setWidgetStyle(state: AppearanceState, style: WidgetStyle): AppearanceState =
        state.copy(widgetStyle = style)

    /** 只接受已打包素材的壁纸 id；非法 id 保持原值（脏设置不能把背景打空）。 */
    fun setWallpaper(state: AppearanceState, id: String): AppearanceState =
        if (BuiltInWallpapers.all.any { it.id == id }) state.copy(wallpaperId = id) else state

    fun setTransparency(state: AppearanceState, value: Float): AppearanceState =
        state.copy(transparency = value.coerceIn(0f, 1f))

    fun setFontScale(state: AppearanceState, value: Float): AppearanceState =
        state.copy(fontScale = value.coerceIn(AppSizes.minFontScale, AppSizes.maxFontScale))
}

enum class AppearanceSection(val title: String) {
    Wallpaper("Wallpaper"),
    Themes("Themes & Colors"),
    Widgets("Widgets"),
    Layout("Layout"),
    Appearance("Appearance"),
}

private val layoutPresets = listOf("Default", "Compact", "Minimal")

/**
 * Wallpaper & Personalization（权威规格 F）：左导航 + 中 Live Preview + 底部四段控制条。
 * `preview` 由调用方注入，因此本组件不依赖 DashboardMode，可独立测试。
 *
 * 窄窗口（竖屏手机 / 折叠外屏）改成"导航条在上 + 预览 + 四段控制条纵向排列并滚动"：
 * 180dp 的侧导航加四段并排控制条在 412dp 宽的手机上会把预览压成一条缝。
 */
@Composable
fun AppearanceScreen(
    state: AppearanceState,
    onStateChange: (AppearanceState) -> Unit,
    modifier: Modifier = Modifier,
    selectedSection: AppearanceSection = AppearanceSection.Wallpaper,
    onSectionChange: (AppearanceSection) -> Unit = {},
    preview: @Composable (AppearanceState) -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().testTag("appearance")) {
        val inline = Breakpoints.usesInlineNav(maxWidth)
        if (inline) {
            Column(modifier = Modifier.fillMaxSize()) {
                AppearanceNav(section = selectedSection, onSectionChange = onSectionChange, compact = true)
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    PreviewFrame(state, preview, Modifier.weight(1f))
                    ControlBar(state, onStateChange, stacked = true)
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                AppearanceNav(section = selectedSection, onSectionChange = onSectionChange, compact = false)
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight().padding(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    PreviewFrame(state, preview, Modifier.weight(1f))
                    ControlBar(state, onStateChange, stacked = false)
                }
            }
        }
    }
}

/** Live Preview 框：`fontScale` 只作用于预览子树（权威规格 F：Font Scale 是预览项）。 */
@Composable
private fun PreviewFrame(
    state: AppearanceState,
    preview: @Composable (AppearanceState) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text("Live Preview — Your Home Screen", style = MaterialTheme.typography.labelSmall)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(AppShapes.card)
                .border(Spacing.hairline, DarkSurface.outline, AppShapes.card)
                .testTag("appearance:preview-frame"),
        ) {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, state.fontScale),
            ) {
                preview(state)
            }
        }
    }
}

/** 外观分区导航：宽窗口是左侧竖排列表，窄窗口是顶部横排滚动条（同一批 testTag）。 */
@Composable
private fun AppearanceNav(
    section: AppearanceSection,
    onSectionChange: (AppearanceSection) -> Unit,
    compact: Boolean,
) {
    val entries: @Composable () -> Unit = {
        AppearanceSection.entries.forEach { entry ->
            val selected = entry == section
            Text(
                text = entry.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .then(if (compact) Modifier else Modifier.fillMaxWidth())
                    .clip(AppShapes.button)
                    .background(if (selected) DarkSurface.card.copy(alpha = 0.6f) else DarkSurface.card.copy(alpha = 0f))
                    .clickable { onSectionChange(entry) }
                    .padding(Spacing.sm)
                    .testTag("appearance:nav:${entry.name}"),
            )
        }
    }

    if (compact) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                .horizontalScroll(rememberScrollState())
                .testTag("appearance:nav"),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            entries()
            ResetToDefaultRow(onReset = {})
        }
    } else {
        Column(
            modifier = Modifier
                .width(AppSizes.appearanceNavWidth)
                .fillMaxHeight()
                .padding(Spacing.md)
                .testTag("appearance:nav"),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            entries()
            HorizontalDivider(color = DarkSurface.outline)
            ResetToDefaultRow(onReset = {})
        }
    }
}

@Composable
private fun ControlBar(
    state: AppearanceState,
    onStateChange: (AppearanceState) -> Unit,
    stacked: Boolean,
) {
    val chrome = Modifier
        .fillMaxWidth()
        .clip(AppShapes.card)
        .background(DarkSurface.card.copy(alpha = 0.45f))
        .padding(Spacing.md)
        .testTag("appearance:controls")

    if (stacked) {
        // 窄窗口：四段纵向排列 + 滚动。并排会把每段压到 90dp 宽，缩略图和滑杆都没法用。
        Column(
            modifier = chrome.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            WallpaperSegment(state, onStateChange, Modifier.fillMaxWidth())
            LanguageSegment(state, onStateChange, Modifier.fillMaxWidth())
            MonitorCardsSegment(state, onStateChange, Modifier.fillMaxWidth())
            AccentSegment(state, onStateChange, Modifier.fillMaxWidth())
            StyleSegment(state, onStateChange, Modifier.fillMaxWidth())
            AppearanceSegment(state, onStateChange, Modifier.fillMaxWidth())
        }
    } else {
        // 权威规格 F 是"底部控制条四段"，四段并排；竖排堆叠会把 Live Preview 挤到看不见。
        Row(modifier = chrome, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            WallpaperSegment(state, onStateChange, Modifier.weight(1f))
            LanguageSegment(state, onStateChange, Modifier.weight(1f))
            MonitorCardsSegment(state, onStateChange, Modifier.weight(1f))
            AccentSegment(state, onStateChange, Modifier.weight(1f))
            StyleSegment(state, onStateChange, Modifier.weight(1f))
            AppearanceSegment(state, onStateChange, Modifier.weight(1f))
        }
    }
}

/**
 * Monitor 卡片编辑器：每行一张卡 —— 上移 / 下移 / 显示开关 / 信息类别循环。
 * 排序用按钮而不是拖拽：拖拽要处理长按、自动滚动、无障碍，成本远高于收益（计划期定案）。
 */
@Composable
private fun MonitorCardsSegment(
    state: AppearanceState,
    onStateChange: (AppearanceState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = strings.monitorCards,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.monitorCards.forEach { card ->
            val details = MonitorLayout.supportedDetails(card.id)
            val detail = MonitorLayout.effectiveDetail(card)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CardChip("\u2191", "appearance:card-${card.id.key}-up") {
                    onStateChange(state.copy(monitorCards = MonitorLayout.move(state.monitorCards, card.id, -1)))
                }
                CardChip("\u2193", "appearance:card-${card.id.key}-down") {
                    onStateChange(state.copy(monitorCards = MonitorLayout.move(state.monitorCards, card.id, +1)))
                }
                Text(
                    text = strings.localize(card.id.label),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (card.enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                if (details.isNotEmpty() && detail != null) {
                    CardChip(strings.localize(detail.label), "appearance:card-${card.id.key}-detail") {
                        onStateChange(state.copy(monitorCards = MonitorLayout.cycleDetail(state.monitorCards, card.id)))
                    }
                }
                CardChip(if (card.enabled) "\u2713" else "\u2014", "appearance:card-${card.id.key}-toggle") {
                    onStateChange(state.copy(monitorCards = MonitorLayout.toggle(state.monitorCards, card.id)))
                }
            }
        }
    }
}

/** 编辑器里的小按钮（上移/下移/类别/开关）：字号与颜色都走令牌。 */
@Composable
private fun CardChip(label: String, tag: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        modifier = Modifier
            .clip(AppShapes.button)
            .background(DarkSurface.card.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
            .testTag(tag),
    )
}

/**
 * 语言（V1：English / 简体中文）。和强调色一样是"立刻生效"的设置——切换后整个界面的文案换语言，
 * 包括电源栏的状态词、Monitor 的卡片标题与设置导航。
 */
@Composable
private fun LanguageSegment(
    state: AppearanceState,
    onStateChange: (AppearanceState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = strings.language,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            AppLanguage.entries.forEach { language ->
                val selected = state.language == language
                Text(
                    text = language.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .clip(AppShapes.button)
                        .background(if (selected) MaterialTheme.colorScheme.primary else DarkSurface.card.copy(alpha = 0.5f))
                        .clickable { onStateChange(state.copy(language = language)) }
                        .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                        .testTag("appearance:language-${language.code}"),
                )
            }
        }
    }
}

@Composable
private fun WallpaperSegment(
    state: AppearanceState,
    onStateChange: (AppearanceState) -> Unit,
    modifier: Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            SegmentLabel("Wallpaper", "appearance:segment:wallpaper")
            // 7 张内置壁纸 + `+ More` 在 1280dp 下会超出这一段，因此允许横向滚动，而不是压扁缩略图。
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).testTag("appearance:wallpapers"),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                BuiltInWallpapers.all.forEach { wallpaper ->
                    val selected = wallpaper.id == state.wallpaperId
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(AppShapes.button)
                            .border(
                                Spacing.hairline,
                                if (selected) MaterialTheme.colorScheme.primary else DarkSurface.outline,
                                AppShapes.button,
                            )
                            .clickable { onStateChange(AppearanceReducer.setWallpaper(state, wallpaper.id)) }
                            .padding(Spacing.xs)
                            .testTag("appearance:wallpaper:${wallpaper.id}"),
                    ) {
                        Box(modifier = Modifier.size(AppSizes.wallpaperChip)) {
                            Image(
                                painter = painterResource(wallpaper.thumbnail()),
                                contentDescription = wallpaper.label,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(AppSizes.wallpaperChip)
                                    .clip(AppShapes.button)
                                    .background(DarkSurface.outline),
                            )
                            if (selected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(AppSizes.iconSmall)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    AppIcon(
                                        kind = AppIconKind.Check,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        size = AppSizes.iconSmall,
                                        modifier = Modifier.testTag("appearance:wallpaper-selected"),
                                    )
                                }
                            }
                        }
                        Text(
                            text = wallpaper.label,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(AppSizes.wallpaperChip),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Row(
                    modifier = Modifier.align(Alignment.CenterVertically).testTag("appearance:wallpaper-more"),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("+", style = MaterialTheme.typography.labelSmall)
                    Text("More", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
}

@Composable
private fun AccentSegment(
    state: AppearanceState,
    onStateChange: (AppearanceState) -> Unit,
    modifier: Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            SegmentLabel("Theme & Accent", "appearance:segment:accent")
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ThemeAccent.entries.forEach { accent ->
                    val selected = accent == state.accent
                    Box(
                        modifier = Modifier
                            .size(AppSizes.accentDot)
                            .clip(CircleShape)
                            .background(accent.palette().accent)
                            .border(
                                Spacing.hairline,
                                if (selected) MaterialTheme.colorScheme.onSurface else DarkSurface.outline,
                                CircleShape,
                            )
                            .clickable { onStateChange(AppearanceReducer.setAccent(state, accent)) }
                            .testTag("appearance:accent:${accent.name}"),
                    )
                }
            }
            Text(
                text = state.accent.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
}

@Composable
private fun StyleSegment(
    state: AppearanceState,
    onStateChange: (AppearanceState) -> Unit,
    modifier: Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            SegmentLabel("Widget Style", "appearance:segment:style")
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                WidgetStyle.entries.forEach { style ->
                    val selected = style == state.widgetStyle
                    Text(
                        text = style.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .clip(AppShapes.button)
                            .border(
                                Spacing.hairline,
                                if (selected) MaterialTheme.colorScheme.primary else DarkSurface.outline,
                                AppShapes.button,
                            )
                            .clickable { onStateChange(AppearanceReducer.setWidgetStyle(state, style)) }
                            .padding(Spacing.xs)
                            .testTag("appearance:style:${style.name}"),
                    )
                }
            }
        }
}

@Composable
private fun AppearanceSegment(
    state: AppearanceState,
    onStateChange: (AppearanceState) -> Unit,
    modifier: Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            SegmentLabel("Appearance", "appearance:segment:appearance")
            Text("Transparency ${(state.transparency * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
            Slider(
                value = state.transparency,
                onValueChange = { onStateChange(AppearanceReducer.setTransparency(state, it)) },
                modifier = Modifier.fillMaxWidth().height(AppSizes.sliderHeight).testTag("appearance:transparency"),
            )
            Text("Font Scale ${(state.fontScale * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
            Slider(
                value = state.fontScale,
                valueRange = AppSizes.minFontScale..AppSizes.maxFontScale,
                onValueChange = { onStateChange(AppearanceReducer.setFontScale(state, it)) },
                modifier = Modifier.fillMaxWidth().height(AppSizes.sliderHeight).testTag("appearance:font-scale"),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                layoutPresets.forEach { preset ->
                    val selected = preset == "Default"
                    Text(
                        text = preset,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .clip(AppShapes.button)
                            .border(
                                Spacing.hairline,
                                if (selected) MaterialTheme.colorScheme.primary else DarkSurface.outline,
                                AppShapes.button,
                            )
                            .padding(Spacing.xs)
                            .testTag("appearance:layout:$preset"),
                    )
                }
            }
    }
}

@Composable
private fun SegmentLabel(text: String, tag: String) {
    Text(text = text, style = MaterialTheme.typography.labelSmall, modifier = Modifier.testTag(tag))
}
