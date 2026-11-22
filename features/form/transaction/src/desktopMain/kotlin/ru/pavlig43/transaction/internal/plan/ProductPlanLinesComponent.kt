package ru.pavlig43.transaction.internal.plan

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.core.model.CollectionObject
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.loadinitdata.api.component.LoadInitDataState
import ru.pavlig43.mutable.api.multiLine.component.MutableTableComponent
import ru.pavlig43.mutable.api.multiLine.component.MutableUiEvent
import ru.pavlig43.mutable.api.multiLine.data.UpdateCollectionRepository
import ru.pavlig43.tablecore.model.IMultiLineTableUi
import ru.pavlig43.tablecore.utils.FilterMatcher
import ru.pavlig43.tablecore.utils.SortMatcher
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.state.SortState

internal data class PlanLineRow(override val composeId: Int, val line: PlanLine) : IMultiLineTableUi, CollectionObject {
    override val id: Int get() = composeId
}

internal data class PlanValidationError(val message: String, val rowKey: String? = null)

internal class ProductPlanLinesRepository(
    private val repository: ProductPlanRepository,
    initialPlanId: Int?,
) : UpdateCollectionRepository<PlanLineRow, PlanLineRow> {
    var savedPlanId: Int? = initialPlanId
        private set
    private val _savedDraft = MutableStateFlow<PlanDraft?>(null)
    val savedDraft = _savedDraft.asStateFlow()
    val products = repository.products

    override suspend fun getInit(id: Int): Result<List<PlanLineRow>> = runCatching {
        val draft = savedPlanId?.let { repository.load(it) } ?: PlanDraft()
        _savedDraft.value = draft
        draft.lines.mapIndexed(::PlanLineRow)
    }

    override suspend fun update(changeSet: ChangeSet<List<PlanLineRow>>): Result<Unit> = runCatching {
        val rows = changeSet.new
        val draft = checkNotNull(_savedDraft.value).copy(lines = rows.map(PlanLineRow::line))
        savedPlanId = repository.save(savedPlanId, draft) { position -> rows[position].composeId + 1 }
        _savedDraft.value = draft
    }
}

internal class ProductPlanLinesComponent(
    componentContext: ComponentContext,
    planId: Int,
    private val repository: ProductPlanLinesRepository,
    isBusy: () -> Boolean,
    private val onDraftChanged: () -> Unit,
    onPickProduct: (String) -> Unit,
    onOpenProduct: (Int) -> Unit,
) : MutableTableComponent<PlanLineRow, PlanLineRow, PlanLineRow, PlanLineField>(
    componentContext = componentContext,
    parentId = planId,
    title = "Строки плана",
    sortMatcher = PlanLineSorter,
    filterMatcher = PlanLineFilterMatcher,
    repository = repository,
) {
    private val canEdit = { !isBusy() && initDataComponent.loadState.value is LoadInitDataState.Success }
    override val columns = productPlanLineColumns(::onEvent, onPickProduct, onOpenProduct)
    val createdAt = repository.savedDraft.map { it?.createdAt }
    val dirty = combine(itemList, repository.savedDraft, ::hasChanges)
    val validationErrors = combine(itemList, repository.products, initDataComponent.loadState) { rows, products, loadState ->
        when {
            loadState !is LoadInitDataState.Success -> emptyList()
            rows.isEmpty() -> listOf(PlanValidationError("Добавьте хотя бы одну строку"))
            else -> {
                val productsById = products.associateBy(Product::id)
                rows.mapNotNull { row ->
                    row.line.validationError(productsById[row.line.productId])?.let { message ->
                        PlanValidationError(planLineErrorMessage(row.composeId + 1, message), row.line.syncId)
                    }
                }
            }
        }
    }
    override val errorMessages = validationErrors.map { errors -> errors.map(PlanValidationError::message) }

    fun hasChanges(): Boolean = hasChanges(itemList.value, repository.savedDraft.value)

    private fun hasChanges(rows: List<PlanLineRow>, draft: PlanDraft?): Boolean =
        repository.savedPlanId == null || rows.map(PlanLineRow::line) != draft?.lines

    override fun onEvent(event: MutableUiEvent) {
        if (!canEdit()) return
        super.onEvent(event)
        if (event !is MutableUiEvent.Selection) onDraftChanged()
    }

    fun selectProduct(key: String, productId: Int, productName: String) {
        val row = itemList.value.firstOrNull { it.line.syncId == key } ?: return
        onEvent(MutableUiEvent.UpdateItem(row.copy(line = row.line.copy(productId = productId, productName = productName))))
    }

    suspend fun save(): Int {
        onUpdate().getOrThrow()
        return checkNotNull(repository.savedPlanId)
    }

    override fun createNewItem(composeId: Int): PlanLineRow = PlanLineRow(composeId, PlanLine())
    override fun PlanLineRow.toUi(composeId: Int): PlanLineRow = copy(composeId = composeId)
    override fun PlanLineRow.toBDIn(): PlanLineRow = this
}

private object PlanLineSorter : SortMatcher<PlanLineRow, PlanLineField> {
    override fun sort(items: List<PlanLineRow>, sort: SortState<PlanLineField>?): List<PlanLineRow> = items
}

private object PlanLineFilterMatcher : FilterMatcher<PlanLineRow, PlanLineField>() {
    override fun matchesRules(item: PlanLineRow, column: PlanLineField, stateAny: TableFilterState<*>): Boolean = true
}
