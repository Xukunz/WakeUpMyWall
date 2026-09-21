package com.xukunz.wakeupmywall.app

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.waitUntilExactlyOneExists
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.xukunz.wakeupmywall.core.storage.InMemoryKeyValueStore
import com.xukunz.wakeupmywall.core.storage.JsonSettingsStorage
import com.xukunz.wakeupmywall.core.wallpaper.BuiltInWallpapers
import com.xukunz.wakeupmywall.data.mock.MockData
import com.xukunz.wakeupmywall.domain.model.MacAddress
import com.xukunz.wakeupmywall.domain.model.PcDevice
import com.xukunz.wakeupmywall.ui.components.WallpaperBackground
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class AppUiTest {

    @Test
    fun `dashboard renders first`() = runComposeUiTest {
        setContent { App() }
        // Dashboard 已是真实的 Dashboard 形态（Task 6），不再是占位页。
        onNodeWithTag("dashboard:greeting").assertIsDisplayed()
        onNodeWithTag("powerrail").assertIsDisplayed()
    }

    @Test
    fun `switching workspace renders monitor`() = runComposeUiTest {
        val navigator = AppNavigator().apply { goTo(Workspace.Monitor) }
        setContent { App(navigator) }
        // Monitor 也是真实形态（Task 8），不再是占位页。
        onNodeWithTag("monitor:identity").assertIsDisplayed()
    }

    @Test
    fun `swiping walks between dashboard and standby while the monitor is click only`() = runComposeUiTest {
        setContent { App() }

        // 滑动只在 Dashboard ⇄ StandBy；**硬件页（Monitor）不是主页之一**，
        // 它只能从 PC 摘要卡进入、用返回退出（否则它会抢走卡片里的横向手势）。
        onNodeWithTag("home:surface").performTouchInput { swipeLeft() }
        onNodeWithTag("standby:clock", useUnmergedTree = true).assertIsDisplayed()

        onNodeWithTag("home:surface").performTouchInput { swipeRight() }
        onNodeWithTag("dashboard:greeting").assertIsDisplayed()

        // 进入硬件页：点 PC 摘要卡。
        onNodeWithTag("dashboard:pc-summary").performClick()
        onNodeWithTag("monitor:identity").assertIsDisplayed()

        // 硬件页上滑动不再切页。
        onNodeWithTag("home:surface").performTouchInput { swipeLeft() }
        onNodeWithTag("monitor:identity").assertIsDisplayed()

        // 左上角返回回到 Dashboard。
        onNodeWithTag("monitor:back").performClick()
        onNodeWithTag("dashboard:greeting").assertIsDisplayed()
    }

    @Test
    fun `default wallpaper is the dusk lake master`() = runComposeUiTest {
        setContent { App() }
        onNodeWithTag("wallpaper:dusk_lake").assertIsDisplayed()
    }

    @Test
    fun `wallpaper survives workspace switching`() = runComposeUiTest {
        val navigator = AppNavigator()
        setContent { App(navigator) }

        navigateThroughAllWorkspaces(navigator)

        onNodeWithTag("wallpaper:dusk_lake").assertIsDisplayed()
        // Settings 已是真实工作空间（Task 10），不再是占位页。
        onNodeWithTag("settings:nav").assertIsDisplayed()
    }

    @Test
    fun `alternate wallpaper can be selected by id`() = runComposeUiTest {
        setContent { App(wallpaperId = "minimal") }
        onNodeWithTag("wallpaper:minimal").assertIsDisplayed()
    }

    @Test
    fun `unknown wallpaper id falls back to the default background`() = runComposeUiTest {
        setContent { App(wallpaperId = "no-such-wallpaper") }
        onNodeWithTag("wallpaper:dusk_lake").assertIsDisplayed()
    }

    @Test
    fun `every built in wallpaper renders`() = runComposeUiTest {
        var current by mutableStateOf(BuiltInWallpapers.DefaultId)
        setContent { WallpaperBackground(current) }

        BuiltInWallpapers.all.forEach { wallpaper ->
            current = wallpaper.id
            onNodeWithTag("wallpaper:${wallpaper.id}").assertIsDisplayed()
        }
    }

    @Test
    fun `the rail shows the active device that came from storage`() = runComposeUiTest {
        val storage = JsonSettingsStorage(InMemoryKeyValueStore())
        runBlocking {
            storage.writeDevices(
                listOf(
                    PcDevice(
                        id = "den",
                        name = "Den PC",
                        macAddress = MacAddress.parse("00:1A:2B:3C:4D:5E"),
                        isDefault = true,
                    ),
                ),
            )
        }

        setContent { App(storage = storage) }
        // 设备列表要等 `repository.load()`（挂起读存储）落定，所以先等标题变成存储里的名字。
        waitUntilExactlyOneExists(hasTestTag("powerrail:name") and hasText("Den PC"), timeoutMillis = 5_000)

        // 标题必须来自存储里的激活设备，而不是 MockData 常量（"My PC"）。
        onNodeWithTag("powerrail:name", useUnmergedTree = true).assertTextEquals("Den PC")
    }

    @Test
    fun `a cold start syncs the form to the stored values when the id matches the seed`() = runComposeUiTest {
        // 种子设备与存储设备共用 id `desktop-alpha`：只按 id 判断"换设备了没有"就漏掉这一种，
        // 表单会一直停在 Mock 值上，而 Test Connection 用的是仓库里的值（真机踩到过）。
        val storage = JsonSettingsStorage(InMemoryKeyValueStore())
        runBlocking {
            storage.writeDevices(
                listOf(MockData.defaultDevice.copy(agentHost = "127.0.0.1", agentPort = 9877)),
            )
        }
        val navigator = AppNavigator().apply { goTo(Workspace.Settings) }

        setContent { App(navigator = navigator, storage = storage) }

        // 表单必须跟着仓库走：主机名与端口都来自存储，而不是 MockData 的 192.168.1.10 / 9876。
        waitUntilExactlyOneExists(
            hasTestTag("device:field:agentHost") and hasText("127.0.0.1"),
            timeoutMillis = 5_000,
        )
        onNodeWithTag("device:field:agentHost").assert(hasText("127.0.0.1"))
        onNodeWithTag("device:wolPort", useUnmergedTree = true).assertExists()
    }

    private fun navigateThroughAllWorkspaces(navigator: AppNavigator) {
        Workspace.entries.forEach(navigator::goTo)
    }
}
