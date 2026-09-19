package com.xukunz.wakeupmywall.app

import kotlin.test.Test
import kotlin.test.assertEquals

class AppNavigatorTest {

    @Test
    fun `starts on dashboard`() {
        assertEquals(Workspace.Dashboard, AppNavigator().current.value)
    }

    @Test
    fun `next moves dashboard to monitor and stops at settings`() {
        val navigator = AppNavigator()
        navigator.next()
        assertEquals(Workspace.Monitor, navigator.current.value)
        navigator.next()
        assertEquals(Workspace.Settings, navigator.current.value)
        navigator.next()
        assertEquals(Workspace.Settings, navigator.current.value)
    }

    @Test
    fun `previous from dashboard stays on dashboard`() {
        val navigator = AppNavigator()
        navigator.previous()
        assertEquals(Workspace.Dashboard, navigator.current.value)
    }

    @Test
    fun `goTo jumps directly`() {
        val navigator = AppNavigator()
        navigator.goTo(Workspace.Settings)
        assertEquals(Workspace.Settings, navigator.current.value)
    }
}
