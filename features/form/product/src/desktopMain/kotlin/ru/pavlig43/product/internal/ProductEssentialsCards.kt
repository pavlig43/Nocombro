package ru.pavlig43.product.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.mutable.api.singleLine.component.SingleLineComponent
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineCardsScreen
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineDateField
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineDecimalField
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineFormField
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineFormSection
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineIntField
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineItemTypeField
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineTextField
import ru.pavlig43.mutable.api.singleLine.ui.singleLineFormRow
import ru.pavlig43.product.internal.model.ProductEssentialsUi

/** Карточная форма основных полей товара для создания и правки. */
@Composable
internal fun ProductEssentialsCards(
    component: SingleLineComponent<*, ProductEssentialsUi>,
    onOpenDateDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sections = remember(component) {
        productEssentialsSections(
            component = component,
            onOpenDateDialog = onOpenDateDialog,
        )
    }

    SingleLineCardsScreen(
        component = component,
        sections = sections,
        modifier = modifier,
    )
}

/** Строит неизменяемую схему секций и явно заданных строк товара. */
private fun productEssentialsSections(
    component: SingleLineComponent<*, ProductEssentialsUi>,
    onOpenDateDialog: () -> Unit,
): ImmutableList<SingleLineFormSection<ProductEssentialsUi>> = persistentListOf(
    SingleLineFormSection(
        title = "Карточка товара",
        rows = persistentListOf(
            singleLineFormRow(
                SingleLineFormField(
                    label = "Название продукта",
                    content = { item, fieldModifier ->
                        SingleLineTextField(
                            state = item.displayNameState,
                            modifier = fieldModifier,
                        )
                    },
                ),
                SingleLineFormField(
                    label = "Тип продукта",
                    content = { item, fieldModifier ->
                        SingleLineItemTypeField(
                            value = item.productType,
                            options = ProductType.entries,
                            readOnly = item.id != 0,
                            onValueChange = { value ->
                                component.onChangeItem { it.copy(productType = value) }
                            },
                            modifier = fieldModifier,
                        )
                    },
                ),
            ),
            singleLineFormRow(
                SingleLineFormField(
                    label = "SN",
                    content = { item, fieldModifier ->
                        SingleLineTextField(
                            state = item.secondNameState,
                            modifier = fieldModifier,
                        )
                    },
                ),
            ),
        ),
    ),
    SingleLineFormSection(
        title = "Продажи",
        rows = persistentListOf(
            singleLineFormRow(
                SingleLineFormField(
                    label = "Цена продажи (₽)",
                    content = { item, fieldModifier ->
                        SingleLineDecimalField(
                            state = item.priceForSaleState,
                            decimalPlaces = 2,
                            modifier = fieldModifier,
                        )
                    },
                ),
                SingleLineFormField(
                    label = "НДС %",
                    content = { item, fieldModifier ->
                        SingleLineIntField(
                            state = item.recNdsState,
                            range = 0..99,
                            modifier = fieldModifier,
                        )
                    },
                ),
            ),
        ),
    ),
    SingleLineFormSection(
        title = "Учёт",
        rows = persistentListOf(
            singleLineFormRow(
                SingleLineFormField(
                    label = "Дата создания",
                    content = { item, fieldModifier ->
                        SingleLineDateField(
                            value = item.createdAt,
                            onOpenDialog = onOpenDateDialog,
                            modifier = fieldModifier,
                        )
                    },
                ),
                SingleLineFormField(
                    label = "Срок годности (дней)",
                    content = { item, fieldModifier ->
                        SingleLineIntField(
                            state = item.shelfLifeDaysState,
                            range = 0..Int.MAX_VALUE,
                            modifier = fieldModifier,
                        )
                    },
                ),
            ),
            singleLineFormRow(
                SingleLineFormField(
                    label = "Комментарий",
                    content = { item, fieldModifier ->
                        SingleLineTextField(
                            state = item.commentState,
                            modifier = fieldModifier,
                            singleLine = false,
                            minLines = 2,
                        )
                    },
                ),
            ),
        ),
    ),
)
