package ru.pavlig43.declaration.internal.create.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.coreui.DatePickerDialog
import ru.pavlig43.declaration.internal.create.component.CreateDeclarationSingleLineComponent
import ru.pavlig43.declaration.internal.create.component.DialogChild
import ru.pavlig43.immutable.api.component.MBSImmutableTableComponent
import ru.pavlig43.immutable.api.ui.MBSImmutableTable
import ru.pavlig43.mutable.api.singleLine.ui.CreateSingleItemScreen

@Composable
internal fun CreateDeclarationSingleLineScreen(
    component: CreateDeclarationSingleLineComponent,
) {
    val dialog by component.dialog.subscribeAsState()

    CreateSingleItemScreen(component)


    dialog.child?.instance?.also { dialogChild ->
        when(dialogChild){
            is DialogChild.Date -> DatePickerDialog(dialogChild.component)
            is DialogChild.Vendor -> MBSImmutableTable(dialogChild.component)
        }
    }

}
