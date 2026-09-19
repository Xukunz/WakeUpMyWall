package com.xukunz.wakeupmywall.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import com.xukunz.wakeupmywall.core.wallpaper.BuiltInWallpapers
import com.xukunz.wakeupmywall.core.wallpaper.Wallpaper
import com.xukunz.wakeupmywall.resources.Res
import com.xukunz.wakeupmywall.resources.aurora_wallpaper
import com.xukunz.wakeupmywall.resources.minimal_wallpaper
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * 编目项 → 打包资源。刻意不用 `else` 兜底：新增壁纸却忘记打包资源时必须直接炸掉，
 * 而不是静默显示成另一张图。`AppUiTest.every built in wallpaper renders` 会遍历整张表，
 * 因此新增编目项却没打包资源会在测试阶段暴露，而不是等到用户看到错图。
 */
internal fun Wallpaper.drawable(): DrawableResource = when (id) {
    BuiltInWallpapers.Aurora.id -> Res.drawable.aurora_wallpaper
    BuiltInWallpapers.Minimal.id -> Res.drawable.minimal_wallpaper
    else -> error("Unmapped built-in wallpaper: $id")
}

@Composable
fun WallpaperBackground(wallpaperId: String?, modifier: Modifier = Modifier) {
    val wallpaper = BuiltInWallpapers.byId(wallpaperId)
    Box(modifier = modifier.fillMaxSize().testTag("wallpaper:${wallpaper.id}")) {
        Image(
            painter = painterResource(wallpaper.drawable()),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = wallpaper.scrimAlpha)))
    }
}
