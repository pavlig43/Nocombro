package ru.pavlig43.product.internal.update.tabs.specification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineCardsScreen
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineFormField
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineFormSection
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineTextField
import ru.pavlig43.mutable.api.singleLine.ui.singleLineFormRow

/** Показывает спецификацию товара как набор карточек. */
@Composable
internal fun ProductSpecificationCards(
    component: ProductSpecificationComponent,
    modifier: Modifier = Modifier,
) {
    val sections = remember(component) {
        productSpecificationSections(component)
    }

    SingleLineCardsScreen(
        component = component,
        sections = sections,
        modifier = modifier,
    )
}

/** Строит неизменяемую схему полей спецификации товара. */
@Suppress("LongMethod")
private fun productSpecificationSections(
    component: ProductSpecificationComponent,
): ImmutableList<SingleLineFormSection<ProductSpecificationUi>> = persistentListOf(
    SingleLineFormSection(
        title = "Спецификация товара",
        actions = {
            SpecificationPdfAction(component)
        },
        rows = persistentListOf(
            singleLineFormRow(
                SingleLineFormField(
                    label = "Дозировка",
                    content = { item, fieldModifier ->
                        SingleLineTextField(
                            value = item.dosage,
                            onValueChange = { value ->
                                component.onChangeItem { it.copy(dosage = value) }
                            },
                            modifier = fieldModifier,
                            singleLine = false,
                            minLines = 2,
                        )
                    },
                ),
                SingleLineFormField(
                    label = "Срок годности",
                    content = { item, fieldModifier ->
                        SingleLineTextField(
                            value = item.shelfLifeText,
                            onValueChange = { value ->
                                component.onChangeItem { it.copy(shelfLifeText = value) }
                            },
                            modifier = fieldModifier,
                            singleLine = false,
                            minLines = 2,
                        )
                    },
                ),
            ),
            singleLineFormRow(
                SingleLineFormField(
                    label = "Состав",
                    content = { item, fieldModifier ->
                        Row(
                            modifier = fieldModifier,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            SingleLineTextField(
                                value = item.composition,
                                onValueChange = { value ->
                                    component.onChangeItem { it.copy(composition = value) }
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = false,
                                minLines = 3,
                            )
                            TextButton(
                                onClick = component::generateComposition,
                            ) {
                                Text("Сгенерировать")
                            }
                        }
                    },
                ),
            ),
            singleLineFormRow(
                SingleLineFormField(
                    label = "Условия хранения",
                    content = { item, fieldModifier ->
                        SingleLineTextField(
                            value = item.storageConditions,
                            onValueChange = { value ->
                                component.onChangeItem { it.copy(storageConditions = value) }
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
    SingleLineFormSection(
        title = "Органолептические показатели",
        rows = persistentListOf(
            singleLineFormRow(
                SingleLineFormField(
                    label = "Внешний вид",
                    content = { item, fieldModifier ->
                        SingleLineTextField(
                            value = item.appearance,
                            onValueChange = { value ->
                                component.onChangeItem { it.copy(appearance = value) }
                            },
                            modifier = fieldModifier,
                            singleLine = false,
                            minLines = 2,
                        )
                    },
                ),
            ),
            singleLineFormRow(
                SingleLineFormField(
                    label = "Цвет",
                    content = { item, fieldModifier ->
                        SingleLineTextField(
                            value = item.color,
                            onValueChange = { value ->
                                component.onChangeItem { it.copy(color = value) }
                            },
                            modifier = fieldModifier,
                        )
                    },
                ),
                SingleLineFormField(
                    label = "Запах",
                    content = { item, fieldModifier ->
                        SingleLineTextField(
                            value = item.smell,
                            onValueChange = { value ->
                                component.onChangeItem { it.copy(smell = value) }
                            },
                            modifier = fieldModifier,
                        )
                    },
                ),
                SingleLineFormField(
                    label = "Вкус",
                    content = { item, fieldModifier ->
                        SingleLineTextField(
                            value = item.taste,
                            onValueChange = { value ->
                                component.onChangeItem { it.copy(taste = value) }
                            },
                            modifier = fieldModifier,
                        )
                    },
                ),
            ),
        ),
    ),
    SingleLineFormSection(
        title = "Показатели",
        rows = persistentListOf(
            singleLineFormRow(
                SingleLineFormField(
                    label = "Физико-химические показатели",
                    content = { item, fieldModifier ->
                        SingleLineTextField(
                            value = item.physicalChemicalIndicators,
                            onValueChange = { value ->
                                component.onChangeItem {
                                    it.copy(physicalChemicalIndicators = value)
                                }
                            },
                            modifier = fieldModifier,
                            singleLine = false,
                            minLines = 4,
                        )
                    },
                ),
            ),
            singleLineFormRow(
                SingleLineFormField(
                    label = "Микробиологические показатели",
                    content = { item, fieldModifier ->
                        SingleLineTextField(
                            value = item.microbiologicalIndicators,
                            onValueChange = { value ->
                                component.onChangeItem {
                                    it.copy(microbiologicalIndicators = value)
                                }
                            },
                            modifier = fieldModifier,
                            singleLine = false,
                            minLines = 4,
                        )
                    },
                ),
            ),
            singleLineFormRow(
                SingleLineFormField(
                    label = "Токсичные элементы",
                    content = { item, fieldModifier ->
                        SingleLineTextField(
                            value = item.toxicElements,
                            onValueChange = { value ->
                                component.onChangeItem { it.copy(toxicElements = value) }
                            },
                            modifier = fieldModifier,
                            singleLine = false,
                            minLines = 4,
                        )
                    },
                ),
            ),
        ),
    ),
    SingleLineFormSection(
        title = "Безопасность",
        rows = persistentListOf(
            singleLineFormRow(
                SingleLineFormField(
                    label = "Аллергены (название; в продукте да/нет; на производстве да/нет)",
                    content = { item, fieldModifier ->
                        SingleLineTextField(
                            value = item.allergens,
                            onValueChange = { value ->
                                component.onChangeItem { it.copy(allergens = value) }
                            },
                            modifier = fieldModifier,
                            singleLine = false,
                            minLines = 6,
                        )
                    },
                ),
            ),
            singleLineFormRow(
                SingleLineFormField(
                    label = "Информация о ГМО",
                    content = { item, fieldModifier ->
                        SingleLineTextField(
                            value = item.gmoInfo,
                            onValueChange = { value ->
                                component.onChangeItem { it.copy(gmoInfo = value) }
                            },
                            modifier = fieldModifier,
                            singleLine = false,
                            minLines = 4,
                        )
                    },
                ),
            ),
        ),
    ),
)

/** Показывает действие генерации PDF в заголовке карточки. */
@Composable
private fun SpecificationPdfAction(
    component: ProductSpecificationComponent,
) {
    val generationProgress by component.generationProgress.collectAsState()

    Button(
        onClick = component::generatePdf,
        enabled = generationProgress == null,
    ) {
        if (generationProgress == null) {
            Text("Сгенерировать PDF")
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}
