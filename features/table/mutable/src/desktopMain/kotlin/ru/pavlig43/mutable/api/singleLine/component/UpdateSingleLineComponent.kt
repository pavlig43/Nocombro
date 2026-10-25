package ru.pavlig43.mutable.api.singleLine.component

import com.arkivanov.decompose.ComponentContext
import ru.pavlig43.core.FormTabComponent
import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.core.model.SingleItem
import ru.pavlig43.mutable.api.singleLine.data.UpdateSingleLineRepository

/** Базовый компонент правки одной записи через карточную форму. */
@Suppress("LongParameterList")
abstract class UpdateSingleLineComponent<I : SingleItem, T : Any>(
    componentContext: ComponentContext,
    componentFactory: SingleLineComponentFactory<I, T>,
    id: Int,
    private val updateSingleLineRepository: UpdateSingleLineRepository<I>,
    private val mapperToDTO: T.() -> I,
    observeOnItem: (T) -> Unit = {},
    onSuccessInitData: (T) -> Unit = {},
) : SingleLineComponent<I, T>(
    componentContext = componentContext,
    componentFactory = componentFactory,
    getInitData = { updateSingleLineRepository.getInit(id) },
    observeOnItem = observeOnItem,
    onSuccessInitData = onSuccessInitData,
), FormTabComponent {
    override val title: String = "Основная информация"

    override suspend fun refreshDataAfterUpsert() {
        initDataComponent.retryLoadInitData()
    }

    override suspend fun onUpdate(): Result<Unit> {
        val current = item.value
        val inputErrors = (current as? SingleLineInputState)?.inputErrors.orEmpty()
        if (inputErrors.isNotEmpty()) {
            return Result.failure(IllegalArgumentException(inputErrors.joinToString("\n")))
        }
        val old = initialItem
        val new = current.mapperToDTO()
        return updateSingleLineRepository.update(ChangeSet(old, new))
    }
}
