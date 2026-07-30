package ru.pavlig43.mutable.api.singleLine.ui

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import ru.pavlig43.core.model.DecimalData
import ru.pavlig43.core.model.ItemType
import ru.pavlig43.datetime.dateFormat
import ru.pavlig43.mutable.api.input.normalizeDecimalInput
import ru.pavlig43.mutable.api.input.toDecimalInputText
import ru.pavlig43.mutable.api.input.toDecimalInputOrNull
import ru.pavlig43.mutable.api.input.toIntInputValueOrNull

/** Текстовое поле карточной формы с лимитом длины и режимом чтения. */
@Composable
fun SingleLineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    maxLength: Int = Int.MAX_VALUE,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            if (!readOnly && newValue.length <= maxLength) onValueChange(newValue)
        },
        modifier = modifier.fillMaxWidth(),
        readOnly = readOnly,
        singleLine = singleLine,
        minLines = minLines,
    )
}

/** Целочисленное поле, принимающее лишь значения из [range]. */
@Composable
fun SingleLineIntField(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
) {
    var text by remember { mutableStateOf(value.takeUnless { it == 0 }?.toString().orEmpty()) }

    LaunchedEffect(value, range) {
        if (text.toIntInputValueOrNull(range) != value) {
            text = value.takeUnless { it == 0 }?.toString().orEmpty()
        }
    }

    OutlinedTextField(
        value = text,
        onValueChange = valueChange@{ newText ->
            if (readOnly) return@valueChange
            val parsedValue = newText.toIntInputValueOrNull(range) ?: return@valueChange
            text = newText
            onValueChange(parsedValue)
        },
        modifier = modifier.fillMaxWidth(),
        readOnly = readOnly,
        singleLine = true,
        placeholder = { Text("0") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

/** Дробное поле на базе [DecimalData] с числом знаков из модели. */
@Composable
fun <D : DecimalData> SingleLineDecimalField(
    value: D,
    onValueChange: (D) -> Unit,
    modifier: Modifier = Modifier,
    range: LongRange = 0L..Long.MAX_VALUE,
    readOnly: Boolean = false,
) {
    var text by remember { mutableStateOf(value.toDecimalInputText()) }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(value.value, value.countDecimal, range) {
        if (text.toDecimalInputOrNull(value.countDecimal, range)?.value != value.value) {
            text = value.toDecimalInputText()
            error = false
        }
    }

    OutlinedTextField(
        value = text,
        onValueChange = valueChange@{ newText ->
            if (readOnly) return@valueChange
            val normalizedText = newText.normalizeDecimalInput(value.countDecimal)
            text = normalizedText
            val parsedInput = normalizedText.toDecimalInputOrNull(value.countDecimal, range)
            if (parsedInput == null) {
                error = true
                return@valueChange
            }
            error = false
            @Suppress("UNCHECKED_CAST")
            onValueChange(value.copyValue(parsedInput.value) as D)
        },
        modifier = modifier.fillMaxWidth(),
        readOnly = readOnly,
        singleLine = true,
        isError = error,
        supportingText = { if (error) Text("Не число") },
        placeholder = { Text("0") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

/** Поле выбора [ItemType], которое также умеет работать только для чтения. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : ItemType> SingleLineItemTypeField(
    value: T?,
    options: List<T>,
    onValueChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
) {
    if (readOnly) {
        SingleLineReadOnlyField(
            value = value?.displayName.orEmpty(),
            modifier = modifier,
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
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayName) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** Однострочное поле только для чтения. */
@Composable
fun SingleLineReadOnlyField(
    value: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        modifier = modifier.fillMaxWidth(),
        readOnly = true,
        singleLine = true,
    )
}

/** Поле даты, которое открывает внешний диалог выбора. */
@Composable
fun SingleLineDateField(
    value: LocalDate,
    onOpenDialog: () -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
) {
    if (readOnly) {
        SingleLineReadOnlyField(
            value = value.format(dateFormat),
            modifier = modifier,
        )
        return
    }

    OutlinedButton(
        onClick = onOpenDialog,
        modifier = modifier.heightIn(min = 56.dp),
    ) {
        Text(value.format(dateFormat))
    }
}
