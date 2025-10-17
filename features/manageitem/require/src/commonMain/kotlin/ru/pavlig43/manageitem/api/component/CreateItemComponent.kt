package ru.pavlig43.manageitem.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import ru.pavlig43.core.data.Item
import ru.pavlig43.core.data.ItemType
import ru.pavlig43.manageitem.api.data.CreateItemRepository
import ru.pavlig43.manageitem.api.data.DefaultRequireValues
import ru.pavlig43.upsertitem.api.component.CreateComponent
import ru.pavlig43.upsertitem.api.component.ICreateComponent

class CreateItemComponent<I: Item,S: ItemType>(
    componentContext: ComponentContext,
    typeVariantList: List<S>,
    mapper: DefaultRequireValues.() -> I,
    createItemRepository: CreateItemRepository<I>,
    onSuccessCreate: (Int) -> Unit,
    onChangeValueForMainTab: (String) -> Unit
): ComponentContext by componentContext {
    val requires = RequireValuesSlotComponent<I, S>(
        componentContext = componentContext,
        typeVariantList = typeVariantList,
        onChangeValueForMainTab = onChangeValueForMainTab,
        getInitData = null
    )
    val createComponent: ICreateComponent<Int> = CreateComponent(
        componentContext = childContext("create"),
        onSaveResult = {createItemRepository.createItem(requires.requireValues.value.mapper())},
        otherValidValue = requires.isValidAllValue,
        onSuccessAction = onSuccessCreate
    )
}