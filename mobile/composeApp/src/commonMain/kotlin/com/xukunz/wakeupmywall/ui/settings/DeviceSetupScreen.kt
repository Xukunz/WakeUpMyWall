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
import com.xukunz.wakeupmywall.core.connectivity.ConnectionReport
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
    report: ConnectionReport?,
    isTesting: Boolean,
    onInputChange: (DeviceSetupInput) -> Unit,
    onSave: () -> Unit,
    onTestConnection: () -> Unit,
    onSelectDevice: (String) -> Unit,
    onDeleteDevice: (String) -> Unit,
    onAddDevice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().testTag("device:setup")) {
        if (Breakpoints.stacksRows(maxWidth)) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                WolForm(
                    input = input,
                    result = result,
                    report = report,
                    isTesting = isTesting,
                    onInputChange = onInputChange,
                    onSave = onSave,
                    onTestConnection = onTestConnection,
                    modifier = Modifier.fillMaxWidth(),
                )
                SavedComputers(devices, onSelectDevice, onDeleteDevice, onAddDevice, Modifier.fillMaxWidth())
                IntegratedServices(Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                WolForm(
                    input = input,
                    result = result,
                    report = report,
                    isTesting = isTesting,
                    onInputChange = onInputChange,
                    onSave = onSave,
                    onTestConnection = onTestConnection,
                    modifier = Modifier.weight(1f),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    SavedComputers(devices, onSelectDevice, onDeleteDevice, onAddDevice, Modifier.fillMaxWidth())
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
    report: ConnectionReport?,
    isTesting: Boolean,
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
        // WOL Port 是用户会反复微调的旋钮（spec §7.5 写的是 "Port 步进器"），
        // 用步进器替掉文本框；Agent Port 不是旋钮，保持文本框。
        PortStepper(
            label = "WOL Port",
            value = input.wolPort,
            error = result.errors["wolPort"],
            tag = "wolPort",
            onValueChange = { onInputChange(input.copy(wolPort = it)) },
        )
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

        // 原来是静态的 "WOL Ready / Fix the highlighted fields"：测试连接的真实结论落地后，
        // 这里换成结论块，失败时给出具体原因（roadmap Phase 2 的验收要求）。
        ConnectionStatus(report = report, isTesting = isTesting, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun PortStepper(
    label: String,
    value: String,
    error: String?,
    tag: String,
    onValueChange: (String) -> Unit,
) {
    val current = value.toIntOrNull() ?: 9
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = { onValueChange((current - 1).coerceAtLeast(1).toString()) },
                modifier = Modifier.testTag("device:$tag-decrement"),
            ) { Text("-") }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("device:$tag"),
            )
            OutlinedButton(
                onClick = { onValueChange((current + 1).coerceAtMost(65535).toString()) },
                modifier = Modifier.testTag("device:$tag-increment"),
            ) { Text("+") }
        }
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("device:error:$tag"),
            )
        }
    }
}

@Composable
private fun SavedComputers(
    devices: List<PcDevice>,
    onSelectDevice: (String) -> Unit,
    onDeleteDevice: (String) -> Unit,
    onAddDevice: () -> Unit,
    modifier: Modifier,
) {
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
