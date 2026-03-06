package ru.pavlig43.storage.api.component.batchMovement

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.serialization.Serializable
import ru.pavlig43.core.DateTimeComponent
import ru.pavlig43.core.MainTabComponent
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.getCurrentLocalDateTime
import ru.pavlig43.core.dateTimeFormat
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.database.data.storage.BatchMovementWithBalanceBD
import ru.pavlig43.database.data.storage.BatchMovementWithBalanceInfoBD
import ru.pavlig43.storage.api.StorageDependencies
import ru.pavlig43.storage.internal.di.StorageRepository
import ru.pavlig43.storage.internal.di.createStorageModule

class BatchMovementComponent(
    componentContext: ComponentContext,
    dependencies: StorageDependencies,
    private val batchId: Int,
    private val productName: String,
    start: LocalDateTime,
    end: LocalDateTime
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

    private val _dateTimePeriodUi = MutableStateFlow(DateTimePeriod(start, end))
    internal val dateTimePeriodUi: StateFlow<DateTimePeriod> = _dateTimePeriodUi.asStateFlow()

    private val _dateTimePeriodForData = MutableStateFlow(DateTimePeriod(start, end))
    internal val dateTimePeriodForData = _dateTimePeriodForData.asStateFlow()

    fun updateDateTimePeriod() {
        _dateTimePeriodForData.update { _dateTimePeriodUi.value }
    }

    private val dialogNavigation = SlotNavigation<BatchMovementDialog>()

    internal val dialog = childSlot(
        source = dialogNavigation,
        key = "batch_movement_dialog",
        serializer = BatchMovementDialog.serializer(),
        handleBackButton = true,
        childFactory = ::createDialogChild
    )

    private fun createDialogChild(dialogConfig: BatchMovementDialog, context: ComponentContext): DialogChild {
        val currentPeriod = dateTimePeriodUi.value
        return when (dialogConfig) {
            is BatchMovementDialog.StartDateTime -> {
                DialogChild.DateTime(
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
            is BatchMovementDialog.EndDateTime -> {
                DialogChild.DateTime(
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

    fun openStartDateTimeDialog() = dialogNavigation.activate(BatchMovementDialog.StartDateTime)
    fun openEndDateTimeDialog() = dialogNavigation.activate(BatchMovementDialog.EndDateTime)

    @OptIn(ExperimentalCoroutinesApi::class)
    internal val loadState: StateFlow<BatchMovementLoadState> =
        _dateTimePeriodForData
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

internal data class DateTimePeriod(
    val start: LocalDateTime = getCurrentLocalDateTime(),
    val end: LocalDateTime = getCurrentLocalDateTime()
)

@Serializable
internal sealed interface BatchMovementDialog {
    @Serializable
    data object StartDateTime : BatchMovementDialog

    @Serializable
    data object EndDateTime : BatchMovementDialog
}

sealed interface DialogChild {
    class DateTime(val component: DateTimeComponent) : DialogChild
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
