package com.xukunz.wakeupmywall.ui.dashboard.widgets

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.xukunz.wakeupmywall.ui.components.WidgetStyle
import com.xukunz.wakeupmywall.ui.components.WidgetSurface

/** 权威规格 B 行 1：`Good Evening` 的后半段用 accent 色，下面一行全大写标语。 */
@Composable
fun GreetingWidget(
    greeting: String,
    style: WidgetStyle,
    modifier: Modifier = Modifier,
) {
    WidgetSurface(style = style, modifier = modifier.testTag("dashboard:greeting")) {
        val words = greeting.trim().split(' ')
        val head = words.dropLast(1).joinToString(" ")
        val tail = words.lastOrNull().orEmpty()
        Text(
            text = buildAnnotatedString {
                if (head.isNotEmpty()) append("$head ")
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append(tail) }
            },
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = "A CALMER DESKTOP. A BRIGHTER YOU.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("dashboard:greeting-tagline"),
        )
    }
}
