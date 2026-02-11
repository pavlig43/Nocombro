package ru.pavlig43.transaction.internal.component.tabs.component

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import ru.pavlig43.core.component.EssentialComponentFactory
import ru.pavlig43.database.data.transaction.Transaction
import ru.pavlig43.transaction.internal.model.TransactionEssentialsUi
import ru.pavlig43.transaction.internal.model.toDto
import ru.pavlig43.update.component.UpdateEssentialsComponent
import ru.pavlig43.update.data.UpdateSingleLineRepository

internal class TransactionEssentialComponent(
    componentContext: ComponentContext,
    id: Int,
    updateRepository: UpdateSingleLineRepository<Transaction>,
    componentFactory: EssentialComponentFactory<Transaction, TransactionEssentialsUi>,
    onSuccessInitData:(TransactionEssentialsUi)-> Unit,
    observeOnEssentials:(TransactionEssentialsUi)-> Unit,
) : UpdateEssentialsComponent<Transaction, TransactionEssentialsUi>(
    componentContext = componentContext,
    id = id,
    updateSingleLineRepository = updateRepository,
    componentFactory = componentFactory,
    mapperToDTO = { toDto() },
    observeOnEssentials = observeOnEssentials,
    onSuccessInitData = onSuccessInitData
){
    override val errorMessages: Flow<List<String>> = flowOf(emptyList())
}