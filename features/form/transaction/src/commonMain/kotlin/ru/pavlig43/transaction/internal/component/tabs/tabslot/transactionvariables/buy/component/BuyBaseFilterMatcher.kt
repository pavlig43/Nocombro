//package ru.pavlig43.transaction.internal.component.tabs.tabslot.transactionvariables.buy.component
//
//import ru.pavlig43.tablecore.utils.FilterMatcher
//import ru.pavlig43.transaction.internal.component.tabs.tabslot.transactionvariables.buy.BuyBaseUi
//import ua.wwind.table.filter.data.TableFilterState
//
//internal object BuyBaseFilterMatcher: FilterMatcher<BuyBaseUi, BuyBaseField>() {
//
//    override fun matchesRules(
//        item: BuyBaseUi,
//        column: BuyBaseField,
//        stateAny: TableFilterState<*>
//    ): Boolean {
//        val matches = when(column){
//            BuyBaseField.SELECTION -> true
//            BuyBaseField.COMPOSE_KEY -> true
//            BuyBaseField.PRODUCT_NAME -> matchesTextField(item.productName,stateAny)
//            BuyBaseField.DECLARATION_NAME -> matchesTextField(item.declarationName,stateAny)
//            BuyBaseField.VENDOR_NAME -> matchesTextField(item.vendorName,stateAny)
//            BuyBaseField.DATE_BORN -> matchesDateField(item.dateBorn,stateAny)
//            BuyBaseField.PRICE -> matchesIntField(item.price,stateAny)
//            BuyBaseField.COMMENT -> matchesTextField(item.comment,stateAny)
//        }
//        return matches
//        }
//    }
//
