package ru.pavlig43.vendor.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import ru.pavlig43.mutable.api.singleLine.component.SingleLineComponent
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineCardsScreen
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineFormField
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineFormSection
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineTextField
import ru.pavlig43.mutable.api.singleLine.ui.singleLineFormRow
import ru.pavlig43.vendor.internal.model.VendorEssentialsUi

/** Показывает общую карточную форму поставщика для создания и правки. */
@Composable
internal fun VendorEssentialsCards(
    component: SingleLineComponent<*, VendorEssentialsUi>,
    modifier: Modifier = Modifier,
) {
    val sections = remember(component) {
        vendorEssentialsSections(component)
    }

    SingleLineCardsScreen(
        component = component,
        sections = sections,
        modifier = modifier,
    )
}

/** Строит неизменяемую схему секций и строк поставщика. */
private fun vendorEssentialsSections(
    component: SingleLineComponent<*, VendorEssentialsUi>,
): ImmutableList<SingleLineFormSection<VendorEssentialsUi>> = persistentListOf(
    SingleLineFormSection(
        title = "Карточка поставщика",
        rows = persistentListOf(
            singleLineFormRow(
                SingleLineFormField(
                    label = "Название поставщика",
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
