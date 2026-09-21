package com.xukunz.wakeupmywall.ui.icons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import com.xukunz.wakeupmywall.core.theme.AppSizes
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.sin

/**
 * 代码绘制的线性图标集。
 *
 * 依据（Phase 1 全局约束）：**不引入图标库、不内置第三方商标**，缺失的图形用几何路径画出来。
 * 因此这里的每个图标都是 24×24 网格上的路径，尺寸由调用方给（`AppSizes` 令牌），颜色由主题或
 * `MetricColors` 给——文件里没有裸 dp，也没有字面量颜色，`DesignTokenDisciplineTest` 守住这条。
 *
 * 不在这里画的东西：Quick Actions 里的 Discord / Steam / Spotify 是第三方商标，
 * 用 [AppIconKind.AppTile] 这种中性图形代替，不存在"画得像"这回事。
 */
enum class AppIconKind {
    /** 设置齿轮（Rail 右上角）。 */
    Gear,

    /** 进入下一层的 `>`。 */
    ChevronRight,
    ChevronLeft,

    /** 新增（加号）。 */
    Plus,

    /** 删除（叉号）。Saved Computers 每行的删除按钮。 */
    Close,

    /** 待办已完成的对勾。 */
    Check,

    /** 位置（天气卡的地点行）。 */
    LocationPin,

    /** Sleep / 夜间模式（月牙）。 */
    Moon,

    /** Shut Down（停止方块）。 */
    StopSquare,

    /** Restart（环形箭头）。 */
    Restart,

    /** 显示器（StandBy 底部状态条、Open Browser）。 */
    Display,

    /** 第三方应用的中性替身图形。 */
    AppTile,

    Cpu,
    Gpu,
    Ram,
    Storage,
    Thermometer,
    Fan,
    Wifi,
    Clock,
    List,

    /** StandBy 的 Auto-Dim（太阳）与 Night Mode（月牙）是一对。 */
    Sun,

    /** 日程：StandBy 的 Next Event 标题行。 */
    Calendar,

    /** 参与者：日程来源行。 */
    People,
}

/** 图标绘制网格：所有坐标都写成 0–24 的网格单位，`u` 是 1 单位对应的像素数。 */
private const val Grid = 24f
private const val StrokeUnits = 1.8f

@Composable
fun AppIcon(
    kind: AppIconKind,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = AppSizes.iconMedium,
) {
    Canvas(modifier = modifier.size(size)) {
        val u = this.size.minDimension / Grid
        val stroke = Stroke(width = StrokeUnits * u, cap = StrokeCap.Round)
        when (kind) {
            AppIconKind.Gear -> drawGear(tint, u, stroke)
            AppIconKind.ChevronRight -> drawPolyline(tint, stroke, 9f to 6f, 15f to 12f, 9f to 18f)
            AppIconKind.ChevronLeft -> drawPolyline(tint, stroke, 15f to 6f, 9f to 12f, 15f to 18f)
            AppIconKind.Plus -> {
                drawPolyline(tint, stroke, 12f to 5f, 12f to 19f)
                drawPolyline(tint, stroke, 5f to 12f, 19f to 12f)
            }
            AppIconKind.Close -> {
                drawPolyline(tint, stroke, 6.8f to 6.8f, 17.2f to 17.2f)
                drawPolyline(tint, stroke, 17.2f to 6.8f, 6.8f to 17.2f)
            }
            AppIconKind.Check -> drawPolyline(tint, stroke, 5.5f to 12.5f, 10f to 17f, 18.5f to 7f)
            AppIconKind.LocationPin -> drawLocationPin(tint, u, stroke)
            AppIconKind.Moon -> drawMoon(tint, u)
            AppIconKind.StopSquare -> drawRoundRect(
                color = tint,
                topLeft = Offset(5.6f * u, 5.6f * u),
                size = Size(12.8f * u, 12.8f * u),
                cornerRadius = CornerRadius(3.4f * u),
            )
            AppIconKind.Restart -> drawRestart(tint, u, stroke)
            AppIconKind.Display -> drawDisplay(tint, u, stroke)
            AppIconKind.AppTile -> drawAppTile(tint, u, stroke)
            AppIconKind.Cpu -> drawCpu(tint, u, stroke)
            AppIconKind.Gpu -> drawGpu(tint, u, stroke)
            AppIconKind.Ram -> drawRam(tint, u, stroke)
            AppIconKind.Storage -> drawStorage(tint, u, stroke)
            AppIconKind.Thermometer -> drawThermometer(tint, u, stroke)
            AppIconKind.Fan -> drawFan(tint, u)
            AppIconKind.Wifi -> drawWifi(tint, u, stroke)
            AppIconKind.Clock -> drawClock(tint, u, stroke)
            AppIconKind.List -> drawList(tint, u, stroke)
            AppIconKind.Sun -> drawSun(tint, u, stroke)
            AppIconKind.Calendar -> drawCalendar(tint, u, stroke)
            AppIconKind.People -> drawPeople(tint, u, stroke)
        }
    }
}

/**
 * 主电源环中央的电源字形（概念图 A3 的 `⏻`）。与 [AppIcon] 分开是因为它按环的直径缩放，
 * 笔画更粗，而且要和环的发光一起画。
 */
@Composable
fun PowerGlyph(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = AppSizes.powerGlyph,
) {
    Canvas(modifier = modifier.size(size)) {
        val u = this.size.minDimension / Grid
        val stroke = Stroke(width = 2.1f * u, cap = StrokeCap.Round)
        // 竖直柄：从顶部伸到圆心。
        drawPolyline(tint, stroke, 12f to 3.4f, 12f to 12.4f)
        // 缺口圆环：从 -60° 起扫 300°，顶部正好留出柄的位置。
        drawArc(
            color = tint,
            startAngle = -60f,
            sweepAngle = 300f,
            useCenter = false,
            topLeft = Offset(12f * u - 7.6f * u, 12.6f * u - 7.6f * u),
            size = Size(15.2f * u, 15.2f * u),
            style = stroke,
        )
    }
}

private fun DrawScope.drawPolyline(color: Color, stroke: Stroke, vararg points: Pair<Float, Float>) {
    val u = size.minDimension / Grid
    val path = Path()
    points.forEachIndexed { index, (x, y) ->
        if (index == 0) path.moveTo(x * u, y * u) else path.lineTo(x * u, y * u)
    }
    drawPath(path, color = color, style = stroke)
}

/**
 * 齿轮（设置）：**圆角齿**的经典造型 —— 8 个圆乎乎的凸齿 + 中心空心圆，全描边不填充。
 * 早期版本画的是"圆 + 8 根直齿"，看起来像太阳/尖刺，与设计稿不符（用户实测指出）。
 *
 * 做法：沿极坐标采样半径 `r(θ) = 内径 + (外径 - 内径) * smoothstep(cos(8θ))`，
 * 连成闭合路径再描边；smoothstep 保证齿是圆角而不是尖角。
 */
private fun DrawScope.drawGear(tint: Color, u: Float, stroke: Stroke) {
    val teeth = 8
    val inner = 7.0f
    val outer = 9.6f
    val steps = 240
    val path = Path()

    for (step in 0..steps) {
        val angle = (step.toFloat() / steps) * 2f * PI.toFloat()
        val lobe = (cos(teeth * angle) + 1f) / 2f          // 0..1
        val smooth = lobe * lobe * (3f - 2f * lobe)        // smoothstep：圆角齿
        val radius = (inner + (outer - inner) * smooth) * u
        val x = center.x + cos(angle) * radius
        val y = center.y + sin(angle) * radius
        if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color = tint, style = stroke)
    // 中心空心圆（描边），与设计稿一致的"圆环 + 齿轮"。
    drawCircle(tint, radius = 3.2f * u, center = center, style = stroke)
}

private fun DrawScope.drawLocationPin(tint: Color, u: Float, stroke: Stroke) {
    val head = Offset(12f * u, 10f * u)
    drawCircle(tint, radius = 4.6f * u, center = head, style = stroke)
    drawPolyline(tint, stroke, 8.7f to 13.3f, 12f to 21.4f)
    drawPolyline(tint, stroke, 15.3f to 13.3f, 12f to 21.4f)
}

private fun DrawScope.drawMoon(tint: Color, u: Float) {
    val outer = Path().apply { addOval(Rect(center = center, radius = 8.6f * u)) }
    val bite = Path().apply { addOval(Rect(center = Offset(17.2f * u, 8.2f * u), radius = 8.2f * u)) }
    val crescent = Path()
    crescent.op(outer, bite, PathOperation.Difference)
    drawPath(crescent, color = tint)
}

private fun DrawScope.drawRestart(tint: Color, u: Float, stroke: Stroke) {
    val radius = 7.4f * u
    val startAngle = -50f
    drawArc(
        color = tint,
        startAngle = startAngle,
        sweepAngle = 300f,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = stroke,
    )
    // 箭头落在圆弧起点（-50°）上，指向顺圆周方向。
    val radians = Math.toRadians(startAngle.toDouble())
    val tip = Offset(
        center.x + (cos(radians) * radius).toFloat(),
        center.y + (sin(radians) * radius).toFloat(),
    )
    val radial = Offset(cos(radians).toFloat(), sin(radians).toFloat())
    val tangent = Offset(-radial.y, radial.x)
    val apex = tip + tangent * (2.8f * u)
    val base = tip - tangent * (0.6f * u)
    val half = 2.1f * u
    val head = Path().apply {
        moveTo(apex.x, apex.y)
        lineTo(base.x + radial.x * half, base.y + radial.y * half)
        lineTo(base.x - radial.x * half, base.y - radial.y * half)
        close()
    }
    drawPath(head, color = tint)
}

private fun DrawScope.drawDisplay(tint: Color, u: Float, stroke: Stroke) {
    drawRoundRect(
        color = tint,
        topLeft = Offset(3f * u, 5f * u),
        size = Size(18f * u, 12f * u),
        cornerRadius = CornerRadius(2.2f * u),
        style = stroke,
    )
    drawPolyline(tint, stroke, 9f to 20f, 15f to 20f)
    drawPolyline(tint, stroke, 12f to 17f, 12f to 20f)
}

private fun DrawScope.drawAppTile(tint: Color, u: Float, stroke: Stroke) {
    drawRoundRect(
        color = tint,
        topLeft = Offset(3.2f * u, 3.2f * u),
        size = Size(17.6f * u, 17.6f * u),
        cornerRadius = CornerRadius(4.6f * u),
        style = stroke,
    )
    drawRoundRect(
        color = tint,
        topLeft = Offset(8.8f * u, 8.8f * u),
        size = Size(6.4f * u, 6.4f * u),
        cornerRadius = CornerRadius(1.6f * u),
        style = Stroke(width = stroke.width * 0.8f),
    )
}

private fun DrawScope.drawCpu(tint: Color, u: Float, stroke: Stroke) {
    drawRoundRect(
        color = tint,
        topLeft = Offset(6f * u, 6f * u),
        size = Size(12f * u, 12f * u),
        cornerRadius = CornerRadius(2f * u),
        style = stroke,
    )
    drawRoundRect(
        color = tint,
        topLeft = Offset(10f * u, 10f * u),
        size = Size(4f * u, 4f * u),
        cornerRadius = CornerRadius(0.8f * u),
    )
    listOf(9f, 12f, 15f).forEach { offset ->
        drawPolyline(tint, stroke, 6f to offset, 3.2f to offset)
        drawPolyline(tint, stroke, 18f to offset, 20.8f to offset)
        drawPolyline(tint, stroke, offset to 6f, offset to 3.2f)
        drawPolyline(tint, stroke, offset to 18f, offset to 20.8f)
    }
}

private fun DrawScope.drawGpu(tint: Color, u: Float, stroke: Stroke) {
    drawRoundRect(
        color = tint,
        topLeft = Offset(3f * u, 6.4f * u),
        size = Size(18f * u, 11.2f * u),
        cornerRadius = CornerRadius(2f * u),
        style = stroke,
    )
    drawCircle(tint, radius = 3.1f * u, center = Offset(9f * u, 12f * u), style = stroke)
    listOf(10.2f, 12f, 13.8f).forEach { y ->
        drawPolyline(tint, stroke, 14.6f to y, 18.4f to y)
    }
    drawPolyline(tint, stroke, 6.4f to 17.6f, 6.4f to 20.4f, 17.6f to 20.4f, 17.6f to 17.6f)
}

private fun DrawScope.drawRam(tint: Color, u: Float, stroke: Stroke) {
    drawRoundRect(
        color = tint,
        topLeft = Offset(3f * u, 7.6f * u),
        size = Size(18f * u, 7.6f * u),
        cornerRadius = CornerRadius(1.6f * u),
        style = stroke,
    )
    listOf(9f, 12f, 15f).forEach { x ->
        drawPolyline(tint, stroke, x to 7.6f, x to 15.2f)
    }
    listOf(5.4f, 8f, 10.6f, 13.4f, 16f, 18.6f).forEach { x ->
        drawPolyline(tint, stroke, x to 15.2f, x to 18f)
    }
}

private fun DrawScope.drawStorage(tint: Color, u: Float, stroke: Stroke) {
    drawRoundRect(
        color = tint,
        topLeft = Offset(3f * u, 5.4f * u),
        size = Size(18f * u, 13.2f * u),
        cornerRadius = CornerRadius(2.2f * u),
        style = stroke,
    )
    drawCircle(tint, radius = 2.8f * u, center = Offset(12f * u, 10.4f * u), style = stroke)
    drawCircle(tint, radius = 1.1f * u, center = Offset(12f * u, 16f * u))
}

private fun DrawScope.drawThermometer(tint: Color, u: Float, stroke: Stroke) {
    drawRoundRect(
        color = tint,
        topLeft = Offset(10.3f * u, 3.6f * u),
        size = Size(3.4f * u, 11.6f * u),
        cornerRadius = CornerRadius(1.7f * u),
        style = stroke,
    )
    drawCircle(tint, radius = 3.2f * u, center = Offset(12f * u, 17.4f * u), style = stroke)
    drawPolyline(
        tint,
        Stroke(width = stroke.width * 0.9f, cap = StrokeCap.Round),
        12f to 12.4f,
        12f to 15.6f,
    )
}

private fun DrawScope.drawFan(tint: Color, u: Float) {
    // 三叶螺旋桨：三根粗圆头辐条 + 中心点。扇叶在 20dp 的图标槽里会被缩成一个小三角，
    // 反而认不出来；辐条版本在 16dp 仍然可辨（快捷动作与指标卡都用它）。
    val blade = Stroke(width = 3f * u, cap = StrokeCap.Round)
    repeat(3) { index ->
        val radians = Math.toRadians((index * 120f - 90f).toDouble())
        val inner = 2.2f
        val outer = 8.6f
        drawLine(
            color = tint,
            start = Offset(
                center.x + (cos(radians) * inner * u).toFloat(),
                center.y + (sin(radians) * inner * u).toFloat(),
            ),
            end = Offset(
                center.x + (cos(radians) * outer * u).toFloat(),
                center.y + (sin(radians) * outer * u).toFloat(),
            ),
            strokeWidth = blade.width,
            cap = StrokeCap.Round,
        )
    }
    drawCircle(tint, radius = 1.6f * u, center = center)
}

private fun DrawScope.drawWifi(tint: Color, u: Float, stroke: Stroke) {
    val origin = Offset(12f * u, 18f * u)
    listOf(4.4f, 8f, 11.6f).forEach { radius ->
        drawArc(
            color = tint,
            startAngle = 215f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = Offset(origin.x - radius * u, origin.y - radius * u),
            size = Size(radius * 2 * u, radius * 2 * u),
            style = Stroke(width = stroke.width * 0.85f, cap = StrokeCap.Round),
        )
    }
    drawCircle(tint, radius = 1.5f * u, center = Offset(origin.x, origin.y - 1.2f * u))
}

private fun DrawScope.drawClock(tint: Color, u: Float, stroke: Stroke) {
    drawCircle(tint, radius = 8f * u, center = center, style = stroke)
    drawPolyline(tint, stroke, 12f to 7.4f, 12f to 12.6f)
    drawPolyline(tint, stroke, 12f to 12.6f, 16f to 14.6f)
}

private fun DrawScope.drawList(tint: Color, u: Float, stroke: Stroke) {
    listOf(7f, 12f, 17f).forEach { y ->
        drawCircle(tint, radius = 1.3f * u, center = Offset(5.6f * u, y * u))
        drawPolyline(tint, stroke, 9.6f to y, 19f to y)
    }
}

private fun DrawScope.drawSun(tint: Color, u: Float, stroke: Stroke) {
    drawCircle(tint, radius = 4.4f * u, center = center, style = stroke)
    repeat(8) { index ->
        val radians = Math.toRadians((index * 45f).toDouble())
        drawLine(
            color = tint,
            start = Offset(
                center.x + (cos(radians) * 6.8f * u).toFloat(),
                center.y + (sin(radians) * 6.8f * u).toFloat(),
            ),
            end = Offset(
                center.x + (cos(radians) * 9.4f * u).toFloat(),
                center.y + (sin(radians) * 9.4f * u).toFloat(),
            ),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawCalendar(tint: Color, u: Float, stroke: Stroke) {
    drawRoundRect(
        color = tint,
        topLeft = Offset(3.6f * u, 5f * u),
        size = Size(16.8f * u, 15.4f * u),
        cornerRadius = CornerRadius(2.4f * u),
        style = stroke,
    )
    // 顶部标题栏 + 两个挂环。
    drawPolyline(tint, stroke, 3.6f to 9.4f, 20.4f to 9.4f)
    drawPolyline(tint, stroke, 8f to 3.2f, 8f to 6.6f)
    drawPolyline(tint, stroke, 16f to 3.2f, 16f to 6.6f)
    listOf(8f, 12f, 16f).forEach { x ->
        drawCircle(tint, radius = 1f * u, center = Offset(x * u, 14.4f * u))
    }
}

private fun DrawScope.drawPeople(tint: Color, u: Float, stroke: Stroke) {
    drawCircle(tint, radius = 2.6f * u, center = Offset(9.4f * u, 8.4f * u), style = stroke)
    drawCircle(tint, radius = 2f * u, center = Offset(16.4f * u, 9.6f * u), style = stroke)
    // 肩膀：两条下半圆弧。
    drawArc(
        color = tint,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(4f * u, 12.4f * u),
        size = Size(10.8f * u, 9f * u),
        style = stroke,
    )
    drawArc(
        color = tint,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(12.8f * u, 13.2f * u),
        size = Size(8.4f * u, 7.6f * u),
        style = Stroke(width = stroke.width * 0.8f, cap = StrokeCap.Round),
    )
}
