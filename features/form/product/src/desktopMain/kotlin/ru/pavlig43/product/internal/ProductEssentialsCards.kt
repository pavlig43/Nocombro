package ru.pavlig43.product.internal

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.format
import ru.pavlig43.core.model.DecimalData2
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.datetime.dateFormat
import ru.pavlig43.mutable.api.singleLine.component.SingleLineComponent
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineCardsScreen
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineFormField
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineFormSection
import ru.pavlig43.product.internal.model.ProductEssentialsUi

@Composable
internal fun ProductEssentialsCards(
    component: SingleLineComponent<*, ProductEssentialsUi, ProductField>,
    editableProductType: Boolean,
    onOpenDateDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sections: ImmutableList<SingleLineFormSection<ProductEssentialsUi>> = persistentListOf(
        SingleLineFormSection(
            title = "Карточка товара",
            fields = persistentListOf(
                SingleLineFormField(
                    label = "Название продукта",
                    content = { item, fieldModifier ->
                        ProductTextField(
                            value = item.displayName,
                            onValueChange = { value ->
                                component.onChangeItem { it.copy(displayName = value) }
                            },
                            modifier = fieldModifier,
                        )
                    },
                ),
                SingleLineFormField(
                    label = "Тип продукта",
                    content = { item, fieldModifier ->
                        ProductTypeField(
                            value = item.productType,
                            editable = editableProductType,
                            onValueChange = { value ->
                                component.onChangeItem { it.copy(productType = value) }
                            },
                            modifier = fieldModifier,
                        )
                    },
                ),
                SingleLineFormField(
                    label = "SN",
                    content = { item, fieldModifier ->
                        ProductTextField(
                            value = item.secondName,
                            onValueChange = { value ->
                                component.onChangeItem { it.copy(secondName = value) }
                            },
                            modifier = fieldModifier,
                        )
                    },
                ),
            ),
        ),
        SingleLineFormSection(
            title = "Продажи",
            fields = persistentListOf(
                SingleLineFormField(
                    label = "Цена продажи (₽)",
                    content = { item, fieldModifier ->
                        ProductPriceField(
                            resetKey = item.id,
                            value = item.priceForSale,
                            onValueChange = { value ->
                                component.onChangeItem { it.copy(priceForSale = value) }
                            },
                            modifier = fieldModifier,
                        )
                    },
                ),
                SingleLineFormField(
                    label = "НДС %",
                    content = { item, fieldModifier ->
                        ProductIntField(
                            resetKey = item.id,
                            value = item.recNds,
                            range = 0..99,
                            onValueChange = { value ->
                                component.onChangeItem { it.copy(recNds = value) }
                            },
                            modifier = fieldModifier,
                        )
                    },
                ),
            ),
        ),
        SingleLineFormSection(
            title = "Учёт",
            fields = persistentListOf(
                SingleLineFormField(
                    label = "Дата создания",
                    content = { item, fieldModifier ->
                        OutlinedButton(
                            onClick = onOpenDateDialog,
                            modifier = fieldModifier.heightIn(min = 56.dp),
                        ) {
                            Text(item.createdAt.format(dateFormat))
                        }
                    },
                ),
                SingleLineFormField(
                    label = "Срок годности (дней)",
                    content = { item, fieldModifier ->
                        ProductIntField(
                            resetKey = item.id,
                            value = item.shelfLifeDays,
                            range = 0..Int.MAX_VALUE,
                            onValueChange = { value ->
                                component.onChangeItem { it.copy(shelfLifeDays = value) }
                            },
                            modifier = fieldModifier,
                        )
                    },
                ),
                SingleLineFormField(
                    label = "Комментарий",
                    fullWidth = true,
                    content = { item, fieldModifier ->
                        ProductTextField(
                            value = item.comment,
                            onValueChange = { value ->
                                component.onChangeItem { it.copy(comment = value) }
                            },
                            modifier = fieldModifier,
                            singleLine = false,
                            minLines = 2,
                        )
                    },
                ),
            ),
        ),
    )

    SingleLineCardsScreen(
        component = component,
        sections = sections,
        modifier = modifier,
    )
}

@Composable
private fun ProductTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        minLines = minLines,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductTypeField(
    value: ProductType?,
    editable: Boolean,
    onValueChange: (ProductType) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!editable) {
        OutlinedTextField(
            value = value?.displayName.orEmpty(),
            onValueChange = {},
            modifier = modifier.fillMaxWidth(),
            readOnly = true,
            singleLine = true,
        )
        return
    }

    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = value?.displayName ?: "Выберите тип",
            onValueChange = {},
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth(),
            readOnly = true,
            singleLine = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            ProductType.entries.forEach { productType ->
                DropdownMenuItem(
                    text = { Text(productType.displayName) },
                    onClick = {
                        onValueChange(productType)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ProductPriceField(
    resetKey: Int,
    value: DecimalData2,
    onValueChange: (DecimalData2) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember(resetKey) {
        mutableStateOf(value.takeUnless { it.value == 0L }?.toString().orEmpty())
    }
    OutlinedTextField(
        value = text,
        onValueChange = valueChange@{ newText ->
            val parsedValue = newText.toPriceValueOrNull() ?: return@valueChange
            text = newText
            onValueChange(DecimalData2(parsedValue))
        },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text("0") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

@Composable
private fun ProductIntField(
    resetKey: Int,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember(resetKey) {
        mutableStateOf(value.takeUnless { it == 0 }?.toString().orEmpty())
    }
    OutlinedTextField(
        value = text,
        onValueChange = valueChange@{ newText ->
            if (newText.any { !it.isDigit() }) return@valueChange
            val parsedValue = if (newText.isEmpty()) 0 else newText.toIntOrNull() ?: return@valueChange
            if (parsedValue !in range) return@valueChange
            text = newText
            onValueChange(parsedValue)
        },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text("0") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

private fun String.toPriceValueOrNull(): Long? {
    if (isEmpty()) return 0L
    val normalized = replace(',', '.')
    if (normalized.count { it == '.' } > 1) return null
    if (normalized.any { !it.isDigit() && it != '.' }) return null

    val parts = normalized.split('.', limit = 2)
    val fractionText = parts.getOrNull(1).orEmpty()
    if (fractionText.length > 2) return null

    val whole = parts.firstOrNull().orEmpty().ifEmpty { "0" }.toLongOrNull() ?: return null
    if (whole > Long.MAX_VALUE / 100L) return null
    val fraction = fractionText.padEnd(2, '0').ifEmpty { "0" }.toLongOrNull() ?: return null
    return whole * 100L + fraction
}
