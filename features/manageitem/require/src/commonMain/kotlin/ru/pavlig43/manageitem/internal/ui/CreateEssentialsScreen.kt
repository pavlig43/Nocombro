package ru.pavlig43.manageitem.internal.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import ru.pavlig43.core.data.GenericItem
import ru.pavlig43.manageitem.api.ui.CreateButton
import ru.pavlig43.manageitem.internal.component.CreateEssentialsComponent
import ru.pavlig43.manageitem.internal.data.ItemEssentialsUi

@Composable
fun<I: ItemEssentialsUi> CreateEssentialsScreen(
    component: CreateEssentialsComponent<out GenericItem, I>,
    modifier: Modifier = Modifier,
    fieldsBody: @Composable (item: I, updateItem: (I) -> Unit) -> Unit,

    ){
    val enabled by component.isValidFields.collectAsState()
    val createState by component.createState.collectAsState()
    Column(modifier.fillMaxSize()) {
        EssentialBlockScreen(
            component = component,
            fieldsBody = fieldsBody
        )
        CreateButton(
            onCreate = component::create,
            enabled = enabled,
            onSuccess = { component.onSuccessCreate(it) },
            createState = createState,
        )
    }
}