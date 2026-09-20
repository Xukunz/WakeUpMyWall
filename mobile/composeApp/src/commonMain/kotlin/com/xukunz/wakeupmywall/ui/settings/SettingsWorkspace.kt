package com.xukunz.wakeupmywall.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import com.xukunz.wakeupmywall.core.theme.AppSizes
import com.xukunz.wakeupmywall.core.theme.AppShapes
import com.xukunz.wakeupmywall.core.theme.DarkSurface
import com.xukunz.wakeupmywall.core.theme.Spacing

/**
 * Settings 工作空间外壳：左固定宽度导航 + 右内容插槽。
 * 外层仍由 AppShell 提供常驻 Power Rail（见 Task 10 说明）。
 */
@Composable
fun SettingsWorkspace(
    section: SettingsSection,
    onSectionChange: (SettingsSection) -> Unit,
    modifier: Modifier = Modifier,
    onBackHome: () -> Unit = {},
    content: @Composable BoxScope.(SettingsSection) -> Unit,
) {
    Row(modifier = modifier.fillMaxSize().testTag("settings")) {
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
                val selected = entry == section
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppShapes.button)
                        .background(
                            if (selected) DarkSurface.card.copy(alpha = 0.6f) else DarkSurface.card.copy(alpha = 0f),
                        )
                        .clickable { onSectionChange(entry) }
                        .padding(Spacing.sm)
                        .testTag("settings:nav:${entry.name}"),
                )
            }

            Box(modifier = Modifier.weight(1f))
            ResetToDefaultRow(onReset = {})
        }

        Box(
            modifier = Modifier.weight(1f).fillMaxHeight().padding(Spacing.lg),
            contentAlignment = Alignment.TopStart,
        ) {
            content(section)
        }
    }
}
