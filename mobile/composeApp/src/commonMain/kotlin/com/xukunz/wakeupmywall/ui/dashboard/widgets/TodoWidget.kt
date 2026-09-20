package com.xukunz.wakeupmywall.ui.dashboard.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.text.style.TextDecoration
import com.xukunz.wakeupmywall.core.theme.AppSizes
import com.xukunz.wakeupmywall.core.theme.DarkSurface
import com.xukunz.wakeupmywall.core.theme.Spacing
import com.xukunz.wakeupmywall.domain.model.TodoItem
import com.xukunz.wakeupmywall.ui.components.SectionHeader
import com.xukunz.wakeupmywall.ui.components.WidgetStyle
import com.xukunz.wakeupmywall.ui.components.WidgetSurface
import com.xukunz.wakeupmywall.ui.icons.AppIcon
import com.xukunz.wakeupmywall.ui.icons.AppIconKind

@Composable
fun TodoWidget(
    todos: List<TodoItem>,
    style: WidgetStyle,
    modifier: Modifier = Modifier,
) {
    WidgetSurface(style = style, modifier = modifier.testTag("dashboard:todo")) {
        SectionHeader(
            title = "My Tasks",
            trailing = {
                Text(
                    // 概念图的 `3 of 5` 数的是**待办剩余**（图里 2 项已完成）而不是已完成数。
                    text = "${todos.count { !it.done }} of ${todos.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("dashboard:todo-count"),
                )
            },
        )

        todos.forEach { todo ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 完成项实心圆 + 白色对勾、未完成项空心圈（权威规格 B 行 2），图形是代码绘制的。
                Checkbox(done = todo.done)
                Text(
                    text = todo.title,
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = if (todo.done) TextDecoration.LineThrough else null,
                    color = if (todo.done) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }

        Row(
            modifier = Modifier.testTag("dashboard:todo-add"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            AppIcon(
                kind = AppIconKind.Plus,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                size = AppSizes.iconSmall,
            )
            Text(
                text = "Add a task",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 待办勾选圈：完成 = 实心强调色圆 + 对勾，未完成 = 空心圆。 */
@Composable
private fun Checkbox(done: Boolean) {
    val accent = MaterialTheme.colorScheme.primary
    if (done) {
        Box(
            modifier = Modifier.size(AppSizes.checkbox).clip(CircleShape).background(accent),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(
                kind = AppIconKind.Check,
                tint = MaterialTheme.colorScheme.onPrimary,
                size = AppSizes.checkbox,
            )
        }
    } else {
        Box(
            modifier = Modifier
                .size(AppSizes.checkbox)
                .clip(CircleShape)
                .border(Spacing.hairline, DarkSurface.outline, CircleShape),
        )
    }
}
