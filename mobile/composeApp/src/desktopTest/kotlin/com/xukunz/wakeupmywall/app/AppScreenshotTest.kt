package com.xukunz.wakeupmywall.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.io.File
import kotlin.test.Test

/**
 * 无头环境下的可视验证：用桌面渲染管线把 App 的真实帧截成 PNG，落到 `build/screenshots/`。
 *
 * 用途有两个：一是这台机器没有可操作的图形界面、也拿不到 KVM，截图是"看到界面"的唯一途径；
 * 二是 Phase 1 Task 15 的视觉复核可以直接拿这些图与 `imgs/concept/` 对比。
 * 渲染或布局出问题时 `captureToImage()` 会直接让测试失败，因此它同时也是一道渲染回归检查。
 */
@OptIn(ExperimentalTestApi::class)
class AppScreenshotTest {

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

    private fun capture(name: String, content: @androidx.compose.runtime.Composable () -> Unit) =
        runComposeUiTest {
            setContent {
                Box(Modifier.size(1280.dp, 720.dp)) { content() }
            }

            val target = File("build/screenshots/$name.png")
            onRoot().captureToImage().writePng(target)
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
