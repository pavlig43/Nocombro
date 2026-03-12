package ru.pavlig43.declaration.api

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.declaration.internal.create.ui.CreateDeclarationSingleLineScreen
import ru.pavlig43.declaration.internal.update.DeclarationTabChild
import ru.pavlig43.declaration.internal.update.tabs.essential.UpdateDeclarationSingleLineScreen
import ru.pavlig43.files.api.ui.FilesScreen
import ru.pavlig43.update.ui.FormTabsUi

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


                is DeclarationFormComponent.Child.Create -> CreateDeclarationSingleLineScreen(instance.component)
                is DeclarationFormComponent.Child.Update -> FormTabsUi(
                    component = instance.component,
                    tabChildFactory = { slotForm: DeclarationTabChild? ->
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
        is DeclarationTabChild.Essential -> UpdateDeclarationSingleLineScreen(declarationTabChild.component)
        is DeclarationTabChild.File -> FilesScreen(declarationTabChild.component)
        null -> Box {}
    }
}
