package ru.pavlig43.product.internal.create.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import kotlinx.collections.immutable.ImmutableList
import kotlinx.serialization.Serializable
import ru.pavlig43.core.DateComponent
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.mutable.api.singleLine.component.CreateSingleLineComponent
import ru.pavlig43.mutable.api.singleLine.component.SingleLineComponentFactory
import ru.pavlig43.mutable.api.singleLine.data.CreateSingleItemRepository
import ru.pavlig43.product.internal.ProductField
import ru.pavlig43.product.internal.model.ProductEssentialsUi
import ru.pavlig43.product.internal.model.toDto
import ua.wwind.table.ColumnSpec

/**
 * Компонент для создания продукта через таблицу с одной строкой.
 *
 * Использует [ru.pavlig43.mutable.api.singleLine.component.CreateSingleLineComponent] как базу и добавляет:
 * - Диалог выбора даты через [com.arkivanov.decompose.router.slot.SlotNavigation]
 * - Колонки таблицы для полей продукта
 * - Валидацию обязательных полей
 *
 * @param componentContext Decompose контекст компонента
 * @param onSuccessCreate Callback при успешном создании (принимает ID нового продукта)
 * @param observeOnItem Callback для обновления заголовка вкладки
 * @param componentFactory Фабрика для создания компонентов SingleLine
 * @param createProductRepository Репозиторий для создания продукта
 */
internal class CreateProductSingleLineComponent(
    componentContext: ComponentContext,
    onSuccessCreate: (Int) -> Unit,
    observeOnItem: (ProductEssentialsUi) -> Unit,
    componentFactory: SingleLineComponentFactory<Product, ProductEssentialsUi>,
    createProductRepository: CreateSingleItemRepository<Product>,
) : CreateSingleLineComponent<Product, ProductEssentialsUi, ProductField>(
    componentContext = componentContext,
    onSuccessCreate = onSuccessCreate,
    componentFactory = componentFactory,
    createSingleItemRepository = createProductRepository,
    mapperToDTO = ProductEssentialsUi::toDto,
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

    override val columns: ImmutableList<ColumnSpec<ProductEssentialsUi, ProductField, Unit>> =
        createProductColumns0(
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
