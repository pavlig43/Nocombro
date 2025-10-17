package ru.pavlig43.coreui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ProgressIndicator(modifier: Modifier = Modifier){
    Box(modifier) { CircularProgressIndicator() }
}