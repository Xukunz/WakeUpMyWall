package com.xukunz.wakeupmywall.ui.settings

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.xukunz.wakeupmywall.core.theme.WakeUpMyWallTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SettingsWorkspaceTest {

    @Test
    fun `renders all navigation entries and forwards selection`() = runComposeUiTest {
        var selected = SettingsSection.DeviceSetup
        setContent {
            WakeUpMyWallTheme {
                SettingsWorkspace(SettingsSection.DeviceSetup, { selected = it }) { section ->
                    Text(section.title, modifier = Modifier.testTag("settings:content"))
                }
            }
        }

        SettingsSection.entries.forEach { onNodeWithTag("settings:nav:${it.name}").assertExists() }
        onNodeWithTag("settings:nav:Appearance").performClick()

        assertEquals(SettingsSection.Appearance, selected)
    }

    @Test
    fun `content slot receives the selected section`() = runComposeUiTest {
        setContent {
            WakeUpMyWallTheme {
                SettingsWorkspace(SettingsSection.About, {}) { section ->
                    Text(section.title, modifier = Modifier.testTag("settings:content"))
                }
            }
        }

        onNodeWithTag("settings:content").assertIsDisplayed()
    }

    @Test
    fun `reset to default is available`() = runComposeUiTest {
        var reset = false
        setContent { WakeUpMyWallTheme { ResetToDefaultRow { reset = true } } }

        onNodeWithTag("settings:reset").performClick()

        assertTrue(reset)
    }

    @Test
    fun `home entry exists so settings is not a dead end`() = runComposeUiTest {
        var home = false
        setContent {
            WakeUpMyWallTheme {
                SettingsWorkspace(SettingsSection.DeviceSetup, {}, onBackHome = { home = true }) { }
            }
        }

        onNodeWithTag("settings:back").performClick()

        assertTrue(home)
    }
}
