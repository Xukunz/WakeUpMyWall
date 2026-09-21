package com.xukunz.wakeupmywall.ui.monitor

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import com.xukunz.wakeupmywall.core.theme.WakeUpMyWallTheme
import com.xukunz.wakeupmywall.data.mock.MockData
import com.xukunz.wakeupmywall.domain.model.MonitorCardDetail
import com.xukunz.wakeupmywall.domain.model.MonitorCardId
import com.xukunz.wakeupmywall.domain.model.MonitorLayout
import com.xukunz.wakeupmywall.ui.components.WidgetStyle
import kotlin.test.Test

/**
 * 卡片自定义在**渲染侧**的效果：藏起来的卡不出现、信息类别决定页脚主行。
 * 归约规则本身在 `MonitorLayoutTest` 里验，这里只验"配置真的传到界面上"。
 */
@OptIn(ExperimentalTestApi::class)
class MonitorCardLayoutTest {

    private val history = mapOf(MetricKeys.Cpu to List(60) { 20f })

    @Test
    fun `a hidden card is not rendered while its neighbours stay`() = runComposeUiTest {
        val cards = MonitorLayout.toggle(MonitorLayout.Default, MonitorCardId.Cpu)

        setContent {
            WakeUpMyWallTheme {
                MonitorMode(MockData.metrics, history, WidgetStyle.Glass, identity = MockData.hardware, cards = cards)
            }
        }

        onNodeWithTag("metric:cpu").assertDoesNotExist()
        onNodeWithTag("metric:gpu").assertExists()
        onNodeWithTag("metric:storage").assertExists()
    }

    @Test
    fun `the chosen detail drives the card footer`() = runComposeUiTest {
        val cards = MonitorLayout.setDetail(MonitorLayout.Default, MonitorCardId.Cpu, MonitorCardDetail.Cores)

        setContent {
            WakeUpMyWallTheme {
                MonitorMode(MockData.metrics, history, WidgetStyle.Glass, identity = MockData.hardware, cards = cards)
            }
        }

        onNodeWithTag("metric:cpu-footer-primary").assertTextEquals("8 cores 16 threads")
        onNodeWithTag("metric:cpu-footer-secondary").assertTextEquals("4.9 GHz")
    }

    @Test
    fun `the default layout still renders every card`() = runComposeUiTest {
        setContent {
            WakeUpMyWallTheme {
                MonitorMode(MockData.metrics, history, WidgetStyle.Glass, identity = MockData.hardware)
            }
        }

        listOf("cpu", "gpu", "ram", "storage", "temps", "network", "uptime").forEach { key ->
            onNodeWithTag("metric:$key").assertExists()
        }
        onNodeWithTag("monitor:activity").assertExists()
        onNodeWithTag("monitor:quote").assertExists()
    }
}
