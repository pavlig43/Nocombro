package ru.pavlig43.notification.api.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.coreui.tab.TabStaticNavigationContent
import ru.pavlig43.notification.api.component.NotificationComponent
import ru.pavlig43.notification.api.model.NotificationLevel
import ru.pavlig43.notification.internal.ui.NotificationsLevelScreen

@Composable
fun NotificationTabs(
    pageNotificationComponent: NotificationComponent,
    modifier: Modifier = Modifier
) {
    NotificationTabsUi(
        component = pageNotificationComponent,
        modifier = modifier,
    )

}


@Composable
private fun NotificationTabsUi(
    component: NotificationComponent,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TabStaticNavigationContent(
            navigationComponent = component.tabNavigationComponent,
            tabContent = { index, tabChild, isSelected ->
                val notificationList by tabChild.component.notificationFlow.collectAsState()

                TabContent(
                    countNotification = notificationList.size,
                    level = tabChild.component.level,
                    isSelected = isSelected,
                    onSelect = { component.tabNavigationComponent.onSelectTab(index) },
                    modifier = modifier.weight(1f),

                    )
            },
        ) { tabChild ->
            tabChild?.let {
                NotificationsLevelScreen(it.component)
            }
        }

    }
}


@Composable
private fun TabContent(
    countNotification: Int,
    level: NotificationLevel,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier

) {


    NotificationIcon(
        level = level,
        countNotification = countNotification,
        isSelected = isSelected,
        modifier = modifier
            .height(36.dp)
            .fillMaxWidth()
            .clickable { onSelect() }
    )
}
