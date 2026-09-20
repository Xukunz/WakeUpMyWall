package com.xukunz.wakeupmywall.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import com.xukunz.wakeupmywall.ui.components.Breakpoints
import com.xukunz.wakeupmywall.ui.components.WidgetStyle
import com.xukunz.wakeupmywall.ui.components.WidgetSurface

/**
 * Device Setup（权威规格 E）：Wake-on-LAN 配置表单 + Saved Computers + Integrated Services。
 * 表单只渲染传入的 [result]，校验逻辑全在 `DeviceSetupValidator` 里。
 *
 * 窄窗口（竖屏手机 20:9、折叠外屏 21.1:9）改成纵向堆叠：宽屏下"表单 + 右列两卡"的
 * 三块并排在 400dp 宽度里会把开关和字段挤到互相重叠。
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
    BoxWithConstraints(modifier = modifier.fillMaxSize().testTag("device:setup")) {
        if (Breakpoints.stacksRows(maxWidth)) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                WolForm(input, result, onInputChange, onSave, onTestConnection, Modifier.fillMaxWidth())
                SavedComputers(devices, Modifier.fillMaxWidth())
                IntegratedServices(Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                WolForm(input, result, onInputChange, onSave, onTestConnection, Modifier.weight(1f))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    SavedComputers(devices, Modifier.fillMaxWidth())
                    IntegratedServices(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun WolForm(
    input: DeviceSetupInput,
    result: DeviceSetupResult,
    onInputChange: (DeviceSetupInput) -> Unit,
    onSave: () -> Unit,
    onTestConnection: () -> Unit,
    modifier: Modifier,
) {
    WidgetSurface(style = WidgetStyle.Glass, modifier = modifier) {
        SectionHeader(title = "Wake-on-LAN Configuration")

        Field("PC Name", input.name, result.errors["name"], "name", "device:error:name") { onInputChange(input.copy(name = it)) }
        Field("MAC Address", input.mac, result.errors["mac"], "mac", "device:error:mac") { onInputChange(input.copy(mac = it)) }
        Field("IP Address", input.ip, result.errors["ip"], "ip", "device:error:ip") { onInputChange(input.copy(ip = it)) }
        Field("Broadcast IP", input.broadcast, result.errors["broadcast"], "broadcast", "device:error:broadcast") { onInputChange(input.copy(broadcast = it)) }
        Field("WOL Port", input.wolPort, result.errors["wolPort"], "wolPort", "device:error:wolPort") { onInputChange(input.copy(wolPort = it)) }
        Field("Agent Port", input.agentPort, result.errors["agentPort"], "agentPort", "device:error:agentPort") { onInputChange(input.copy(agentPort = it)) }
        Field("Agent Host", input.agentHost, result.errors["agentHost"], "agentHost", "device:error:agentHost") { onInputChange(input.copy(agentHost = it)) }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Button(onClick = onSave, enabled = result.isValid, modifier = Modifier.fillMaxWidth().testTag("device:save")) {
                Text("Save")
            }
            OutlinedButton(onClick = onTestConnection, modifier = Modifier.fillMaxWidth().testTag("device:test")) {
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
}

@Composable
private fun SavedComputers(devices: List<PcDevice>, modifier: Modifier) {
    WidgetSurface(style = WidgetStyle.Glass, modifier = modifier.testTag("device:saved")) {
        SectionHeader(title = "Saved Computers")
        devices.forEach { device ->
            Row(
                modifier = Modifier.fillMaxWidth().testTag("device:saved:${device.id}"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
}

@Composable
private fun IntegratedServices(modifier: Modifier) {
    WidgetSurface(style = WidgetStyle.Glass, modifier = modifier.testTag("device:services")) {
        SectionHeader(title = "Integrated Services")
        ServiceRow(label = "Weather Provider", value = "OpenWeatherMap · Riverside, CA", tag = "device:services:weather")
        ServiceRow(label = "Calendar Source", value = "Android Calendar", tag = "device:services:calendar")
        ServiceRow(label = "Task Source", value = "Local", tag = "device:services:tasks")
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
