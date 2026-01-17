package ru.pavlig43.create.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ru.pavlig43.create.component.CreateState
import ru.pavlig43.core.model.GenericItem
import ru.pavlig43.core.model.ItemEssentialsUi
import ru.pavlig43.core.ui.EssentialBlockScreen
import ru.pavlig43.coreui.LoadingUi
import ru.pavlig43.create.component.CreateEssentialsComponent

@Composable
fun<I: ItemEssentialsUi> CreateEssentialsScreen(
    component: CreateEssentialsComponent<out GenericItem, I>,
    modifier: Modifier = Modifier,
    fieldsBody: @Composable (item: I, updateItem: (I) -> Unit) -> Unit,

    ){
    val enabled by component.isValidFields.collectAsState()
    val createState by component.createState.collectAsState()
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        EssentialBlockScreen(
            component = component,
            fieldsBody = fieldsBody,
        )
        CreateButton(
            onCreate = component::create,
            enabled = enabled,
            onSuccess = { component.onSuccessCreate(it) },
            createState = createState,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}
@Composable
private fun CreateButton(
    onCreate:()-> Unit,
    enabled: Boolean,
    onSuccess:(Int)-> Unit,
    createState: CreateState,
    modifier: Modifier = Modifier
){
    Column(modifier) {
        Button(onCreate, enabled = enabled) {
            when (createState) {
                is CreateState.Error -> Box(Modifier.background(MaterialTheme.colorScheme.error)) {
                    Text(
                        "Повторить"
                    )
                }

                CreateState.Init -> Text( "Создать")
                CreateState.Loading -> LoadingUi()
                is CreateState.Success -> {
                    LaunchedEffect(Unit) { onSuccess(createState.id) } }
            }
        }
    }
    if (createState is CreateState.Error) {

        TextField(
            value = createState.message,
            onValueChange = {},
        )
    }
}