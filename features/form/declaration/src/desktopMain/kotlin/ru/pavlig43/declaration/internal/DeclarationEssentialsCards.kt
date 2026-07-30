package ru.pavlig43.declaration.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import ru.pavlig43.declaration.internal.model.DeclarationEssentialsUi
import ru.pavlig43.mutable.api.singleLine.component.SingleLineComponent
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineCardsScreen
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineDateField
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineFormField
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineFormSection
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineReferenceField
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineSwitchField
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineTextField
import ru.pavlig43.mutable.api.singleLine.ui.singleLineFormRow

/** Показывает общую карточную форму декларации для создания и правки. */
@Composable
internal fun DeclarationEssentialsCards(
    component: SingleLineComponent<*, DeclarationEssentialsUi, *>,
    onOpenVendorDialog: () -> Unit,
    onOpenVendor: () -> Unit,
    onOpenBornDateDialog: () -> Unit,
    onOpenBestBeforeDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sections = remember(component) {
        declarationEssentialsSections(
            component = component,
            onOpenVendorDialog = onOpenVendorDialog,
            onOpenVendor = onOpenVendor,
            onOpenBornDateDialog = onOpenBornDateDialog,
            onOpenBestBeforeDialog = onOpenBestBeforeDialog,
        )
    }

    SingleLineCardsScreen(
        component = component,
        sections = sections,
        modifier = modifier,
    )
}

/** Строит неизменяемую схему секций и строк декларации. */
@Suppress("LongParameterList")
private fun declarationEssentialsSections(
    component: SingleLineComponent<*, DeclarationEssentialsUi, *>,
    onOpenVendorDialog: () -> Unit,
    onOpenVendor: () -> Unit,
    onOpenBornDateDialog: () -> Unit,
    onOpenBestBeforeDialog: () -> Unit,
): ImmutableList<SingleLineFormSection<DeclarationEssentialsUi>> = persistentListOf(
    SingleLineFormSection(
        title = "Карточка декларации",
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
                    label = "Поставщик",
                    content = { item, fieldModifier ->
                        SingleLineReferenceField(
                            value = item.vendorName.orEmpty(),
                            onSelect = onOpenVendorDialog,
                            onOpenSelected = onOpenVendor,
                            selectEnabled = true,
                            openEnabled = item.vendorId != null,
                            modifier = fieldModifier,
                        )
                    },
                ),
            ),
            singleLineFormRow(
                SingleLineFormField(
                    label = "Создана",
                    content = { item, fieldModifier ->
                        SingleLineDateField(
                            value = item.bornDate,
                            onOpenDialog = onOpenBornDateDialog,
                            modifier = fieldModifier,
                        )
                    },
                ),
                SingleLineFormField(
                    label = "Истекает",
                    content = { item, fieldModifier ->
                        SingleLineDateField(
                            value = item.bestBefore,
                            onOpenDialog = onOpenBestBeforeDialog,
                            modifier = fieldModifier,
                        )
                    },
                ),
            ),
            singleLineFormRow(
                SingleLineFormField(
                    label = "Отслеживать в оповещениях",
                    content = { item, fieldModifier ->
                        SingleLineSwitchField(
                            value = item.isObserveFromNotification,
                            onValueChange = { value ->
                                component.onChangeItem {
                                    it.copy(isObserveFromNotification = value)
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
