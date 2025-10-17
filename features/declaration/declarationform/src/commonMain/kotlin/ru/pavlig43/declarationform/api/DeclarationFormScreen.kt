package ru.pavlig43.declarationform.api

import androidx.compose.foundation.layout.Arrangement
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
import ru.pavlig43.declarationform.internal.component.DeclarationFileTabSlot
import ru.pavlig43.declarationform.internal.component.DeclarationTabSlot
import ru.pavlig43.declarationform.internal.component.UpdateDeclarationTabSlot
import ru.pavlig43.declarationform.internal.ui.CreateDeclarationScreen
import ru.pavlig43.declarationform.internal.ui.DeclarationRequireScreen
import ru.pavlig43.form.api.ui.ItemTabsUi

@Composable
fun DeclarationFormScreen(
    component: DeclarationFormComponent,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,

        modifier = modifier
            .padding(horizontal = 8.dp)
            .verticalScroll(scrollState)
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
                        DocumentSlotScreen(slotForm)
                    })
            }
        }

    }

}

@Composable
private fun DocumentSlotScreen(declarationTabSlot: DeclarationTabSlot?) {
    when (declarationTabSlot) {
        is UpdateDeclarationTabSlot -> DeclarationRequireScreen(declarationTabSlot.requires)

        is DeclarationFileTabSlot -> FilesScreen(declarationTabSlot.fileComponent)
        else -> error("document slot not found $declarationTabSlot")
    }
}