package ru.pavlig43.declarationform.internal.component.tabs.tabslot

import com.arkivanov.decompose.ComponentContext
import ru.pavlig43.database.data.declaration.DeclarationIn
import ru.pavlig43.declarationform.internal.component.VendorDialogComponent
import ru.pavlig43.declarationform.internal.data.DeclarationEssentialsUi
import ru.pavlig43.declarationform.internal.data.toDto
import ru.pavlig43.itemlist.api.ItemListDependencies
import ru.pavlig43.manageitem.api.data.UpdateEssentialsRepository
import ru.pavlig43.manageitem.internal.component.EssentialComponentFactory
import ru.pavlig43.manageitem.internal.component.UpdateEssentialsComponent

internal class EssentialTabSlot(
    componentContext: ComponentContext,
    declarationId: Int,
    itemListDependencies: ItemListDependencies,
    onOpenVendorTab:(Int)-> Unit,
    updateRepository: UpdateEssentialsRepository<DeclarationIn>,
    componentFactory: EssentialComponentFactory<DeclarationIn, DeclarationEssentialsUi>,
) : UpdateEssentialsComponent<DeclarationIn, DeclarationEssentialsUi>(
    componentContext = componentContext,
    id = declarationId,
    updateEssentialsRepository = updateRepository,
    componentFactory = componentFactory,
    mapperToDTO = {toDto()}
), DeclarationTabSlot{
    val vendorDialogComponent = VendorDialogComponent(
        parentComponentContext = componentContext,
        onChangeVendor = { id, name ->
            val declaration = itemFields.value.copy(vendorId = id, vendorName = name)
            onChangeItem(declaration)
        },
        itemListDependencies = itemListDependencies,
        onOpenVendorTab = onOpenVendorTab
    )
}