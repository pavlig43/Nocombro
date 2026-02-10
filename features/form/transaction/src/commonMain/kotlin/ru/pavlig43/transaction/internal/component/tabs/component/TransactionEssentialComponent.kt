package ru.pavlig43.transaction.internal.component.tabs.component

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import ru.pavlig43.core.component.EssentialComponentFactory
import ru.pavlig43.database.data.transaction.Transact
import ru.pavlig43.transaction.internal.model.TransactionEssentialsUi
import ru.pavlig43.transaction.internal.model.toDto
import ru.pavlig43.update.component.UpdateEssentialsComponent
import ru.pavlig43.update.data.UpdateEssentialsRepository

internal class TransactionEssentialComponent(
    componentContext: ComponentContext,
    id: Int,
    updateRepository: UpdateEssentialsRepository<Transact>,
    componentFactory: EssentialComponentFactory<Transact, TransactionEssentialsUi>,
    onSuccessInitData:(TransactionEssentialsUi)-> Unit,
    observeOnEssentials:(TransactionEssentialsUi)-> Unit,
) : UpdateEssentialsComponent<Transact, TransactionEssentialsUi>(
    componentContext = componentContext,
    id = id,
    updateEssentialsRepository = updateRepository,
    componentFactory = componentFactory,
    mapperToDTO = { toDto() },
    observeOnEssentials = observeOnEssentials,
    onSuccessInitData = onSuccessInitData
){
    override val errorMessages: Flow<List<String>> = flowOf(emptyList())
}