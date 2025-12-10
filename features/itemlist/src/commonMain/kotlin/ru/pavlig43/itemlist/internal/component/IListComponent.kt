package ru.pavlig43.itemlist.internal.component

import ru.pavlig43.core.data.GenericItem
import ru.pavlig43.itemlist.api.data.IItemUi

internal sealed interface IListComponent<O : GenericItem, U : IItemUi> {
    val staticItemsBodyComponent: StaticItemsBodyComponent<O, U>
}