package ru.pavlig43.itemlist.statik.internal.component

import ru.pavlig43.core.data.GenericItem
import ru.pavlig43.itemlist.core.data.IItemUi

internal sealed interface IStaticListContainer<O : GenericItem, U : IItemUi> {
    val staticListComponent: StaticListComponent<O, U>
}