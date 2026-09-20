package com.xukunz.wakeupmywall.ui.monitor

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import com.xukunz.wakeupmywall.core.theme.AppSizes

/** 60 秒指标曲线。数据不足两点时画一条基准线，绝不抛异常。 */
@Composable
fun MetricSparkline(values: List<Float>, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier.testTag("sparkline")) {
        val strokeWidth = AppSizes.sparklineStroke.toPx()
        if (values.size < 2) {
            drawLine(
                color = accent.copy(alpha = 0.3f),
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = strokeWidth,
            )
            return@Canvas
        }

        val maxValue = values.max().coerceAtLeast(1f)
        val dx = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = index * dx
            val y = size.height - (value / maxValue) * size.height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path = path, color = accent, style = Stroke(width = strokeWidth))
    }
}
