package ru.pavlig43.rootnocombro.internal.navigation.drawer.component

import kotlinx.coroutines.flow.StateFlow

interface IDrawerComponent {
    fun onSelect(configuration: DrawerDestination)
    /**
     * Вкладки для открытия общих вкладок, никаких дополнительных параметров
     */
    val drawerConfigurationsState: StateFlow<List<DrawerDestination>>


}
enum class DrawerDestination(val title: String) {
    Documents("Документы"),
    CreateDocument("Создать документ"),
    ProductForm("Создать продукт"),
    ProductList("Продукты")
}
