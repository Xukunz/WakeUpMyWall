package com.xukunz.wakeupmywall.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.xukunz.wakeupmywall.core.theme.Spacing
import com.xukunz.wakeupmywall.ui.components.SectionHeader
import com.xukunz.wakeupmywall.ui.components.WidgetStyle
import com.xukunz.wakeupmywall.ui.components.WidgetSurface

/** Agent 区的状态与回调，集中成一个参数，避免 DeviceSetupScreen 的签名继续膨胀。 */
data class AgentPairingUi(
    val paired: Boolean,
    val isPairing: Boolean,
    val note: String?,
    val onPair: (String) -> Unit,
    val onUnpair: () -> Unit,
)

/**
 * Device Setup 的 `Advanced / Agent` 区（spec §7.5）：显示 Agent 地址、用 PC 端打印的
 * 6 位配对码换 Token、以及解除配对。配对成功后 Token 由 App 交给 Keystore 存储。
 */
@Composable
fun AgentSection(
    host: String,
    port: String,
    ui: AgentPairingUi,
    modifier: Modifier = Modifier,
) {
    var code by remember { mutableStateOf("") }

    WidgetSurface(style = WidgetStyle.Glass, modifier = modifier.testTag("device:agent")) {
        SectionHeader(title = "Agent")
        Text(
            text = "${host.ifBlank { "no host yet" }}:${port.ifBlank { "—" }}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("device:agent-address"),
        )

        if (ui.paired) {
            Text(
                text = "Paired · token stored in Keystore",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.testTag("device:pairing-state"),
            )
            OutlinedButton(
                onClick = ui.onUnpair,
                modifier = Modifier.fillMaxWidth().testTag("device:unpair"),
            ) {
                Text("Unpair")
            }
        } else {
            OutlinedTextField(
                value = code,
                onValueChange = { input -> code = input.filter { it.isDigit() }.take(6) },
                label = { Text("Pairing code") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("device:pairing-code"),
            )
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Button(
                    onClick = { ui.onPair(code) },
                    enabled = code.length == 6 && !ui.isPairing,
                    modifier = Modifier.fillMaxWidth().testTag("device:pair"),
                ) {
                    Text(if (ui.isPairing) "Pairing…" else "Pair")
                }
                Text(
                    text = "Not paired yet",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("device:pairing-state"),
                )
            }
        }

        ui.note?.let { note ->
            Text(
                text = note,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("device:pairing-note"),
            )
        }
    }
}
