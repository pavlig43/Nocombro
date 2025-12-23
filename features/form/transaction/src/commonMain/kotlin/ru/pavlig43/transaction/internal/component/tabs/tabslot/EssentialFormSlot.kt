package ru.pavlig43.transaction.internal.component.tabs.tabslot

import com.arkivanov.decompose.ComponentContext
import ru.pavlig43.core.component.EssentialComponentFactory
import ru.pavlig43.database.data.transaction.ProductTransaction
import ru.pavlig43.transaction.internal.model.TransactionEssentialsUi
import ru.pavlig43.transaction.internal.model.toDto
import ru.pavlig43.update.component.UpdateEssentialsComponent
import ru.pavlig43.update.data.UpdateEssentialsRepository

internal class EssentialFormSlot(
    componentContext: ComponentContext,
    documentId: Int,
    updateRepository: UpdateEssentialsRepository<ProductTransaction>,
    componentFactory: EssentialComponentFactory<ProductTransaction, TransactionEssentialsUi>,
) : UpdateEssentialsComponent<ProductTransaction, TransactionEssentialsUi>(
    componentContext = componentContext,
    id = documentId,
    updateEssentialsRepository = updateRepository,
    componentFactory = componentFactory,
    mapperToDTO = { toDto() }
), TransactionFormSlot