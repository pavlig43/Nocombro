package ru.pavlig43.notification.internal.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import ru.pavlig43.notification.internal.model.NotificationUi

@Suppress("MagicNumber")
@Composable
internal fun NotificationItemList(
    listNotificationUi: List<NotificationUi>,
    onOpenTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        listNotificationUi.forEachIndexed { index, notification ->
            NotificationCard(
                notification = notification,
                index = index,
                onClick = { onOpenTab(notification.id) }
            )
        }
    }
}

@Composable
private fun NotificationCard(
    notification: NotificationUi,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isPressed) 8.dp else 4.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (index == 0) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (index == 0) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        NotificationCardContent(
            notification = notification,
            isHighlighted = index == 0
        )
    }
}

@Composable
private fun NotificationCardContent(
    notification: NotificationUi,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NotificationIcon(isHighlighted = isHighlighted)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            NotificationTitle(isHighlighted = isHighlighted)
            NotificationText(text = notification.text)
        }

        NotificationBadge(isHighlighted = isHighlighted)
    }
}

@Composable
private fun NotificationIcon(
    isHighlighted: Boolean,
    modifier: Modifier = Modifier
) {
    val iconColor = if (isHighlighted) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondary
    }

    val containerColor = if (isHighlighted) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
    }

    Surface(
        modifier = modifier.size(48.dp),
        color = containerColor,
        shape = CircleShape
    ) {
        Text(
            text = "🔔",
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
private fun NotificationTitle(
    isHighlighted: Boolean,
    modifier: Modifier = Modifier
) {
    val titleText = if (isHighlighted) "Новое уведомление" else "Уведомление"

    Text(
        text = titleText,
        modifier = modifier,
        style = MaterialTheme.typography.titleSmall,
        color = if (isHighlighted) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    )
}

@Composable
private fun NotificationText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun NotificationBadge(
    isHighlighted: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isHighlighted) return

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.5.dp,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
        )
    ) {
        Text(
            text = "New",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}
