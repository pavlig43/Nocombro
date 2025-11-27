package ru.pavlig43.declarationform.internal.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import ru.pavlig43.database.data.declaration.DeclarationIn
import ru.pavlig43.declarationform.internal.toDeclarationIn
import ru.pavlig43.itemlist.api.ItemListDependencies
import ru.pavlig43.manageitem.api.data.CreateItemRepository
import ru.pavlig43.upsertitem.api.component.CreateComponent
import ru.pavlig43.upsertitem.api.component.ICreateComponent

internal class CreateDeclarationComponent(
    componentContext: ComponentContext,
    itemListDependencies: ItemListDependencies,
    createItemRepository: CreateItemRepository<DeclarationIn>,
    onSuccessCreate: (Int) -> Unit,
    onOpenVendorTab: (Int) -> Unit,
    onChangeValueForMainTab: (String) -> Unit
): ComponentContext by componentContext {
    val requires = DeclarationRequiresComponent(
        componentContext = componentContext,
        onChangeValueForMainTab = onChangeValueForMainTab,
        getInitData = null,
        itemListDependencies = itemListDependencies,
        openVendorTab = onOpenVendorTab,
    )
    val createComponent: ICreateComponent<Int> = CreateComponent(
        componentContext = childContext("create"),
        onSaveResult = {createItemRepository.createItem(requires.requiresValuesWithDate.value.toDeclarationIn())},
        otherValidValue = requires.isValidAllValue,
        onSuccessAction = onSuccessCreate
    )
}