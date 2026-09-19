package com.xukunz.wakeupmywall.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.xukunz.wakeupmywall.core.theme.WakeUpMyWallTheme
import com.xukunz.wakeupmywall.ui.components.PlaceholderScreen

@Composable
fun App(navigator: AppNavigator = remember { AppNavigator() }) {
    val workspace by navigator.current.collectAsState()
    WakeUpMyWallTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (workspace) {
                Workspace.Dashboard -> PlaceholderScreen("Dashboard")
                Workspace.Monitor -> PlaceholderScreen("PC Monitor")
                Workspace.Settings -> PlaceholderScreen("Settings")
            }
        }
    }
}
