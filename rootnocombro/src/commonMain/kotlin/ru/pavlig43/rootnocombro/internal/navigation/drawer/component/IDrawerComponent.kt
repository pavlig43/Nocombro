package ru.pavlig43.rootnocombro.internal.navigation.drawer.component

import kotlinx.coroutines.flow.StateFlow
import ru.pavlig43.rootnocombro.internal.navigation.drawer.component.DrawerDestination.Documents
import ru.pavlig43.rootnocombro.internal.navigation.drawer.component.DrawerDestination.CreateDocument
import ru.pavlig43.rootnocombro.internal.navigation.drawer.component.DrawerDestination.ProductForm
import ru.pavlig43.rootnocombro.internal.navigation.tab.component.TabConfig

interface IDrawerComponent {
    fun onSelect(configuration: DrawerDestination)

    val drawerConfigurationsState: StateFlow<List<DrawerDestination>>


}
sealed class DrawerDestination(val title: DrawerDestinationTitle, val intField: Int? = null) {
    class Documents : DrawerDestination(DrawerDestinationTitle.DOCUMENTS)
    class CreateDocument : DrawerDestination(DrawerDestinationTitle.CreateDocument)
    class ProductForm:DrawerDestination(DrawerDestinationTitle.ProductForm)
}

enum class DrawerDestinationTitle(val title: String) {
    DOCUMENTS("Документы"),
    CreateDocument("Создать документ"),
    ProductForm("Создать продукт")
}

internal fun DrawerDestinationTitle.toDrawerDestination() = when (this) {
    DrawerDestinationTitle.DOCUMENTS -> Documents()
    DrawerDestinationTitle.CreateDocument -> CreateDocument()
    DrawerDestinationTitle.ProductForm -> ProductForm()
}

