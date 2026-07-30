package ru.pavlig43.document.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.document.internal.model.DocumentEssentialsUi
import ru.pavlig43.mutable.api.singleLine.component.SingleLineComponent
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineCardsScreen
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineDateField
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineFormField
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineFormSection
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineItemTypeField
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineTextField
import ru.pavlig43.mutable.api.singleLine.ui.singleLineFormRow

/** Показывает общую карточную форму документа для создания и правки. */
@Composable
internal fun DocumentEssentialsCards(
    component: SingleLineComponent<*, DocumentEssentialsUi, *>,
    onOpenDateDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sections = remember(component) {
        documentEssentialsSections(
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

/** Строит неизменяемую схему секций и строк документа. */
private fun documentEssentialsSections(
    component: SingleLineComponent<*, DocumentEssentialsUi, *>,
    onOpenDateDialog: () -> Unit,
): ImmutableList<SingleLineFormSection<DocumentEssentialsUi>> = persistentListOf(
    SingleLineFormSection(
        title = "Карточка документа",
        rows = persistentListOf(
            singleLineFormRow(
                SingleLineFormField(
                    label = "Название",
                    content = { item, fieldModifier ->
                        SingleLineTextField(
                            value = item.displayName,
                            onValueChange = { value ->
                                component.onChangeItem { it.copy(displayName = value) }
                            },
                            modifier = fieldModifier,
                        )
                    },
                ),
                SingleLineFormField(
                    label = "Тип документа",
                    content = { item, fieldModifier ->
                        SingleLineItemTypeField(
                            value = item.type,
                            options = DocumentType.entries,
                            onValueChange = { value ->
                                component.onChangeItem { it.copy(type = value) }
                            },
                            modifier = fieldModifier,
                            readOnly = item.id != 0,
                        )
                    },
                ),
            ),
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
