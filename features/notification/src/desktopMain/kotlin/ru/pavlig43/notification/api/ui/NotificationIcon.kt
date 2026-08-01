package ru.pavlig43.notification.api.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.pavlig43.notification.api.model.NotificationLevel

@Composable
fun NotificationIcon(
    level: NotificationLevel,
    countNotification: Int,
    modifier: Modifier = Modifier.size(36.dp),
    isSelected: Boolean = false,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .border(
                border = if (isSelected) {
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                } else {
                    CardDefaults.outlinedCardBorder()
                },
                shape = CardDefaults.shape,
            )
            .background(
                color = level.toColor(),
                shape = CardDefaults.shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = countNotification.toString(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = level.toContentColor(),
            ),
        )
    }
}

@Composable
internal fun NotificationLevel.toColor(): Color = when (this) {
    NotificationLevel.HIGH -> MaterialTheme.colorScheme.errorContainer
    NotificationLevel.MEDIUM -> MaterialTheme.colorScheme.primary
    NotificationLevel.LOW -> Color.Green
}

@Composable
internal fun NotificationLevel.toContentColor(): Color = when (this) {
    NotificationLevel.HIGH -> MaterialTheme.colorScheme.onErrorContainer
    NotificationLevel.MEDIUM -> MaterialTheme.colorScheme.onPrimary
    NotificationLevel.LOW -> Color.Black
}
