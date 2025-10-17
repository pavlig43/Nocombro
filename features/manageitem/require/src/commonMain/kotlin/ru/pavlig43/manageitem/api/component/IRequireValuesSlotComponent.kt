package ru.pavlig43.manageitem.api.component

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import ru.pavlig43.core.data.ItemType
import ru.pavlig43.loadinitdata.api.component.ILoadInitDataComponent
import ru.pavlig43.manageitem.api.data.DefaultRequireValues

interface DefaultRequireValuesSlotComponent {
    val initComponent: ILoadInitDataComponent<DefaultRequireValues>
    val requireValues: StateFlow<DefaultRequireValues>
    fun onNameChange(name: String)
    val typeVariants: StateFlow<List<ItemType>>
    fun onSelectType(type: ItemType)
    fun onCommentChange(comment: String)
    val isValidAllValue: Flow<Boolean>
}