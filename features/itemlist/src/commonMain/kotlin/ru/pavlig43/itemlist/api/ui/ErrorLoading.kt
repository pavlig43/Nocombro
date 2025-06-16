package ru.pavlig43.itemlist.api.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
internal fun ErrorScreen(modifier: Modifier = Modifier) {

}

@Composable
internal fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()

    ) {
        CircularProgressIndicator(Modifier.align(Alignment.Center))
    }
}