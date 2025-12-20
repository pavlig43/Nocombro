package ru.pavlig43.declarationform.internal.component.tabs.tabslot

import com.arkivanov.decompose.ComponentContext
import ru.pavlig43.core.component.EssentialComponentFactory
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.declarationform.internal.component.VendorDialogComponent
import ru.pavlig43.declarationform.internal.data.DeclarationEssentialsUi
import ru.pavlig43.declarationform.internal.data.toDto
import ru.pavlig43.itemlist.api.dependencies
import ru.pavlig43.update.component.UpdateEssentialsComponent
import ru.pavlig43.update.data.UpdateEssentialsRepository

internal class EssentialTabSlot(
    componentContext: ComponentContext,
    declarationId: Int,
    dependencies: dependencies,
    onOpenVendorTab:(Int)-> Unit,
    updateRepository: UpdateEssentialsRepository<Declaration>,
    componentFactory: EssentialComponentFactory<Declaration, DeclarationEssentialsUi>,
) : UpdateEssentialsComponent<Declaration, DeclarationEssentialsUi>(
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
        dependencies = dependencies,
        onOpenVendorTab = onOpenVendorTab
    )
}