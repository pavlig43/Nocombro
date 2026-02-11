package ru.pavlig43.mutable.api.singleLine.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.pavlig43.core.model.SingleItem
import ru.pavlig43.coreui.LoadingUi
import ru.pavlig43.coreui.ValidationErrorsCard
import ru.pavlig43.mutable.api.singleLine.component.CreateSingleLineComponent
import ru.pavlig43.mutable.api.singleLine.component.CreateState
import ru.pavlig43.mutable.api.singleLine.model.ISingleLineTableUi


@Composable
fun <I : ISingleLineTableUi, C>CreateSingleItemScreen(
    component: CreateSingleLineComponent<out SingleItem,I,C>
){
    val createState by component.createState.collectAsState()
    val errorMessages by component.errorTableMessages.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SingleLineBlockScreen(
            component = component,
            modifier = Modifier.weight(1f)
        )
        CreateButton(
            onCreate = component::create,
            errorMessages = errorMessages,
            onSuccess = {component.onSuccessCreate(it)},
            createState = createState,
        )
    }


}
@Composable
private fun CreateButton(
    onCreate:()-> Unit,
    errorMessages:List<String>,
    onSuccess:(Int)-> Unit,
    createState: CreateState,
    modifier: Modifier = Modifier
){
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Кнопка создания
        Button(
            onClick = onCreate,
            enabled = errorMessages.isEmpty() && createState !is CreateState.Loading,
            modifier = Modifier.fillMaxWidth(),
            colors = when (createState) {
                is CreateState.Error -> ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
                else -> ButtonDefaults.buttonColors()
            }
        ) {
            when (createState) {
                is CreateState.Error -> Text("Повторить")
                CreateState.Init -> Text("Создать")
                CreateState.Loading -> LoadingUi()
                is CreateState.Success -> {
                    LaunchedEffect(Unit) { onSuccess(createState.id) }
                }
            }
        }

        // Ошибки валидации
        if (errorMessages.isNotEmpty()) {
            ValidationErrorsCard(
                errorMessages = errorMessages,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Ошибка сервера
        if (createState is CreateState.Error) {
            ValidationErrorsCard(
                errorMessages = listOf(createState.message),
                modifier = Modifier.fillMaxWidth()
            )

        }
    }
}

