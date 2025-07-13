package ru.pavlig43.documentlist.api.component


import ru.pavlig43.itemlist.api.component.IItemListComponent

interface IDocumentListComponent {
    val itemListComponent:IItemListComponent

//    val stack: Value<ChildStack<*, Child>>


    sealed interface Child{
        class ItemList(val component: IItemListComponent): Child
    }
}
