package ru.pavlig43.documentform.api.component

import ru.pavlig43.addfile.api.component.IAddFileComponent
import ru.pavlig43.database.data.document.DocumentWithFiles
import ru.pavlig43.manageitem.api.component.IManageBaseValueItemComponent
import ru.pavlig43.upsertitem.api.component.ISaveItemComponent

interface IDocumentFormComponent {
    val manageBaseValuesOfComponent: IManageBaseValueItemComponent
    val addFileComponent: IAddFileComponent
    val saveDocumentComponent:ISaveItemComponent<DocumentWithFiles>
    fun closeScreen()

}
