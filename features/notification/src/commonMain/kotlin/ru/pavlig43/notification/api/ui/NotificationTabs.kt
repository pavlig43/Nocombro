package ru.pavlig43.notification.api.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.core.tabs.ITabNavigationComponent
import ru.pavlig43.notification.api.component.PageNotificationComponent
import ru.pavlig43.notification.api.data.NotificationLevel
import ru.pavlig43.notification.internal.component.ILevelNotificationComponent
import ru.pavlig43.notification.internal.ui.NotificationsLevelScreen

@Composable
fun NotificationTabs(
    pageNotificationComponent: PageNotificationComponent,
    modifier: Modifier = Modifier
) {
    NotificationTabsUi(
        component = pageNotificationComponent,
        modifier = modifier,
    )

}

@Composable
private fun NotificationTabsUi(
    component: PageNotificationComponent,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        TabNavigationContent(
            navigationComponent = component.tabNavigationComponent,
            containerContent = { innerTabs: @Composable (modifier: Modifier) -> Unit,
                                 notificationComponent: ILevelNotificationComponent? ->

                innerTabs(modifier)
                Spacer(Modifier.padding(bottom = 4.dp))
                notificationComponent?.itemNotifications?.takeIf { it.isNotEmpty() }?.let {
                    NotificationsLevelScreen(
                        it
                    )
                } ?: Box(modifier)

            }
        )
    }
}

@Composable
private fun TabNavigationContent(
    navigationComponent: ITabNavigationComponent<NotificationLevel, ILevelNotificationComponent>,
    containerContent: @Composable (innerTabs: @Composable (modifier: Modifier) -> Unit, slotComponent: ILevelNotificationComponent?) -> Unit
) {
    val children by navigationComponent.children.subscribeAsState()

    containerContent(
        { tabRowModifier ->
            Row(tabRowModifier.fillMaxWidth()) {
                children.items.map { it.instance }.forEachIndexed {index,level->
                    TabContent(
                        notificationSlot = level,
                        isSelected = children.selectedIndex == index,
                        onSelect = { navigationComponent.onSelectTab(index) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

        },
        children.selectedIndex?.let { children.items[it] }?.instance
    )
}

val tabBorder = @Composable { isSelected: Boolean ->
    if (isSelected) {
        BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface)
    } else {
        CardDefaults.outlinedCardBorder()
    }
}
val tabOnClickModifier =
    @Composable { onSelect: () -> Unit, interactionSource: MutableInteractionSource ->
        Modifier.clickable(
            onClick = onSelect,
            interactionSource = interactionSource,
            indication = null
        )
    }


@Composable
private fun TabContent(
    notificationSlot: ILevelNotificationComponent,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier

) {
    val countNotification by notificationSlot.countNotification.collectAsState()
    val interactionSource = remember(notificationSlot) { MutableInteractionSource() }
    val pressedAsState = interactionSource.collectIsPressedAsState()
    LaunchedEffect(pressedAsState.value) {
        if (pressedAsState.value) {
            onSelect()
        }
    }

        Row(
            modifier = modifier.fillMaxWidth().border(tabBorder(isSelected)).then(tabOnClickModifier(onSelect, interactionSource)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            NotificationIcon(
                level = notificationSlot.level,
                countNotification = countNotification,
                modifier = Modifier.height(36.dp).fillMaxWidth()
            )

        }
    }
