package com.xukunz.wakeupmywall.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
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
    }

    @Test
    fun `capture dashboard with default wallpaper`() = capture("dashboard-aurora") { App() }

    @Test
    fun `capture monitor workspace`() = capture("monitor-aurora") {
        App(navigator = AppNavigator().apply { goTo(Workspace.Monitor) })
    }

    @Test
    fun `capture settings workspace`() = capture("settings-aurora") {
        App(navigator = AppNavigator().apply { goTo(Workspace.Settings) })
    }

    @Test
    fun `capture dashboard with minimal wallpaper`() = capture("dashboard-minimal") {
        App(wallpaperId = "minimal")
    }

    @Test
    fun `capture dashboard at compact width`() =
        // 800dp 宽时主区约 576dp → Compact：2 列 + 纵向滚动（风险 R7 的兜底形态）。
        capture("dashboard-compact", width = 800, height = FrameHeight) { App() }

    @Test
    fun `capture standby mode`() = capture("standby-aurora") {
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
                        pc = powerRailModel(PcState.ONLINE, MockData.defaultDevice),
                        pcSummary = MockData.summaryMetrics,
                        widgets = DashboardLayout.default,
                    ),
                    rail = powerRailModel(PcState.ONLINE, MockData.defaultDevice),
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
                Box(Modifier.fillMaxSize()) { content() }
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
