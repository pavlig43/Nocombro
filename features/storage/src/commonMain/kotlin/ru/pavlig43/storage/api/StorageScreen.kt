package ru.pavlig43.storage.api

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import ru.pavlig43.coreui.ErrorScreen
import ru.pavlig43.coreui.LoadingUi
import ru.pavlig43.storage.api.component.LoadState
import ru.pavlig43.storage.api.component.StorageComponent

@Composable
fun StorageScreen(
    component: StorageComponent
){
    val loadState by component.loadState.collectAsState()
    when(val state = loadState) {
        is LoadState.Error -> ErrorScreen(state.message)
        is LoadState.Loading -> LoadingUi()
        is LoadState.Success -> {
            Column(Modifier.verticalScroll(rememberScrollState())){
                state.products.forEach { product ->
                    Text("${product.productName}: ${product.balanceOnEnd}")
                    product.batches.forEach { batch ->
                        Text("  ${batch.batchName}: ${batch.balanceOnEnd}")
                    }
                }
            }
        }
    }
}