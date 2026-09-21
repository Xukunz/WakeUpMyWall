package com.xukunz.wakeupmywall.core.platform

import androidx.compose.runtime.Composable

/** 桌面预览没有系统返回：什么都不做（UI 测试靠点左上角返回按钮）。 */
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit
