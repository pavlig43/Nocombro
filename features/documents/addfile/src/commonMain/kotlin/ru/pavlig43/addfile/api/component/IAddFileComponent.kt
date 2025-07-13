package ru.pavlig43.addfile.api.component


import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import ru.pavlig43.addfile.api.data.AddedFile

interface IAddFileComponent {

    val addedFiles:StateFlow<List<AddedFile>>
    val isAllFilesUpload: Flow<Boolean>
    fun addFile(platformFile: PlatformFile)
    fun retryLoadFile(index: Int)
    fun openFile(platformFile: PlatformFile)
    fun removeFile(index: Int)
}