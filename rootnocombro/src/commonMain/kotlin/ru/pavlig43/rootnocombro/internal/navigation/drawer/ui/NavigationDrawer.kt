package ru.pavlig43.rootnocombro.internal.navigation.drawer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.notification.api.model.NotificationDrawerUi
import ru.pavlig43.notification.api.ui.NotificationIcon
import ru.pavlig43.rootnocombro.internal.navigation.drawer.component.DrawerComponent
import ru.pavlig43.rootnocombro.internal.navigation.drawer.component.DrawerDestination

private const val DRAWER_WIDTH = 0.4f
private const val DEFAULT_BODY_PADDING = 4

@Suppress("LongParameterList")
@Composable
internal fun NavigationDrawer(
    drawerComponent: DrawerComponent,
    drawerState: DrawerState,
    onCloseNavigationDrawer: () -> Unit,
    bodyPadding: PaddingValues = PaddingValues(DEFAULT_BODY_PADDING.dp),
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val drawerItems by drawerComponent.drawerConfigurationsState.collectAsState()
    val notifications by drawerComponent.notificationsState.collectAsState()


    ModalNavigationDrawer(
        drawerState = drawerState,
        modifier = modifier.padding(bodyPadding),
        drawerContent = {
            DrawerContent(
                items = drawerItems,
                notifications = notifications,
                onNotificationScreen = {
                    drawerComponent.onNotificationScreen()
                    onCloseNavigationDrawer()
                },
                onItemClick = {
                    drawerComponent.onSelect(it)
                    onCloseNavigationDrawer()
                }
            )
        }
    ) {
        content()
    }
}

@Composable
private fun DrawerContent(
    items: List<DrawerDestination>,
    notifications: List<NotificationDrawerUi>,
    onNotificationScreen: () -> Unit,
    onItemClick: (DrawerDestination) -> Unit,
    modifier: Modifier = Modifier,

    ) {
    val listState = rememberLazyListState()
    ModalDrawerSheet(modifier = Modifier.fillMaxWidth(DRAWER_WIDTH)) {
        LazyColumn(state = listState, modifier = modifier.fillMaxWidth()) {
            item { NotificationsDrawer(notifications, onNotificationScreen) }
            items(items = items, key = { it.title }) { item ->
                DrawerItem(
                    drawerDestination = item,
                    onSelect = onItemClick,
                )
            }


        }
    }
}

@Composable
private fun DrawerItem(
    drawerDestination: DrawerDestination,
    onSelect: (DrawerDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(onClick = { onSelect(drawerDestination) }, modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Text(text = drawerDestination.title)
        }

    }
}

@Composable
private fun NotificationsDrawer(
    notifications: List<NotificationDrawerUi>,
    onNotificationScreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actualNotification = notifications.filter { it.count != 0 }
    if (actualNotification.isNotEmpty()) {
        Card(
            onClick = onNotificationScreen,
            modifier.fillMaxWidth()
        ) {
            Text("Оповещения")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                notifications.forEach {
                    NotificationIcon(it.level, it.count)
                }
            }
        }
    }


}


