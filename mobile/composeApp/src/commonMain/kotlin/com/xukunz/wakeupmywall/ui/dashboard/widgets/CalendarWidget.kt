package com.xukunz.wakeupmywall.ui.dashboard.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import com.xukunz.wakeupmywall.core.theme.AppSizes
import com.xukunz.wakeupmywall.core.theme.Spacing
import com.xukunz.wakeupmywall.domain.model.CalendarEvent
import com.xukunz.wakeupmywall.ui.components.WidgetStyle
import com.xukunz.wakeupmywall.ui.components.WidgetSurface
import com.xukunz.wakeupmywall.ui.icons.AppIcon
import com.xukunz.wakeupmywall.ui.icons.AppIconKind

private val weekLetters = listOf("S", "M", "T", "W", "T", "F", "S")
private val weekDates = (20..26).map { it.toString() }
private const val SELECTED_DAY = "22"

@Composable
fun CalendarWidget(
    date: String,
    events: List<CalendarEvent>,
    style: WidgetStyle,
    modifier: Modifier = Modifier,
) {
    WidgetSurface(style = style, modifier = modifier.testTag("dashboard:calendar")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(date, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = "This Week",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AppIcon(
                    kind = AppIconKind.Plus,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    size = AppSizes.iconSmall,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().testTag("dashboard:calendar-week"),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            weekLetters.forEachIndexed { index, letter ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = letter,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val day = weekDates[index]
                    if (day == SELECTED_DAY) {
                        Box(
                            modifier = Modifier
                                .size(AppSizes.statusDot * 2)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(day, style = MaterialTheme.typography.labelSmall)
                        }
                    } else {
                        Text(day, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        events.forEachIndexed { index, event ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                val dotColor = when (index % 3) {
                    0 -> MaterialTheme.colorScheme.primary
                    1 -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.error
                }
                Box(Modifier.size(AppSizes.statusDot).clip(CircleShape).background(dotColor))
                Text(
                    text = event.time,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(event.title, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
