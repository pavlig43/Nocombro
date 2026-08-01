package ru.pavlig43.notification.internal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.coreui.tooltip.ToolTipIconButton
import ru.pavlig43.notification.api.model.NotificationLevel
import ru.pavlig43.notification.api.ui.toColor
import ru.pavlig43.notification.api.ui.toContentColor
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.arrow_downward
import ru.pavlig43.theme.arrow_upward

@Suppress("LongParameterList")
@Composable
internal fun NotificationTitle(
    isShowFullList: Boolean,
    showFullList: () -> Unit,
    title: String,
    level: NotificationLevel,
    countNotification: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = level.toColor(),
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = countNotification.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = level.toContentColor(),
                )
            }
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
        )
        ToolTipIconButton(
            tooltipText = if (isShowFullList) "Свернуть" else "Показать",
            onClick = showFullList,
            icon = if (isShowFullList) Res.drawable.arrow_upward else Res.drawable.arrow_downward,
        )
    }
}
