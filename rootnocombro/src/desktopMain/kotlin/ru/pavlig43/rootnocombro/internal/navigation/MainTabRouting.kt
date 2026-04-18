package ru.pavlig43.rootnocombro.internal.navigation

import ru.pavlig43.core.tabs.TabOpener
import ru.pavlig43.notification.api.model.NotificationItem
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.AnalyticConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.DoctorConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.ItemListConfig.DeclarationListConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.ItemListConfig.DocumentListConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.ItemListConfig.ExpenseListConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.ItemListConfig.ProductListConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.ItemListConfig.SafetyListConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.ItemListConfig.TransactionListConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.ItemListConfig.VendorListConfig
import ru.pavlig43.rootnocombro.internal.navigation.drawer.component.DrawerDestination

internal fun DrawerDestination.toMainTabConfig(): MainTabConfig =
    when (this) {
        DrawerDestination.Analytic -> AnalyticConfig()
        DrawerDestination.DocumentList -> DocumentListConfig()
        DrawerDestination.ProductList -> ProductListConfig()
        DrawerDestination.VendorList -> VendorListConfig()
        DrawerDestination.DeclarationList -> DeclarationListConfig()
        DrawerDestination.ProductTransactionList -> TransactionListConfig()
        DrawerDestination.ExpenseList -> ExpenseListConfig()
        DrawerDestination.Safety -> SafetyListConfig()
        DrawerDestination.SampleTable -> MainTabConfig.SampleTableConfig()
        DrawerDestination.Storage -> MainTabConfig.StorageConfig()
        DrawerDestination.Doctor -> DoctorConfig()
    }

internal fun NotificationItem.openIn(tabOpener: TabOpener, id: Int) {
    when (this) {
        NotificationItem.Document -> tabOpener.openDocumentTab(id)
        NotificationItem.Product -> tabOpener.openProductTab(id)
        NotificationItem.Declaration -> tabOpener.openDeclarationTab(id)
        NotificationItem.Transaction -> tabOpener.openTransactionTab(id)
        NotificationItem.BatchExpiry -> tabOpener.openTransactionTab(id)
    }
}
