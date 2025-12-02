package ru.pavlig43.declarationform.internal.component

import com.arkivanov.decompose.ComponentContext
import kotlinx.serialization.Serializable
import ru.pavlig43.database.data.declaration.DeclarationIn
import ru.pavlig43.declarationform.internal.data.DeclarationEssentialsUi
import ru.pavlig43.declarationform.internal.data.toDto
import ru.pavlig43.itemlist.api.ItemListDependencies
import ru.pavlig43.manageitem.api.data.CreateEssentialsRepository
import ru.pavlig43.manageitem.internal.component.CreateEssentialsComponent
import ru.pavlig43.manageitem.internal.component.EssentialComponentFactory


internal class CreateDeclarationComponent(
    componentContext: ComponentContext,
    onSuccessCreate: (Int) -> Unit,
    createDeclarationRepository: CreateEssentialsRepository<DeclarationIn>,
    itemListDependencies: ItemListDependencies,
    onOpenVendorTab: (Int) -> Unit,
    componentFactory: EssentialComponentFactory<DeclarationIn, DeclarationEssentialsUi>
) : CreateEssentialsComponent<DeclarationIn, DeclarationEssentialsUi>(
    componentContext = componentContext,
    onSuccessCreate = onSuccessCreate,
    createEssentialsRepository = createDeclarationRepository,
    componentFactory = componentFactory,
    mapperToDTO = { toDto() },
) {
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

@Serializable
internal data object MBSVendorDialog