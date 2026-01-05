package ru.pavlig43.transaction.internal.component.tabs.tabslot.transactionvariables.buy

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import ru.pavlig43.core.component.EssentialComponentFactory
import ru.pavlig43.database.data.transaction.Transaction
import ru.pavlig43.transaction.internal.model.TransactionEssentialsUi
import ru.pavlig43.transaction.internal.model.toDto
import ru.pavlig43.update.component.UpdateEssentialsComponent
import ru.pavlig43.update.data.UpdateEssentialsRepository

internal class BuyEssentialFormSlot(
    componentContext: ComponentContext,
    id: Int,
    updateRepository: UpdateEssentialsRepository<Transaction>,
    componentFactory: EssentialComponentFactory<Transaction, TransactionEssentialsUi>,
) : UpdateEssentialsComponent<Transaction, TransactionEssentialsUi>(
    componentContext = componentContext,
    id = id,
    updateEssentialsRepository = updateRepository,
    componentFactory = componentFactory,
    mapperToDTO = { toDto() }
), BuyFormSlot {
    override val errorMessages: Flow<List<String>>  = flowOf(emptyList())
}