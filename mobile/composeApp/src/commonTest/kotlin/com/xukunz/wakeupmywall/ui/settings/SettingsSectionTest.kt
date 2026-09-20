package com.xukunz.wakeupmywall.ui.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsSectionTest {

    @Test
    fun `navigation order matches spec`() {
        assertEquals(
            listOf(
                "Device Setup",
                "Integrations",
                "Display & Behavior",
                "Appearance",
                "Notifications",
                "Backup & Sync",
                "About",
            ),
            SettingsSection.entries.map { it.title },
        )
    }

    @Test
    fun `device setup is the default section`() {
        assertEquals(SettingsSection.DeviceSetup, SettingsSection.entries.first())
    }

    @Test
    fun `appearance entry owns the wallpaper and personalization screen`() {
        // 概念图里这一项叫 `Wallpaper & Personalization`，本工程统一叫 Appearance（见 Task 12）。
        assertEquals(SettingsSection.Appearance, SettingsSection.entries.first { it.title == "Appearance" })
    }
}
