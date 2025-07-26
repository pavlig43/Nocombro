package ru.pavlig43.documentform.api.component

import kotlinx.coroutines.flow.StateFlow
import ru.pavlig43.addfile.api.component.IAddFileComponent
import ru.pavlig43.database.data.document.DocumentWithFiles
import ru.pavlig43.manageitem.api.component.IManageBaseValueItemComponent
import ru.pavlig43.upsertitem.api.component.ISaveItemComponent

interface IDocumentFormComponent {
    val manageBaseValuesOfComponent: IManageBaseValueItemComponent
    val addFileComponent: IAddFileComponent
    val saveDocumentComponent:ISaveItemComponent<DocumentWithFiles>
    val saveDocumentState:StateFlow<SaveDocumentState>
//    fun saveDocument()
    fun closeScreen()

}
sealed interface SaveDocumentState{
    class Init :SaveDocumentState
    class Loading :SaveDocumentState
    class Success :SaveDocumentState
    class Error(val message:String):SaveDocumentState
}