package ru.pavlig43.storage.api.component.batchMovement

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import ru.pavlig43.core.MainTabComponent
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.database.data.storage.BatchMovementWithBalanceInfoBD
import ru.pavlig43.datetime.dateTimeFormat
import ru.pavlig43.datetime.period.dateTime.DTPeriod
import ru.pavlig43.datetime.period.dateTime.DateTimePeriodComponent
import ru.pavlig43.storage.api.StorageDependencies
import ru.pavlig43.storage.internal.di.StorageRepository
import ru.pavlig43.storage.internal.di.createStorageModule

class BatchMovementComponent(
    componentContext: ComponentContext,
    dependencies: StorageDependencies,
    private val batchId: Int,
    productName: String,
    initStart: LocalDateTime,
    initEnd: LocalDateTime,
) : ComponentContext by componentContext, MainTabComponent {

    private val koinComponent = instanceKeeper.getOrCreate { ComponentKoinContext() }
    private val scope = koinComponent.getOrCreateKoinScope(
        createStorageModule(dependencies)
    )
    private val repository: StorageRepository = scope.get()
    private val tabOpener = dependencies.tabOpener

    private val _model = MutableStateFlow(MainTabComponent.NavTabState("Движения: $productName"))
    override val model = _model.asStateFlow()

    private val coroutineScope = componentCoroutineScope()

    internal val dateTimeComponent = DateTimePeriodComponent(
        componentContext = childContext("date_time_period"),
        initDTPeriod = DTPeriod(initStart,initEnd),
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    internal val loadState: StateFlow<BatchMovementLoadState> =
        dateTimeComponent.dateTimePeriodForData
            .transformLatest { dateTimePeriod ->
                emit(BatchMovementLoadState.Loading)
                repository.observeBatchMovementsWithBalance(
                    batchId = batchId,
                    start = dateTimePeriod.start,
                    end = dateTimePeriod.end
                )
                    .map { result ->
                        result.fold(
                            onSuccess = { it.toLoadState() },
                            onFailure = { BatchMovementLoadState.Error(it.message ?: "") }
                        )
                    }
                    .collect { emit(it) }
            }
            .stateIn(
                coroutineScope,
                SharingStarted.Lazily,
                BatchMovementLoadState.Loading
            )

    fun onRowClick(movement: BatchMovementTableUi) {
        tabOpener.openTransactionTab(movement.transactionId)
    }
}

internal sealed interface BatchMovementLoadState {
    data object Loading : BatchMovementLoadState
    data class Error(val message: String) : BatchMovementLoadState
    data class Success(val info: BatchMovementInfo) : BatchMovementLoadState
}


private fun BatchMovementWithBalanceInfoBD.toLoadState(): BatchMovementLoadState {
    val batchName = "(${this.batchId}) ${this.movements.firstOrNull()?.movementDate?.format(dateTimeFormat) ?: ""}"
    return BatchMovementLoadState.Success(
        BatchMovementInfo(
            productName = this.productName,
            batchName = batchName,
            movements = this.movements.map { movement ->
                BatchMovementTableUi(
                    movementDate = movement.movementDate,
                    balanceBeforeStart = movement.balanceBeforeStart,
                    incoming = movement.incoming,
                    outgoing = movement.outgoing,
                    balanceOnEnd = movement.balanceOnEnd,
                    transactionId = movement.transactionId
                )
            }
        )
    )
}
