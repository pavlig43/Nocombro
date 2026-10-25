package ru.pavlig43.mutable.api.singleLine.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import org.jetbrains.compose.resources.painterResource
import ru.pavlig43.core.model.ItemType
import ru.pavlig43.datetime.dateFormat
import ru.pavlig43.datetime.dateTimeFormat
import ru.pavlig43.mutable.api.input.decimalInputTransformation
import ru.pavlig43.mutable.api.input.intInputTransformation
import ru.pavlig43.mutable.api.input.toDecimalInputOrNull
import ru.pavlig43.mutable.api.input.toIntInputValueOrNull
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.open_in_new
import ru.pavlig43.theme.search

/** Текстовое поле карточной формы с лимитом длины и режимом чтения. */
@Composable
fun SingleLineTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    maxLength: Int = Int.MAX_VALUE,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    OutlinedTextField(
        state = state,
        inputTransformation = InputTransformation.maxLength(maxLength),
        modifier = modifier.fillMaxWidth(),
        readOnly = readOnly,
        lineLimits = if (singleLine) TextFieldLineLimits.SingleLine else {
            TextFieldLineLimits.MultiLine(minHeightInLines = minLines)
        },
    )
}

/** Целочисленное поле, принимающее лишь значения из [range]. */
@Composable
fun SingleLineIntField(
    state: TextFieldState,
    range: IntRange,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
) {
    OutlinedTextField(
        state = state,
        inputTransformation = intInputTransformation(range),
        modifier = modifier.fillMaxWidth(),
        readOnly = readOnly,
        lineLimits = TextFieldLineLimits.SingleLine,
        placeholder = { Text("0") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

/** Дробное поле с заданным числом знаков после точки. */
@Composable
fun SingleLineDecimalField(
    state: TextFieldState,
    decimalPlaces: Int,
    modifier: Modifier = Modifier,
    range: LongRange = 0L..Long.MAX_VALUE,
    readOnly: Boolean = false,
) {
    val error = state.text.toString().toDecimalInputOrNull(decimalPlaces, range) == null

    OutlinedTextField(
        state = state,
        inputTransformation = decimalInputTransformation(decimalPlaces),
        modifier = modifier.fillMaxWidth(),
        readOnly = readOnly,
        lineLimits = TextFieldLineLimits.SingleLine,
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
            state = remember(value?.displayName) { TextFieldState(value?.displayName ?: "Выберите тип") },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth(),
            readOnly = true,
            lineLimits = TextFieldLineLimits.SingleLine,
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

/**
 * Показывает значение в отключённом поле с цветами текущей Material-темы.
 *
 * Поле сохраняет размер и рамку, но не принимает ввод и не открывает меню.
 */
@Composable
fun SingleLineReadOnlyField(
    value: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        state = remember(value) { TextFieldState(value) },
        modifier = modifier.fillMaxWidth(),
        enabled = false,
        readOnly = true,
        lineLimits = TextFieldLineLimits.SingleLine,
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

/**
 * Показывает дату и время либо открывает переданный диалог выбора.
 *
 * @param readOnly при `true` показывает значение через [SingleLineReadOnlyField]
 */
@Composable
fun SingleLineDateTimeField(
    value: LocalDateTime,
    onOpenDialog: () -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
) {
    if (readOnly) {
        SingleLineReadOnlyField(
            value = value.format(dateTimeFormat),
            modifier = modifier,
        )
        return
    }

    OutlinedButton(
        onClick = onOpenDialog,
        modifier = modifier.heightIn(min = 56.dp),
    ) {
        Text(value.format(dateTimeFormat))
    }
}

/**
 * Показывает логический признак как общий переключатель карточной формы.
 *
 * @param readOnly при `true` оставляет значение видимым, но запрещает смену
 */
@Composable
fun SingleLineSwitchField(
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
) {
    Switch(
        checked = value,
        onCheckedChange = onValueChange,
        modifier = modifier,
        enabled = !readOnly,
    )
}

/**
 * Показывает связь и две внешние команды: выбор значения и открытие выбранного.
 *
 * Функция не хранит ID и не знает тип связанной сущности. Доступность каждой
 * команды и переходы задаёт компонент формы.
 *
 * @param value текст выбранного значения
 * @param onSelect открывает диалог выбора
 * @param onOpenSelected открывает выбранный объект
 * @param selectEnabled доступность выбора
 * @param openEnabled доступность открытия выбранного объекта
 */
@Composable
fun SingleLineReferenceField(
    value: String,
    onSelect: () -> Unit,
    onOpenSelected: () -> Unit,
    selectEnabled: Boolean,
    openEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        state = remember(value) { TextFieldState(value) },
        modifier = modifier.fillMaxWidth(),
        readOnly = true,
        lineLimits = TextFieldLineLimits.SingleLine,
        trailingIcon = {
            Row {
                IconButton(
                    onClick = onSelect,
                    enabled = selectEnabled,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.search),
                        contentDescription = "Выбрать значение",
                    )
                }
                IconButton(
                    onClick = onOpenSelected,
                    enabled = openEnabled,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.open_in_new),
                        contentDescription = "Открыть выбранное",
                    )
                }
            }
        },
    )
}
