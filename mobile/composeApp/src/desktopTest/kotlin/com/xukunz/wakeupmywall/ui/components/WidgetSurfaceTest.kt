package com.xukunz.wakeupmywall.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class WidgetSurfaceTest {

    @Test
    fun `glass surface renders its content`() = runComposeUiTest {
        setContent {
            WidgetSurface(style = WidgetStyle.Glass, modifier = Modifier.fillMaxSize()) {
                Text("CPU 28%", modifier = Modifier.testTag("widget:body"))
            }
        }
        onNodeWithTag("widget:body").assertIsDisplayed()
    }

    @Test
    fun `each style renders a tagged surface`() = runComposeUiTest {
        setContent {
            WidgetStyle.entries.forEach { style ->
                WidgetSurface(style = style, modifier = Modifier.fillMaxSize()) { }
            }
        }
        WidgetStyle.entries.forEach { style ->
            onNodeWithTag("surface:${style.name}").assertExists()
        }
    }

    @Test
    fun `section header renders its title tag`() = runComposeUiTest {
        setContent { SectionHeader("My Tasks") }
        onNodeWithTag("header:My Tasks").assertIsDisplayed()
    }
}
