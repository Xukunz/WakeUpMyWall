package com.xukunz.wakeupmywall.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.xukunz.wakeupmywall.core.theme.Spacing
import com.xukunz.wakeupmywall.domain.model.PcDevice
import com.xukunz.wakeupmywall.domain.usecase.DeviceSetupInput
import com.xukunz.wakeupmywall.domain.usecase.DeviceSetupResult
import com.xukunz.wakeupmywall.ui.components.SectionHeader
import com.xukunz.wakeupmywall.ui.components.WidgetStyle
import com.xukunz.wakeupmywall.ui.components.WidgetSurface

/**
 * Device Setup（权威规格 E）：Wake-on-LAN 配置表单 + Saved Computers + Integrated Services。
 * 表单只渲染传入的 [result]，校验逻辑全在 `DeviceSetupValidator` 里。
 */
@Composable
fun DeviceSetupScreen(
    input: DeviceSetupInput,
    result: DeviceSetupResult,
    devices: List<PcDevice>,
    onInputChange: (DeviceSetupInput) -> Unit,
    onSave: () -> Unit,
    onTestConnection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag("device:setup"),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        WidgetSurface(style = WidgetStyle.Glass, modifier = Modifier.weight(1f)) {
            SectionHeader(title = "Wake-on-LAN Configuration")

            Field("PC Name", input.name, result.errors["name"], "name", "device:error:name") {
                onInputChange(input.copy(name = it))
            }
            Field("MAC Address", input.mac, result.errors["mac"], "mac", "device:error:mac") {
                onInputChange(input.copy(mac = it))
            }
            Field("IP Address", input.ip, result.errors["ip"], "ip", "device:error:ip") {
                onInputChange(input.copy(ip = it))
            }
            Field("Broadcast IP", input.broadcast, result.errors["broadcast"], "broadcast", "device:error:broadcast") {
                onInputChange(input.copy(broadcast = it))
            }
            Field("WOL Port", input.wolPort, result.errors["wolPort"], "wolPort", "device:error:wolPort") {
                onInputChange(input.copy(wolPort = it))
            }
            Field("Agent Port", input.agentPort, result.errors["agentPort"], "agentPort", "device:error:agentPort") {
                onInputChange(input.copy(agentPort = it))
            }
            Field("Agent Host", input.agentHost, result.errors["agentHost"], "agentHost", "device:error:agentHost") {
                onInputChange(input.copy(agentHost = it))
            }

            // 两栏布局下表单列只有约 300dp，两枚按钮并排会把 "Test Connection" 挤成三行，改为纵向堆叠。
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Button(
                    onClick = onSave,
                    enabled = result.isValid,
                    modifier = Modifier.fillMaxWidth().testTag("device:save"),
                ) {
                    Text("Save")
                }
                OutlinedButton(
                    onClick = onTestConnection,
                    modifier = Modifier.fillMaxWidth().testTag("device:test"),
                ) {
                    Text("Test Connection")
                }
            }

            Text(
                text = if (result.isValid) "WOL Ready" else "Fix the highlighted fields",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("device:status"),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            WidgetSurface(style = WidgetStyle.Glass, modifier = Modifier.fillMaxWidth().testTag("device:saved")) {
                SectionHeader(title = "Saved Computers")
                devices.forEach { device ->
                    Row(
                        modifier = Modifier.fillMaxWidth().testTag("device:saved:${device.id}"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // 名称与 MAC 竖排：窄栏里横向排会把 MAC 拆成逐字符换行。
                        Column(modifier = Modifier.weight(1f)) {
                            Text(device.name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = device.macAddress?.normalized ?: "—",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (device.isDefault) {
                            Text(
                                text = "Default",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                OutlinedButton(onClick = {}, modifier = Modifier.testTag("device:add")) {
                    Text("Add Device")
                }
            }

            WidgetSurface(style = WidgetStyle.Glass, modifier = Modifier.fillMaxWidth().testTag("device:services")) {
                SectionHeader(title = "Integrated Services")
                ServiceRow(
                    label = "Weather Provider",
                    value = "OpenWeatherMap · Riverside, CA",
                    tag = "device:services:weather",
                )
                ServiceRow(
                    label = "Calendar Source",
                    value = "Android Calendar",
                    tag = "device:services:calendar",
                )
                ServiceRow(
                    label = "Task Source",
                    value = "Local",
                    tag = "device:services:tasks",
                )
            }
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    error: String?,
    name: String,
    errorTag: String,
    onValueChange: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            isError = error != null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("device:field:$name"),
        )
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag(errorTag),
            )
        }
    }
}

@Composable
private fun ServiceRow(label: String, value: String, tag: String) {
    var enabled by remember { mutableStateOf(true) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag(tag),
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = { enabled = it },
            modifier = Modifier.testTag("$tag:toggle"),
        )
    }
}
