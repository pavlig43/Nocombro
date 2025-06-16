package ru.pavlig43

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.theme.NocombroTheme

@Composable
fun AppForPreview(
    isDarkTheme: Boolean = false,
    content: @Composable () -> Unit){
    NocombroTheme(darkTheme = isDarkTheme) {
        Surface(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 24.dp),

                horizontalAlignment = Alignment.CenterHorizontally
            ){
                content()
            }

        }
    }

}