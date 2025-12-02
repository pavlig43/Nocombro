package ru.pavlig43.manageitem.api.ui

import androidx.compose.runtime.Composable
import ru.pavlig43.manageitem.api.component.UpsertEssentialsFactoryComponent
import ru.pavlig43.manageitem.internal.ui.EssentialBlockScreen
import ru.pavlig43.manageitem.internal.ui.UpsertItemLogicScreen

@Composable
fun CreateItemScreen(
    component: UpsertEssentialsFactoryComponent
) {
    UpsertItemLogicScreen(
        component = component.essentialsComponent.upsertEssentialsLogic,
        isCreate = component.essentialsComponent.isCreate,
        onCloseFormScreen = {component.closeFormScreen()}
    ) {
        EssentialBlockScreen(component.essentialsComponent)
    }

}