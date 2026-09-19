package com.xukunz.wakeupmywall.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.xukunz.wakeupmywall.core.wallpaper.BuiltInWallpapers
import com.xukunz.wakeupmywall.ui.components.WallpaperBackground
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class AppUiTest {

    @Test
    fun `dashboard renders first`() = runComposeUiTest {
        setContent { App() }
        onNodeWithTag("screen:Dashboard").assertIsDisplayed()
    }

    @Test
    fun `switching workspace renders monitor`() = runComposeUiTest {
        val navigator = AppNavigator().apply { goTo(Workspace.Monitor) }
        setContent { App(navigator) }
        onNodeWithTag("screen:PC Monitor").assertIsDisplayed()
    }

    @Test
    fun `default wallpaper is the aurora master`() = runComposeUiTest {
        setContent { App() }
        onNodeWithTag("wallpaper:aurora").assertIsDisplayed()
    }

    @Test
    fun `wallpaper survives workspace switching`() = runComposeUiTest {
        val navigator = AppNavigator()
        setContent { App(navigator) }

        navigateThroughAllWorkspaces(navigator)

        onNodeWithTag("wallpaper:aurora").assertIsDisplayed()
        onNodeWithTag("screen:Settings").assertIsDisplayed()
    }

    @Test
    fun `alternate wallpaper can be selected by id`() = runComposeUiTest {
        setContent { App(wallpaperId = "minimal") }
        onNodeWithTag("wallpaper:minimal").assertIsDisplayed()
    }

    @Test
    fun `unknown wallpaper id falls back to the default background`() = runComposeUiTest {
        setContent { App(wallpaperId = "no-such-wallpaper") }
        onNodeWithTag("wallpaper:aurora").assertIsDisplayed()
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

    private fun navigateThroughAllWorkspaces(navigator: AppNavigator) {
        Workspace.entries.forEach(navigator::goTo)
    }
}
