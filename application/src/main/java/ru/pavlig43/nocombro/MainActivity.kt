package ru.pavlig43.nocombro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import ru.pavlig43.rootnocombro.api.ui.RootNocombroScreen
import ru.pavlig43.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                RootNocombroScreen()
            }
        }
    }
}
