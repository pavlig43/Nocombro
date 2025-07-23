package ru.pavlig43.documentform.internal.data

import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.extension
import ru.pavlig43.addfile.api.data.AddedFile
import ru.pavlig43.core.RequestResult
import ru.pavlig43.database.data.document.DocumentFilePath
import ru.pavlig43.documentform.api.component.SaveDocumentState


internal fun RequestResult<Unit>.toSaveStateDocument(): SaveDocumentState {
    return when(this){
        is RequestResult.Error<*> -> SaveDocumentState.Error(this.message ?: "Неизвестная ошибка")
        is RequestResult.InProgress -> SaveDocumentState.Loading()
        is RequestResult.Initial<*> -> SaveDocumentState.Init()
        is RequestResult.Success<*> -> SaveDocumentState.Success()
    }
}


