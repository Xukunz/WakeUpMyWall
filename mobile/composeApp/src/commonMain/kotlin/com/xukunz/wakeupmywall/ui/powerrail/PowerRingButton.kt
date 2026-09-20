package com.xukunz.wakeupmywall.ui.powerrail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import com.xukunz.wakeupmywall.core.theme.AppSizes
import com.xukunz.wakeupmywall.core.theme.Spacing

/**
 * 权威规格 A3 的环形主按钮。`⏻` 是过渡期的字形占位，视觉打磨阶段换矢量路径——
 * 不为它引入图标库（Phase 1 全局约束）。
 */
@Composable
fun PowerRingButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    diameter: Dp = AppSizes.ringDiameter,
) {
    val ringColor = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    }
    Box(
        modifier = modifier
            .size(diameter)
            .clip(CircleShape)
            .drawBehind {
                val stroke = AppSizes.ringStroke.toPx()
                val radius = size.minDimension / 2 - stroke
                drawCircle(
                    color = ringColor.copy(alpha = 0.18f),
                    radius = radius,
                    style = Stroke(stroke * 4),
                )
                drawCircle(color = ringColor, radius = radius, style = Stroke(stroke))
            }
            .clickable(enabled = enabled, onClick = onClick)
            .testTag("powerrail:primary"),
        contentAlignment = Alignment.Center,
    ) {
        Text("⏻", style = MaterialTheme.typography.displayLarge, color = ringColor)
    }
}
