package ru.pavlig43.storage.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.essenty.instancekeeper.getOrCreate
import jdk.jfr.internal.OldObjectSample.emit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.DateTimeComponent
import ru.pavlig43.core.MainTabComponent
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.getCurrentLocalDateTime
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.database.data.storage.StorageProduct
import ru.pavlig43.storage.api.StorageDependencies
import ru.pavlig43.storage.internal.di.StorageRepository
import ru.pavlig43.storage.internal.di.createStorageModule
import ru.pavlig43.storage.internal.model.StorageProductUi
import ru.pavlig43.storage.internal.model.StorageTableData
import ru.pavlig43.tablecore.manger.FilterManager
import ua.wwind.table.filter.data.TableFilterState
import kotlinx.serialization.Serializable
import kotlin.collections.map

class StorageComponent(
    componentContext: ComponentContext,
    dependencies: StorageDependencies

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

    private val dialogNavigation = SlotNavigation<StorageDialog>()

    internal val dialog = childSlot(
        source = dialogNavigation,
        key = "storage_dialog",
        serializer = StorageDialog.serializer(),
        handleBackButton = true,
        childFactory = ::createDialogChild
    )

    private val _dateTimePeriodUi = MutableStateFlow(DateTimePeriod())
    internal val dateTimePeriodUi: StateFlow<DateTimePeriod> = _dateTimePeriodUi.asStateFlow()

    private val dateTimePeriodForData = MutableStateFlow(DateTimePeriod())



    fun updateDateTimePeriod() {
        dateTimePeriodForData.update { _dateTimePeriodUi.value }
    }

    private fun createDialogChild(dialogConfig: StorageDialog, context: ComponentContext): DialogChild {
        val currentPeriod = dateTimePeriodUi.value
        return when (dialogConfig) {
            is StorageDialog.StartDateTime -> {
                DialogChild.StartDateTime(
                    DateTimeComponent(
                        componentContext = context,
                        initDatetime = currentPeriod.start,
                        onChangeDate = { newDateTime ->
                            _dateTimePeriodUi.update { it.copy(start = newDateTime) }
                        },
                        onDismissRequest = { dialogNavigation.dismiss() }
                    )
                )
            }
            is StorageDialog.EndDateTime -> {
                DialogChild.EndDateTime(
                    DateTimeComponent(
                        componentContext = context,
                        initDatetime = currentPeriod.end,
                        onChangeDate = { newDateTime ->
                            _dateTimePeriodUi.update { it.copy(end = newDateTime) }
                        },
                        onDismissRequest = { dialogNavigation.dismiss() }
                    )
                )
            }
        }
    }

    fun openStartDateTimeDialog() = dialogNavigation.activate(StorageDialog.StartDateTime)
    fun openEndDateTimeDialog() = dialogNavigation.activate(StorageDialog.EndDateTime)

    private val _products = MutableStateFlow<List<StorageProductUi>>(emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    internal val loadState: StateFlow<LoadState> = dateTimePeriodForData
        .transformLatest { dateTimePeriod ->
            emit(LoadState.Loading)
            storageRepository.observeOnStorageProducts(
                start = dateTimePeriod.start,
                end = dateTimePeriod.end
            )
                .map { result ->
                    result.fold(
                        onSuccess = { lst ->
                            _products.value = lst.toUi()
                            LoadState.Success(_products.value)
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
    ) { products, filters ->
        val filtered = products.filter { item ->
            StorageFilterMatcher.matchesItem(item, filters)
        }
        StorageTableData(displayedProducts = filtered)
    }.stateIn(
        coroutineScope,
        SharingStarted.Lazily,
        StorageTableData()
    )

    fun toggleExpand(productId: Int) {
        _products.value = _products.value.map { product ->
            if (product.itemId == productId) {
                product.copy(expanded = !product.expanded)
            } else {
                product
            }
        }
    }

    fun updateFilters(filters: Map<StorageProductField, TableFilterState<*>>) {
        filterManager.update(filters)
    }


}

internal sealed interface LoadState {
    data object Loading : LoadState
    data class Error(val message: String) : LoadState
    data class Success(val products: List<StorageProductUi>) : LoadState
}

private fun List<StorageProduct>.toUi(): List<StorageProductUi> {
    return flatMap { it.toUi() }
}

private fun StorageProduct.toUi(): List<StorageProductUi> {
    val productItem = StorageProductUi(
        productId = productId,
        itemId = productId,
        productName = productName,
        itemName = productName,
        balanceBeforeStart = balanceBeforeStart,
        incoming = incoming,
        outgoing = outgoing,
        balanceOnEnd = balanceOnEnd,
        isProduct = true,
        expanded = false
    )

    val batchItems = batches.map { batch ->
        StorageProductUi(
            productId = productId,
            itemId = batch.batchId,
            productName = this.productName,
            itemName = batch.batchName,
            balanceBeforeStart = batch.balanceBeforeStart,
            incoming = batch.incoming,
            outgoing = batch.outgoing,
            balanceOnEnd = batch.balanceOnEnd,
            isProduct = false,
            expanded = false
        )
    }

    return listOf(productItem) + batchItems
}

internal data class DateTimePeriod(
    val start: LocalDateTime = getCurrentLocalDateTime(),
    val end: LocalDateTime = getCurrentLocalDateTime()
)

@Serializable
internal sealed interface StorageDialog {
    @Serializable
    data object StartDateTime : StorageDialog

    @Serializable
    data object EndDateTime : StorageDialog
}

sealed interface DialogChild {
    class StartDateTime(val component: DateTimeComponent) : DialogChild
    class EndDateTime(val component: DateTimeComponent) : DialogChild
}
