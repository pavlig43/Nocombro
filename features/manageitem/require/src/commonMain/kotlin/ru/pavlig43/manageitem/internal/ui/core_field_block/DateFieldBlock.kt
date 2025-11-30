package ru.pavlig43.manageitem.internal.ui.core_field_block

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import ru.pavlig43.core.convertToDate
import ru.pavlig43.coreui.tooltip.IconButtonToolTip

@Composable
internal fun BestBeforeFieldBlock(
    date: Long?,
    onSelectDate: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {


    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Истекает")
        if (date != null) {
            Text(date.convertToDate(), textDecoration = TextDecoration.Underline)
        }
        var isDatePickerVisible by remember { mutableStateOf(false) }
        IconButtonToolTip(
            tooltipText = if (date == null) "Установить дату" else "Изменить дату",
            onClick = { isDatePickerVisible = true },
            icon = if (date == null) Icons.Default.AddCircle else Icons.Default.Refresh,
        )

        if (isDatePickerVisible) {
            DatePickerDialogSample(
                date = date,
                onSelectDate = onSelectDate,
                isShowDialog = isDatePickerVisible,
                onDismissRequest = { isDatePickerVisible = false },

                )
        }


    }


}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialogSample(
    date: Long?,
    onSelectDate: (Long?) -> Unit,
    isShowDialog: Boolean,
    onDismissRequest: () -> Unit,
) {


    val datePickerState = rememberDatePickerState(date)
    val confirmEnabled by remember {
        derivedStateOf { datePickerState.selectedDateMillis != null }
    }

    if (isShowDialog) {

        DatePickerDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = {
                TextButton(
                    onClick = {
                        onSelectDate(datePickerState.selectedDateMillis)

                        onDismissRequest()
                    },
                    enabled = confirmEnabled
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) { Text("Cancel") }
            },
        ) {
            DatePicker(
                state = datePickerState,
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        }
    }
}