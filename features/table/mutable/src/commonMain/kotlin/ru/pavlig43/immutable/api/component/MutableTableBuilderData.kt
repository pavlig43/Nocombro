package ru.pavlig43.immutable.api.component

import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.transaction.TransactionType
import ru.pavlig43.tablecore.model.ITableUi


///////////////////
sealed interface MutableTableBuilderData<I: ITableUi>

data object CompositionTableBuilder: MutableTableBuilderData<CompositionUi>




