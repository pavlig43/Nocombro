package ru.pavlig43.loadinitdata.api.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ru.pavlig43.coreui.LoadingUi
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
        is LoadInitDataState.Loading<I> -> LoadingUi(Modifier.fillMaxSize())
        is LoadInitDataState.Success<I> -> successBody(Modifier)
    }

}
@Composable
private fun RetryLoadInitData(
    error: String,
    retryLoadInitData: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(error, color = MaterialTheme.colorScheme.error)
        Button(retryLoadInitData) {
            Text(RETRY_LOAD)
        }
    }

}
private const val RETRY_LOAD = "Повторить загрузку"