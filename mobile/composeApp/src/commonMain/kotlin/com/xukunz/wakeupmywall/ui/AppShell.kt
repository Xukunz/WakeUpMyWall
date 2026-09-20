package com.xukunz.wakeupmywall.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.xukunz.wakeupmywall.ui.powerrail.PowerRail
import com.xukunz.wakeupmywall.ui.powerrail.PowerRailModel

enum class RailEvent { Primary, Sleep, Shutdown, Restart, Settings }

/**
 * 三工作空间的统一外壳：左 72% 主内容 + 右 28% 常驻 Power Rail。
 * 横屏锁定由外部（清单/运行策略）负责，这里只按 Row 布局，不写竖屏分支。
 */
@Composable
fun AppShell(
    rail: PowerRailModel,
    onRailEvent: (RailEvent) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Row(modifier = modifier.fillMaxSize()) {
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
}
