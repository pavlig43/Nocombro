package ru.pavlig43.rootnocombro.internal.navigation.drawer.component

import kotlinx.coroutines.flow.StateFlow

interface IDrawerComponent {
    fun onSelect(configuration: DrawerDestination)

    val drawerConfigurationsState: StateFlow<List<DrawerDestination>>

}

sealed class DrawerDestination(val title: String, val intField: Int? = null) {
    class DocumentList : DrawerDestination("Документы")
    class CreateDocument : DrawerDestination("Создать документ")
    class CreateProduct:DrawerDestination("Создать продукт")
    class ProductList:DrawerDestination("Продукты")

}

