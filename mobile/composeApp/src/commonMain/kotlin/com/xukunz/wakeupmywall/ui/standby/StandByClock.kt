package com.xukunz.wakeupmywall.ui.standby

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.xukunz.wakeupmywall.core.theme.AppTypography
import com.xukunz.wakeupmywall.core.theme.Spacing

/** 权威规格 D：超大时钟，分钟用 accent 色，下方长日期。 */
@Composable
fun StandByClock(
    time: String,
    meridiem: String,
    date: String,
    modifier: Modifier = Modifier,
) {
    val hour = time.substringBefore(':') + ":"
    val minute = time.substringAfter(':')
    Column(
        modifier = modifier.testTag("standby:clock"),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = hour,
                style = AppTypography.hugeClock,
                modifier = Modifier.testTag("standby:clock-hour"),
            )
            Text(
                text = minute,
                style = AppTypography.hugeClock,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("standby:clock-minute"),
            )
            Text(
                text = meridiem,
                style = AppTypography.metricUnit,
                modifier = Modifier.padding(start = Spacing.sm).testTag("standby:clock-meridiem"),
            )
        }
        Text(
            text = date,
            style = AppTypography.title,
            modifier = Modifier.testTag("standby:clock-date"),
        )
    }
}
