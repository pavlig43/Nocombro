package ru.pavlig43.document.api.component


import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import ru.pavlig43.createitem.api.component.ICreateItemComponent
import ru.pavlig43.itemlist.api.component.IItemListComponent

interface IDocumentComponent {

    val stack:Value<ChildStack<*, Child>>


    sealed interface Child{
        class ItemList(val component: IItemListComponent): Child
        class CreateDocument(val component: ICreateDocumentComponent): Child
    }
}

