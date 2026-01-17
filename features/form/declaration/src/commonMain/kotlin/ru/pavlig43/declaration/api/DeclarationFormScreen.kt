package ru.pavlig43.declaration.api

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.files.api.ui.FilesScreen
import ru.pavlig43.core.ui.EssentialBlockScreen
import ru.pavlig43.declaration.internal.component.tabs.tabslot.DeclarationTabChild
import ru.pavlig43.declaration.internal.component.tabs.tabslot.DeclarationEssentialComponent
import ru.pavlig43.declaration.internal.ui.CreateDeclarationScreen
import ru.pavlig43.declaration.internal.ui.DeclarationFields
import ru.pavlig43.immutable.api.ui.MBSImmutableTable
import ru.pavlig43.update.ui.ItemTabsUi1

@Composable
fun DeclarationFormScreen(
    component: DeclarationFormComponent,
    modifier: Modifier = Modifier,
) {

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,

        modifier = modifier
            .padding(horizontal = 8.dp)
    ) {
        val stack by component.stack.subscribeAsState()

        Children(
            stack = stack,
        ) { child ->
            when (val instance = child.instance) {


                is DeclarationFormComponent.Child.Create -> CreateDeclarationScreen(instance.component)
                is DeclarationFormComponent.Child.Update -> ItemTabsUi1(
                    component = instance.component,
                    slotFactory = { slotForm: DeclarationTabChild? ->
                        DeclarationSlotScreen(slotForm)
                    })
            }
        }

    }

}

@Composable
private fun DeclarationSlotScreen(
    declarationTabChild: DeclarationTabChild?,
) {
    when (declarationTabChild) {
        is DeclarationTabChild.Essential -> UpdateEssentialsBlock(declarationTabChild.component)
        is DeclarationTabChild.File -> FilesScreen(declarationTabChild.component)
        null -> Box(Modifier)
    }
}

@Composable
private fun UpdateEssentialsBlock(
    declarationTabSlot: DeclarationEssentialComponent,
    modifier: Modifier = Modifier
) {
    val dialog by declarationTabSlot.vendorDialogComponent.dialog.subscribeAsState()

    Column(modifier.verticalScroll(rememberScrollState())) {
        EssentialBlockScreen(declarationTabSlot) { item, onItemChange ->
            DeclarationFields(
                declaration = item,
                updateDeclaration = onItemChange,
                onOpenVendorDialog = {
                    declarationTabSlot.vendorDialogComponent.showDialog() }
            )
        }
        dialog.child?.instance?.also {
            MBSImmutableTable(it)
        }
    }

}