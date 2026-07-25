package ru.pavlig43.money.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.MainTabComponent
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.database.data.money.DEFAULT_MONEY_ACCOUNT_SYNC_ID
import ru.pavlig43.database.data.money.MoneyAccount
import ru.pavlig43.database.data.money.MoneyAccountType
import ru.pavlig43.database.data.money.MoneyMovement
import ru.pavlig43.database.data.money.MoneyMovementCategory
import ru.pavlig43.database.data.money.MoneyMovementKind
import ru.pavlig43.database.data.sync.defaultUpdatedAt
import ru.pavlig43.database.data.sync.mirror.MirrorDeletionJournalRepository
import ru.pavlig43.datetime.period.dateTime.DTPeriod
import ru.pavlig43.datetime.period.dateTime.DateTimePeriodComponent
import ru.pavlig43.money.api.MoneyReportDependencies
import ru.pavlig43.money.internal.model.MoneyReportCalculator
import ru.pavlig43.money.internal.model.MoneyReportData

class MoneyReportComponent(
    componentContext: ComponentContext,
    dependencies: MoneyReportDependencies,
) : ComponentContext by componentContext, MainTabComponent {
    private val db = dependencies.db
    private val dao = db.moneyDao
    private val deletionJournal = MirrorDeletionJournalRepository(db)
    private val coroutineScope = componentCoroutineScope()
    private val defaultAccountMutex = Mutex()

    private val _model = MutableStateFlow(MainTabComponent.NavTabState("Деньги"))
    override val model: StateFlow<MainTabComponent.NavTabState> = _model.asStateFlow()

    val dateTimePeriodComponent = DateTimePeriodComponent(
        componentContext = childContext("money_period"),
        initDTPeriod = DTPeriod.thisMonth,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    internal val loadState: StateFlow<MoneyReportLoadState> = dateTimePeriodComponent
        .dateTimePeriodForData
        .transformLatest { period ->
            emit(MoneyReportLoadState.Loading)
            reportFlow(period).collect { emit(it) }
        }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(5_000), MoneyReportLoadState.Loading)

    private val _editor = MutableStateFlow<MoneyEditor?>(null)
    internal val editor: StateFlow<MoneyEditor?> = _editor.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    internal val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private val _actionInProgress = MutableStateFlow(false)
    internal val actionInProgress: StateFlow<Boolean> = _actionInProgress.asStateFlow()

    init {
        coroutineScope.launch {
            runCatching { ensureDefaultAccount() }
                .onFailure { _actionError.value = it.userMessage() }
        }
    }

    internal fun openMovement(
        kind: MoneyMovementKind,
        movement: MoneyMovement? = null,
    ) {
        require(kind == MoneyMovementKind.INCOME || kind == MoneyMovementKind.EXPENSE) {
            "Отчёт поддерживает только приходы и выплаты"
        }
        _editor.value = MoneyEditor.Movement(kind, movement)
    }

    internal fun confirmDelete(movement: MoneyMovement) {
        _editor.value = MoneyEditor.ConfirmDelete(movement)
    }

    internal fun dismissEditor() {
        _editor.value = null
    }

    internal fun clearActionError() {
        _actionError.value = null
    }

    internal fun saveMovement(input: MoneyMovementInput) = runAction {
        require(input.amount > 0) { "Сумма должна быть больше нуля" }
        require(input.kind == MoneyMovementKind.INCOME || input.kind == MoneyMovementKind.EXPENSE) {
            "Можно сохранить только приход или выплату"
        }

        val account = ensureDefaultAccount()
        val existing = input.existing
        val fromAccountId = if (input.kind == MoneyMovementKind.EXPENSE) {
            existing?.fromAccountId ?: account.id
        } else {
            null
        }
        val toAccountId = if (input.kind == MoneyMovementKind.INCOME) {
            existing?.toAccountId ?: account.id
        } else {
            null
        }
        val movement = existing?.copy(
            kind = input.kind,
            category = input.category,
            amount = input.amount,
            occurredAt = input.occurredAt,
            fromAccountId = fromAccountId,
            toAccountId = toAccountId,
            counterparty = input.counterparty.trim(),
            comment = input.comment.trim(),
            updatedAt = defaultUpdatedAt(existing.updatedAt),
        ) ?: MoneyMovement(
            kind = input.kind,
            category = input.category,
            amount = input.amount,
            occurredAt = input.occurredAt,
            fromAccountId = fromAccountId,
            toAccountId = toAccountId,
            counterparty = input.counterparty.trim(),
            comment = input.comment.trim(),
        )

        if (existing == null) dao.createMovement(movement) else dao.updateMovement(movement)
    }

    internal fun deleteMovement(movement: MoneyMovement) = runAction {
        deletionJournal.captureHardDeletes {
            dao.deleteMovementById(movement.id)
        }
    }

    private fun reportFlow(period: DTPeriod): Flow<MoneyReportLoadState> =
        dao.observeMovementsUntil(period.end).map { movements ->
            runCatching {
                MoneyReportCalculator.calculate(movements, period)
            }.fold(
                onSuccess = MoneyReportLoadState::Success,
                onFailure = { MoneyReportLoadState.Error(it.userMessage()) },
            )
        }

    private suspend fun ensureDefaultAccount(): MoneyAccount = defaultAccountMutex.withLock {
        val existing = dao.getAccountBySyncId(DEFAULT_MONEY_ACCOUNT_SYNC_ID)
            ?: dao.getFirstActiveAccount()
        if (existing != null) {
            val normalized = existing.copy(
                openedAt = minOf(existing.openedAt, DEFAULT_ACCOUNT_OPENED_AT),
                isArchived = false,
                deletedAt = null,
            )
            if (normalized != existing) {
                dao.updateAccount(
                    normalized.copy(updatedAt = defaultUpdatedAt(existing.updatedAt))
                )
                return@withLock requireNotNull(dao.getAccount(existing.id))
            }
            return@withLock existing
        }

        val accountId = dao.createAccount(
            MoneyAccount(
                name = "Счёт фирмы",
                accountType = MoneyAccountType.BANK,
                openedAt = DEFAULT_ACCOUNT_OPENED_AT,
                syncId = DEFAULT_MONEY_ACCOUNT_SYNC_ID,
            )
        ).toInt()
        requireNotNull(dao.getAccount(accountId))
    }

    private fun runAction(block: suspend () -> Unit) {
        if (!_actionInProgress.compareAndSet(expect = false, update = true)) return
        coroutineScope.launch {
            try {
                runCatching { block() }
                    .onSuccess {
                        _actionError.value = null
                        _editor.value = null
                    }
                    .onFailure { _actionError.value = it.userMessage() }
            } finally {
                _actionInProgress.value = false
            }
        }
    }
}

internal sealed interface MoneyReportLoadState {
    data object Loading : MoneyReportLoadState
    data class Error(val message: String) : MoneyReportLoadState
    data class Success(val data: MoneyReportData) : MoneyReportLoadState
}

internal sealed interface MoneyEditor {
    data class Movement(
        val kind: MoneyMovementKind,
        val movement: MoneyMovement?,
    ) : MoneyEditor
    data class ConfirmDelete(val movement: MoneyMovement) : MoneyEditor
}

internal data class MoneyMovementInput(
    val existing: MoneyMovement?,
    val kind: MoneyMovementKind,
    val category: MoneyMovementCategory,
    val amount: Long,
    val occurredAt: LocalDateTime,
    val counterparty: String,
    val comment: String,
)

private val DEFAULT_ACCOUNT_OPENED_AT = LocalDateTime(2000, 1, 1, 0, 0)

private fun Throwable.userMessage(): String = message?.takeIf(String::isNotBlank)
    ?: "Не удалось сохранить данные"
