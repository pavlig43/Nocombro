package ru.pavlig43.expense.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import ru.pavlig43.database.data.expense.ExpenseType
import ru.pavlig43.expense.internal.model.ExpenseEssentialsUi
import ru.pavlig43.mutable.api.singleLine.component.SingleLineComponent
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineCardsScreen
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineDateTimeField
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineDecimalField
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineFormField
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineFormSection
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineItemTypeField
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineTextField
import ru.pavlig43.mutable.api.singleLine.ui.singleLineFormRow

/** Показывает общую карточную форму расхода для создания и правки. */
@Composable
internal fun ExpenseEssentialsCards(
    component: SingleLineComponent<*, ExpenseEssentialsUi, *>,
    onOpenDateTimeDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sections = remember(component) {
        expenseEssentialsSections(
            component = component,
            onOpenDateTimeDialog = onOpenDateTimeDialog,
        )
    }

    SingleLineCardsScreen(
        component = component,
        sections = sections,
        modifier = modifier,
    )
}

/** Строит неизменяемую схему секций и строк расхода. */
private fun expenseEssentialsSections(
    component: SingleLineComponent<*, ExpenseEssentialsUi, *>,
    onOpenDateTimeDialog: () -> Unit,
): ImmutableList<SingleLineFormSection<ExpenseEssentialsUi>> = persistentListOf(
    SingleLineFormSection(
        title = "Карточка расхода",
        rows = persistentListOf(
            singleLineFormRow(
                SingleLineFormField(
                    label = "Тип расхода",
                    content = { item, fieldModifier ->
                        SingleLineItemTypeField(
                            value = item.expenseType,
                            options = ExpenseType.entries,
                            onValueChange = { value ->
                                component.onChangeItem { it.copy(expenseType = value) }
                            },
                            modifier = fieldModifier,
                        )
                    },
                ),
                SingleLineFormField(
                    label = "Сумма (₽)",
                    content = { item, fieldModifier ->
                        SingleLineDecimalField(
                            value = item.amount,
                            onValueChange = { value ->
                                component.onChangeItem { it.copy(amount = value) }
                            },
                            modifier = fieldModifier,
                        )
                    },
                ),
            ),
            singleLineFormRow(
                SingleLineFormField(
                    label = "Дата и время",
                    content = { item, fieldModifier ->
                        SingleLineDateTimeField(
                            value = item.expenseDateTime,
                            onOpenDialog = onOpenDateTimeDialog,
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
                            value = item.comment,
                            onValueChange = { value ->
                                component.onChangeItem { it.copy(comment = value) }
                            },
                            modifier = fieldModifier,
                            singleLine = false,
                            minLines = 3,
                        )
                    },
                ),
            ),
        ),
    ),
)
