package ru.pavlig43.notification.internal.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ru.pavlig43.notification.api.model.NotificationItem
import ru.pavlig43.notification.internal.model.NotificationBlockUi


@Suppress("LongParameterList")
@Composable
internal fun NotificationItemBlock(
    notificationBlockUi: NotificationBlockUi,
    onOpenTab: (NotificationItem, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var isShowFullList by remember { mutableStateOf(false) }
    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NotificationTitle(
            title = notificationBlockUi.item.title,
            level = notificationBlockUi.level,
            countNotification = notificationBlockUi.notificationList.size,
            isShowFullList = isShowFullList,
            showFullList = { isShowFullList = !isShowFullList }
        )
        if (isShowFullList) {
            NotificationItemList(
                listNotificationUi = notificationBlockUi.notificationList,
                onOpenTab = {id-> onOpenTab(notificationBlockUi.item,id)},
            )
        }
    }


}