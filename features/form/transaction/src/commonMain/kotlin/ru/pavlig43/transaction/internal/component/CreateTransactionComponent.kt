package ru.pavlig43.transaction.internal.component

import com.arkivanov.decompose.ComponentContext
import ru.pavlig43.core.component.EssentialComponentFactory
import ru.pavlig43.create.component.CreateEssentialsComponent
import ru.pavlig43.create.data.CreateEssentialsRepository
import ru.pavlig43.database.data.transaction.ProductTransaction
import ru.pavlig43.transaction.internal.data.TransactionEssentialsUi
import ru.pavlig43.transaction.internal.data.toDto

internal class CreateTransactionComponent(
    componentContext: ComponentContext,
    onSuccessCreate: (Int) -> Unit,
    createRepository: CreateEssentialsRepository<ProductTransaction>,
    componentFactory: EssentialComponentFactory<ProductTransaction, TransactionEssentialsUi>
) : CreateEssentialsComponent<ProductTransaction, TransactionEssentialsUi>(
    componentContext = componentContext,
    onSuccessCreate = onSuccessCreate,
    createEssentialsRepository = createRepository,
    componentFactory = componentFactory,
    mapperToDTO = { toDto() }
)
