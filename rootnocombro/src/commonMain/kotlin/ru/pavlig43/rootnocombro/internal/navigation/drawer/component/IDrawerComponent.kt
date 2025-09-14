package ru.pavlig43.rootnocombro.internal.navigation.drawer.component

import kotlinx.coroutines.flow.StateFlow
import ru.pavlig43.notification.api.data.NotificationDrawerUi

interface IDrawerComponent {
    fun onSelect(configuration: DrawerDestination)
    val onNotificationScreen:()->Unit
    /**
     * Вкладки для открытия общих вкладок, никаких дополнительных параметров
     */
    val drawerConfigurationsState: StateFlow<List<DrawerDestination>>

    val notificationsState:StateFlow<List<NotificationDrawerUi>>


}
enum class DrawerDestination(val title: String) {
    Documents("Документы"),
    ProductList("Продукты")
}


