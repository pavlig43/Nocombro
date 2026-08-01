package ru.pavlig43.notification.internal.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.notification.api.model.NotificationItem
import ru.pavlig43.notification.internal.model.NotificationBlockUi

@Suppress("LongParameterList")
@Composable
internal fun NotificationItemBlock(
    notificationBlockUi: NotificationBlockUi,
    onOpenTab: (NotificationItem, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isShowFullList by rememberSaveable(
        notificationBlockUi.item,
        notificationBlockUi.level,
    ) { mutableStateOf(value = false) }

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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NotificationTitle(
                title = notificationBlockUi.item.title,
                level = notificationBlockUi.level,
                countNotification = notificationBlockUi.notificationList.size,
                isShowFullList = isShowFullList,
                showFullList = { isShowFullList = !isShowFullList },
            )
            if (isShowFullList) {
                NotificationItemList(
                    listNotificationUi = notificationBlockUi.notificationList,
                    level = notificationBlockUi.level,
                    onOpenTab = { id -> onOpenTab(notificationBlockUi.item, id) },
                )
            }
        }
    }
}
