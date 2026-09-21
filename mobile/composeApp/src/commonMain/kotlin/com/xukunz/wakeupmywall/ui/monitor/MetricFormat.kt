package com.xukunz.wakeupmywall.ui.monitor

import kotlin.math.roundToInt

/**
 * 读数格式化：**没读到就是 `—`**，绝不显示 0、`null` 或 `NaN`。
 * Phase 5B 起所有指标都来自 Agent（字段可空），因此显示层只认这一套规则：
 *  - 百分比与温度取整（`28%` / `68°C`），与 Phase 1 的概念图排版一致；
 *  - 频率/容量/带宽留 1 位小数，整数不拖 `.0`（`2.0` → `2`）；
 *  - 转速带千分位（`1,240`）。
 */
const val UnknownReading: String = "—"

fun Float?.number(): String = this?.let {
    val rounded = (it * 10f).roundToInt() / 10f
    if (rounded % 1f == 0f) rounded.toInt().toString() else rounded.toString()
} ?: UnknownReading

fun Int?.number(): String = this?.toString() ?: UnknownReading

fun Float?.percentText(): String = this?.let { "${it.roundToInt()}%" } ?: UnknownReading

fun Int?.percentText(): String = this?.let { "$it%" } ?: UnknownReading

fun Float?.celsiusText(): String = this?.let { "${it.roundToInt()}°C" } ?: UnknownReading

fun Int?.celsiusText(): String = this?.let { "$it°C" } ?: UnknownReading

/** 千分位：概念图里 `1,240 RPM` 是带分隔符的。 */
fun Int?.rpmText(): String = this?.let { rpm ->
    val text = rpm.toString()
    if (text.length <= 3) text else text.dropLast(3) + "," + text.takeLast(3)
} ?: UnknownReading

/** `3d 6h 24m`；没读到就 `—`。 */
fun Long?.uptimeText(): String = this?.let { seconds ->
    val days = seconds / 86_400
    val hours = (seconds % 86_400) / 3_600
    val minutes = (seconds % 3_600) / 60
    "${days}d ${hours}h ${minutes}m"
} ?: UnknownReading

/** 文本类读数（启动日期等）：空串与 `null` 都是"没读到"。 */
fun String?.orUnknown(): String = this?.takeIf { it.isNotBlank() } ?: UnknownReading
