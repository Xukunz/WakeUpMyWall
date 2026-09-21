package com.xukunz.wakeupmywall.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.xukunz.wakeupmywall.core.i18n.ChineseSimplifiedStrings
import com.xukunz.wakeupmywall.core.i18n.LocalStrings
import com.xukunz.wakeupmywall.core.theme.WakeUpMyWallTheme
import com.xukunz.wakeupmywall.data.mock.MockData
import com.xukunz.wakeupmywall.domain.model.MonitorCardDetail
import com.xukunz.wakeupmywall.domain.model.MonitorCardId
import com.xukunz.wakeupmywall.domain.model.MonitorLayout
import com.xukunz.wakeupmywall.ui.components.WallpaperBackground
import com.xukunz.wakeupmywall.ui.components.WidgetStyle
import com.xukunz.wakeupmywall.ui.monitor.MonitorMode
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.io.File
import kotlin.test.Test

/**
 * 卡片自定义的验收帧：用桌面渲染管线把**真实组合**截成 PNG（无头环境下的可视证据，
 * 与 Phase 1 的 `AppScreenshotTest` 同一套做法）。
 *
 * 三帧分别是：默认布局（英文）、自定义布局（隐藏 CPU、Storage 提前、CPU 改成显示核心数）、中文界面。
 */
@OptIn(ExperimentalTestApi::class)
class CardCustomizationScreenshotTest {

    private val wallWidth = 1280
    private val wallHeight = 720

    @Test
    fun `default layout frame`() = capture("card-customization-default") {
        MonitorMode(MockData.metrics, history, WidgetStyle.Glass, identity = MockData.hardware)
    }

    @Test
    fun `customized layout frame`() = capture("card-customization-custom") {
        var cards = MonitorLayout.toggle(MonitorLayout.Default, MonitorCardId.Cpu)
        cards = MonitorLayout.move(cards, MonitorCardId.Storage, -3)
        cards = MonitorLayout.move(cards, MonitorCardId.Storage, -3)
        cards = MonitorLayout.move(cards, MonitorCardId.Storage, -3)
        cards = MonitorLayout.setDetail(cards, MonitorCardId.Gpu, MonitorCardDetail.Usage)

        MonitorMode(MockData.metrics, history, WidgetStyle.Glass, identity = MockData.hardware, cards = cards)
    }

    @Test
    fun `chinese layout frame`() = capture("card-customization-zh") {
        CompositionLocalProvider(LocalStrings provides ChineseSimplifiedStrings) {
            MonitorMode(MockData.metrics, history, WidgetStyle.Glass, identity = MockData.hardware)
        }
    }

    private val history = mapOf(
        "cpu" to List(60) { 20f + it % 10 },
        "gpu" to List(60) { 60f + it % 5 },
        "ram" to List(60) { 38f },
        "storage" to List(60) { 54f },
        "network" to List(60) { 5f },
    )

    private fun capture(name: String, content: @Composable () -> Unit) =
        runDesktopComposeUiTest(width = wallWidth, height = wallHeight) {
            setContent {
                WakeUpMyWallTheme {
                    WallpaperBackground(com.xukunz.wakeupmywall.core.wallpaper.BuiltInWallpapers.DefaultId)
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
            check(frame.width == wallWidth && frame.height == wallHeight) {
                "截图帧尺寸 ${frame.width}×${frame.height} 与声明的渲染条件 ${wallWidth}×$wallHeight 不符"
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
