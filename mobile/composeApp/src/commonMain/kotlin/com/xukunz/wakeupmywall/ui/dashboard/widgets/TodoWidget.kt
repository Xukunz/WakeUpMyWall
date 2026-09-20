package com.xukunz.wakeupmywall.ui.dashboard.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import com.xukunz.wakeupmywall.core.theme.Spacing
import com.xukunz.wakeupmywall.domain.model.TodoItem
import com.xukunz.wakeupmywall.ui.components.SectionHeader
import com.xukunz.wakeupmywall.ui.components.WidgetStyle
import com.xukunz.wakeupmywall.ui.components.WidgetSurface

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
                    text = "${todos.count { it.done }} of ${todos.size}",
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
                // 完成项实心勾、未完成项空心圈（权威规格 B 行 2）
                Text(
                    text = if (todo.done) "●" else "○",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (todo.done) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
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

        Text(
            text = "+ Add a task",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("dashboard:todo-add"),
        )
    }
}
