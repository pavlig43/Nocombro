package ru.pavlig43.manageitem.api.component


import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import ru.pavlig43.core.data.ItemType
import ru.pavlig43.loadinitdata.api.component.ILoadInitDataComponent
import ru.pavlig43.manageitem.api.data.RequireValues

interface IManageBaseValueItemComponent {
    val initComponent: ILoadInitDataComponent<RequireValues>
    val requireValues:StateFlow<RequireValues>
    fun onNameChange(name:String)
    val typeVariants:StateFlow<List<ItemType>>
    fun onSelectType(type: ItemType)
    val isValidAllValue: Flow<Boolean>


}

