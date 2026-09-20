package com.xukunz.wakeupmywall.ui.monitor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import com.xukunz.wakeupmywall.core.theme.AppSizes
import com.xukunz.wakeupmywall.core.theme.AppShapes
import com.xukunz.wakeupmywall.core.theme.AppTypography
import com.xukunz.wakeupmywall.core.theme.DarkSurface
import com.xukunz.wakeupmywall.core.theme.Spacing
import com.xukunz.wakeupmywall.ui.components.WidgetStyle
import com.xukunz.wakeupmywall.ui.components.WidgetSurface
import com.xukunz.wakeupmywall.ui.icons.AppIcon
import com.xukunz.wakeupmywall.ui.icons.AppIconKind

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
    icon: AppIconKind = AppIconKind.List,
    color: Color = Color.Unspecified,
    onOpen: (() -> Unit)? = null,
) {
    // 每个指标一种语义色（概念图实测，见 core/theme/MetricColors）；未指定时回落到强调色。
    val metricColor = if (color == Color.Unspecified) MaterialTheme.colorScheme.primary else color
    WidgetSurface(
        style = style,
        modifier = modifier.testTag("metric:$key"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(kind = icon, tint = metricColor, size = AppSizes.iconLarge)
                Column {
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = modelLine,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("metric:$key-model"),
                    )
                }
            }
            if (onOpen != null) {
                AppIcon(
                    kind = AppIconKind.ChevronRight,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    size = AppSizes.iconMedium,
                )
            }
        }

        // 权威规格 C：环形进度（中心百分数）→ sparkline → 两行数值。
        // 百分数只出现在环心；窄卡里再另起一个大号数值会把版面撑破。
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ProgressRing(
                percent = percent,
                tag = "metric:$key-ring",
                color = metricColor,
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
    color: Color = Color.Unspecified,
) {
    val fraction = (percent / 100f).coerceIn(0f, 1f)
    val accent = if (color == Color.Unspecified) MaterialTheme.colorScheme.primary else color
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

/**
 * 温度 / 风扇行（权威规格 C 行 2）：彩色圆点 + 名称 + 右侧数值，下面一条细进度条。
 * 概念图里进度条是"灰底 + 彩色填充"，不是只有填充段。
 */
@Composable
fun MetricRow(
    label: String,
    value: String,
    fraction: Float,
    tag: String,
    color: Color = Color.Unspecified,
) {
    val barColor = if (color == Color.Unspecified) MaterialTheme.colorScheme.primary else color
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            // 间距收到 xs：这一行是"点 + 名称 + 右对齐数值"，用 sm 会把 `CPU Fan` 挤成省略号。
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(AppSizes.statusDot)
                    .clip(CircleShape)
                    .background(barColor),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).testTag("$tag-label"),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag(tag),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppSizes.metricBarHeight)
                .clip(AppShapes.badge)
                .background(DarkSurface.outline)
                .testTag("$tag-bar"),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(barColor),
            )
        }
    }
}
