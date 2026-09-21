package com.xukunz.wakeupmywall.domain.model

/** Monitor（硬件界面）上的卡片。默认顺序就是 Phase 1 定下的布局，用户不动就零变化。 */
enum class MonitorCardId(val key: String, val label: String) {
    Cpu("cpu", "CPU"),
    Gpu("gpu", "GPU"),
    Ram("ram", "RAM"),
    Storage("storage", "Storage"),
    Temps("temps", "System Temps"),
    Network("network", "Network"),
    Uptime("uptime", "Uptime"),
    Activity("activity", "Recent Activity"),
    Quote("quote", "Quote"),
}

/** 卡片页脚可以显示哪一类信息（预设集合，不是任意字段——卡片宽度与字号是按固定文案实测的）。 */
enum class MonitorCardDetail(val label: String) {
    Clock("Clock"),
    Cores("Cores"),
    Temp("Temp"),
    Usage("Usage"),
    Vram("VRAM"),
    Free("Free Space"),
}

data class MonitorCardConfig(
    val id: MonitorCardId,
    val enabled: Boolean = true,
    /** null = 用这张卡的默认信息类别。 */
    val detail: MonitorCardDetail? = null,
)

/**
 * 卡片布局的纯函数归约：顺序（move）、显隐（toggle）、信息类别（setDetail/cycleDetail）。
 * 全部返回新列表，非法输入保持原值（与 Phase 1 的 `AppearanceReducer` 同一约定）。
 */
object MonitorLayout {

    val Default: List<MonitorCardConfig> = listOf(
        MonitorCardConfig(MonitorCardId.Cpu, detail = MonitorCardDetail.Clock),
        MonitorCardConfig(MonitorCardId.Gpu, detail = MonitorCardDetail.Temp),
        MonitorCardConfig(MonitorCardId.Ram, detail = MonitorCardDetail.Usage),
        MonitorCardConfig(MonitorCardId.Storage),
        MonitorCardConfig(MonitorCardId.Temps),
        MonitorCardConfig(MonitorCardId.Network),
        MonitorCardConfig(MonitorCardId.Uptime),
        MonitorCardConfig(MonitorCardId.Activity),
        MonitorCardConfig(MonitorCardId.Quote),
    )

    /** 这张卡支持哪些信息类别；空列表 = 整卡语义，页脚不随类别变。 */
    fun supportedDetails(id: MonitorCardId): List<MonitorCardDetail> = when (id) {
        MonitorCardId.Cpu -> listOf(MonitorCardDetail.Clock, MonitorCardDetail.Cores, MonitorCardDetail.Temp)
        MonitorCardId.Gpu -> listOf(MonitorCardDetail.Temp, MonitorCardDetail.Vram, MonitorCardDetail.Usage)
        MonitorCardId.Ram -> listOf(MonitorCardDetail.Usage, MonitorCardDetail.Free)
        MonitorCardId.Storage -> listOf(MonitorCardDetail.Usage, MonitorCardDetail.Free)
        else -> emptyList()
    }

    /** 有效的信息类别：没设过就取该卡的第一项（Storage 这类没有类别的返回 null）。 */
    fun effectiveDetail(config: MonitorCardConfig): MonitorCardDetail? =
        config.detail ?: supportedDetails(config.id).firstOrNull()

    /** 上移/下移一格；到头或到尾不动。 */
    fun move(cards: List<MonitorCardConfig>, id: MonitorCardId, delta: Int): List<MonitorCardConfig> {
        val from = cards.indexOfFirst { it.id == id }
        if (from < 0) return cards
        val to = from + delta
        if (to !in cards.indices) return cards

        return cards.toMutableList().apply { add(to, removeAt(from)) }
    }

    /** 显示/隐藏。**不允许把最后一张卡也关掉**（空白页比少一张卡更让人困惑）。 */
    fun toggle(cards: List<MonitorCardConfig>, id: MonitorCardId): List<MonitorCardConfig> {
        val index = cards.indexOfFirst { it.id == id }
        if (index < 0) return cards
        if (cards[index].enabled && cards.count { it.enabled } <= 1) return cards

        return cards.toMutableList().apply {
            this[index] = this[index].copy(enabled = !this[index].enabled)
        }
    }

    /** 设定信息类别；这张卡不支持的取值一律保持原值。 */
    fun setDetail(
        cards: List<MonitorCardConfig>,
        id: MonitorCardId,
        detail: MonitorCardDetail,
    ): List<MonitorCardConfig> {
        val index = cards.indexOfFirst { it.id == id }
        if (index < 0 || detail !in supportedDetails(id)) return cards
        return cards.toMutableList().apply { this[index] = this[index].copy(detail = detail) }
    }

    /** 在支持的类别里循环，供编辑器上的单按钮使用。 */
    fun cycleDetail(cards: List<MonitorCardConfig>, id: MonitorCardId): List<MonitorCardConfig> {
        val options = supportedDetails(id)
        if (options.isEmpty()) return cards
        val current = cards.firstOrNull { it.id == id }?.let { effectiveDetail(it) }
        val index = options.indexOf(current)
        val next = options[if (index < 0) 0 else (index + 1) % options.size]
        return setDetail(cards, id, next)
    }

    /** 当前显示的卡片（按配置顺序）。 */
    fun visible(cards: List<MonitorCardConfig>): List<MonitorCardConfig> = cards.filter { it.enabled }
}
