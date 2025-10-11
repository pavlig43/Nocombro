package ru.pavlig43.notification.internal.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ru.pavlig43.notification.api.data.NotificationLevel
import ru.pavlig43.notification.internal.component.INotificationItemComponent
import ru.pavlig43.notification.internal.data.NotificationUi


@Composable
internal fun NotificationItemBlock(
    notificationItemComponent: INotificationItemComponent,
    modifier: Modifier = Modifier
) {
    val countNotification by notificationItemComponent.countNotification.collectAsState()
    val notifications by notificationItemComponent.notificationFlow.collectAsState()

if (countNotification !=0){
    NotificationBlock(
        title = notificationItemComponent.item.title,
        level = notificationItemComponent.level,
        countNotification = countNotification,
        listNotificationUi = notifications,
        onOpenTab = notificationItemComponent::onClickItem,
        modifier = modifier
    )
}

}
@Suppress("LongParameterList")
@Composable
private fun NotificationBlock(
    title: String,
    level: NotificationLevel,
    countNotification: Int,
    listNotificationUi: List<NotificationUi>,
    onOpenTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var isShowFullList by remember { mutableStateOf(false) }
    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NotificationTitle(
            title = title,
            level = level,
            countNotification = countNotification,
            isShowFullList = isShowFullList,
            showFullList = { isShowFullList = !isShowFullList }
        )
        if (isShowFullList){
            NotificationItemList(
                listNotificationUi = listNotificationUi,
                onOpenTab = onOpenTab,
            )
        }
    }



}