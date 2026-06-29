package ru.pavlig43.nocombro.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import com.arkivanov.decompose.defaultComponentContext
import org.koin.android.ext.android.get
import ru.pavlig43.nocombro.mobile.navigation.NocombroMobileRootComponent
import ru.pavlig43.nocombro.mobile.navigation.NocombroMobileRootDependencies

/**
 * Android entrypoint для mobile-приложения Nocombro.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val component = NocombroMobileRootComponent(
            componentContext = defaultComponentContext(),
            dependencies = get<NocombroMobileRootDependencies>(),
        )

        setContent {
            NocombroMobileApp(component)
        }
    }
}
