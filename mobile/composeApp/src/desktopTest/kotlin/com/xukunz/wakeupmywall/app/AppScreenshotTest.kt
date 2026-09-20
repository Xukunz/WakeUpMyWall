package com.xukunz.wakeupmywall.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.io.File
import com.xukunz.wakeupmywall.core.theme.ThemeAccent
import com.xukunz.wakeupmywall.core.theme.WakeUpMyWallTheme
import com.xukunz.wakeupmywall.core.wallpaper.BuiltInWallpapers
import com.xukunz.wakeupmywall.data.mock.MockData
import com.xukunz.wakeupmywall.domain.model.DashboardLayout
import com.xukunz.wakeupmywall.domain.model.PcState
import com.xukunz.wakeupmywall.ui.components.WidgetStyle
import com.xukunz.wakeupmywall.ui.dashboard.DashboardData
import com.xukunz.wakeupmywall.ui.dashboard.DashboardMode
import com.xukunz.wakeupmywall.ui.powerrail.powerRailModel
import com.xukunz.wakeupmywall.ui.settings.AppearanceScreen
import com.xukunz.wakeupmywall.ui.settings.AppearanceState
import com.xukunz.wakeupmywall.ui.standby.StandByMode
import com.xukunz.wakeupmywall.ui.components.WallpaperBackground
import kotlin.test.Test

/**
 * 无头环境下的可视验证：用桌面渲染管线把 App 的真实帧截成 PNG，落到 `build/screenshots/`。
 *
 * 用途有两个：一是这台机器没有可操作的图形界面、也拿不到 KVM，截图是"看到界面"的唯一途径；
 * 二是 Phase 1 Task 15 的视觉复核可以直接拿这些图与 `imgs/concept/` 对比。
 * 渲染或布局出问题时 `captureToImage()` 会直接让测试失败，因此它同时也是一道渲染回归检查。
 *
 * **帧尺寸是证据的一部分**：默认的 `runComposeUiTest` 窗口只有 1024×768，`Modifier.size(1280.dp, 720.dp)`
 * 会被入参约束压回 1024 宽——也就是说"按 1280dp 设计"的布局其实是在 737dp 主区里渲染的，
 * 与概念图（1280×720dp，即 2560×1440 @320dpi 的墙面屏）不是同一个断点区间。
 * 现在窗口由 [FrameWidth] × [FrameHeight] 显式指定，并断言产出的 PNG 尺寸，避免再出现"文档说 1280、图是 1024"。
 */
@OptIn(ExperimentalTestApi::class)
class AppScreenshotTest {

    private companion object {
        /** 墙面屏形态：1280×720dp。 */
        const val FrameWidth = 1280
        const val FrameHeight = 720

        /**
         * 手机与折叠屏的实际比例（dp，按各自典型 dpi 折算）：
         * 20:9 主流安卓机 412×915；21.1:9 折叠外屏 412×965；4:3.55 内屏展开 790×700 与竖放 700×790。
         */
        const val PhoneWidth = 412
        const val PhoneHeight = 915
        const val FoldOuterWidth = 412
        const val FoldOuterHeight = 965
        const val FoldInnerWidth = 790
        const val FoldInnerHeight = 700
        const val FoldInnerPortraitWidth = 700
        const val FoldInnerPortraitHeight = 790
    }

    @Test
    fun `capture dashboard with default wallpaper`() = capture("dashboard-default") { App() }

    @Test
    fun `capture monitor workspace`() = capture("monitor-default") {
        App(navigator = AppNavigator().apply { goTo(Workspace.Monitor) })
    }

    @Test
    fun `capture settings workspace`() = capture("settings-default") {
        App(navigator = AppNavigator().apply { goTo(Workspace.Settings) })
    }

    @Test
    fun `capture dashboard with the aurora wallpaper`() = capture("dashboard-aurora") {
        App(wallpaperId = "aurora")
    }

    @Test
    fun `capture dashboard with minimal wallpaper`() = capture("dashboard-minimal") { App(wallpaperId = "minimal") }

    // 手机 / 折叠屏：竖屏形态下常驻控制栏在底部，Dashboard 单列。
    @Test
    fun `capture phone portrait dashboard`() =
        capture("phone-portrait-dashboard", PhoneWidth, PhoneHeight) { App() }

    @Test
    fun `capture phone portrait monitor`() = capture("phone-portrait-monitor", PhoneWidth, PhoneHeight) {
        App(navigator = AppNavigator().apply { goTo(Workspace.Monitor) })
    }

    @Test
    fun `capture phone portrait settings`() = capture("phone-portrait-settings", PhoneWidth, PhoneHeight) {
        App(navigator = AppNavigator().apply { goTo(Workspace.Settings) })
    }

    @Test
    fun `capture foldable outer screen`() =
        capture("fold-outer-dashboard", FoldOuterWidth, FoldOuterHeight) { App() }

    @Test
    fun `capture foldable inner screen landscape`() =
        capture("fold-inner-dashboard", FoldInnerWidth, FoldInnerHeight) { App() }

    @Test
    fun `capture foldable inner screen portrait`() = capture(
        "fold-inner-portrait-dashboard",
        FoldInnerPortraitWidth,
        FoldInnerPortraitHeight,
    ) { App() }

    // 内屏竖放比手机宽（700dp vs 412dp）：Dashboard 仍是 3 列，但 Monitor / Settings 的内部
    // 重排（身份卡与快捷动作是否并排、左导航是否改横向条）与手机不是同一条分支，需要单独出帧。
    @Test
    fun `capture foldable inner screen portrait monitor`() = capture(
        "fold-inner-portrait-monitor",
        FoldInnerPortraitWidth,
        FoldInnerPortraitHeight,
    ) { App(navigator = AppNavigator().apply { goTo(Workspace.Monitor) }) }

    @Test
    fun `capture foldable inner screen portrait settings`() = capture(
        "fold-inner-portrait-settings",
        FoldInnerPortraitWidth,
        FoldInnerPortraitHeight,
    ) { App(navigator = AppNavigator().apply { goTo(Workspace.Settings) }) }

    @Test
    fun `capture dashboard at compact width`() =
        // 800dp 宽时主区约 576dp → Compact：2 列 + 纵向滚动（风险 R7 的兜底形态）。
        capture("dashboard-compact", width = 800, height = FrameHeight) { App() }

    @Test
    fun `capture standby mode`() = capture("standby-default") {
        WakeUpMyWallTheme {
            Box(Modifier.fillMaxSize()) {
                WallpaperBackground(BuiltInWallpapers.DefaultId)
                StandByMode(
                        data = DashboardData(
                            greeting = MockData.greetingText,
                            time = MockData.clockTime,
                            date = MockData.standbyDateLabel,
                        weather = MockData.weather,
                        events = MockData.calendarEvents,
                        todos = MockData.todos,
                    // 概念图的浮层卡是"可按的电源环 + Power On / WAKE YOUR PC"，那对应 WOL_READY；
                    // 用 ONLINE 抓图会渲染成禁用环 + 状态文案，与概念图不是同一形态。
                    pc = powerRailModel(PcState.WOL_READY, MockData.defaultDevice),
                        pcSummary = MockData.summaryMetrics,
                        widgets = DashboardLayout.default,
                    ),
                    rail = powerRailModel(PcState.WOL_READY, MockData.defaultDevice),
                    nextEvent = MockData.nextEvent,
                    onRailEvent = {},
                    onOpenMonitor = {},
                )
            }
        }
    }

    @Test
    fun `capture appearance screen with live preview`() = capture("appearance-live-preview") {
        WakeUpMyWallTheme {
            // 与真实 App 一致的组合：壁纸在底层，Appearance 面板浮在其上。
            Box(Modifier.fillMaxSize()) {
                WallpaperBackground(BuiltInWallpapers.DefaultId)
            AppearanceScreen(
                state = AppearanceState(
                    accent = ThemeAccent.AuroraBlue,
                    widgetStyle = WidgetStyle.Glass,
                    wallpaperId = BuiltInWallpapers.DefaultId,
                    transparency = 0.7f,
                    fontScale = 1f,
                    widgets = DashboardLayout.default,
                ),
                onStateChange = {},
            ) { previewState ->
                DashboardMode(
                    data = DashboardData(
                        greeting = MockData.greetingText,
                        time = MockData.clockTime,
                        date = MockData.calendarDateLabel,
                        weather = MockData.weather,
                        events = MockData.calendarEvents,
                        todos = MockData.todos,
                        pc = powerRailModel(PcState.ONLINE, MockData.defaultDevice),
                        pcSummary = MockData.summaryMetrics,
                        widgets = previewState.widgets,
                    ),
                    style = previewState.widgetStyle,
                    onPcSummaryClick = {},
                )
            }
            }
        }
    }

    private fun capture(
        name: String,
        width: Int = FrameWidth,
        height: Int = FrameHeight,
        content: @androidx.compose.runtime.Composable () -> Unit,
    ) = runDesktopComposeUiTest(width = width, height = height) {
            setContent {
                // 与 App 的真实组合一致：主题 + 透明 Surface，由 Surface 提供 contentColor。
                // 少了这一层，没有显式指定颜色的文字会拿到 LocalContentColor 的默认黑，
                // 在深色壁纸上直接看不见（StandBy 的时钟小时位就这样被漏掉过一版）。
                WakeUpMyWallTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onBackground,
                    ) {
                        Box(Modifier.fillMaxSize()) { content() }
                    }
                }
            }

            val target = File("build/screenshots/$name.png")
            val frame = onRoot().captureToImage()
            // 帧尺寸就是复核文档里写的"渲染条件"，被测试守住，不能只靠注释。
            check(frame.width == width && frame.height == height) {
                "截图帧尺寸 ${frame.width}×${frame.height} 与声明的渲染条件 ${width}×${height} 不符：" +
                    "复核文档的结论会失去依据"
            }
            frame.writePng(target)
            println("SCREENSHOT: ${target.absolutePath}")
        }

    private fun ImageBitmap.writePng(target: File) {
        target.parentFile?.mkdirs()
        val data = Image.makeFromBitmap(asSkiaBitmap())
            .encodeToData(EncodedImageFormat.PNG)
            ?: error("PNG 编码失败：$target")
        target.writeBytes(data.bytes)
    }
}
