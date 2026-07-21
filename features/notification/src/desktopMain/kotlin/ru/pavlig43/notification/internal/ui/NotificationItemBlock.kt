package ru.pavlig43.notification.internal.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ru.pavlig43.notification.api.model.NotificationItem
import ru.pavlig43.notification.internal.model.NotificationBlockUi
import ru.pavlig43.coreui.tab.rememberRetainedTabMutableState


@Suppress("LongParameterList")
@Composable
internal fun NotificationItemBlock(
    notificationBlockUi: NotificationBlockUi,
    onOpenTab: (NotificationItem, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var isShowFullList by rememberRetainedTabMutableState(
        owner = notificationBlockUi.item,
        name = "isShowFullList:${notificationBlockUi.level}",
    ) { false }
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
