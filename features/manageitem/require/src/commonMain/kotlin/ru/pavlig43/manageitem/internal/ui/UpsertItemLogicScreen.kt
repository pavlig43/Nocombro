package ru.pavlig43.manageitem.internal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.manageitem.api.ui.UpsertButton
import ru.pavlig43.manageitem.api.component.UpsertEssentialsLogic1



@Composable
internal fun <I : Any> UpsertItemLogicScreen(
    component: UpsertEssentialsLogic1<I>,
    isCreate: Boolean,
    onCloseFormScreen: () -> Unit,
    modifier: Modifier = Modifier,
    fieldsBody: @Composable () -> Unit
) {
    val upsertState by component.upsertState.collectAsState()
    val enabled by component.isValidValue.collectAsState()
    val scrollState = rememberScrollState()
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        fieldsBody()
        if (isCreate){
            UpsertButton(
                onUpsert = component::upsert,
                enabled = enabled,
                createState = upsertState,
                isCreate = isCreate
            )
        }


    }

}
