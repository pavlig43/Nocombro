package ru.pavlig43.document.internal.update.tabs.essential

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import ru.pavlig43.core.DateComponent
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.document.internal.DocumentField
import ru.pavlig43.document.internal.model.DocumentEssentialsUi
import ru.pavlig43.document.internal.model.toDto
import ru.pavlig43.mutable.api.singleLine.component.SingleLineComponentFactory
import ru.pavlig43.mutable.api.singleLine.component.UpdateSingleLineComponent
import ru.pavlig43.update.data.UpdateSingleLineRepository
import ua.wwind.table.ColumnSpec


internal class DocumentUpdateSingleLineComponent(
    componentContext: ComponentContext,
    documentId: Int,
    updateRepository: UpdateSingleLineRepository<Document>,
    componentFactory: SingleLineComponentFactory<Document, DocumentEssentialsUi>,
) : UpdateSingleLineComponent<Document, DocumentEssentialsUi, DocumentField>(
    componentContext = componentContext,
    id = documentId,
    updateSingleLineRepository = updateRepository,
    componentFactory = componentFactory,
    mapperToDTO = { toDto() }
) {
    private val dialogNavigation = SlotNavigation<UpdateDatePickerDialogConfig>()

    // Slot для диалога выбора даты
    val dialog = childSlot(
        source = dialogNavigation,
        key = "date_picker_dialog",
        serializer = UpdateDatePickerDialogConfig.serializer(),
        handleBackButton = true,
        childFactory = { _, context ->
            createDatePickerDialog(context)
        }
    )

    override val columns: ImmutableList<ColumnSpec<DocumentEssentialsUi, DocumentField, Unit>> =
        createDocumentColumns1(
            onOpenDateDialog = {
                dialogNavigation.activate(UpdateDatePickerDialogConfig)
            },
            onChangeItem = { item -> onChangeItem(item) }
        )

    /**
     * Создаёт компонент диалога выбора даты
     */
    private fun createDatePickerDialog(
        context: ComponentContext
    ): DateComponent {
        val item = itemFields.value[0]

        return DateComponent(
            componentContext = context,
            initDate = item.createdAt,
            onDismissRequest = { dialogNavigation.dismiss() },
            onChangeDate = { newDate ->
                onChangeItem(item.copy(createdAt = newDate))
            }
        )
    }
    override val errorMessages: Flow<List<String>> = errorTableMessages

    /**
     * Конфигурация для диалога выбора даты
     */
    @Serializable
    data object UpdateDatePickerDialogConfig
}





