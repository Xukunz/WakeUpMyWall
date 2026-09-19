package com.xukunz.wakeupmywall.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Workspace { Dashboard, Monitor, Settings }

/**
 * 三个工作空间的切换在 Phase 1 只是状态与手势，因此先用可单测的自有实现。
 * 等出现深链或复杂返回栈（Phase 2 设备详情）再评估引入导航库。
 */
class AppNavigator {
    private val state = MutableStateFlow(Workspace.Dashboard)
    val current: StateFlow<Workspace> = state.asStateFlow()

    fun goTo(workspace: Workspace) {
        state.value = workspace
    }

    fun next() {
        val order = Workspace.entries
        state.value = order[(order.indexOf(state.value) + 1).coerceAtMost(order.lastIndex)]
    }

    fun previous() {
        val order = Workspace.entries
        state.value = order[(order.indexOf(state.value) - 1).coerceAtLeast(0)]
    }
}
