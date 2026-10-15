package ru.pavlig43.transaction.internal.multiopzs

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.model.DecimalData3
import ru.pavlig43.core.tabs.TabOpener
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.datetime.single.datetime.DateTimeComponent
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.immutable.api.component.MBSImmutableTableComponent
import ru.pavlig43.immutable.api.component.ProductDeclarationImmutableTableBuilder
import ru.pavlig43.immutable.api.component.ProductImmutableTableBuilder
import ru.pavlig43.immutable.internal.component.items.product.ProductTableUi
import ru.pavlig43.immutable.internal.component.items.productDeclaration.ProductDeclarationTableUi
import ru.pavlig43.mutable.api.multiLine.component.MutableUiEvent
import ru.pavlig43.tablecore.manger.SelectionUiEvent

internal class MultiOpzsComponent(
    componentContext: ComponentContext,
    private val service: CreateOpzsService,
    private val tableDependencies: ImmutableTableDependencies,
    private val tabOpener: TabOpener,
    private val onBack: () -> Unit,
) : ComponentContext by componentContext {
    private val coroutineScope = componentCoroutineScope()
    private val _state = MutableStateFlow(MultiOpzsState())
    val state = _state.asStateFlow()
    private val navigation = SlotNavigation<MultiOpzsDialog>()
    val dialog = childSlot(
        source = navigation,
        serializer = MultiOpzsDialog.serializer(),
        handleBackButton = true,
        childFactory = ::createDialog,
    )

    fun addRow() {
        if (!_state.value.running) _state.update {
            val id = (it.rows.maxOfOrNull(MultiOpzsRow::composeId) ?: -1) + 1
            it.copy(rows = it.rows + MultiOpzsRow(composeId = id))
        }
    }

    fun removeRow(key: String) {
        if (!canEdit(key)) return
        _state.update { state ->
            val rows = state.rows.filterNot { it.syncId == key }
            state.copy(rows = rows, selectedIds = state.selectedIds.intersect(rows.map { it.composeId }.toSet()))
        }
    }

    fun onTableEvent(event: MutableUiEvent) {
        if (_state.value.running) return
        when (event) {
            MutableUiEvent.CreateNewItem -> addRow()
            MutableUiEvent.DeleteSelected -> _state.update { state ->
                state.copy(
                    rows = state.rows.filterNot { it.composeId in state.selectedIds && it.result !is OpzsRowResult.Created },
                    selectedIds = emptySet(),
                )
            }
            is MutableUiEvent.Selection -> _state.update { state ->
                val editable = state.rows.filterNot { it.result is OpzsRowResult.Created }.map { it.composeId }.toSet()
                val selected = when (val selection = event.selectionUiEvent) {
                    SelectionUiEvent.ClearSelection -> emptySet()
                    is SelectionUiEvent.ToggleSelection -> if (selection.id in state.selectedIds) {
                        state.selectedIds - selection.id
                    } else state.selectedIds + selection.id
                    is SelectionUiEvent.ToggleSelectAll -> {
                        val ids = selection.displayedIds.intersect(editable)
                        if (state.selectedIds.containsAll(ids)) state.selectedIds - ids else state.selectedIds + ids
                    }
                }
                state.copy(selectedIds = selected.intersect(editable))
            }
            is MutableUiEvent.UpdateItem -> Unit
        }
    }

    fun changeCount(key: String, count: DecimalData3) = edit(key) { it.copy(count = count) }

    fun changeDate(value: LocalDateTime) {
        if (!_state.value.running) _state.update { it.copy(producedAt = value) }
    }

    fun back() {
        if (!_state.value.running) onBack()
    }

    fun selectProduct(key: String, id: Int, name: String) {
        if (!canEdit(key) || row(key)?.productId == id) return
        edit(key) { it.copy(
            productId = id, productName = name, declarationId = 0,
            declarationName = "", vendorName = "", loading = true,
        ) }
        coroutineScope.launch {
            try {
                val declarations = service.declarations(id)
                if (row(key)?.productId == id) {
                    edit(key) { current ->
                        declarations.singleOrNull()?.let { current.withDeclaration(it) }
                            ?.copy(loading = false) ?: current.copy(loading = false)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (row(key)?.productId == id) updateRow(key) {
                    it.copy(loading = false, result = OpzsRowResult.Failed(error.message ?: "Ошибка чтения деклараций"))
                }
            }
        }
    }

    fun selectDeclaration(key: String, declarationId: Int, name: String, vendor: String) = edit(key) {
        it.copy(declarationId = declarationId, declarationName = name, vendorName = vendor)
    }

    fun openProduct(key: String) {
        if (canEdit(key)) navigation.activate(MultiOpzsDialog.Product(key))
    }

    fun openDeclaration(key: String) {
        if (canEdit(key) && row(key)?.productId != 0) navigation.activate(MultiOpzsDialog.Declaration(key))
    }

    fun openDate() {
        if (!_state.value.running) navigation.activate(MultiOpzsDialog.Date)
    }

    /** Проверяет весь список до первой записи и затем ждёт сохранения каждой строки. */
    fun create() {
        val snapshot = _state.value
        if (snapshot.running || snapshot.rows.any { it.loading }) return
        val pending = snapshot.rows.filterNot { it.result is OpzsRowResult.Created }
        if (pending.isEmpty()) return
        navigation.dismiss()
        _state.update { it.copy(running = true, finished = false, selectedIds = emptySet(), rows = it.rows.map { row ->
            if (row.result is OpzsRowResult.Created) row else row.copy(result = OpzsRowResult.Waiting)
        }) }
        coroutineScope.launch {
            try {
                var valid = true
                for (row in pending) {
                    try {
                        service.validate(row)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        valid = false
                        updateRow(row.syncId) { it.copy(result = OpzsRowResult.Failed(error.message ?: "Ошибка проверки")) }
                    }
                }
                if (!valid) return@launch
                for (row in pending) {
                    updateRow(row.syncId) { it.copy(result = OpzsRowResult.Creating) }
                    try {
                        val id = service.create(row, snapshot.producedAt)
                        updateRow(row.syncId) { it.copy(result = OpzsRowResult.Created(id)) }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        updateRow(row.syncId) { it.copy(result = OpzsRowResult.Failed(error.message ?: "Ошибка создания ОПЗС")) }
                        break
                    }
                }
            } finally {
                _state.update { it.copy(running = false, finished = true, rows = it.rows.map { row ->
                    if (row.result == OpzsRowResult.Creating) row.copy(result = OpzsRowResult.Waiting) else row
                }) }
            }
        }
    }

    private fun row(key: String) = _state.value.rows.find { it.syncId == key }
    private fun canEdit(key: String) = !_state.value.running && row(key)?.result !is OpzsRowResult.Created
    private fun updateRow(key: String, transform: (MultiOpzsRow) -> MultiOpzsRow) {
        _state.update { it.copy(rows = it.rows.map { row -> if (row.syncId == key) transform(row) else row }) }
    }
    private fun edit(key: String, transform: (MultiOpzsRow) -> MultiOpzsRow) {
        if (canEdit(key)) updateRow(key) { transform(it).copy(result = OpzsRowResult.Empty) }
    }

    private fun createDialog(config: MultiOpzsDialog, context: ComponentContext): MultiOpzsDialogChild = when (config) {
        MultiOpzsDialog.Date -> MultiOpzsDialogChild.DateTime(DateTimeComponent(
            componentContext = context, initDatetime = _state.value.producedAt,
            onChangeDate = ::changeDate, onDismissRequest = navigation::dismiss,
        ))
        is MultiOpzsDialog.Product -> MultiOpzsDialogChild.Table(MBSImmutableTableComponent<ProductTableUi>(
            componentContext = context, onDismissed = navigation::dismiss,
            dependencies = tableDependencies, tabOpener = tabOpener,
            immutableTableBuilderData = ProductImmutableTableBuilder(ProductType.entries, false),
            onItemClick = {
                selectProduct(config.key, it.composeId, it.displayName)
                navigation.dismiss()
            },
        ))
        is MultiOpzsDialog.Declaration -> MultiOpzsDialogChild.Table(MBSImmutableTableComponent<ProductDeclarationTableUi>(
            componentContext = context, onDismissed = navigation::dismiss,
            dependencies = tableDependencies, tabOpener = tabOpener,
            immutableTableBuilderData = ProductDeclarationImmutableTableBuilder(row(config.key)?.productId ?: 0),
            onItemClick = {
                selectDeclaration(config.key, it.declarationId, it.displayName, it.vendorName)
                navigation.dismiss()
            },
        ))
    }
}

private fun MultiOpzsRow.withDeclaration(declaration: Declaration) = copy(
    declarationId = declaration.id, declarationName = declaration.displayName, vendorName = declaration.vendorName,
)

@Serializable
internal sealed interface MultiOpzsDialog {
    @Serializable data object Date : MultiOpzsDialog
    @Serializable data class Product(val key: String) : MultiOpzsDialog
    @Serializable data class Declaration(val key: String) : MultiOpzsDialog
}

internal sealed interface MultiOpzsDialogChild {
    class DateTime(val component: DateTimeComponent) : MultiOpzsDialogChild
    class Table(val component: MBSImmutableTableComponent<*>) : MultiOpzsDialogChild
}
