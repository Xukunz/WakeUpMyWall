package com.xukunz.wakeupmywall.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import com.xukunz.wakeupmywall.core.theme.AppShapes
import com.xukunz.wakeupmywall.core.theme.DarkSurface
import com.xukunz.wakeupmywall.core.theme.Spacing

@Composable
fun WidgetSurface(
    style: WidgetStyle,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val chrome = Modifier
        .clip(AppShapes.card)
        .background(DarkSurface.card.copy(alpha = style.surfaceAlpha()))
        .let { base ->
            if (style.showsBorder()) {
                base.border(Spacing.hairline, DarkSurface.outline, AppShapes.card)
            } else {
                base
            }
        }
        .padding(Spacing.md)
        .testTag("surface:${style.name}")

    Column(modifier = modifier.then(chrome), content = content)
}
