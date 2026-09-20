package com.xukunz.wakeupmywall.ui.settings

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import com.xukunz.wakeupmywall.core.theme.ThemeAccent
import com.xukunz.wakeupmywall.core.theme.WakeUpMyWallTheme
import com.xukunz.wakeupmywall.core.wallpaper.BuiltInWallpapers
import com.xukunz.wakeupmywall.domain.model.DashboardLayout
import com.xukunz.wakeupmywall.ui.components.WidgetStyle
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class AppearanceScreenTest {

    private val state = AppearanceState(
        accent = ThemeAccent.AuroraBlue,
        widgetStyle = WidgetStyle.Glass,
        wallpaperId = BuiltInWallpapers.DefaultId,
        transparency = 0.7f,
        fontScale = 1f,
        widgets = DashboardLayout.default,
    )

    @Test
    fun `preview slot receives the current appearance state`() = runComposeUiTest {
        setContent {
            WakeUpMyWallTheme {
                AppearanceScreen(state, {}) { previewState ->
                    Text(previewState.wallpaperId, modifier = Modifier.testTag("appearance:preview"))
                }
            }
        }

        onNodeWithTag("appearance:preview").assertTextEquals(BuiltInWallpapers.DefaultId)
    }

    @Test
    fun `accent swatch selection is forwarded`() = runComposeUiTest {
        var latest: AppearanceState? = null
        setContent { WakeUpMyWallTheme { AppearanceScreen(state, { latest = it }) { } } }

        onNodeWithTag("appearance:accent:Emerald").performClick()

        assertEquals(ThemeAccent.Emerald, latest?.accent)
    }

    @Test
    fun `wallpaper thumbnails cover the bundled catalogue`() = runComposeUiTest {
        var latest: AppearanceState? = null
        setContent { WakeUpMyWallTheme { AppearanceScreen(state, { latest = it }) { } } }

        BuiltInWallpapers.all.forEach { onNodeWithTag("appearance:wallpaper:${it.id}").assertExists() }
        // 7 张缩略图在控制条里横向滚动（1280dp 下这一段放不下 7 张），最后一页必须先滚进视口。
        onNodeWithTag("appearance:wallpaper:${BuiltInWallpapers.Minimal.id}").performScrollTo().performClick()

        assertEquals(BuiltInWallpapers.Minimal.id, latest?.wallpaperId)
    }

    @Test
    fun `widget style cards are selectable`() = runComposeUiTest {
        var latest: AppearanceState? = null
        setContent { WakeUpMyWallTheme { AppearanceScreen(state, { latest = it }) { } } }

        WidgetStyle.entries.forEach { onNodeWithTag("appearance:style:${it.name}").assertExists() }
        onNodeWithTag("appearance:style:Solid").performClick()

        assertEquals(WidgetStyle.Solid, latest?.widgetStyle)
    }

    @Test
    fun `bottom bar exposes the four concept segments`() = runComposeUiTest {
        setContent { WakeUpMyWallTheme { AppearanceScreen(state, {}) { } } }

        listOf(
            "appearance:segment:wallpaper",
            "appearance:segment:accent",
            "appearance:segment:style",
            "appearance:segment:appearance",
            "appearance:transparency",
            "appearance:font-scale",
            "appearance:layout:Default",
            "appearance:wallpaper-more",
        ).forEach { onNodeWithTag(it).assertExists() }
    }

    @Test
    fun `left navigation lists every appearance section`() = runComposeUiTest {
        setContent { WakeUpMyWallTheme { AppearanceScreen(state, {}) { } } }

        AppearanceSection.entries.forEach { onNodeWithTag("appearance:nav:${it.name}").assertExists() }
    }
}
