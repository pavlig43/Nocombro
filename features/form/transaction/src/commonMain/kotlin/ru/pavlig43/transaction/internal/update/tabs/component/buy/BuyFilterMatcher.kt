package ru.pavlig43.transaction.internal.update.tabs.component.buy

import ru.pavlig43.tablecore.utils.FilterMatcher
import ua.wwind.table.filter.data.TableFilterState


internal object BuyFilterMatcher: FilterMatcher<BuyUi, BuyField>() {

    override fun matchesRules(
        item: BuyUi,
        column: BuyField,
        stateAny: TableFilterState<*>
    ): Boolean {
        val matches = when(column){
            BuyField.SELECTION -> true
            BuyField.COMPOSE_ID -> true
            BuyField.PRODUCT_NAME -> matchesTextField(item.productName,stateAny)
            BuyField.DECLARATION_NAME -> matchesTextField(item.declarationName,stateAny)
            BuyField.VENDOR_NAME -> matchesTextField(item.vendorName,stateAny)
            BuyField.DATE_BORN -> matchesDateField(item.dateBorn,stateAny)
            BuyField.PRICE -> matchesIntField(item.price,stateAny)
            BuyField.COMMENT -> matchesTextField(item.comment,stateAny)
            BuyField.COUNT -> matchesIntField(item.count,stateAny)
            else -> true
        }
        return matches
        }
    }

