package com.xukunz.wakeupmywall.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 图标选型是纯逻辑，必须可测：素材只有 11 类，provider 会返回几十种 condition 写法，
 * 落到哪一类决定了用户看到哪张图，因此这里把映射规则钉住。
 */
class WeatherKindTest {

    @Test
    fun `day and night pick different clear and partly cloudy icons`() {
        assertEquals(WeatherKind.ClearDay, weatherKind("Clear", isNight = false))
        assertEquals(WeatherKind.ClearNight, weatherKind("Clear", isNight = true))
        assertEquals(WeatherKind.PartlyCloudyDay, weatherKind("Partly Cloudy", isNight = false))
        assertEquals(WeatherKind.PartlyCloudyNight, weatherKind("Partly Cloudy", isNight = true))
    }

    @Test
    fun `matching is case insensitive and tolerates provider wording`() {
        assertEquals(WeatherKind.Cloudy, weatherKind("OVERCAST", isNight = false))
        assertEquals(WeatherKind.Rain, weatherKind("light drizzle", isNight = false))
        assertEquals(WeatherKind.Rain, weatherKind("showers", isNight = false))
        assertEquals(WeatherKind.PartlyCloudyDay, weatherKind("Few Clouds", isNight = false))
        assertEquals(WeatherKind.Snow, weatherKind("light snow flurries", isNight = false))
    }

    @Test
    fun `specific conditions win over the generic ones`() {
        // `freezing rain` 里既有 rain 又有 freezing：必须落到 Sleet，否则下冻雨的图标会变成普通雨。
        assertEquals(WeatherKind.Sleet, weatherKind("Freezing Rain", isNight = false))
        // `thunderstorm` 里既有 thunder 又有 storm 也有 cloud，取最具体的雷暴。
        assertEquals(WeatherKind.Thunderstorm, weatherKind("Thunderstorm", isNight = false))
    }

    @Test
    fun `unknown conditions fall back to the dedicated unknown icon instead of failing`() {
        // 空串与刁钻天气都走 weather_unknown.png（2026-09-20 用户补的素材），
        // 不再假装成"多云"——把不知道的天气画成多云是错误信息，不是兜底。
        assertEquals(WeatherKind.Unknown, weatherKind("", isNight = false))
        assertEquals(WeatherKind.Unknown, weatherKind("volcanic ash", isNight = true))
    }

    @Test
    fun `every concept hourly slot maps to a bundled icon`() {
        // 概念图四列依次是 月亮+云 / 云 / 云 / 云（见 MockData.weather）。
        assertEquals(WeatherKind.PartlyCloudyNight, weatherKind("Partly Cloudy", isNight = true))
        assertEquals(WeatherKind.Cloudy, weatherKind("Cloudy", isNight = true))
    }
}
