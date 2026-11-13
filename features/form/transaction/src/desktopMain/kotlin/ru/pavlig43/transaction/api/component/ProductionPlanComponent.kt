package ru.pavlig43.transaction.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.pavlig43.core.MainTabComponent
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.tabs.TabOpener
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.immutable.api.component.MBSImmutableTableComponent
import ru.pavlig43.immutable.api.component.ProductImmutableTableBuilder
import ru.pavlig43.immutable.internal.component.items.product.ProductTableUi
import ru.pavlig43.transaction.api.TransactionFormDependencies
import ru.pavlig43.transaction.internal.production.PlanDraft
import ru.pavlig43.transaction.internal.production.PlanLine
import ru.pavlig43.transaction.internal.production.ProductionPlanRepository
import ru.pavlig43.transaction.internal.production.openProductionPdf
import ru.pavlig43.transaction.internal.production.readProductionLayout

internal data class PlanEditorState(
    val id: Int?,
    val draft: PlanDraft = PlanDraft(),
    val busy: Boolean = id != null,
    val loaded: Boolean = id == null,
    val dirty: Boolean = id == null,
    val error: String? = null,
)

class ProductionPlanComponent(
    componentContext: ComponentContext,
    id: Int,
    dependencies: TransactionFormDependencies,
    tabOpener: TabOpener,
    private val onOpenPlan: (Int) -> Unit,
    private val onOpenMultiOpzs: (Int) -> Unit,
    private val onClose: () -> Unit,
) : ComponentContext by componentContext, MainTabComponent {
    private val db = dependencies.db
    private val repository = ProductionPlanRepository(db)
    private val coroutineScope = componentCoroutineScope()
    private val _model = MutableStateFlow(MainTabComponent.NavTabState("Новый план производства"))
    override val model = _model.asStateFlow()
    private val _state = MutableStateFlow(PlanEditorState(id.takeIf { it != 0 }))
    internal val state = _state.asStateFlow()
    private val navigation = SlotNavigation<String>()
    internal val productPicker = childSlot(
        source = navigation,
        serializer = null,
        handleBackButton = true,
    ) { key, context ->
        MBSImmutableTableComponent<ProductTableUi>(
            componentContext = context,
            onDismissed = navigation::dismiss,
            dependencies = dependencies.immutableTableDependencies,
            tabOpener = tabOpener,
            immutableTableBuilderData = ProductImmutableTableBuilder(listOf(ProductType.FOOD_PF), false),
            onItemClick = { product ->
                changeLine(key) { it.copy(productId = product.composeId, productName = product.displayName) }
                navigation.dismiss()
            },
        )
    }

    fun start() {
        val id = _state.value.id ?: return
        runAction {
            val draft = repository.load(id)
            _state.update { it.copy(draft = draft, loaded = true, dirty = false) }
            _model.value = MainTabComponent.NavTabState(draft.title)
        }
    }

    internal fun changeTitle(title: String) = edit { it.copy(title = title) }
    internal fun addLine() = edit { it.copy(lines = it.lines + PlanLine()) }
    internal fun removeLine(key: String) = edit { it.copy(lines = it.lines.filterNot { line -> line.syncId == key }) }
    internal fun changePlanned(key: String, value: String) = changeLine(key) { it.copy(planned = value) }
    internal fun changePackage(key: String, value: String) = changeLine(key) { it.copy(packageWeight = value) }

    internal fun pickProduct(key: String) {
        if (!_state.value.busy) navigation.activate(key)
    }

    internal fun save() {
        val snapshot = _state.value
        if (snapshot.busy || !snapshot.loaded) return
        runAction {
            val id = repository.save(snapshot.id, snapshot.draft)
            val draft = snapshot.draft.copy(title = snapshot.draft.title.trim())
            _state.update { it.copy(id = id, draft = draft, dirty = false) }
            _model.value = MainTabComponent.NavTabState(draft.title)
            if (snapshot.id == null) {
                onOpenPlan(id)
                onClose()
            }
        }
    }

    internal fun print() = savedAction { id ->
        val layout = db.readProductionLayout(id)
        withContext(Dispatchers.IO) { openProductionPdf(layout) }
    }

    internal fun openMultiOpzs() = savedAction { id -> onOpenMultiOpzs(id) }
    internal fun copy() = savedAction { id -> onOpenPlan(repository.copy(id)) }

    internal fun delete() {
        val id = _state.value.id ?: return
        if (_state.value.busy) return
        runAction {
            repository.delete(id)
            onClose()
        }
    }

    private fun changeLine(key: String, change: (PlanLine) -> PlanLine) =
        edit { it.copy(lines = it.lines.map { line -> if (line.syncId == key) change(line) else line }) }

    private fun edit(change: (PlanDraft) -> PlanDraft) {
        _state.update { state ->
            if (state.busy || !state.loaded) state else state.copy(draft = change(state.draft), dirty = true, error = null)
        }
    }

    private fun savedAction(action: suspend (Int) -> Unit) {
        val snapshot = _state.value
        val id = snapshot.id ?: return
        if (!snapshot.dirty && !snapshot.busy && snapshot.loaded) runAction { action(id) }
    }

    private fun runAction(action: suspend () -> Unit) {
        _state.update { it.copy(busy = true, error = null) }
        coroutineScope.launch {
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update { it.copy(error = error.message ?: "Не удалось выполнить операцию") }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }
}
