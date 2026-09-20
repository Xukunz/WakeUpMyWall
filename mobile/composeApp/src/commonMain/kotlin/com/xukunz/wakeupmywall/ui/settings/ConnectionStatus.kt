package com.xukunz.wakeupmywall.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.xukunz.wakeupmywall.core.connectivity.ConnectionReport

/**
 * 连接结论块（`device:connection`）。只负责把 [ConnectionReport] 渲染成一句话，
 * 文案由 `ConnectivityTester` 决定——失败时永远是**具体原因**，不允许只显示 `Failed`。
 */
@Composable
fun ConnectionStatus(report: ConnectionReport?, isTesting: Boolean, modifier: Modifier = Modifier) {
    val text = when {
        isTesting -> "Testing…"
        report is ConnectionReport.AgentOnline -> "Agent online · ${report.hostname} · v${report.version}"
        report is ConnectionReport.WolOnly -> "WOL Ready · no Agent configured yet"
        report is ConnectionReport.Failed -> report.message
        else -> "Not tested yet"
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = if (report is ConnectionReport.Failed) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = modifier.testTag("device:connection"),
    )
}
