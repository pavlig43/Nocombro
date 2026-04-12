package ru.pavlig43.rootnocombro.internal.navigation

import ru.pavlig43.core.MainTabComponent
import ru.pavlig43.declaration.api.DeclarationFormComponent
import ru.pavlig43.doctor.api.component.DoctorComponent
import ru.pavlig43.document.api.component.DocumentFormComponent
import ru.pavlig43.expense.api.component.ExpenseFormComponent
import ru.pavlig43.immutable.api.component.ImmutableTableComponentFactoryMain
import ru.pavlig43.main.api.component.AnalyticMainComponent
import ru.pavlig43.notification.api.component.NotificationComponent
import ru.pavlig43.product.api.component.ProductFormComponent
import ru.pavlig43.profitability.internal.component.ProfitabilityComponent
import ru.pavlig43.sampletable.api.component.SampleTableComponent
import ru.pavlig43.storage.api.component.batchMovement.BatchMovementComponent
import ru.pavlig43.storage.api.component.storage.StorageComponent
import ru.pavlig43.transaction.api.component.TransactionFormComponent
import ru.pavlig43.vendor.api.component.VendorFormComponent

internal sealed interface MainTabChild {
    val component: MainTabComponent

    class NotificationChild(override val component: NotificationComponent) : MainTabChild

    class SampleTableChild(override val component: SampleTableComponent) : MainTabChild

    class StorageChild(override val component: StorageComponent) : MainTabChild

    class MainMoneyChild(override val component: AnalyticMainComponent) : MainTabChild

    class ProfitabilityChild(override val component: ProfitabilityComponent) : MainTabChild

    class DoctorChild(override val component: DoctorComponent) : MainTabChild

    class BatchMovementChild(override val component: BatchMovementComponent) : MainTabChild

    class ImmutableTableChild(override val component: ImmutableTableComponentFactoryMain) : MainTabChild

    sealed interface ItemFormChild: MainTabChild{

        class DocumentFormChild(override val component: DocumentFormComponent): ItemFormChild
        class ProductFormChild(override val component: ProductFormComponent): ItemFormChild
        class VendorFormChild(override val component: VendorFormComponent): ItemFormChild
        class DeclarationFormChild(override val component: DeclarationFormComponent): ItemFormChild
        class TransactionFormChild(override val component: TransactionFormComponent): ItemFormChild
        class ExpenseFormChild(override val component: ExpenseFormComponent): ItemFormChild
    }
}
