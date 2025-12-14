//package ru.pavlig43.transaction.internal.component.tabs.tabslot
//
//import com.arkivanov.decompose.ComponentContext
//import com.arkivanov.decompose.childContext
//import com.arkivanov.decompose.router.slot.SlotNavigation
//import com.arkivanov.decompose.router.slot.activate
//import com.arkivanov.decompose.router.slot.childSlot
//import com.arkivanov.decompose.router.slot.dismiss
//import kotlinx.serialization.Serializable
//import ru.pavlig43.core.RequestResult
//import ru.pavlig43.database.data.product.ProductType
//import ru.pavlig43.database.data.transaction.TransactionProductBDIn
//import ru.pavlig43.database.data.transaction.TransactionProductBDOut
//import ru.pavlig43.itemlist.api.ItemListDependencies
//import ru.pavlig43.itemlist.api.ProductListParamProvider
//import ru.pavlig43.itemlist.api.component.MBSItemListComponent
//import ru.pavlig43.itemlist.dynamic.ColumnDefinition1
//import ru.pavlig43.itemlist.dynamic.DateCellElement
//import ru.pavlig43.itemlist.dynamic.TextCellElement
//import ru.pavlig43.itemlist.dynamic.component.DynamicListComponent
//import ru.pavlig43.itemlist.dynamic.component.IDynamicItemUi
//import ru.pavlig43.itemlist.internal.component.ProductItemUi
//import ru.pavlig43.update.data.UpdateCollectionRepository
//
//class TransactionProductsTabSlot(
//    componentContext: ComponentContext,
//    transactionId: Int,
//    itemListDependencies: ItemListDependencies,
//    openProductTab: (Int) -> Unit,
//    private val updateRepository: UpdateCollectionRepository<TransactionProductBDOut, TransactionProductBDIn>,
//) : ComponentContext by componentContext, TransactionFormSlot {
//    override val title: String = "Продукты"
//
//
//    val columnDefinition = listOf<ColumnDefinition1<TransactionProductUi>>(
//        ColumnDefinition1(
//            width = 25,
//            headerTitle = "Продукт",
//            cellProvider = { ui, width -> TextCellElement(ui.productName ?: "", width) }
//        ),
//        ColumnDefinition1(
//            width = 25,
//            headerTitle = "Дата производства",
//            cellProvider = { ui, width -> DateCellElement(
//                date = ui.dateBorn?: 0,
//                onDateChange = {date->
//                    updateProduct(ui.composeKey, { ui.copy(dateBorn = date) })
//                },
//                columnWith = width
//            ) }
//        )
//
//    )
//    internal val productListComponent = DynamicListComponent(
//        componentContext = childContext("list"),
//        getInitData = { updateRepository.getInit(transactionId) },
//        columnDefinition = columnDefinition,
//        generateEmptyUi = {composeKey -> TransactionProductUi(id = 0, composeKey = composeKey)},
//        mapper = {this.toUi()},
//    )
//    fun updateProduct(composeKey: Int, updateProduct: (TransactionProductUi) -> TransactionProductUi){
//        productListComponent.updateRow(composeKey, updateProduct)
//    }
//
//        private val dialogNavigation = SlotNavigation<DialogConfig>()
//
//    internal val dialog = childSlot(
//        source = dialogNavigation,
//        key = "dialog",
//        serializer = DialogConfig.serializer(),
//        handleBackButton = true,
//    ) { config, context ->
//        when (config) {
//            is DialogConfig.Product -> MBSItemListComponent<ProductItemUi>(
//                componentContext = context,
//                itemListParamProvider = ProductListParamProvider(
//                    fullListProductTypes = ProductType.entries,
//                    withCheckbox = false
//                ),
//                onItemClick = {
//                    productListComponent.updateRow(
//                        composeKey = config.composeKey
//                    ){transactionProductUi ->
//                        transactionProductUi.copy(productId = it.id, productName = it.displayName)
//                    }
//                },
//                onCreate = { openProductTab(0) },
//                itemListDependencies = itemListDependencies,
//                onDismissed = {dialogNavigation.dismiss()}
//            )
//        }
//    }
//
//    private fun showDialog(intent: DialogIntent) {
//        val composeKey = intent.composeKey
//        when (intent) {
//            is DialogIntent.Product -> dialogNavigation.activate(DialogConfig.Product(composeKey))
//        }
//    }
//
//
//
//
//    override suspend fun onUpdate(): RequestResult<Unit> {
//        TODO("Not yet implemented")
//    }
//
//}
//
//
//internal sealed interface DialogIntent{
//    abstract val composeKey: Int
//    data class Product(override val composeKey: Int): DialogIntent
//}
//
//@Serializable
//internal sealed interface DialogConfig {
//    abstract val composeKey: Int
//
//    data class Product(override val composeKey: Int) : DialogConfig
//}
//
///**
// * Покупка, продажа
// */
//data class TransactionProductUi(
//    override val id: Int,
//    override val composeKey: Int,
//    val productId: Int? = null,
//    val productName: String? = null,
//    val dateBorn: Long? = null,
//    val batch: Int? = null,
//    val declarationId: Int? = null,
//    val declarationName: String? = null,
//    val vendorName: String? = null,
//    override val isChecked: Boolean = false,
//): IDynamicItemUi
//
//private fun List<TransactionProductBDOut>.toUi(): List<TransactionProductUi> {
//    return this.mapIndexed { index, out -> out.toUi(index) }
//}
//
//private fun TransactionProductBDOut.toUi(composeKey: Int): TransactionProductUi {
//    return TransactionProductUi(
//        productId = productId,
//        productName = productName,
//        dateBorn = dateBorn,
//        batch = batch,
//        declarationId = declarationId,
//        declarationName = declarationName,
//        vendorName = vendorName,
//        id = id,
//        composeKey = composeKey
//    )
//}
