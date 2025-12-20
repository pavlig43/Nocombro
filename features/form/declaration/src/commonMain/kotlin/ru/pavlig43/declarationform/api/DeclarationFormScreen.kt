package ru.pavlig43.declarationform.api

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
import ru.pavlig43.addfile.api.ui.FilesScreen
import ru.pavlig43.core.ui.EssentialBlockScreen
import ru.pavlig43.declarationform.internal.component.tabs.tabslot.DeclarationFileTabSlot
import ru.pavlig43.declarationform.internal.component.tabs.tabslot.DeclarationTabSlot
import ru.pavlig43.declarationform.internal.component.tabs.tabslot.EssentialTabSlot
import ru.pavlig43.declarationform.internal.ui.CreateDeclarationScreen
import ru.pavlig43.declarationform.internal.ui.DeclarationFields
import ru.pavlig43.itemlist.api.ui.MBSImmutableTable
import ru.pavlig43.update.ui.ItemTabsUi

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
                is DeclarationFormComponent.Child.Update -> ItemTabsUi(
                    component = instance.component,
                    slotFactory = { slotForm: DeclarationTabSlot? ->
                        DeclarationSlotScreen(slotForm)
                    })
            }
        }

    }

}

@Composable
private fun DeclarationSlotScreen(
    declarationTabSlot: DeclarationTabSlot?,
) {
    when (declarationTabSlot) {
        is DeclarationFileTabSlot -> FilesScreen(declarationTabSlot.fileComponent)
        is EssentialTabSlot -> UpdateEssentialsBlock(declarationTabSlot)
        null -> Box(Modifier)
    }
}

@Composable
private fun UpdateEssentialsBlock(
    declarationTabSlot: EssentialTabSlot,
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


