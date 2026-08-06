package ru.pavlig43.storage.api.component.storage

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import ru.pavlig43.core.model.DecimalData3
import ru.pavlig43.core.model.toStartDoubleFormat
import ru.pavlig43.core.MainTabComponent
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.tabs.TabOpener
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.database.data.batch.StorageLocation
import ru.pavlig43.database.data.storage.StorageOperationPreview
import ru.pavlig43.database.data.storage.StorageProduct
import ru.pavlig43.database.data.storage.StorageTransferRequest
import ru.pavlig43.database.data.storage.StorageWriteOffRequest
import ru.pavlig43.datetime.period.dateTime.DTPeriod
import ru.pavlig43.database.data.transact.StockOperationReason
import ru.pavlig43.datetime.period.dateTime.DateTimePeriodComponent
import ru.pavlig43.datetime.getCurrentLocalDateTime
import ru.pavlig43.datetime.single.datetime.DateTimeComponent
import ru.pavlig43.storage.api.StorageDependencies
import ru.pavlig43.storage.internal.di.StorageRepository
import ru.pavlig43.storage.internal.di.createStorageModule
import ru.pavlig43.storage.internal.model.StorageProductUi
import ru.pavlig43.storage.internal.model.StorageTableData
import ru.pavlig43.tablecore.manger.FilterManager
import ua.wwind.table.filter.data.TableFilterState

class StorageComponent(
    componentContext: ComponentContext,
    dependencies: StorageDependencies,
    private val tabOpener: TabOpener,

) : ComponentContext by componentContext, MainTabComponent {

    private val koinComponent = instanceKeeper.getOrCreate { ComponentKoinContext() }
    private val scope = koinComponent.getOrCreateKoinScope(
        createStorageModule(dependencies)
    )
    private val storageRepository: StorageRepository = scope.get()

    private val _model = MutableStateFlow(MainTabComponent.NavTabState("Склад"))
    override val model = _model.asStateFlow()
    private val coroutineScope = componentCoroutineScope()

    private val filterManager = FilterManager<StorageProductField>(childContext("filter"))

    internal val dTPeriodComponent = DateTimePeriodComponent(
        componentContext = childContext("date_time_period"),
        initDTPeriod = DTPeriod.now
    )


    private val _products = MutableStateFlow<List<StorageProductUi>>(emptyList())

    /** Изменяемая строка поиска, которой владеет компонент экрана склада. */
    private val _searchQuery = MutableStateFlow("")

    /** Текущий запрос поиска по сводным строкам товаров. */
    internal val searchQuery = _searchQuery.asStateFlow()

    /**
     * Все партии с историей отрицательного остатка независимо от поиска,
     * табличных фильтров и состояния раскрытия товара.
     */
    internal val negativeBatches: StateFlow<List<StorageProductUi>> = _products
        .map { products ->
            products.filter { item -> !item.isProduct && item.hasNegativeBalanceHistory }
        }
        .stateIn(
            coroutineScope,
            SharingStarted.Lazily,
            emptyList(),
        )

    private val _storageLocation = MutableStateFlow(StorageLocation.MAIN)
    internal val storageLocation = _storageLocation.asStateFlow()

    private val _batchActions = MutableStateFlow<StorageBatchActionsState?>(null)
    internal val batchActions = _batchActions.asStateFlow()

    private val _operationDialog = MutableStateFlow<StorageOperationDialogState?>(null)
    internal val operationDialog = _operationDialog.asStateFlow()
    @OptIn(ExperimentalCoroutinesApi::class)
    internal val loadState: StateFlow<LoadState> = combine(
        dTPeriodComponent.dateTimePeriodForData,
        _storageLocation,
    ) { period, location -> period to location }
        .transformLatest { (dateTimePeriod, location) ->
            emit(LoadState.Loading)
            storageRepository.observeOnStorageProducts(
                start = dateTimePeriod.start,
                end = dateTimePeriod.end,
                storageLocation = location,
            )
                .map { result ->
                    result.fold(
                        onSuccess = { lst ->
                            _products.update { lst.toUi() }
                            LoadState.Success
                        },
                        onFailure = { throwable -> LoadState.Error(throwable.message ?: "") }
                    )
                }
                .collect { emit(it) }
        }
        .stateIn(
            coroutineScope,
            SharingStarted.Lazily,
            LoadState.Loading
        )

    internal val tableData: StateFlow<StorageTableData> = combine(
        _products,
        filterManager.filters,
        _searchQuery,
    ) { products, filters, searchQuery ->
        val normalizedQuery = searchQuery.trim()
        val matchingProductIds = if (normalizedQuery.isEmpty()) {
            emptySet()
        } else {
            products
                .filter { item -> item.matchesProductSearch(normalizedQuery) }
                .mapTo(mutableSetOf()) { item -> item.productId }
        }
        val expandedProductIds = products
            .filter { it.isProduct && it.isExpanded }
            .map { it.productId }
            .toSet()
        val productItems = products.filter { it.isProduct }

        val filtered = products.filter { item ->
            val matchesFilter = StorageFilterMatcher.matchesItem(item, filters)
            val hasNegativeValues = item.balanceBeforeStart < 0 ||
                                    item.incoming < 0 ||
                                    item.outgoing < 0 ||
                                    item.balanceOnEnd < 0
            val matchesSearch = normalizedQuery.isEmpty() || item.productId in matchingProductIds
            val matchesExpansion = when {
                item.isProduct -> true
                item.hasNegativeBalanceHistory -> true
                hasNegativeValues -> true  // Показывать партии с отрицательными значениями
                else -> item.productId in expandedProductIds
            }
            matchesFilter && matchesSearch && matchesExpansion
        }
        StorageTableData(
            displayedProducts = filtered,
            areAllProductsExpanded = productItems.isNotEmpty() && productItems.all { it.isExpanded },
        )
    }.stateIn(
        coroutineScope,
        SharingStarted.Lazily,
        StorageTableData()
    )

    fun toggleExpand(productId: Int) {
        _products.value = _products.value.map { product ->
            if (product.productId == productId && product.isProduct) {
                product.copy(isExpanded = !product.isExpanded)
            } else {
                product
            }
        }
    }

    internal fun toggleExpandAll() {
        _products.update { products ->
            val shouldExpand = products.any { it.isProduct && !it.isExpanded }
            products.map { product ->
                if (product.isProduct) {
                    product.copy(isExpanded = shouldExpand)
                } else {
                    product
                }
            }
        }
    }

    internal fun expandProduct(productId: Int) {
        _products.update { products ->
            products.map { product ->
                if (product.productId == productId && product.isProduct && !product.isExpanded) {
                    product.copy(isExpanded = true)
                } else {
                    product
                }
            }
        }
    }

    fun updateFilters(filters: Map<StorageProductField, TableFilterState<*>>) {
        filterManager.update(filters)
    }

    /** Обновляет запрос поиска по данным товаров, не включая названия партий. */
    internal fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    internal fun openProduct(productId: Int) {
        tabOpener.openProductTab(productId)
    }

    internal fun onSelectStorageLocation(location: StorageLocation) {
        _storageLocation.value = location
        _batchActions.value = null
    }

    internal fun onDismissBatchActions() {
        _batchActions.value = null
    }

    internal fun onOpenBatchHistory() {
        val item = _batchActions.value?.item ?: return
        _batchActions.value = null
        tabOpener.openBatchMovementTab(
            batchId = item.itemId,
            productName = item.productName,
            start = dTPeriodComponent.dateTimePeriodForData.value.start,
            end = dTPeriodComponent.dateTimePeriodForData.value.end,
        )
    }

    internal fun onOpenTransfer() {
        val actions = _batchActions.value ?: return
        val target = when (actions.storageLocation) {
            StorageLocation.MAIN -> StorageLocation.EXPERIMENTAL
            StorageLocation.EXPERIMENTAL -> StorageLocation.MAIN
        }
        openOperation(
            kind = StorageOperationKind.TRANSFER,
            item = actions.item,
            source = actions.storageLocation,
            target = target,
            reason = if (target == StorageLocation.MAIN) {
                StockOperationReason.RETURN
            } else {
                StockOperationReason.EXPERIMENT
            },
        )
    }

    internal fun onOpenWriteOff() {
        val actions = _batchActions.value ?: return
        openOperation(
            kind = StorageOperationKind.WRITE_OFF,
            item = actions.item,
            source = actions.storageLocation,
            target = null,
            reason = StockOperationReason.EXPERIMENT,
        )
    }

    private fun openOperation(
        kind: StorageOperationKind,
        item: StorageProductUi,
        source: StorageLocation,
        target: StorageLocation?,
        reason: StockOperationReason,
    ) {
        _operationDialog.value = StorageOperationDialogState(
            kind = kind,
            batchId = item.itemId,
            productName = item.productName,
            batchName = item.itemName,
            source = source,
            target = target,
            availableCount = item.balanceOnEnd,
            occurredAt = getCurrentLocalDateTime(),
            reason = reason,
        )
        coroutineScope.launch {
            storageRepository.previewOperation(item.itemId, source, 0L).fold(
                onSuccess = { preview -> applyPreview(item.itemId, preview) },
                onFailure = { error ->
                    _operationDialog.update { state ->
                        state?.takeIf { it.batchId == item.itemId }?.copy(
                            isPreviewLoading = false,
                            error = error.message ?: "Не удалось рассчитать остаток партии",
                        )
                    }
                },
            )
        }
    }

    private fun applyPreview(batchId: Int, preview: StorageOperationPreview) {
        _operationDialog.update { state ->
            state?.takeIf { it.batchId == batchId }?.copy(
                availableCount = preview.availableCount,
                costPricePerUnit = preview.costPricePerUnit,
                expiryDate = preview.expiryDate,
                isPreviewLoading = false,
                error = null,
            )
        }
    }

    internal fun onUpdateOperationCount(value: String) {
        _operationDialog.update { current ->
            current?.copy(countText = value, error = null)?.let { updated ->
                updated.copy(
                    selectedCost = updated.costPricePerUnit
                        ?.let { cost -> updated.count?.let { count -> cost * count / 1000 } }
                        ?: 0,
                    requiresMissingCostConfirmation = false,
                )
            }
        }
    }

    internal fun onUpdateOperationDateTime(value: kotlinx.datetime.LocalDateTime) {
        _operationDialog.update { it?.copy(occurredAt = value, error = null) }
    }

    internal fun onUpdateOperationReason(value: StockOperationReason) {
        _operationDialog.update { it?.copy(reason = value, error = null) }
    }

    internal fun onUpdateOperationComment(value: String) {
        _operationDialog.update { it?.copy(comment = value, error = null) }
    }

    internal fun onDismissOperation() {
        if (_operationDialog.value?.isSaving != true) _operationDialog.value = null
    }

    internal fun onSubmitOperation(confirmMissingCost: Boolean = false) {
        val state = _operationDialog.value ?: return
        val count = state.count
        if (count == null || count <= 0) {
            _operationDialog.value = state.copy(error = "Укажите количество больше нуля")
            return
        }
        if (count > state.availableCount) {
            _operationDialog.value = state.copy(error = "Количество больше остатка на складе")
            return
        }
        if (state.kind == StorageOperationKind.WRITE_OFF && state.costPricePerUnit == null && !confirmMissingCost) {
            _operationDialog.value = state.copy(
                requiresMissingCostConfirmation = true,
                error = "Себестоимость не рассчитана. В расходы попадёт 0 ₽.",
            )
            return
        }
        saveOperation(state, count)
    }

    private fun saveOperation(state: StorageOperationDialogState, count: Long) {
        _operationDialog.value = state.copy(isSaving = true, error = null)
        coroutineScope.launch {
            val result = when (state.kind) {
                StorageOperationKind.TRANSFER -> storageRepository.transfer(
                    StorageTransferRequest(state.batchId, state.source, requireNotNull(state.target), count, state.occurredAt, state.reason, state.comment)
                )
                StorageOperationKind.WRITE_OFF -> storageRepository.writeOff(
                    StorageWriteOffRequest(
                        batchId = state.batchId,
                        source = state.source,
                        count = count,
                        occurredAt = state.occurredAt,
                        reason = state.reason,
                        comment = state.comment,
                        allowMissingCost = state.requiresMissingCostConfirmation,
                    )
                )
            }
            result.fold(
                onSuccess = { _operationDialog.value = null; _batchActions.value = null },
                onFailure = { error ->
                    _operationDialog.update { it?.copy(isSaving = false, error = error.message ?: "Не удалось сохранить операцию") }
                },
            )
        }
    }

    internal fun onRowClick(item: StorageProductUi) {
        if (!item.isProduct) {
            _batchActions.value = StorageBatchActionsState(item, _storageLocation.value)
            return
        }
        // Товар открывается по отдельной кнопке в строке.
    }

}

internal sealed interface LoadState {
    data object Loading : LoadState
    data class Error(val message: String) : LoadState
    data object Success : LoadState
}

@Suppress("UnusedPrivateMember")
private fun List<StorageProduct>.toUi(): List<StorageProductUi> {
    return flatMap { it.toUi() }
}
@Suppress("UnusedPrivateMember")
private fun StorageProduct.toUi(): List<StorageProductUi> {
    val productItem = StorageProductUi(
        productId = productId,
        itemId = productId,
        productName = productName,
        vendorNames = vendorNames,
        itemName = productName,
        balanceBeforeStart = balanceBeforeStart,
        incoming = incoming,
        outgoing = outgoing,
        balanceOnEnd = balanceOnEnd,
        isProduct = true,
    )

    val batchItems = batches.map { batch ->
        StorageProductUi(
            productId = productId,
            itemId = batch.batchId,
            productName = this.productName,
            vendorNames = batch.vendorName,
            itemName = batch.batchName,
            balanceBeforeStart = batch.balanceBeforeStart,
            incoming = batch.incoming,
            outgoing = batch.outgoing,
            balanceOnEnd = batch.balanceOnEnd,
            isProduct = false,
            hasNegativeBalanceHistory = batch.hasNegativeBalanceHistory,
        )
    }

    return listOf(productItem) + batchItems
}

/**
 * Проверяет совпадение запроса с видимыми полями сводной строки товара.
 *
 * Строки партий намеренно всегда возвращают `false`: совпавший товар выводится
 * вместе со своими партиями по обычным правилам раскрытия и отображения ошибок.
 */
private fun StorageProductUi.matchesProductSearch(normalizedQuery: String): Boolean {
    if (!isProduct) return false
    return sequenceOf(
        productName,
        vendorNames,
        DecimalData3(balanceBeforeStart).toStartDoubleFormat(),
        DecimalData3(incoming).toStartDoubleFormat(),
        DecimalData3(outgoing).toStartDoubleFormat(),
        DecimalData3(balanceOnEnd).toStartDoubleFormat(),
    ).any { value -> value.contains(normalizedQuery, ignoreCase = true) }
}



@Serializable
internal sealed interface StorageDialog {
    @Serializable
    data object StartDateTime : StorageDialog

    @Serializable
    data object EndDateTime : StorageDialog
}

sealed interface DialogChild {
    class DateTime(val component: DateTimeComponent) : DialogChild
}
