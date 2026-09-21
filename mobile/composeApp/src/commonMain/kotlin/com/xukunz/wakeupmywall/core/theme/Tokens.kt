package com.xukunz.wakeupmywall.core.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class ThemeAccent { AuroraBlue, Emerald, Purple, Amber, Red, Dynamic }

data class AccentPalette(val accent: Color, val onAccent: Color, val onlineColor: Color)

/**
 * `onAccent` 必须与 `accent` 保持 ≥ 4.5:1 对比度（见 ThemeAccentTest），
 * `onlineColor` 用于在线状态点，必须与强调色可区分，避免"在线"与"主按钮"混淆。
 */
fun ThemeAccent.palette(): AccentPalette = when (this) {
    ThemeAccent.AuroraBlue -> AccentPalette(Color(0xFF4C8DFF), Color(0xFF06101F), Color(0xFF3DDC97))
    ThemeAccent.Emerald -> AccentPalette(Color(0xFF2FBF71), Color(0xFF03170B), Color(0xFF7BE495))
    ThemeAccent.Purple -> AccentPalette(Color(0xFF8B5CF6), Color(0xFF120726), Color(0xFF5EEAD4))
    ThemeAccent.Amber -> AccentPalette(Color(0xFFF5A524), Color(0xFF2A1A00), Color(0xFF7BE495))
    ThemeAccent.Red -> AccentPalette(Color(0xFFE5484D), Color(0xFF1F0407), Color(0xFF7BE495))
    ThemeAccent.Dynamic -> AccentPalette(Color(0xFF4C8DFF), Color(0xFF06101F), Color(0xFF3DDC97))
}

object Spacing {
    /** 分隔线与卡片描边的线宽。写进令牌，`ui/` 里就不允许出现裸 `1.dp`。 */
    val hairline = 1.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}

/** 组件尺寸令牌（与间距分开，避免把"元素多大"和"元素之间多远"混成一个量表）。 */
object AppSizes {
    val ringDiameter = 220.dp
    /**
     * StandBy 浮层卡里的环。概念图 D 的环占卡片高度约 2/3；140dp 会把卡片撑高到
     * 和时钟抢空间，且旋钮底图的深色圆盘会盖住卡片左侧的标题，实测后收到 112dp。
     */
    /**
     * StandBy 浮层卡里的环。概念图 D 的环直径约为卡片宽度的 2/3，实测按 168dp 才接近；
     * 之前 112/140dp 是因为旋钮位图把小卡片压得发闷，改用代码发光环后不再有这个问题。
     */
    val standbyRingDiameter = 168.dp
    const val standbyCardWidthFraction = 0.34f
    val ringStroke = 3.dp
    /** 主电源环的环宽（概念图 A3 是粗环 + 外发光，不是细描边）。 */
    val powerRingStroke = 6.dp
    /** 竖屏底部常驻栏里的环：整条栏要放在 400dp 级别的宽度里。 */
    val compactRingDiameter = 88.dp
    val statusDot = 8.dp
    val barHeight = 6.dp
    val sparklineStroke = 2.dp
    val ringArcStroke = 6.dp
    val progressRing = 84.dp
    val thumbnail = 48.dp
    /**
     * 响应式断点阈值（风险 R7）。放在令牌里，`ui/` 不允许出现裸断点数值。
     *
     * `900.dp` 来自概念图本身：墙面屏是 1280×720dp，72% 主区 = 921dp，概念图在这个宽度下排的就是
     * 5 列指标卡。Task 14 起初写的是 1000dp（估算"5 列要 200dp/张"），但那会让目标屏落进 3 列区间，
     * 等于还原不出概念图的栅格——2026-09-20 按 1280×720 真实成帧复核后下调。
     */
    val fiveColumnMinWidth = 900.dp
    val threeColumnMinWidth = 600.dp
    /** 主页形态切换的水平滑动阈值。 */
    val swipeThreshold = 60.dp
    val settingsNavWidth = 240.dp
    val appearanceNavWidth = 180.dp
    val wallpaperChip = 40.dp
    val accentDot = 24.dp
    val sliderHeight = 32.dp
    /** 图标尺寸：小号用于行内元数据，中号用于按钮与列表行，大号用于卡片标题行与快捷动作。 */
    val iconSmall = 16.dp
    val iconMedium = 20.dp
    val iconLarge = 28.dp
    /** 快捷动作的方形图标瓦片。 */
    val iconTile = 44.dp
    /** 天气图标：卡片主图标与逐时列的小图标。 */
    val weatherIcon = 56.dp
    val weatherIconHourly = 24.dp
    /** 主电源环内部代码绘制的电源字形外接边长。 */
    val powerGlyph = 92.dp
    /** 待办勾选圈直径。 */
    val checkbox = 18.dp
    /** 概念图里的指标条是细线，不是进度条（与 6dp 的进度条分开）。 */
    val metricBarHeight = 3.dp
    /**
     * 页数指示（Storage 卡片翻页）：一元一格，当前页是一段更宽的胶囊。
     * 参考图是 Apple Music 小部件下面那排点（点 : 胶囊 ≈ 1 : 2.5）。直径按"墙面屏上看得见"定到 8dp——
     * 参考图里那粒 6px 的点照搬到这个尺寸的卡片上会糊成一团灰。
     */
    val pageDot = 8.dp
    val pageDotActiveWidth = 20.dp
    /** PC 封面缩略图：摘要卡（小）与 Monitor 身份卡（中）两个尺寸。 */
    val coverThumbnailWidth = 72.dp
    val coverThumbnailHeight = 48.dp
    val coverIdentityWidth = 112.dp
    val coverIdentityHeight = 72.dp
    /**
     * 摘要卡"横向四列"所需的最小宽度。窄于它时（Compact 形态下摘要卡只有 ~260dp）
     * 四列会把 `CPU` 压成竖排单字、`12%` 折行，因此改成"封面一行 + 指标 2×2"。
     */
    val summaryMetricsMinWidth = 340.dp
    /**
     * 常驻 Power Rail 从"侧栏"切成"底栏"的宽度下限。
     * 手机的 20:9（~412dp 宽）与折叠外屏的 21.1:9（~412dp）都远低于它 → 底栏；
     * 展开内屏（4:3.55，横放约 790dp）与墙面屏（1280dp）→ 侧栏。
     */
    val sideRailMinWidth = 600.dp
    /** Dashboard 在竖屏窄机上退成单列：天气 / 日历 / 任务三张卡都不适合再并排。 */
    val dashboardSingleColumnMaxWidth = 480.dp
    /** 低于这个宽度时，Monitor 的身份卡与快捷动作不再并排（竖屏手机）。 */
    val stackedRowsMaxWidth = 560.dp
    /** 低于这个宽度时，Settings / Appearance 的左导航改成横向可滚动的条目条。 */
    val inlineNavMaxWidth = 620.dp
    /** 品牌条下面的短横线长度（概念图 D 顶行）。 */
    val brandDividerWidth = 64.dp
    const val minFontScale = 0.8f
    const val maxFontScale = 1.4f
}

object DarkSurface {
    val background = Color(0xFF0B0F17)
    val card = Color(0xFF151B26)
    val outline = Color(0x33FFFFFF)
    val textPrimary = Color(0xFFE8EDF5)
    val textSecondary = Color(0xFF9AA6B8)
}
