package ru.pavlig43.notification.internal.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.notification.internal.component.NotificationLevelComponent

@Composable
internal fun NotificationsLevelScreen(
    component: NotificationLevelComponent,
    modifier: Modifier = Modifier
) {
    val notifications by component.notificationFlow.collectAsState()
    LazyColumn(
        modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.onBackground),
    ){
        items(notifications){notification->
            NotificationItemBlock(
                notificationBlockUi = notification,
                onOpenTab = component::onClickItem
            )
        }
    }
}






