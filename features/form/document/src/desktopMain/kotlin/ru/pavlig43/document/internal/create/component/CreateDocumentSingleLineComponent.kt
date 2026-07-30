package ru.pavlig43.document.internal.create.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import kotlinx.serialization.Serializable
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.datetime.single.date.DateComponent
import ru.pavlig43.document.internal.model.DocumentEssentialsUi
import ru.pavlig43.document.internal.model.toDto
import ru.pavlig43.mutable.api.singleLine.component.CreateSingleLineComponent
import ru.pavlig43.mutable.api.singleLine.component.SingleLineComponentFactory
import ru.pavlig43.mutable.api.singleLine.data.CreateSingleItemRepository

/**
 * Компонент карточной формы создания документа.
 *
 * Использует [ru.pavlig43.mutable.api.singleLine.component.CreateSingleLineComponent] как базу и добавляет:
 * - Диалог выбора даты через [com.arkivanov.decompose.router.slot.SlotNavigation]
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
) : CreateSingleLineComponent<Document, DocumentEssentialsUi>(
    componentContext = componentContext,
    onSuccessCreate = onSuccessCreate,
    componentFactory = componentFactory,
    createSingleItemRepository = createDocumentRepository,
    mapperToDTO = DocumentEssentialsUi::toDto,
    observeOnItem = observeOnItem,
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

    /** Открывает диалог выбора даты документа. */
    fun onOpenDateDialog() {
        dialogNavigation.activate(CreateDatePickerDialogConfig)
    }

    /**
     * Создаёт компонент диалога выбора даты
     */
    private fun createDatePickerDialog(
        context: ComponentContext
    ): DateComponent {
        val item = item.value

        return DateComponent(
            componentContext = context,
            initDate = item.createdAt,
            onDismissRequest = { dialogNavigation.dismiss() },
            onChangeDate = { newDate ->
                onChangeItem { it.copy(createdAt = newDate) }
            }
        )
    }

    /**
     * Конфигурация для диалога выбора даты
     */
    @Serializable
    data object CreateDatePickerDialogConfig
}