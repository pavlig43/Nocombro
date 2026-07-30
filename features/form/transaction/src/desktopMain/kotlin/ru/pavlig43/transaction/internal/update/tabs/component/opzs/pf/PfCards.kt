package ru.pavlig43.transaction.internal.update.tabs.component.opzs.pf

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineCardsScreen
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineDecimalField
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineFormField
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineFormSection
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineReadOnlyField
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineReferenceField
import ru.pavlig43.mutable.api.singleLine.ui.singleLineFormRow

/** Показывает ПФ транзакции как карточную форму. */
@Composable
internal fun PfCards(
    component: PfComponent,
    modifier: Modifier = Modifier,
) {
    val sections = remember(component) {
        pfSections(component)
    }

    SingleLineCardsScreen(
        component = component,
        sections = sections,
        modifier = modifier,
    )
}

/** Строит неизменяемую схему полей ПФ. */
private fun pfSections(
    component: PfComponent,
): ImmutableList<SingleLineFormSection<PfUi>> = persistentListOf(
    SingleLineFormSection(
        title = "Полуфабрикат",
        actions = { item ->
            PfLabelAction(
                enabled = item.productId != 0,
                onClick = component::openThermalLabelDialog,
            )
        },
        rows = persistentListOf(
            singleLineFormRow(
                SingleLineFormField(
                    label = "Продукт",
                    content = { item, fieldModifier ->
                        SingleLineReferenceField(
                            value = item.productName,
                            onSelect = component::openProductDialog,
                            onOpenSelected = component::openSelectedProduct,
                            selectEnabled = true,
                            openEnabled = item.productId != 0,
                            modifier = fieldModifier,
                        )
                    },
                ),
                SingleLineFormField(
                    label = "Декларация",
                    content = { item, fieldModifier ->
                        SingleLineReferenceField(
                            value = item.declarationName,
                            onSelect = component::openDeclarationDialog,
                            onOpenSelected = component::openSelectedDeclaration,
                            selectEnabled = item.productId != 0,
                            openEnabled = item.declarationId != 0,
                            modifier = fieldModifier,
                        )
                    },
                ),
            ),
            singleLineFormRow(
                SingleLineFormField(
                    label = "Поставщик",
                    content = { item, fieldModifier ->
                        SingleLineReadOnlyField(
                            value = item.vendorName,
                            modifier = fieldModifier,
                        )
                    },
                ),
                SingleLineFormField(
                    label = "Количество",
                    content = { item, fieldModifier ->
                        SingleLineDecimalField(
                            value = item.count,
                            onValueChange = { value ->
                                component.onChangeItem { it.copy(count = value) }
                            },
                            modifier = fieldModifier,
                        )
                    },
                ),
            ),
        ),
    ),
)

/** Показывает кнопку этикетки в заголовке карточки. */
@Composable
private fun PfLabelAction(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Text("Этикетка")
    }
}
