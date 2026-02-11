package ru.pavlig43.declaration.internal.update.tabs.essential

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.coreui.DatePickerDialog
import ru.pavlig43.declaration.internal.DeclarationField
import ru.pavlig43.declaration.internal.create.component.DialogChild
import ru.pavlig43.declaration.internal.model.DeclarationEssentialsUi
import ru.pavlig43.declaration.internal.update.tabs.essential.DeclarationUpdateSingleLineComponent
import ru.pavlig43.immutable.api.component.MBSImmutableTableComponent
import ru.pavlig43.immutable.api.ui.MBSImmutableTable
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineBlockScreen

@Composable
internal fun UpdateDeclarationSingleLineScreen(
    component: DeclarationUpdateSingleLineComponent,
) {
    val dialog by component.dialog.subscribeAsState()

    SingleLineBlockScreen(component)

    dialog.child?.instance?.also { dialogChild ->
        when(dialogChild){
            is UpdateDialogChild.Date -> DatePickerDialog(dialogChild.component)
            is UpdateDialogChild.Vendor -> MBSImmutableTable(dialogChild.component)
        }
    }
}
