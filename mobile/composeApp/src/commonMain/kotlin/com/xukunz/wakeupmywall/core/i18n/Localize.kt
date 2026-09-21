package com.xukunz.wakeupmywall.core.i18n

/**
 * 把**领域层产出的英文词**翻成当前语言。
 *
 * 为什么不把 `AppStrings` 塞进领域层（`PcState.capabilities`）：
 * 那些英文词是 Phase 1/3 视觉基线与一堆断言的基础，改签名会牵动状态机与所有测试；
 * 而"按已知英文词查表"能在**英文逐字不变**的前提下支持多语言。
 *
 * 约定：表里没有的词原样返回（宁可不翻，也不要翻错）。
 */
fun AppStrings.localize(english: String): String {
    if (this === EnglishStrings) return english
    return Translations[english]?.invoke(this) ?: english
}

private val Translations: Map<String, (AppStrings) -> String> = mapOf(
    // 状态词（PcState.shortLabel）
    "Online" to { it.online },
    "Offline" to { it.offline },
    "Ready to wake" to { it.readyToWake },
    "Waking…" to { it.waking },
    "Sleeping…" to { it.sleeping },
    "Restarting…" to { it.restarting },
    "Shutting down…" to { it.shuttingDown },
    "Agent unavailable" to { it.agentUnavailable },
    "Setup PC" to { it.setupPc },
    "Error" to { it.restart },
    // 主按钮与次级动作
    "POWER CONTROL" to { it.powerControl },
    "Wake PC" to { it.wakePc },
    "PC Online" to { it.pcOnline },
    "Power On" to { it.wakePc },
    "Restart" to { it.restart },
    "SNAP MODE" to { it.snapMode },
    "POWER OFF" to { it.powerOff },
    "FRESH START" to { it.freshStart },
    // 连接条与状态行（capabilities.statusText）
    "Agent connected over LAN" to { it.agentConnected },
    "Wake-on-LAN Ready" to { it.wakeOnLanReady },
    "Wake-on-LAN requires MAC" to { it.wakeOnLanReady },
    "Host reachable, Agent not responding" to { it.agentUnavailable },
    "Waiting for Agent" to { it.waking },
    "Add your PC to begin" to { it.setupPc },
    "Last command failed" to { it.restart },
    // Monitor 卡片与身份卡
    "Storage" to { it.storage },
    "Network" to { it.network },
    "Uptime" to { it.uptime },
    "System Temps" to { it.systemTemps },
    "Fans" to { it.fans },
    "Recent Activity" to { it.recentActivity },
    "Quick Actions" to { it.quickActions },
    "Working set" to { it.workingSet },
    "RAM" to { it.ram },
    "CPU" to { it.cpu },
    "GPU" to { it.gpu },
    // 设置导航
    "Settings" to { it.settings },
    "Device Setup" to { it.deviceSetup },
    "Integrations" to { it.integrations },
    "Display & Behavior" to { it.displayAndBehavior },
    "Notifications" to { it.notifications },
    "Appearance" to { it.appearance },
    "Backup & Sync" to { it.backupAndSync },
    "About" to { it.about },
    "Reset to Default" to { it.resetToDefault },
    // App 层的提示语
    "Pair the phone in Device Setup first (Agent section)" to { it.pairFirstHint },
    "Add a device first" to { it.addDeviceFirst },
    "Could not reach the Agent" to { it.couldNotReachAgent },
)
