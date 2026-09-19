package com.xukunz.wakeupmywall.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import com.xukunz.wakeupmywall.core.theme.WakeUpMyWallTheme
import com.xukunz.wakeupmywall.core.wallpaper.BuiltInWallpapers
import com.xukunz.wakeupmywall.ui.components.PlaceholderScreen
import com.xukunz.wakeupmywall.ui.components.WallpaperBackground

@Composable
fun App(
    navigator: AppNavigator = remember { AppNavigator() },
    wallpaperId: String = BuiltInWallpapers.DefaultId,
) {
    val workspace by navigator.current.collectAsState()
    WakeUpMyWallTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            WallpaperBackground(wallpaperId)
            // Surface 保持透明，只借用 Material3 的 contentColor，让壁纸透出来。
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
            ) {
                when (workspace) {
                    Workspace.Dashboard -> PlaceholderScreen("Dashboard")
                    Workspace.Monitor -> PlaceholderScreen("PC Monitor")
                    Workspace.Settings -> PlaceholderScreen("Settings")
                }
            }
        }
    }
}
