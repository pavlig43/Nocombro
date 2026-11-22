package ru.pavlig43.immutable.internal.component.items.product.plan

import com.arkivanov.decompose.ComponentContext
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.product.plan.ProductPlan
import ru.pavlig43.immutable.api.component.ProductPlanImmutableTableBuilder
import ru.pavlig43.immutable.internal.column.idWithSelection
import ru.pavlig43.immutable.internal.column.readDateTimeColumn
import ru.pavlig43.immutable.internal.column.toTableDisplayText
import ru.pavlig43.immutable.internal.component.ImmutableTableComponent
import ru.pavlig43.immutable.internal.component.displayedTextSearchMatcher
import ru.pavlig43.immutable.internal.data.ImmutableListRepository
import ru.pavlig43.tablecore.export.TableExportConfiguration
import ru.pavlig43.tablecore.model.IMultiLineTableUi
import ru.pavlig43.tablecore.model.TableData
import ru.pavlig43.tablecore.utils.FilterMatcher
import ru.pavlig43.tablecore.utils.SortMatcher
import ua.wwind.table.data.SortOrder
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.state.SortState
import ua.wwind.table.tableColumns

data class ProductPlanTableUi(override val composeId: Int, val createdAt: LocalDateTime) : IMultiLineTableUi

internal enum class ProductPlanField { SELECTION, ID, CREATED_AT }

internal class ProductPlanTableComponent(
    componentContext: ComponentContext,
    tableBuilder: ProductPlanImmutableTableBuilder,
    onItemClick: (ProductPlanTableUi) -> Unit,
    repository: ImmutableListRepository<ProductPlan>,
) : ImmutableTableComponent<ProductPlan, ProductPlanTableUi, ProductPlanField>(
    componentContext = componentContext,
    tableBuilder = tableBuilder,
    onCreate = tableBuilder.onCreate,
    onItemClick = onItemClick,
    mapper = { ProductPlanTableUi(id, createdAt) },
    filterMatcher = ProductPlanFilterMatcher,
    searchMatcher = displayedTextSearchMatcher { listOf(it.composeId.toString(), it.createdAt.toTableDisplayText()) },
    sortMatcher = ProductPlanSorter,
    repository = repository,
) {
    override val columns = tableColumns<ProductPlanTableUi, ProductPlanField, TableData<ProductPlanTableUi>> {
        idWithSelection(
            selectionKey = ProductPlanField.SELECTION,
            idKey = ProductPlanField.ID,
            onEvent = ::onEvent,
        )
        readDateTimeColumn(
            headerText = "Дата и время",
            column = ProductPlanField.CREATED_AT,
            valueOf = ProductPlanTableUi::createdAt,
            filterType = TableFilterType.DateTableFilter(),
        )
    }

    override val exportConfiguration = TableExportConfiguration<ProductPlanTableUi, ProductPlanField>(
        suggestedFileName = "product-plans-export",
    )
}

private object ProductPlanFilterMatcher : FilterMatcher<ProductPlanTableUi, ProductPlanField>() {
    override fun matchesRules(item: ProductPlanTableUi, column: ProductPlanField, stateAny: TableFilterState<*>) =
        when (column) {
            ProductPlanField.SELECTION, ProductPlanField.ID -> true
            ProductPlanField.CREATED_AT -> matchesDateTimeField(item.createdAt, stateAny)
        }
}

private object ProductPlanSorter : SortMatcher<ProductPlanTableUi, ProductPlanField> {
    override fun sort(items: List<ProductPlanTableUi>, sort: SortState<ProductPlanField>?): List<ProductPlanTableUi> {
        if (sort?.column != ProductPlanField.CREATED_AT) return items
        return if (sort.order == SortOrder.DESCENDING) items.sortedByDescending(ProductPlanTableUi::createdAt)
        else items.sortedBy(ProductPlanTableUi::createdAt)
    }
}
