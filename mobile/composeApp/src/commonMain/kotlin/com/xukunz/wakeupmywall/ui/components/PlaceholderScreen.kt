package com.xukunz.wakeupmywall.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/**
 * 骨架占位页。Phase 1 会逐页替换为真实内容，但标签约定（`screen:<title>`）保持不变，
 * 这样 UI 测试可以跨越实现替换继续验证导航。
 */
@Composable
fun PlaceholderScreen(title: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().testTag("screen:$title"),
        contentAlignment = Alignment.Center,
    ) {
        Text(title)
    }
}
