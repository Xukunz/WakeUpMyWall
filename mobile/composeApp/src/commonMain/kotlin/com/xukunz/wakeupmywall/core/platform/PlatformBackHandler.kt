package com.xukunz.wakeupmywall.core.platform

import androidx.compose.runtime.Composable

/**
 * 系统返回（Android 的返回手势/返回键）。放在 commonMain 的 expect 里，
 * 是因为桌面 target 只用于 UI 测试与设计预览，没有"系统返回"这个概念。
 *
 * 为什么需要它：进入 PC 卡片（Monitor）后，用户的第一反应是侧滑返回 / 按返回键，
 * 而不是去点卡片右上角那个 `>`（用户实测反馈"反直觉"）。
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
