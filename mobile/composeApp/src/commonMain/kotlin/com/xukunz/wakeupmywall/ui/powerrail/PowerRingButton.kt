package com.xukunz.wakeupmywall.ui.powerrail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import com.xukunz.wakeupmywall.core.theme.AppSizes
import com.xukunz.wakeupmywall.core.theme.DarkSurface
import com.xukunz.wakeupmywall.core.theme.LocalAccentPalette
import com.xukunz.wakeupmywall.ui.icons.PowerGlyph

/**
 * 权威规格 A3 的主按钮，按概念图实现：**发光圆环 + 环心电源字形**，环内透出壁纸。
 *
 * 全部由代码绘制，没有位图底图，因此：
 * - 强调色 / 在线色一换，环与发光跟着换（Phase 1 的 Dynamic 强调色要靠这个才有意义）；
 * - 任意直径都不糊：StandBy 的小环（112dp）与 Rail 的大环（220dp）用同一套绘制；
 * - 状态只用一个 `enabled` 表达"按得动 / 按不动"，可用性仍然只由 `PcState.capabilities` 决定。
 *
 * 环色取 `LocalAccentPalette.onlineColor`（概念图里三个屏幕的电源环都是"电源绿"，
 * 与用于选中态的蓝色 accent 是两套语义）。
 */
@Composable
fun PowerRingButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    diameter: Dp = AppSizes.ringDiameter,
) {
    val tone = LocalAccentPalette.current.onlineColor
    val strength = if (enabled) 1f else 0.35f
    Box(
        modifier = modifier
            .size(diameter)
            .clip(CircleShape)
            .drawBehind { drawPowerRing(tone = tone, strength = strength) }
            .clickable(enabled = enabled, onClick = onClick)
            .testTag("powerrail:primary"),
        contentAlignment = Alignment.Center,
    ) {
        PowerGlyph(
            tint = tone.copy(alpha = if (enabled) 1f else 0.45f),
            size = diameter * GLYPH_RATIO,
            modifier = Modifier.testTag("powerrail:primary-glyph"),
        )
    }
}

/** 环心字形与按钮直径的比例（概念图实测：字形外接约为环内径的 55%）。 */
private const val GLYPH_RATIO = 0.42f

/**
 * 概念图的发光是"三层递减外辉光 + 一条实心环 + 环内极淡的暗色底"。
 * 用叠加描边而不是 `Modifier.blur`：模糊在 API 30 以下与部分设备上开销大且表现不一致，
 * 而这里的直径固定、层数固定，叠三层已经足够接近概念图的柔光。
 */
private fun DrawScope.drawPowerRing(tone: Color, strength: Float) {
    val stroke = AppSizes.powerRingStroke.toPx()
    val radius = size.minDimension / 2f - stroke * 3f

    // 环内薄薄一层暗色：字形压在壁纸上会糊，概念图里环心也是比背景更暗的。
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, DarkSurface.background.copy(alpha = 0.35f * strength)),
            center = center,
            radius = radius,
        ),
        radius = radius,
    )

    listOf(
        stroke * 7f to 0.04f,
        stroke * 4f to 0.07f,
        stroke * 2.2f to 0.12f,
    ).forEach { (width, alpha) ->
        drawCircle(
            color = tone.copy(alpha = alpha * strength),
            radius = radius,
            style = Stroke(width = width, cap = StrokeCap.Round),
        )
    }
    drawCircle(
        color = tone.copy(alpha = 0.92f * strength),
        radius = radius,
        style = Stroke(width = stroke, cap = StrokeCap.Round),
    )
}
