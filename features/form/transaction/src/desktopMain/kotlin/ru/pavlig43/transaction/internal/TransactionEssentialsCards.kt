package ru.pavlig43.transaction.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import ru.pavlig43.database.data.transact.TransactionType
import ru.pavlig43.mutable.api.singleLine.component.SingleLineComponent
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineCardsScreen
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineDateTimeField
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineFormField
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineFormSection
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineItemTypeField
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineSwitchField
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineTextField
import ru.pavlig43.mutable.api.singleLine.ui.singleLineFormRow
import ru.pavlig43.transaction.internal.model.TransactionEssentialsUi

/**
 * Типы транзакций, для которых есть форма создания и рабочие вкладки.
 *
 * Списание, инвентаризация и перенос между складами остаются доступны лишь
 * для чтения старых данных, пока для них нет редактора.
 */
internal val creatableTransactionTypes = TransactionType.entries.filterNot {
    (it == TransactionType.WRITE_OFF) ||
        (it == TransactionType.INVENTORY) ||
        (it == TransactionType.STORAGE_TRANSFER)
}

/** Показывает общую карточную форму транзакции для создания и правки. */
@Composable
internal fun TransactionEssentialsCards(
    component: SingleLineComponent<*, TransactionEssentialsUi, *>,
    onOpenCreatedAtDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sections = remember(component) {
        transactionEssentialsSections(
            component = component,
            onOpenCreatedAtDialog = onOpenCreatedAtDialog,
        )
    }

    SingleLineCardsScreen(
        component = component,
        sections = sections,
        modifier = modifier,
    )
}

/** Строит неизменяемую схему секций и строк транзакции. */
private fun transactionEssentialsSections(
    component: SingleLineComponent<*, TransactionEssentialsUi, *>,
    onOpenCreatedAtDialog: () -> Unit,
): ImmutableList<SingleLineFormSection<TransactionEssentialsUi>> = persistentListOf(
    SingleLineFormSection(
        title = "Карточка транзакции",
        rows = persistentListOf(
            singleLineFormRow(
                SingleLineFormField(
                    label = "Тип транзакции",
                    content = { item, fieldModifier ->
                        SingleLineItemTypeField(
                            value = item.transactionType,
                            options = creatableTransactionTypes,
                            onValueChange = { value ->
                                component.onChangeItem { it.copy(transactionType = value) }
                            },
                            modifier = fieldModifier,
                            readOnly = item.id != 0,
                        )
                    },
                ),
                SingleLineFormField(
                    label = "Дата и время",
                    content = { item, fieldModifier ->
                        SingleLineDateTimeField(
                            value = item.createdAt,
                            onOpenDialog = onOpenCreatedAtDialog,
                            modifier = fieldModifier,
                        )
                    },
                ),
            ),
            singleLineFormRow(
                SingleLineFormField(
                    label = "Проведена",
                    content = { item, fieldModifier ->
                        SingleLineSwitchField(
                            value = item.isCompleted,
                            onValueChange = { value ->
                                component.onChangeItem { it.copy(isCompleted = value) }
                            },
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
