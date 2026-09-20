package com.xukunz.wakeupmywall.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.xukunz.wakeupmywall.core.theme.Spacing

/**
 * Settings 左导航条目。概念图方案 A 只有 6 项，这里保留 7 项：简报 §14 的显示/省电设置
 * 必须有入口（相对概念图为刻意增加，已记录在路标的决策表里）。
 */
enum class SettingsSection(val title: String) {
    DeviceSetup("Device Setup"),
    Integrations("Integrations"),
    Display("Display & Behavior"),
    Appearance("Appearance"),
    Notifications("Notifications"),
    Backup("Backup & Sync"),
    About("About"),
}

@Composable
fun ResetToDefaultRow(modifier: Modifier = Modifier, onReset: () -> Unit) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        TextButton(onClick = onReset, modifier = Modifier.testTag("settings:reset")) {
            Text(
                text = "Reset to Default",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
