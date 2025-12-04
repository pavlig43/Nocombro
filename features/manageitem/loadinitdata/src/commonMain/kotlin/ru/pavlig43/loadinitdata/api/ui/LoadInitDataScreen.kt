package ru.pavlig43.loadinitdata.api.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import ru.pavlig43.coreui.ProgressIndicator
import ru.pavlig43.coreui.RetryLoadInitData
import ru.pavlig43.loadinitdata.api.component.LoadInitDataComponent
import ru.pavlig43.loadinitdata.api.component.LoadInitDataState

@Composable
fun <I : Any> LoadInitDataScreen(
    component: LoadInitDataComponent<I>,
    successBody:@Composable (modifier:Modifier)->Unit,
) {
    val loadInitDataState by component.loadState.collectAsState()
    when(val state = loadInitDataState){
        is LoadInitDataState.Error<I> -> RetryLoadInitData(
            error = state.message,
            retryLoadInitData = component::retryLoadInitData,
            modifier = Modifier.fillMaxSize()
        )
        is LoadInitDataState.Loading<I> -> ProgressIndicator(Modifier.fillMaxSize())
        is LoadInitDataState.Success<I> -> successBody(Modifier)
    }

}