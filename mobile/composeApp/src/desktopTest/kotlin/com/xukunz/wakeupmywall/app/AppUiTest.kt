package com.xukunz.wakeupmywall.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
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
}
