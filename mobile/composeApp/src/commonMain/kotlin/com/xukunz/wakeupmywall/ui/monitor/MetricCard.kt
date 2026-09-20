package com.xukunz.wakeupmywall.ui.monitor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import com.xukunz.wakeupmywall.core.theme.AppSizes
import com.xukunz.wakeupmywall.core.theme.AppTypography
import com.xukunz.wakeupmywall.core.theme.Spacing
import com.xukunz.wakeupmywall.ui.components.WidgetStyle
import com.xukunz.wakeupmywall.ui.components.WidgetSurface

/**
 * 指标卡（权威规格 C 统一结构）：名称 + 型号小字 → 环形进度（中心百分数）→ 曲线 → 两行页脚。
 * `key` 用于生成 `metric:<key>-*` 的测试标签。
 */
@Composable
fun MetricCard(
    key: String,
    label: String,
    percent: Float,
    modelLine: String,
    values: List<Float>,
    footerPrimary: String,
    footerSecondary: String,
    style: WidgetStyle,
    modifier: Modifier = Modifier,
    onOpen: (() -> Unit)? = null,
) {
    WidgetSurface(
        style = style,
        modifier = modifier.testTag("metric:$key"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = modelLine,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("metric:$key-model"),
                )
            }
            if (onOpen != null) {
                Text(">", style = MaterialTheme.typography.bodyMedium)
            }
        }

        // 权威规格 C：环形进度（中心百分数）→ sparkline → 两行数值。
        // 百分数只出现在环心；窄卡里再另起一个大号数值会把版面撑破。
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ProgressRing(
                percent = percent,
                tag = "metric:$key-ring",
                modifier = Modifier.size(AppSizes.progressRing),
            )
        }
        MetricSparkline(values = values, modifier = Modifier.fillMaxWidth().height(Spacing.lg))

        Text(
            text = footerPrimary,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.testTag("metric:$key-footer-primary"),
        )
        Text(
            text = footerSecondary,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("metric:$key-footer-secondary"),
        )
    }
}

/** 环形进度：背景环 + 前景弧 + 中心百分数。 */
@Composable
fun ProgressRing(
    percent: Float,
    tag: String,
    modifier: Modifier = Modifier,
) {
    val fraction = (percent / 100f).coerceIn(0f, 1f)
    val accent = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.outline
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(AppSizes.progressRing)
                .drawBehind {
                    val stroke = AppSizes.ringArcStroke.toPx()
                    val inset = stroke / 2f
                    drawArc(
                        color = track,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(width = stroke),
                    )
                    drawArc(
                        color = accent,
                        startAngle = -90f,
                        sweepAngle = 360f * fraction,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                },
        )
        Text(
            text = "${percent.toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag(tag),
        )
    }
}

@Composable
fun MetricRow(label: String, value: String, fraction: Float, tag: String) {
    val barColor = MaterialTheme.colorScheme.primary
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = "$label $value",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.testTag(tag),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(AppSizes.barHeight)
                .testTag("$tag-bar")
                .drawBehind { drawRect(color = barColor) },
        )
    }
}
