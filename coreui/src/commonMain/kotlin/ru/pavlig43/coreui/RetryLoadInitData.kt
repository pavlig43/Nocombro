package ru.pavlig43.coreui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun RetryLoadInitData(
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