package ru.pavlig43.document.api.component

import ru.pavlig43.createitem.api.component.ICreateItemComponent

interface ICreateDocumentComponent {
    val createBaseRowsOfComponent: ICreateItemComponent
    fun onBack()

}