package com.xukunz.wakeupmywall.ui.dashboard.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import com.xukunz.wakeupmywall.core.theme.AppShapes
import com.xukunz.wakeupmywall.core.theme.DarkSurface
import com.xukunz.wakeupmywall.core.theme.Spacing
import com.xukunz.wakeupmywall.ui.components.WidgetStyle
import com.xukunz.wakeupmywall.ui.components.WidgetSurface

/** 右上三行透视装饰（规范 D10，可关闭，默认开启）。 */
@Composable
fun PerspectiveMark(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.testTag("dashboard:perspective"),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        listOf("SAME ROOM", "DIFFERENT", "PERSPECTIVE").forEach { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.End,
            )
        }
    }
}

/** 引用装饰卡：衬线两行 + 分割线 + 标语。 */
@Composable
fun QuoteCard(modifier: Modifier = Modifier) {
    WidgetSurface(style = WidgetStyle.Minimal, modifier = modifier.testTag("dashboard:quote")) {
        Text(
            // 概念图引用卡的实际文案（Task 15 逐屏比对时确认，替换掉 Task 6 的临时占位）。
            text = "Better Tools\nA Calmer Mind",
            style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
        )
        HorizontalDivider(color = DarkSurface.outline)
        Text(
            text = "SAME PROGRESS A BRIGHTER TOMORROW",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 底部通栏品牌条。 */
@Composable
fun BrandStrip(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().testTag("dashboard:brand"),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = "A MORE FOCUSED TOMORROW",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(color = DarkSurface.outline)
    }
}

/** 纯几何装饰（默认关闭，可在 Appearance 打开）。 */
@Composable
fun DecorativeWidget(style: WidgetStyle, modifier: Modifier = Modifier) {
    WidgetSurface(style = style, modifier = modifier.testTag("dashboard:decorative")) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            listOf(1, 2, 3).forEach { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(Spacing.lg * index)
                        .background(DarkSurface.outline, AppShapes.button),
                )
            }
        }
    }
}
