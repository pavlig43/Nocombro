package ru.pavlig43.product.internal.update.tabs.essential

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import ru.pavlig43.core.DateComponent
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.mutable.api.singleLine.component.SingleLineComponentFactory
import ru.pavlig43.mutable.api.singleLine.component.UpdateSingleLineComponent
import ru.pavlig43.product.internal.ProductField
import ru.pavlig43.product.internal.model.ProductEssentialsUi
import ru.pavlig43.product.internal.model.toDto
import ru.pavlig43.update.data.UpdateSingleLineRepository
import ua.wwind.table.ColumnSpec

/**
 * Компонент для редактирования продукта через таблицу с одной строкой.
 *
 * Использует [ru.pavlig43.mutable.api.singleLine.component.UpdateSingleLineComponent] как базу и добавляет:
 * - Диалог выбора даты через [com.arkivanov.decompose.router.slot.SlotNavigation]
 * - Колонки таблицы для полей продукта (createdAt read-only в режиме редактирования)
 *
 * @param componentContext Decompose контекст компонента
 * @param productId ID продукта для редактирования
 * @param updateRepository Репозиторий для обновления продукта
 * @param componentFactory Фабрика для создания компонентов SingleLine
 * @param observeOnItem Callback для обновления заголовка вкладки
 */
internal class ProductUpdateSingleLineComponent(
    componentContext: ComponentContext,
    productId: Int,
    updateRepository: UpdateSingleLineRepository<Product>,
    componentFactory: SingleLineComponentFactory<Product, ProductEssentialsUi>,
    observeOnItem: (ProductEssentialsUi) -> Unit,
) : UpdateSingleLineComponent<Product, ProductEssentialsUi, ProductField>(
    componentContext = componentContext,
    id = productId,
    updateSingleLineRepository = updateRepository,
    componentFactory = componentFactory,
    observeOnItem = observeOnItem,
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

    override val columns: ImmutableList<ColumnSpec<ProductEssentialsUi, ProductField, Unit>> =
        createProductColumns1(
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
