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
import ru.pavlig43.manageitem.internal.component.UpsertState
@Composable
internal fun CreateButton(
    onUpsert: () -> Unit,
    enabled: Boolean,
    isCreate: Boolean,
    onSuccessUpsert: (Int) -> Unit,
    upsertState: UpsertState,
    modifier: Modifier = Modifier
) {
    UpsertButton(
        onUpsert = onUpsert,
        enabled = enabled,
        isCreate = isCreate,
        onSuccess = { onSuccessUpsert((upsertState as UpsertState.Success).id) },
        upsertState = upsertState,
        modifier = modifier
    )
}
@Composable
internal fun UpdateButton(
    onUpsert: () -> Unit,
    enabled: Boolean,
    isCreate: Boolean,
    onCloseTab: () -> Unit,
    upsertState: UpsertState,
    modifier: Modifier = Modifier
) {
    UpsertButton(
        onUpsert = onUpsert,
        enabled = enabled,
        isCreate = isCreate,
        onSuccess = onCloseTab,
        upsertState = upsertState,
        modifier = modifier
    )
}
@Composable
private fun UpsertButton(
    onUpsert:()-> Unit,
    enabled: Boolean,
    isCreate: Boolean,
    onSuccess:()-> Unit,
    upsertState: UpsertState,
    modifier: Modifier = Modifier
){
    Column(modifier) {
        Button(onUpsert, enabled = enabled) {
            when (upsertState) {
                is UpsertState.Error -> Box(Modifier.background(MaterialTheme.colorScheme.error)) {
                    Text(
                        "Повторить"
                    )
                }

                UpsertState.Init -> Text(if (isCreate) "Создать" else " Обновить")
                UpsertState.Loading -> LoadingScreen()
                is UpsertState.Success -> {
                    if (isCreate) {
                        LaunchedEffect(Unit) { onSuccess() }
                    }
                }
            }
        }
        if (upsertState is UpsertState.Error) {

            TextField(
                value = upsertState.message,
                onValueChange = {},
            )
        }
    }

}
