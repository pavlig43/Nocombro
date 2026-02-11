package ru.pavlig43.mutable.api.singleLine.component

import com.arkivanov.decompose.ComponentContext
import ru.pavlig43.core.FormTabComponent
import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.core.model.SingleItem
import ru.pavlig43.tablecore.model.ISingleLineTableUi
import ru.pavlig43.update.data.UpdateSingleLineRepository

@Suppress("LongParameterList")
abstract class UpdateSingleLineComponent<I : SingleItem, T : ISingleLineTableUi, C>(
    componentContext: ComponentContext,
    componentFactory: SingleLineComponentFactory<I, T>,
    id: Int,
    private val updateSingleLineRepository: UpdateSingleLineRepository<I>,
    private val mapperToDTO: T.() -> I,
    observeOnItem: (T) -> Unit = {},
    onSuccessInitData: (T) -> Unit = {}
) : SingleLineComponent<I, T, C>(
    componentContext = componentContext,
    componentFactory = componentFactory,
    getInitData = { updateSingleLineRepository.getInit(id) },
    observeOnItem = observeOnItem
), FormTabComponent {
    override val title: String = "Основная информация"

    override suspend fun onUpdate(): Result<Unit> {
        val old = initDataComponent.firstData.value?.mapperToDTO()
        val new = itemFields.value[0].mapperToDTO()
        return updateSingleLineRepository.update(ChangeSet(old, new))
    }

}