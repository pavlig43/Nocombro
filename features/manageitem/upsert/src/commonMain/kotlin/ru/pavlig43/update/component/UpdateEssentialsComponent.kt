package ru.pavlig43.update.component

import com.arkivanov.decompose.ComponentContext
import ru.pavlig43.core.FormTabComponent
import ru.pavlig43.core.component.EssentialComponentFactory
import ru.pavlig43.core.component.EssentialsComponent
import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.core.model.SingleItem
import ru.pavlig43.core.model.ItemEssentialsUi
import ru.pavlig43.update.data.UpdateSingleLineRepository

/**
 * Абстрактный компонент для создания новых обязательных полей.
 * @see [ru.pavlig43.create.component.CreateEssentialsComponent]
 */
@Suppress("LongParameterList")
abstract class UpdateEssentialsComponent<I : SingleItem, T : ItemEssentialsUi>(
    componentContext: ComponentContext,
    componentFactory: EssentialComponentFactory<I, T>,
    id: Int,
    private val updateSingleLineRepository: UpdateSingleLineRepository<I>,
    private val mapperToDTO: T.() -> I,
    observeOnEssentials:(T)-> Unit = {},
    onSuccessInitData:(T)-> Unit = {}
) : EssentialsComponent<I, T>(
    componentContext = componentContext,
    componentFactory = componentFactory,
    getInitData = { updateSingleLineRepository.getInit(id) },
    observeOnEssentials = observeOnEssentials,
    onSuccessInitData = onSuccessInitData
), FormTabComponent {
    override val title: String = "Основная информация"

    override suspend fun onUpdate(): Result<Unit> {
        val old = initDataComponent.firstData.value?.mapperToDTO()
        val new = itemFields.value.mapperToDTO()
        return updateSingleLineRepository.update(ChangeSet(old, new))
    }

}