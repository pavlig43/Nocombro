package ru.pavlig43.transaction.internal.component

import com.arkivanov.decompose.ComponentContext
import ru.pavlig43.core.component.EssentialComponentFactory
import ru.pavlig43.create.component.CreateEssentialsComponent
import ru.pavlig43.create.data.CreateEssentialsRepository
import ru.pavlig43.database.data.transaction.Transaction
import ru.pavlig43.transaction.internal.model.TransactionEssentialsUi
import ru.pavlig43.transaction.internal.model.toDto

internal class CreateTransactionComponent(
    componentContext: ComponentContext,
    onSuccessCreate: (Int) -> Unit,
    createRepository: CreateEssentialsRepository<Transaction>,
    componentFactory: EssentialComponentFactory<Transaction, TransactionEssentialsUi>
) : CreateEssentialsComponent<Transaction, TransactionEssentialsUi>(
    componentContext = componentContext,
    onSuccessCreate = onSuccessCreate,
    createEssentialsRepository = createRepository,
    componentFactory = componentFactory,
    mapperToDTO = { toDto() }
)
