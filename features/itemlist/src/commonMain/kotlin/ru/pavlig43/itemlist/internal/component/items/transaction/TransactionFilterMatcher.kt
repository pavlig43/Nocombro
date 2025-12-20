package ru.pavlig43.itemlist.internal.component.items.transaction

import ru.pavlig43.itemlist.internal.utils.FilterMatcher
import ua.wwind.table.filter.data.TableFilterState

internal object TransactionFilterMatcher: FilterMatcher<TransactionItemUi, TransactionField>() {
    override fun matchesRules(
        item: TransactionItemUi,
        column: TransactionField,
        stateAny: TableFilterState<*>
    ): Boolean {
        val matches =
            when (column) {

                TransactionField.SELECTION -> true
                TransactionField.ID -> true
                TransactionField.IS_COMPLETED -> matchesBooleanField(item.isCompleted,stateAny)
                TransactionField.TRANSACTION_TYPE -> matchesTypeField(item.transactionType,stateAny)
                TransactionField.CREATED_AT -> matchesDateTimeField(item.createdAt,stateAny)
                TransactionField.COMMENT -> matchesTextField(item.comment, stateAny)

            }
        return  matches
    }

}