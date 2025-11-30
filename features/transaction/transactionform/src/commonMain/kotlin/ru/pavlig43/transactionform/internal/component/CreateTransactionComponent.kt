package ru.pavlig43.transactionform.internal.component

import com.arkivanov.decompose.ComponentContext
import ru.pavlig43.database.data.transaction.ProductTransaction
import ru.pavlig43.itemlist.api.ItemListDependencies
import ru.pavlig43.manageitem.api.data.CreateEssentialsRepository

internal class CreateTransactionComponent(
    componentContext: ComponentContext,
    itemListDependencies: ItemListDependencies,
    updateItemRepository: CreateEssentialsRepository<ProductTransaction>,

    onSuccessCreate: (Int) -> Unit,
    onChangeValueForMainTab: (String) -> Unit
) {
    val requires = TransactionRequiresComponent(
        componentContext = componentContext,
        onChangeValueForMainTab = onChangeValueForMainTab,
        getInitData = null,
        itemListDependencies = itemListDependencies,
        openProductTab = TODO(),
    )
}
//internal class CreateDeclarationComponent(
//    componentContext: ComponentContext,
//    createItemRepository: CreateItemRepository<DeclarationIn>,
//    onSuccessCreate: (Int) -> Unit,
//    onOpenVendorTab: (Int) -> Unit,
//    vendorListRepository: IItemListRepository<Vendor, VendorType>,
//    onChangeValueForMainTab: (String) -> Unit
//): ComponentContext by componentContext {
//    val requires = DeclarationRequiresComponent(
//        componentContext = componentContext,
//        onChangeValueForMainTab = onChangeValueForMainTab,
//        getInitData = null,
//        openVendorTab = onOpenVendorTab,
//        vendorListRepository = vendorListRepository,
//    )
//    val createComponent: ICreateComponent<Int> = CreateComponent(
//        componentContext = childContext("create"),
//        onSaveResult = {createItemRepository.createItem(requires.requiresValuesWithDate.value.toDeclarationIn())},
//        otherValidValue = requires.isValidAllValue,
//        onSuccessAction = onSuccessCreate
//    )
//}