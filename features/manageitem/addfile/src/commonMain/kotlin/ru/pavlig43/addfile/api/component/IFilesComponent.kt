package ru.pavlig43.addfile.api.component


import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import ru.pavlig43.addfile.api.data.FileUi
import ru.pavlig43.loadinitdata.api.component.ILoadInitDataComponent

interface IFilesComponent {
    val loadInitDataComponent:ILoadInitDataComponent<List<FileUi>>
    val filesUi:StateFlow<List<FileUi>>
    val isAllFilesUpload: Flow<Boolean>
    fun addFilePath(path:String)
    fun retryLoadFile(index: Int)
    fun removeFile(index: Int)
}



