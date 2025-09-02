package ru.pavlig43.addfile.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import ru.pavlig43.core.data.ChangeSet
import ru.pavlig43.addfile.api.data.FileUi
import ru.pavlig43.core.FormTabSlot
import ru.pavlig43.core.data.FileData
import ru.pavlig43.upsertitem.api.data.UpdateCollectionRepository


abstract class UpdateFilesComponent<I : FileData>(
    componentContext: ComponentContext,
    private val id: Int,
    private val mapper: FileUi.(id:Int) -> I,
    private val updateRepository: UpdateCollectionRepository<I,I>,
) : ComponentContext by componentContext, FormTabSlot {

    override val title: String = "Файлы"
    val fileComponent = FilesComponent(
        componentContext = childContext("files"),
        getInitData = {updateRepository.getInit(id)}
    )

    override suspend fun onUpdate() {
        val old = fileComponent.loadInitDataComponent.firstData.value?.map{it.mapper(id)}
        val new = fileComponent.filesUi.value.map{it.mapper(id)}
        updateRepository.update(ChangeSet(old, new))
    }

}
