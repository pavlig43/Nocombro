package ru.pavlig43.documentform.api.component

import kotlinx.coroutines.flow.StateFlow
import ru.pavlig43.addfile.api.component.IAddFileComponent
import ru.pavlig43.manageitem.api.component.IManageBaseValueItemComponent

interface IDocumentFormComponent {
    val manageBaseValuesOfComponent: IManageBaseValueItemComponent
    val addFileComponent: IAddFileComponent
    val isValidAllValue: StateFlow<Boolean>
    val saveDocumentState:StateFlow<SaveDocumentState>
    fun saveDocument()
    fun closeScreen()

}
sealed interface SaveDocumentState{
    class Init():SaveDocumentState
    class Loading():SaveDocumentState
    class Success():SaveDocumentState
    class Error(val message:String):SaveDocumentState
}