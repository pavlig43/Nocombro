package ru.pavlig43.declarationform.internal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.core.convertToDate
import ru.pavlig43.coreui.StringColumnField
import ru.pavlig43.coreui.tooltip.IconButtonToolTip
import ru.pavlig43.declarationform.internal.component.DeclarationRequiresComponent
import ru.pavlig43.declarationform.internal.data.RequiresValuesWithDate
import ru.pavlig43.itemlist.api.ui.MBSItemList
import ru.pavlig43.loadinitdata.api.ui.LoadInitDataScreen


@Composable
internal fun DeclarationRequireScreen(
    component: DeclarationRequiresComponent,
    modifier: Modifier = Modifier
) {
    val requireValues by component.requiresValuesWithDate.collectAsState()
    val dialog by component.dialog.subscribeAsState()



    Column(modifier) {
        LoadInitDataScreen(
            component.initComponent
        ) {
            RequireValuesBody(
                requireValues = requireValues,
                onOpenVendorDialog = component::showDialog,
                onChangeName = component::onNameChange,
                onSelectDate = component::changeBestBefore,
                onCheckedNotificationVisible = component::onChangeCheckedNotificationVisible,
            )

        }
    }
    dialog.child?.instance?.also {
        MBSItemList(it)
    }


}


@Suppress("LongParameterList")
@Composable
private fun RequireValuesBody(
    requireValues: RequiresValuesWithDate,
    onOpenVendorDialog: () -> Unit,
    onChangeName: (String) -> Unit,
    onSelectDate: (Long?) -> Unit,
    onCheckedNotificationVisible: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {

    Column(modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)

        ) {
            IsObserveFromNotificationBlock(
                isObserveFromNotification = requireValues.isObserveFromNotification,
                onCheckedNotificationVisible = onCheckedNotificationVisible,
            )
            StringColumnField(
                value = requireValues.name,
                onValueChange = onChangeName,
                headText = "Имя"
            )
            VendorBlock(
                vendorName = requireValues.vendorName,
                onOpenVendorDialog = onOpenVendorDialog
            )

            DateBlock(
                date = requireValues.bestBefore,
                onSelectDate = onSelectDate
            )


        }

    }

}

@Composable
private fun IsObserveFromNotificationBlock(
    isObserveFromNotification: Boolean,
    onCheckedNotificationVisible: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Отслеживать в оповещениях")
        Checkbox(
            checked = isObserveFromNotification,
            onCheckedChange = onCheckedNotificationVisible,
        )
    }
}

@Composable
private fun VendorBlock(
    vendorName: String?,
    onOpenVendorDialog: () -> Unit,
    modifier: Modifier = Modifier
) {

    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Поставщик")
        if (vendorName != null) {
            Text(vendorName, textDecoration = TextDecoration.Underline)
        }
        IconButtonToolTip(
            tooltipText = if (vendorName == null) "Добавить поставщика" else "Изменить поставщика",
            onClick = onOpenVendorDialog,
            icon = if (vendorName == null) Icons.Default.AddCircle else Icons.Default.Refresh,
        )
    }


}

@Composable
private fun DateBlock(
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




