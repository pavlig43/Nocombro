package ru.pavlig43.declaration.internal.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.create.ui.CreateEssentialsScreen
import ru.pavlig43.declaration.internal.component.CreateDeclarationComponent
import ru.pavlig43.immutable.api.ui.MBSImmutableTable

@Composable
internal fun CreateDeclarationScreen(
    component: CreateDeclarationComponent
) {
    val dialog by component.vendorDialog.dialog.subscribeAsState()
    CreateEssentialsScreen(component) { item, onItemChange ->
        DeclarationFields(
            declaration = item,
            updateDeclaration = onItemChange,
            onOpenVendorDialog = { component.vendorDialog.showDialog() }
        )
    }
    dialog.child?.instance?.also {
        MBSImmutableTable(it)
    }
}