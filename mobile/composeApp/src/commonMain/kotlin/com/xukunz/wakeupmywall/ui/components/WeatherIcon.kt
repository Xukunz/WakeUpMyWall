package com.xukunz.wakeupmywall.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.xukunz.wakeupmywall.core.theme.AppSizes
import com.xukunz.wakeupmywall.resources.Res
import com.xukunz.wakeupmywall.resources.weather_clear_day
import com.xukunz.wakeupmywall.resources.weather_clear_night
import com.xukunz.wakeupmywall.resources.weather_cloudy
import com.xukunz.wakeupmywall.resources.weather_fog
import com.xukunz.wakeupmywall.resources.weather_partly_cloudy_day
import com.xukunz.wakeupmywall.resources.weather_partly_cloudy_night
import com.xukunz.wakeupmywall.resources.weather_rain
import com.xukunz.wakeupmywall.resources.weather_sleet
import com.xukunz.wakeupmywall.resources.weather_snow
import com.xukunz.wakeupmywall.resources.weather_thunderstorm
import com.xukunz.wakeupmywall.resources.weather_unknown
import com.xukunz.wakeupmywall.resources.weather_wind
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * 图标选型用的天气分类。素材里有 11 类具名图标 + 1 张 [Unknown] 兜底：
 * provider 会返回几十种 condition 写法，认不出来的走 [Unknown]（用户补的专属素材），
 * 而不是假装成"多云"。
 */
enum class WeatherKind {
    ClearDay,
    ClearNight,
    PartlyCloudyDay,
    PartlyCloudyNight,
    Cloudy,
    Rain,
    Snow,
    Sleet,
    Fog,
    Wind,
    Thunderstorm,
    /** provider 返回的 condition 无法归类。 */
    Unknown,
}

/**
 * condition 文本 + 昼夜 → 图标分类。纯函数，`WeatherKindTest` 覆盖大小写、同义词与兜底。
 *
 * 匹配顺序有意义：`freezing rain` 必须是 Rain 而不是 Sleet，`thunderstorm` 必须在
 * `storm` 之前判掉，因此这里按"最具体的词先匹配"排列，而不是按字母序。
 */
fun weatherKind(condition: String, isNight: Boolean): WeatherKind {
    val text = condition.lowercase()
    fun has(vararg keys: String) = keys.any { it in text }

    return when {
        has("thunder", "storm") -> WeatherKind.Thunderstorm
        has("sleet", "freezing", "ice", "hail") -> WeatherKind.Sleet
        has("snow", "flurr", "blizzard") -> WeatherKind.Snow
        has("rain", "shower", "drizzle", "downpour") -> WeatherKind.Rain
        has("fog", "mist", "haze") -> WeatherKind.Fog
        has("wind", "gust", "breez") -> WeatherKind.Wind
        has("partly", "mostly clear", "few clouds", "scattered") ->
            if (isNight) WeatherKind.PartlyCloudyNight else WeatherKind.PartlyCloudyDay
        has("cloud", "overcast") -> WeatherKind.Cloudy
        has("clear", "sunny", "fair") ->
            if (isNight) WeatherKind.ClearNight else WeatherKind.ClearDay
        else -> WeatherKind.Unknown
    }
}

internal fun WeatherKind.drawable(): DrawableResource = when (this) {
    WeatherKind.ClearDay -> Res.drawable.weather_clear_day
    WeatherKind.ClearNight -> Res.drawable.weather_clear_night
    WeatherKind.PartlyCloudyDay -> Res.drawable.weather_partly_cloudy_day
    WeatherKind.PartlyCloudyNight -> Res.drawable.weather_partly_cloudy_night
    WeatherKind.Cloudy -> Res.drawable.weather_cloudy
    WeatherKind.Rain -> Res.drawable.weather_rain
    WeatherKind.Snow -> Res.drawable.weather_snow
    WeatherKind.Sleet -> Res.drawable.weather_sleet
    WeatherKind.Fog -> Res.drawable.weather_fog
    WeatherKind.Wind -> Res.drawable.weather_wind
    WeatherKind.Thunderstorm -> Res.drawable.weather_thunderstorm
    WeatherKind.Unknown -> Res.drawable.weather_unknown
}

/** 打包的天气图标（`imgs/ui/weather_*.png`，已由 tools/prepare_ui_assets.py 统一到 256×256 + 12% 边距）。 */
@Composable
fun WeatherIcon(
    condition: String,
    isNight: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = AppSizes.weatherIcon,
    contentDescription: String? = condition,
) {
    Image(
        painter = painterResource(weatherKind(condition, isNight).drawable()),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
    )
}
