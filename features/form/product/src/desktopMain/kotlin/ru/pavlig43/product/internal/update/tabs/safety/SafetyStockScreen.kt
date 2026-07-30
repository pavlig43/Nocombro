package ru.pavlig43.product.internal.update.tabs.safety

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineCardsScreen
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineDecimalField
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineFormField
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineFormSection
import ru.pavlig43.mutable.api.singleLine.ui.singleLineFormRow

/** Показывает запас товара как карточную форму. */
@Composable
internal fun SafetyStockScreen(
    component: SafetyStockComponent,
) {
    val sections = remember(component) {
        safetyStockSections(component)
    }

    SingleLineCardsScreen(
        component = component,
        sections = sections,
    )
}

/** Строит неизменяемую схему полей запаса товара. */
private fun safetyStockSections(
    component: SafetyStockComponent,
): ImmutableList<SingleLineFormSection<SafetyStockUi>> = persistentListOf(
    SingleLineFormSection(
        title = "Запас товара",
        rows = persistentListOf(
            singleLineFormRow(
                SingleLineFormField(
                    label = "Точка перезаказа",
                    content = { item, fieldModifier ->
                        SingleLineDecimalField(
                            value = item.reorderPoint,
                            onValueChange = { value ->
                                component.onChangeItem {
                                    it.copy(reorderPoint = value)
                                }
                            },
                            modifier = fieldModifier,
                        )
                    },
                ),
                SingleLineFormField(
                    label = "Количество для заказа",
                    content = { item, fieldModifier ->
                        SingleLineDecimalField(
                            value = item.orderQuantity,
                            onValueChange = { value ->
                                component.onChangeItem {
                                    it.copy(orderQuantity = value)
                                }
                            },
                            modifier = fieldModifier,
                        )
                    },
                ),
            ),
        ),
    ),
)
