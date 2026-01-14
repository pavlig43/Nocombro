package ru.pavlig43.update.component

import com.arkivanov.decompose.ComponentContext
import ru.pavlig43.core.FormTabComponent
import ru.pavlig43.core.component.EssentialComponentFactory
import ru.pavlig43.core.component.EssentialsComponent
import ru.pavlig43.core.data.ChangeSet
import ru.pavlig43.core.data.GenericItem
import ru.pavlig43.core.data.ItemEssentialsUi
import ru.pavlig43.update.data.UpdateEssentialsRepository

abstract class UpdateEssentialsComponent<I : GenericItem, T : ItemEssentialsUi>(
    componentContext: ComponentContext,
    componentFactory: EssentialComponentFactory<I, T>,
    id: Int,
    private val updateEssentialsRepository: UpdateEssentialsRepository<I>,
    private val mapperToDTO: T.() -> I,
) : EssentialsComponent<I, T>(
    componentContext = componentContext,
    componentFactory = componentFactory,
    getInitData = { updateEssentialsRepository.getInit(id) },
), FormTabComponent {
    override val title: String = "Основная информация"

    override suspend fun onUpdate(): Result<Unit> {
        val old = initDataComponent.firstData.value?.mapperToDTO()
        val new = itemFields.value.mapperToDTO()
        return updateEssentialsRepository.update(ChangeSet(old, new))
    }

}