package ru.pavlig43.document.internal.create.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import kotlinx.collections.immutable.ImmutableList
import kotlinx.serialization.Serializable
import ru.pavlig43.core.DateComponent
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.document.internal.DocumentField
import ru.pavlig43.document.internal.model.DocumentEssentialsUi
import ru.pavlig43.document.internal.model.toDto
import ru.pavlig43.mutable.api.singleLine.component.CreateSingleLineComponent
import ru.pavlig43.mutable.api.singleLine.component.SingleLineComponentFactory
import ru.pavlig43.mutable.api.singleLine.data.CreateSingleItemRepository
import ua.wwind.table.ColumnSpec

/**
 * Компонент для создания документа через таблицу с одной строкой.
 *
 * Использует [ru.pavlig43.mutable.api.singleLine.component.CreateSingleLineComponent] как базу и добавляет:
 * - Диалог выбора даты через [com.arkivanov.decompose.router.slot.SlotNavigation]
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
    observeOnItem:(DocumentEssentialsUi)-> Unit,
    componentFactory: SingleLineComponentFactory<Document, DocumentEssentialsUi>,
    createDocumentRepository: CreateSingleItemRepository<Document>,
) : CreateSingleLineComponent<Document, DocumentEssentialsUi, DocumentField>(
    componentContext = componentContext,
    onSuccessCreate = onSuccessCreate,
    componentFactory = componentFactory,
    createSingleItemRepository = createDocumentRepository,
    mapperToDTO = DocumentEssentialsUi::toDto,
    observeOnItem = observeOnItem
) {
    // Навигация для диалогов
    private val dialogNavigation = SlotNavigation<CreateDatePickerDialogConfig>()

    // Slot для диалога выбора даты
    val dialog = childSlot(
        source = dialogNavigation,
        key = "date_picker_dialog",
        serializer = CreateDatePickerDialogConfig.serializer(),
        handleBackButton = true,
        childFactory = { _, context ->
            createDatePickerDialog(context)
        }
    )

    override val columns: ImmutableList<ColumnSpec<DocumentEssentialsUi, DocumentField, Unit>> =
        createDocumentColumns0(
            onOpenDateDialog = {
                dialogNavigation.activate(CreateDatePickerDialogConfig)
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

    /**
     * Конфигурация для диалога выбора даты
     */
    @Serializable
    data object CreateDatePickerDialogConfig
}