package ru.pavlig43.document.internal.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import kotlinx.collections.immutable.ImmutableList
import kotlinx.serialization.Serializable
import ru.pavlig43.core.DateComponent
import ru.pavlig43.core.model.GenericItem
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.document.internal.data.DocumentEssentialsUi
import ru.pavlig43.document.internal.data.toDto
import ru.pavlig43.document.internal.data.toUi
import ru.pavlig43.mutable.api.component.singleLine.CreateSingleLineComponent
import ru.pavlig43.mutable.api.component.singleLine.SingleLineComponentFactory
import ua.wwind.table.ColumnSpec

/**
 * Компонент для создания документа через таблицу с одной строкой.
 *
 * Использует [CreateSingleLineComponent] как базу и добавляет:
 * - Диалог выбора даты через [SlotNavigation]
 * - Колонки таблицы для полей документа
 * - Валидацию обязательных полей
 *
 * @param componentContext Decompose контекст компонента
 * @param onSuccessCreate Callback при успешном создании (принимает ID нового документа)
 * @param createDocumentRepository Репозиторий для создания документа
 */
internal class CreateDocumentSingleLineComponent(
    componentContext: ComponentContext,
    onSuccessCreate: (Int) -> Unit,
    createDocumentRepository: ru.pavlig43.create.data.CreateSingleItemRepository<Document>,
) : CreateSingleLineComponent<Document, DocumentEssentialsUi, DocumentField>(
    componentContext = componentContext,
    onSuccessCreate = onSuccessCreate,
    componentFactory = SingleLineComponentFactory(
        initItem = DocumentEssentialsUi(),
        isValidFieldsFactory = {
            displayName.isNotBlank() && type != null
        },
        mapperToUi = Document::toUi,
        produceInfoForTabName = { /* Не используется для создания */ }
    ),
    createSingleItemRepository = createDocumentRepository,
    mapperToDTO = DocumentEssentialsUi::toDto,
) {
    // Навигация для диалогов
    private val dialogNavigation = SlotNavigation<DatePickerDialogConfig>()

    // Slot для диалога выбора даты
    val dialog = childSlot(
        source = dialogNavigation,
        key = "date_picker_dialog",
        serializer = DatePickerDialogConfig.serializer(),
        handleBackButton = true,
        childFactory = { config, context ->
            createDatePickerDialog(config, context)
        }
    )

    override val columns: ImmutableList<ColumnSpec<DocumentEssentialsUi, DocumentField, Unit>> =
        createDocumentColumns(
            onOpenDateDialog = { composeId ->
                dialogNavigation.activate(DatePickerDialogConfig(composeId))
            },
            onChangeItem = { item -> onChangeItem(item) }
        )

    /**
     * Создаёт компонент диалога выбора даты
     */
    private fun createDatePickerDialog(
        config: DatePickerDialogConfig,
        context: ComponentContext
    ): DateComponent {
        val currentItem = itemFields.value.firstOrNull { it.composeId == config.composeId }
            ?: return DateComponent(
                componentContext = context,
                initDate = ru.pavlig43.core.getCurrentLocalDate(),
                onDismissRequest = { dialogNavigation.dismiss() },
                onChangeDate = { }
            )

        return DateComponent(
            componentContext = context,
            initDate = currentItem.createdAt,
            onDismissRequest = { dialogNavigation.dismiss() },
            onChangeDate = { newDate ->
                onChangeItem(currentItem.copy(createdAt = newDate))
            }
        )
    }

    /**
     * Конфигурация для диалога выбора даты
     */
    @Serializable
    data class DatePickerDialogConfig(
        val composeId: Int
    )
}
