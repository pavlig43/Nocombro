package ru.pavlig43.rootnocombro.internal.navigation

import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.tabs.TabOpener
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.BatchMovementListConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.ItemFormConfig.DeclarationFormConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.ItemFormConfig.DocumentFormConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.ItemFormConfig.ExpenseFormConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.ItemFormConfig.ProductFormConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.ItemFormConfig.TransactionFormConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.ItemFormConfig.VendorFormConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.ProfitabilityConfig

internal fun createMainTabOpener(
    addTab: (MainTabConfig) -> Unit,
): TabOpener = object : TabOpener {
    override fun openDocumentTab(id: Int) {
        addTab(DocumentFormConfig(id))
    }

    override fun openProductTab(id: Int) {
        addTab(ProductFormConfig(id))
    }

    override fun openVendorTab(id: Int) {
        addTab(VendorFormConfig(id))
    }

    override fun openDeclarationTab(id: Int) {
        addTab(DeclarationFormConfig(id))
    }

    override fun openTransactionTab(id: Int) {
        addTab(TransactionFormConfig(id))
    }

    override fun openBatchMovementTab(
        batchId: Int,
        productName: String,
        start: LocalDateTime,
        end: LocalDateTime,
    ) {
        addTab(BatchMovementListConfig(batchId, productName, start, end))
    }

    override fun openExpenseFormTab(id: Int) {
        addTab(ExpenseFormConfig(id))
    }

    override fun openProfitabilityTab() {
        addTab(ProfitabilityConfig())
    }
}
