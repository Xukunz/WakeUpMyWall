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
import com.xukunz.wakeupmywall.resources.cherry_wallpaper
import com.xukunz.wakeupmywall.resources.cherry_wallpaper_thumb
import com.xukunz.wakeupmywall.resources.aurora_wallpaper_thumb
import com.xukunz.wakeupmywall.resources.city_night_wallpaper
import com.xukunz.wakeupmywall.resources.city_night_wallpaper_thumb
import com.xukunz.wakeupmywall.resources.dusk_lake_wallpaper
import com.xukunz.wakeupmywall.resources.dusk_lake_wallpaper_thumb
import com.xukunz.wakeupmywall.resources.forest_mist_wallpaper
import com.xukunz.wakeupmywall.resources.forest_mist_wallpaper_thumb
import com.xukunz.wakeupmywall.resources.minimal_wallpaper
import com.xukunz.wakeupmywall.resources.minimal_wallpaper_thumb
import com.xukunz.wakeupmywall.resources.space_wallpaper
import com.xukunz.wakeupmywall.resources.space_wallpaper_thumb
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * 编目项 → 打包资源。刻意不用 `else` 兜底：新增壁纸却忘记打包资源时必须直接炸掉，
 * 而不是静默显示成另一张图。`AppUiTest.every built in wallpaper renders` 会遍历整张表，
 * 因此新增编目项却没打包资源会在测试阶段暴露，而不是等到用户看到错图。
 */
internal fun Wallpaper.drawable(): DrawableResource = when (id) {
    BuiltInWallpapers.Aurora.id -> Res.drawable.aurora_wallpaper
    BuiltInWallpapers.DuskLake.id -> Res.drawable.dusk_lake_wallpaper
    BuiltInWallpapers.ForestMist.id -> Res.drawable.forest_mist_wallpaper
    BuiltInWallpapers.CityNight.id -> Res.drawable.city_night_wallpaper
    BuiltInWallpapers.Space.id -> Res.drawable.space_wallpaper
    BuiltInWallpapers.Cherry.id -> Res.drawable.cherry_wallpaper
    BuiltInWallpapers.Minimal.id -> Res.drawable.minimal_wallpaper
    else -> error("Unmapped built-in wallpaper: $id")
}

/**
 * 编目项 → 缩略图资源（Appearance 的 Wallpaper 段）。与 [drawable] 一样刻意不兜底：
 * 新增壁纸却漏了缩略图，`AppearanceScreenTest.wallpaper thumbnails cover the bundled catalogue`
 * 会在渲染阶段直接失败，而不是悄悄显示成别的图。
 */
internal fun Wallpaper.thumbnail(): DrawableResource = when (id) {
    BuiltInWallpapers.Aurora.id -> Res.drawable.aurora_wallpaper_thumb
    BuiltInWallpapers.DuskLake.id -> Res.drawable.dusk_lake_wallpaper_thumb
    BuiltInWallpapers.ForestMist.id -> Res.drawable.forest_mist_wallpaper_thumb
    BuiltInWallpapers.CityNight.id -> Res.drawable.city_night_wallpaper_thumb
    BuiltInWallpapers.Space.id -> Res.drawable.space_wallpaper_thumb
    BuiltInWallpapers.Cherry.id -> Res.drawable.cherry_wallpaper_thumb
    BuiltInWallpapers.Minimal.id -> Res.drawable.minimal_wallpaper_thumb
    else -> error("Unmapped wallpaper thumbnail: $id")
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
