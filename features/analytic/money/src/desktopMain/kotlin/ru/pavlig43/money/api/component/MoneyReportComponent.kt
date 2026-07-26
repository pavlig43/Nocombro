package ru.pavlig43.money.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import ru.pavlig43.core.MainTabComponent
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.expense.ExpenseType
import ru.pavlig43.database.data.money.MoneyMovementCategory
import ru.pavlig43.database.data.money.RecordedExpenseMoneyRow
import ru.pavlig43.database.data.money.RecordedTransactionMoneyLine
import ru.pavlig43.datetime.period.dateTime.DTPeriod
import ru.pavlig43.datetime.period.dateTime.DateTimePeriodComponent
import ru.pavlig43.money.api.MoneyReportDependencies
import ru.pavlig43.money.internal.model.MoneyReportCalculator
import ru.pavlig43.money.internal.model.MoneyReportData
import ru.pavlig43.money.internal.model.MoneyReportEntry
import ru.pavlig43.money.internal.model.MoneyReportEntryKind

class MoneyReportComponent(
    componentContext: ComponentContext,
    dependencies: MoneyReportDependencies,
) : ComponentContext by componentContext, MainTabComponent {
    private val coroutineScope = componentCoroutineScope()
    private val repository = MoneyReportRepository(dependencies.db)

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
            repository.observe(period).collect { emit(it) }
        }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(5_000), MoneyReportLoadState.Loading)
}

internal class MoneyReportRepository(db: NocombroDatabase) {
    private val dao = db.moneyDao

    fun observe(period: DTPeriod): Flow<MoneyReportLoadState> = combine(
        dao.observeRecordedSalesUntil(period.end),
        dao.observeRecordedPurchasesUntil(period.end),
        dao.observeRecordedExpensesUntil(period.end),
    ) { sales, purchases, expenses ->
        runCatching {
            val saleEntries = sales.toEntries(RecordedDocumentType.SALE)
            val purchaseEntries = purchases.toEntries(RecordedDocumentType.PURCHASE)
            val documentByGroupKey = (saleEntries + purchaseEntries)
                .associateBy(MoneyReportEntry::operationGroupKey)
            val entries = buildList {
                addAll(saleEntries)
                addAll(purchaseEntries)
                addAll(
                    expenses.map { expense ->
                        expense.toEntry(
                            linkedDocument = expense.transactionSyncId
                                ?.let { documentByGroupKey["transaction:$it"] },
                        )
                    },
                )
            }
            MoneyReportCalculator.calculate(entries, period)
        }.fold(
            onSuccess = MoneyReportLoadState::Success,
            onFailure = { MoneyReportLoadState.Error(it.userMessage()) },
        )
    }
}

internal sealed interface MoneyReportLoadState {
    data object Loading : MoneyReportLoadState
    data class Error(val message: String) : MoneyReportLoadState
    data class Success(val data: MoneyReportData) : MoneyReportLoadState
}

private enum class RecordedDocumentType {
    SALE,
    PURCHASE,
}

private fun List<RecordedTransactionMoneyLine>.toEntries(
    type: RecordedDocumentType,
): List<MoneyReportEntry> = groupBy(RecordedTransactionMoneyLine::documentSyncId)
    .values
    .map { lines ->
        val first = lines.first()
        val isSale = type == RecordedDocumentType.SALE
        val sourceLabel = (if (isSale) "Продажа №" else "Закупка №") + first.documentId
        MoneyReportEntry(
            sourceKey = (if (isSale) "sale:" else "purchase:") + first.documentSyncId,
            sourceLabel = sourceLabel,
            kind = if (isSale) MoneyReportEntryKind.INCOME else MoneyReportEntryKind.EXPENSE,
            category = if (isSale) {
                MoneyMovementCategory.SALE_PAYMENT
            } else {
                MoneyMovementCategory.PURCHASE_PAYMENT
            },
            amount = lines.sumOf { line -> line.count * line.price / 1000 },
            occurredAt = first.occurredAt,
            counterparty = lines.map(RecordedTransactionMoneyLine::counterparty)
                .filter(String::isNotBlank)
                .distinct()
                .joinToString(", "),
            comment = joinComments(
                first.documentComment,
                lines.map(RecordedTransactionMoneyLine::lineComment)
                    .filter(String::isNotBlank)
                    .distinct()
                    .joinToString(", "),
            ),
            operationGroupKey = "transaction:${first.documentSyncId}",
            operationGroupLabel = sourceLabel,
        )
    }

private fun RecordedExpenseMoneyRow.toEntry(
    linkedDocument: MoneyReportEntry?,
): MoneyReportEntry = MoneyReportEntry(
    sourceKey = "expense:$sourceSyncId",
    sourceLabel = "Трата №$sourceId",
    kind = MoneyReportEntryKind.EXPENSE,
    category = if (expenseType == ExpenseType.DIVIDENDS) {
        MoneyMovementCategory.DIVIDEND
    } else {
        MoneyMovementCategory.BUSINESS_EXPENSE
    },
    amount = amount,
    occurredAt = occurredAt,
    counterparty = "",
    comment = joinComments(expenseType.displayName, comment),
    operationGroupKey = transactionSyncId?.let { "transaction:$it" },
    operationGroupLabel = linkedDocument?.sourceLabel
        ?: transactionId?.let { "Операция №$it" },
)

private fun joinComments(vararg values: String): String = values
    .map(String::trim)
    .filter(String::isNotEmpty)
    .distinct()
    .joinToString(" · ")

private fun Throwable.userMessage(): String = message?.takeIf(String::isNotBlank)
    ?: "Не удалось посчитать деньги"
