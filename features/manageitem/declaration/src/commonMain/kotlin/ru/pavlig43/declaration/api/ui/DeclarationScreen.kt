package ru.pavlig43.declaration.api.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.core.data.DeclarationIn
import ru.pavlig43.core.data.DeclarationOut
import ru.pavlig43.declaration.api.component.DeclarationTabSlot
import ru.pavlig43.declaration.internal.ui.DeclarationListScreen
import ru.pavlig43.declaration.internal.ui.MBS

@Composable
fun<In: DeclarationIn,Out: DeclarationOut> DeclarationScreen(
    component: DeclarationTabSlot<Out,In>,
    modifier: Modifier = Modifier,
){
    val dialog by component.dialog.subscribeAsState()
    DeclarationListScreen(
        component = component.declarationList,
        openChooseDialog = component::openDialog,
        modifier = modifier
    )
    dialog.child?.instance?.also {
        MBS(it)
    }
}