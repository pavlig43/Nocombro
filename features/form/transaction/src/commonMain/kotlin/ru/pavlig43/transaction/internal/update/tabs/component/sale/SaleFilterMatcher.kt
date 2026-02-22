package ru.pavlig43.transaction.internal.update.tabs.component.sale

import ru.pavlig43.tablecore.utils.FilterMatcher
import ua.wwind.table.filter.data.TableFilterState


internal object SaleFilterMatcher : FilterMatcher<SaleUi, SaleField>() {

    override fun matchesRules(
        item: SaleUi,
        column: SaleField,
        stateAny: TableFilterState<*>
    ): Boolean {
        val matches = when (column) {
            SaleField.SELECTION -> true
            SaleField.COMPOSE_ID -> true
            SaleField.PRODUCT_NAME -> matchesTextField(item.productName, stateAny)
            SaleField.CLIENT_NAME -> matchesTextField(item.clientName, stateAny)
            SaleField.VENDOR_NAME -> matchesTextField(item.vendorName, stateAny)
            SaleField.DATE_BORN -> matchesDateField(item.dateBorn, stateAny)
            SaleField.PRICE -> matchesIntField(item.price, stateAny)
            SaleField.COMMENT -> matchesTextField(item.comment, stateAny)
            SaleField.COUNT -> matchesIntField(item.count, stateAny)
            SaleField.BATCH_ID -> matchesIntField(item.batchId, stateAny)
            else -> true
        }
        return matches
    }
}
