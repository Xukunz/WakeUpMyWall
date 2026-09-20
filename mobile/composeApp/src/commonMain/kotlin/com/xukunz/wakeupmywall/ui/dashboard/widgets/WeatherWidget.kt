package com.xukunz.wakeupmywall.ui.dashboard.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import com.xukunz.wakeupmywall.core.theme.AppSizes
import com.xukunz.wakeupmywall.core.theme.AppTypography
import com.xukunz.wakeupmywall.core.theme.DarkSurface
import com.xukunz.wakeupmywall.core.theme.Spacing
import com.xukunz.wakeupmywall.domain.model.WeatherSnapshot
import com.xukunz.wakeupmywall.ui.components.WeatherIcon
import com.xukunz.wakeupmywall.ui.components.WidgetStyle
import com.xukunz.wakeupmywall.ui.components.WidgetSurface
import com.xukunz.wakeupmywall.ui.icons.AppIcon
import com.xukunz.wakeupmywall.ui.icons.AppIconKind

/**
 * 天气卡（权威规格 B 行 2 槽 1）：图标 + 大字温度 + 右侧上下两行的最高/最低温 → 天气文字 →
 * 地点 → 分隔线 → 四列逐时（时刻在上、图标在中、温度在下）。
 *
 * 图标是打包素材（`imgs/ui/weather_*.png`，11 类 + 兜底），**不是字形占位**。
 */
@Composable
fun WeatherWidget(
    weather: WeatherSnapshot,
    style: WidgetStyle,
    modifier: Modifier = Modifier,
) {
    WidgetSurface(style = style, modifier = modifier.testTag("dashboard:weather")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            WeatherIcon(
                condition = weather.condition,
                isNight = weather.isNight,
                modifier = Modifier.testTag("dashboard:weather-icon"),
            )
            Text(
                text = "${weather.temperatureC}°",
                style = AppTypography.metricValue,
                modifier = Modifier.testTag("dashboard:weather-temp"),
            )
            Spacer(Modifier.weight(1f))
            // 概念图里最高/最低温是右对齐的两行，不是一行。
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "↑${weather.highC}°",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("dashboard:weather-high"),
                )
                Text(
                    text = "↓${weather.lowC}°",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("dashboard:weather-low"),
                )
            }
        }

        Text(
            text = weather.condition,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag("dashboard:weather-condition"),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            AppIcon(
                kind = AppIconKind.LocationPin,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                size = AppSizes.iconSmall,
            )
            Text(
                text = weather.location,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("dashboard:weather-location"),
            )
        }

        HorizontalDivider(color = DarkSurface.outline)

        Row(
            modifier = Modifier.fillMaxWidth().testTag("dashboard:weather-hours"),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            weather.nextHours.forEach { hour ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Text(
                        text = hour.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    WeatherIcon(
                        condition = hour.condition,
                        isNight = hour.isNight,
                        size = AppSizes.weatherIconHourly,
                        contentDescription = null,
                    )
                    Text(
                        text = "${hour.temperatureC}°",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("dashboard:weather-hour-${hour.label}"),
                    )
                }
            }
        }
    }
}
