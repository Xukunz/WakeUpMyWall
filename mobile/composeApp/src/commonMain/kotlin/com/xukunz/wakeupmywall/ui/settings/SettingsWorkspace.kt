package com.xukunz.wakeupmywall.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.xukunz.wakeupmywall.core.i18n.LocalStrings
import com.xukunz.wakeupmywall.core.i18n.localize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import com.xukunz.wakeupmywall.core.theme.AppSizes
import com.xukunz.wakeupmywall.core.theme.AppShapes
import com.xukunz.wakeupmywall.core.theme.DarkSurface
import com.xukunz.wakeupmywall.core.theme.Spacing
import com.xukunz.wakeupmywall.ui.components.Breakpoints

/**
 * Settings 工作空间外壳：左固定宽度导航 + 右内容插槽。
 * 外层仍由 AppShell 提供常驻 Power Rail（见 Task 10 说明）。
 *
 * 窄窗口（竖屏手机 20:9、折叠外屏 21.1:9）改成"导航条在上、内容在下"：
 * 240dp 的侧导航在 412dp 宽的手机上会把内容挤到 170dp，等于不可用。
 * 两种形态的 testTag 完全一致，界面结构变了但语义没变。
 */
@Composable
fun SettingsWorkspace(
    section: SettingsSection,
    onSectionChange: (SettingsSection) -> Unit,
    modifier: Modifier = Modifier,
    onBackHome: () -> Unit = {},
    content: @Composable BoxScope.(SettingsSection) -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().testTag("settings")) {
        if (Breakpoints.usesInlineNav(maxWidth)) {
            Column(modifier = Modifier.fillMaxSize()) {
                SettingsNavStrip(section, onSectionChange, onBackHome)
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(Spacing.md),
                    contentAlignment = Alignment.TopStart,
                ) {
                    content(section)
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                SettingsNavColumn(section, onSectionChange, onBackHome)
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight().padding(Spacing.lg),
                    contentAlignment = Alignment.TopStart,
                ) {
                    content(section)
                }
            }
        }
    }
}

@Composable
private fun SettingsNavColumn(
    section: SettingsSection,
    onSectionChange: (SettingsSection) -> Unit,
    onBackHome: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(AppSizes.settingsNavWidth)
            .fillMaxHeight()
            .padding(Spacing.lg)
            .testTag("settings:nav"),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        // 返回入口不属于"固定 6/7 项"导航列表，单独放在列表之上，避免 Settings 成为死路。
        Text(
            text = "← Home",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.button)
                .clickable(onClick = onBackHome)
                .padding(Spacing.sm)
                .testTag("settings:back"),
        )

        Text(
            text = "SETTINGS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SettingsSection.entries.forEach { entry ->
            NavEntry(entry, entry == section, Modifier.fillMaxWidth()) { onSectionChange(entry) }
        }

        Box(modifier = Modifier.weight(1f))
        ResetToDefaultRow(onReset = {})
    }
}

/** 窄窗口的导航条：横向可滚动，条目与侧栏版本逐个对应（同一批 testTag）。 */
@Composable
private fun SettingsNavStrip(
    section: SettingsSection,
    onSectionChange: (SettingsSection) -> Unit,
    onBackHome: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
            .testTag("settings:nav"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "← Home",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(AppShapes.button)
                    .border(Spacing.hairline, DarkSurface.outline, AppShapes.button)
                    .clickable(onClick = onBackHome)
                    .padding(Spacing.sm)
                    .testTag("settings:back"),
            )
            SettingsSection.entries.forEach { entry ->
                NavEntry(entry, entry == section) { onSectionChange(entry) }
            }
            ResetToDefaultRow(onReset = {})
        }
    }
}

@Composable
private fun NavEntry(
    entry: SettingsSection,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Text(
        text = entry.title,
        style = MaterialTheme.typography.bodyMedium,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .clip(AppShapes.button)
            .background(
                if (selected) DarkSurface.card.copy(alpha = 0.6f) else DarkSurface.card.copy(alpha = 0f),
            )
            .clickable(onClick = onClick)
            .padding(Spacing.sm)
            .testTag("settings:nav:${entry.name}"),
    )
}
