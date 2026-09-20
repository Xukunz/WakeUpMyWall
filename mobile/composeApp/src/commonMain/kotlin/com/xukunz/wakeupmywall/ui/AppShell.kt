package com.xukunz.wakeupmywall.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.xukunz.wakeupmywall.ui.components.Breakpoints
import com.xukunz.wakeupmywall.ui.powerrail.PowerRail
import com.xukunz.wakeupmywall.ui.powerrail.PowerRailCompact
import com.xukunz.wakeupmywall.ui.powerrail.PowerRailModel

enum class RailEvent { Primary, Sleep, Shutdown, Restart, Settings }

/**
 * 三工作空间的统一外壳，两种形态由窗口比例决定（见 [Breakpoints.usesSideRail]）：
 *
 * - **侧栏**：概念图的 72% 主内容 + 28% Power Rail，用于墙面屏（1280×720）与展开内屏这类横屏宽窗口；
 * - **底栏**：竖屏手机（20:9）、折叠外屏（21.1:9）、以及任何窄窗口，主内容在上、常驻控制在底部。
 *
 * 两种形态渲染的是同一份 `PowerRailModel` 与同一批 testTag，界面结构变了但语义没变。
 * 真正的朝向兼容靠的是这个判断 + 各屏内部的断点，而不是锁死横屏。
 */
@Composable
fun AppShell(
    rail: PowerRailModel,
    onRailEvent: (RailEvent) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (Breakpoints.usesSideRail(maxWidth, maxHeight)) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier.weight(0.72f).fillMaxHeight().testTag("appshell:main"),
                    content = content,
                )
                // 槽位标签放在外层 Box：同一个节点上链式 testTag 只有先写的那个生效，
                // 直接给 PowerRail 传 tag 会把 Rail 自己的 `powerrail` 标签顶掉。
                Box(modifier = Modifier.weight(0.28f).fillMaxHeight().testTag("appshell:rail")) {
                    PowerRail(
                        model = rail,
                        onPrimary = { onRailEvent(RailEvent.Primary) },
                        onSleep = { onRailEvent(RailEvent.Sleep) },
                        onShutdown = { onRailEvent(RailEvent.Shutdown) },
                        onRestart = { onRailEvent(RailEvent.Restart) },
                        onSettings = { onRailEvent(RailEvent.Settings) },
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().testTag("appshell:main"),
                    content = content,
                )
                Box(modifier = Modifier.fillMaxWidth().testTag("appshell:rail")) {
                    PowerRailCompact(
                        model = rail,
                        onPrimary = { onRailEvent(RailEvent.Primary) },
                        onSleep = { onRailEvent(RailEvent.Sleep) },
                        onShutdown = { onRailEvent(RailEvent.Shutdown) },
                        onRestart = { onRailEvent(RailEvent.Restart) },
                        onSettings = { onRailEvent(RailEvent.Settings) },
                    )
                }
            }
        }
    }
}
