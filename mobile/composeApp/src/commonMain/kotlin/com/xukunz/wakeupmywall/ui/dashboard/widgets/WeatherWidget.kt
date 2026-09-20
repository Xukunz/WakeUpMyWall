package com.xukunz.wakeupmywall.ui.dashboard.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.xukunz.wakeupmywall.core.theme.AppTypography
import com.xukunz.wakeupmywall.domain.model.WeatherSnapshot
import com.xukunz.wakeupmywall.ui.components.WidgetStyle
import com.xukunz.wakeupmywall.ui.components.WidgetSurface

@Composable
fun WeatherWidget(
    weather: WeatherSnapshot,
    style: WidgetStyle,
    modifier: Modifier = Modifier,
) {
    WidgetSurface(style = style, modifier = modifier.testTag("dashboard:weather")) {
        Row(horizontalArrangement = Arrangement.spacedBy(com.xukunz.wakeupmywall.core.theme.Spacing.sm)) {
            Text("☁", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "${weather.temperatureC}°",
                style = AppTypography.metricValue,
                modifier = Modifier.testTag("dashboard:weather-temp"),
            )
        }
        Text(weather.condition, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = "↑${weather.highC}° ↓${weather.lowC}°",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("dashboard:weather-range"),
        )
        Text(
            text = "📍${weather.location}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("dashboard:weather-location"),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            weather.nextHours.forEach { hour ->
                Text(
                    text = "${hour.label} ${hour.temperatureC}°",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
