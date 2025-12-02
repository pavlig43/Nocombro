package ru.pavlig43.manageitem.api.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import ru.pavlig43.coreui.LoadingScreen
import ru.pavlig43.manageitem.internal.component.CreateState


@Composable
internal fun CreateButton(
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
                CreateState.Loading -> LoadingScreen()
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


