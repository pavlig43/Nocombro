package ru.pavlig43.product.internal.component.tabs.tabslot

import com.arkivanov.decompose.ComponentContext
import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.database.data.product.CompositionIn
import ru.pavlig43.database.data.product.CompositionOut
import ru.pavlig43.mutable.api.component.MutableTableComponent
import ru.pavlig43.product.internal.component.tabs.tabslot.compositionData.CompositionField
import ru.pavlig43.product.internal.component.tabs.tabslot.compositionData.CompositionFilterMatcher
import ru.pavlig43.product.internal.component.tabs.tabslot.compositionData.CompositionSorter
import ru.pavlig43.product.internal.component.tabs.tabslot.compositionData.CompositionUi
import ru.pavlig43.product.internal.component.tabs.tabslot.compositionData.createCompositionColumn
import ru.pavlig43.tablecore.model.TableData
import ru.pavlig43.tablecore.utils.FilterMatcher
import ru.pavlig43.update.data.UpdateCollectionRepository
import ua.wwind.table.ColumnSpec

internal class CompositionTabSlot1(
    componentContext: ComponentContext,
    productId: Int,
    repository: UpdateCollectionRepository<CompositionOut, CompositionIn>,
) : MutableTableComponent<CompositionOut, CompositionIn, CompositionUi, CompositionField>(
    componentContext = componentContext,
    parentId = productId,
    title = "Состав",
    sortMatcher = CompositionSorter,
    repository = repository
), ProductTabSlot {

    override val columns: ImmutableList<ColumnSpec<CompositionUi, CompositionField, TableData<CompositionUi>>> =
        createCompositionColumn(::onEvent)

    override val filterMatcher: FilterMatcher<CompositionUi, CompositionField> =
        CompositionFilterMatcher



    override fun createNewItem(composeId: Int): CompositionUi {
        return CompositionUi(
            composeId = composeId,
            id = 0,
            productId = 0,
            productName = "",
            count = 0
        )
    }

    override fun CompositionOut.toUi(composeId: Int): CompositionUi {
        return CompositionUi(
            composeId = composeId,
            id = id,
            productId = productId,
            productName = productName,
            count = count
        )
    }

    override fun CompositionUi.toBDIn(): CompositionIn {
        return CompositionIn(
            id = id,
            productId = productId,
            count = count
        )
    }


    override fun CompositionUi.isValidate(): Boolean {
        return productId != 0 && count != 0
    }


}