package com.xukunz.wakeupmywall.app

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
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
    fun `swiping left twice walks from dashboard to standby and back again`() = runComposeUiTest {
        setContent { App() }

        // Task 13 要求三形态可达：Dashboard → Monitor → StandBy，右滑逐级退回。
        onNodeWithTag("home:surface").performTouchInput { swipeLeft() }
        onNodeWithTag("monitor:identity").assertIsDisplayed()

        onNodeWithTag("home:surface").performTouchInput { swipeLeft() }
        onNodeWithTag("standby:clock", useUnmergedTree = true).assertIsDisplayed()

        onNodeWithTag("home:surface").performTouchInput { swipeRight() }
        onNodeWithTag("monitor:identity").assertIsDisplayed()

        onNodeWithTag("home:surface").performTouchInput { swipeRight() }
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

    private fun navigateThroughAllWorkspaces(navigator: AppNavigator) {
        Workspace.entries.forEach(navigator::goTo)
    }
}
