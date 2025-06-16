package ru.pavlig43.nocombro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.arkivanov.decompose.defaultComponentContext
import org.koin.java.KoinJavaComponent.getKoin
import ru.pavlig43.rootnocombro.api.IRootDependencies
import ru.pavlig43.rootnocombro.api.component.RootNocombroComponent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val rootComponent = RootNocombroComponent(
            componentContext = defaultComponentContext(),
            rootDependencies = getKoin().get<IRootDependencies>()
            )
        setContent {
            App(rootComponent)
        }
    }
}


