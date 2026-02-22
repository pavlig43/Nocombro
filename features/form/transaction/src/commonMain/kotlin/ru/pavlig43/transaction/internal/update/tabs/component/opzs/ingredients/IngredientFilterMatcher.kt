package ru.pavlig43.transaction.internal.update.tabs.component.opzs.ingredients

import ru.pavlig43.tablecore.utils.FilterMatcher
import ua.wwind.table.filter.data.TableFilterState

internal object IngredientFilterMatcher : FilterMatcher<IngredientUi, IngredientField>() {

    override fun matchesRules(
        item: IngredientUi,
        column: IngredientField,
        stateAny: TableFilterState<*>
    ): Boolean {
        val matches = when (column) {
            IngredientField.SELECTION -> true
            IngredientField.COMPOSE_ID -> true
            IngredientField.PRODUCT_NAME -> matchesTextField(item.productName, stateAny)
            IngredientField.VENDOR_NAME -> matchesTextField(item.vendorName, stateAny)
            IngredientField.COUNT -> matchesIntField(item.balance, stateAny)
            IngredientField.BATCH_NAME -> matchesIntField(item.batchId, stateAny)
        }
        return matches
    }
}
