package ru.pavlig43.itemlist.api.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
internal fun ErrorScreen(message: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center){
        Text(message)

    }


}

@Composable
internal fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()

    ) {
        CircularProgressIndicator(Modifier.align(Alignment.Center))
    }
}