package com.xukunz.wakeupmywall.ui.dashboard

import kotlin.test.Test
import kotlin.test.assertEquals

class HomeSurfaceStateTest {

    @Test
    fun `starts on dashboard`() {
        assertEquals(HomeMode.Dashboard, HomeModeController().current.value)
    }

    @Test
    fun `swipe left shows monitor and swipe right returns`() {
        val controller = HomeModeController()

        controller.onSwipeLeft()
        assertEquals(HomeMode.Monitor, controller.current.value)

        controller.onSwipeRight()
        assertEquals(HomeMode.Dashboard, controller.current.value)
    }

    @Test
    fun `toggle cycles through all three modes`() {
        val controller = HomeModeController()

        controller.toggle()
        assertEquals(HomeMode.Monitor, controller.current.value)

        controller.toggle()
        assertEquals(HomeMode.StandBy, controller.current.value)

        controller.toggle()
        assertEquals(HomeMode.Dashboard, controller.current.value)
    }

    @Test
    fun `swipe left walks forward and clamps at standby`() {
        val controller = HomeModeController()

        controller.onSwipeLeft()
        assertEquals(HomeMode.Monitor, controller.current.value)
        controller.onSwipeLeft()
        assertEquals(HomeMode.StandBy, controller.current.value)
        controller.onSwipeLeft()
        assertEquals(HomeMode.StandBy, controller.current.value)
    }

    @Test
    fun `swipe right on dashboard stays on dashboard`() {
        val controller = HomeModeController()

        controller.onSwipeRight()

        assertEquals(HomeMode.Dashboard, controller.current.value)
    }

    @Test
    fun `show jumps directly`() {
        val controller = HomeModeController()

        controller.show(HomeMode.Monitor)

        assertEquals(HomeMode.Monitor, controller.current.value)
    }
}
