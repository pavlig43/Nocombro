package ru.pavlig43.nocombro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.tooling.preview.Preview
import com.arkivanov.decompose.defaultComponentContext
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.manualFileKitCoreInitialization
import org.koin.java.KoinJavaComponent.getKoin
import ru.pavlig43.rootnocombro.api.RootDependencies
import ru.pavlig43.rootnocombro.api.component.RootNocombroComponent
import ua.wwind.table.sample.app.SampleApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        FileKit.manualFileKitCoreInitialization(this)
        val rootComponent = RootNocombroComponent(
            componentContext = defaultComponentContext(),
            rootDependencies = getKoin().get<RootDependencies>()
            )
        setContent {
            App(rootComponent)
        }
    }
}
