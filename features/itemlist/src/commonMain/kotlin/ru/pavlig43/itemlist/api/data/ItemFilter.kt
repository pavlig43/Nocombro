package ru.pavlig43.itemlist.api.data

import ru.pavlig43.core.data.ItemType

interface ItemFilter<Type:ItemType> {
    val types:List<Type>
    val searchText:String
}
class DefaultItemFilter<Type:ItemType>(
    override val types: List<Type>,
    override val searchText: String
):ItemFilter<Type>