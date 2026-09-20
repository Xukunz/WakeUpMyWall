package com.xukunz.wakeupmywall.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.xukunz.wakeupmywall.core.theme.AppShapes
import com.xukunz.wakeupmywall.core.theme.DarkSurface
import com.xukunz.wakeupmywall.core.theme.Spacing
import com.xukunz.wakeupmywall.resources.Res
import com.xukunz.wakeupmywall.resources.pc_default_cover
import com.xukunz.wakeupmywall.resources.pc_cover_1
import com.xukunz.wakeupmywall.resources.pc_cover_2
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * 设备 → 封面素材。3 张素材（`imgs/ui/pc_*cover*.png` → JPEG）分别给默认机与两台已配好的机器，
 * **取不到的 id 一律回落默认封面**：设备列表是用户数据，脏 id 不能让界面开天窗。
 */
internal fun pcCoverDrawable(deviceId: String?): DrawableResource = when (deviceId) {
    "living-room" -> Res.drawable.pc_cover_1
    "workstation" -> Res.drawable.pc_cover_2
    else -> Res.drawable.pc_default_cover
}

/** PC 封面缩略图（打包素材，尺寸由调用方给：`AppSizes.coverThumbnail*` / `coverIdentity*`）。 */
@Composable
fun PcCover(modifier: Modifier = Modifier, deviceId: String? = null) {
    Image(
        painter = painterResource(pcCoverDrawable(deviceId)),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(AppShapes.button)
            .background(DarkSurface.outline)
            .border(Spacing.hairline, DarkSurface.outline, AppShapes.button),
    )
}
