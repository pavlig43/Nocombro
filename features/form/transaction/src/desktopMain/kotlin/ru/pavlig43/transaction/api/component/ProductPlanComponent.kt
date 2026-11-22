package ru.pavlig43.transaction.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.format
import ru.pavlig43.core.MainTabComponent
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.tabs.TabOpener
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.datetime.dateTimeWithSecondsFormat
import ru.pavlig43.immutable.api.component.MBSImmutableTableComponent
import ru.pavlig43.immutable.api.component.ProductImmutableTableBuilder
import ru.pavlig43.immutable.internal.component.items.product.ProductTableUi
import ru.pavlig43.loadinitdata.api.component.LoadInitDataState
import ru.pavlig43.transaction.api.TransactionFormDependencies
import ru.pavlig43.transaction.internal.plan.ProductPlanLinesComponent
import ru.pavlig43.transaction.internal.plan.ProductPlanLinesRepository
import ru.pavlig43.transaction.internal.plan.ProductPlanRepository
import ru.pavlig43.transaction.internal.plan.openProductPlanPdf
import ru.pavlig43.transaction.internal.plan.readProductPlanLayout

internal data class PlanEditorState(
    val id: Int?,
    val busy: Boolean = false,
    val error: String? = null,
)

class ProductPlanComponent(
    componentContext: ComponentContext,
    id: Int,
    dependencies: TransactionFormDependencies,
    private val tabOpener: TabOpener,
    private val onOpenPlan: (Int) -> Unit,
    private val onOpenMultiOpzs: (Int) -> Unit,
    private val onClose: () -> Unit,
) : ComponentContext by componentContext, MainTabComponent {
    private val db = dependencies.db
    private val coroutineScope = componentCoroutineScope()
    private val _state = MutableStateFlow(PlanEditorState(id.takeIf { it != 0 }))
    internal val state = _state.asStateFlow()
    private val navigation = SlotNavigation<String>()
    internal val rows = ProductPlanLinesComponent(
        componentContext = componentContext.childContext("rows"),
        planId = id,
        repository = ProductPlanLinesRepository(ProductPlanRepository(db), id.takeIf { it != 0 }),
        isBusy = { _state.value.busy },
        onDraftChanged = { _state.update { it.copy(error = null) } },
        onPickProduct = ::pickProduct,
        onOpenProduct = ::openSelectedProduct,
    )
    override val model = rows.createdAt.map { createdAt ->
        MainTabComponent.NavTabState(createdAt?.format(dateTimeWithSecondsFormat) ?: "Новый план производства")
    }.stateIn(coroutineScope, SharingStarted.Eagerly, MainTabComponent.NavTabState("Новый план производства"))

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
                rows.selectProduct(key, product.composeId, product.displayName)
                navigation.dismiss()
            },
        )
    }

    private fun pickProduct(key: String) {
        if (!_state.value.busy) navigation.activate(key)
    }

    internal fun openSelectedProduct(productId: Int) {
        if (productId != 0) tabOpener.openProductTab(productId)
    }

    internal fun save() {
        val snapshot = _state.value
        if (snapshot.busy || !rowsLoaded() || !rows.hasChanges()) return
        runAction {
            val id = rows.save()
            _state.update { it.copy(id = id) }
            if (snapshot.id == null) {
                onOpenPlan(id)
                onClose()
            }
        }
    }

    internal fun print() = savedAction { id ->
        val layout = db.readProductPlanLayout(id)
        withContext(Dispatchers.IO) { openProductPlanPdf(layout) }
    }

    internal fun openMultiOpzs() = savedAction { id -> onOpenMultiOpzs(id) }

    private fun rowsLoaded(): Boolean = rows.initDataComponent.loadState.value is LoadInitDataState.Success

    private fun savedAction(action: suspend (Int) -> Unit) {
        val snapshot = _state.value
        val id = snapshot.id ?: return
        if (!snapshot.busy && rowsLoaded() && !rows.hasChanges()) runAction { action(id) }
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
