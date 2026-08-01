package ru.pavlig43.notification.internal.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.notification.internal.component.NotificationLevelComponent

@Composable
internal fun NotificationsLevelScreen(
    component: NotificationLevelComponent,
    modifier: Modifier = Modifier,
) {
    val notifications by component.notificationFlow.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (notifications.isEmpty()) {
            item(key = "empty") {
                EmptyNotifications()
            }
        } else {
            items(
                items = notifications,
                key = { "${it.level}:${it.item}" },
            ) { notification ->
                NotificationItemBlock(
                    notificationBlockUi = notification,
                    onOpenTab = component::onClickItem,
                )
            }
        }
    }
}

@Composable
private fun EmptyNotifications(
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Оповещений нет",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Для этого уровня пока нет активных записей.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
