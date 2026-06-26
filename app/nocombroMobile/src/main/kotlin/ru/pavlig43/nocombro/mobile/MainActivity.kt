package ru.pavlig43.nocombro.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import ru.pavlig43.nocombro.mobile.experiments.ExperimentsApp
import ru.pavlig43.nocombro.mobile.experiments.ExperimentsMobileComponent

class MainActivity : ComponentActivity() {
    private val decomposeLifecycle = LifecycleRegistry()
    private lateinit var component: ExperimentsMobileComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        component = ExperimentsMobileComponent(
            componentContext = DefaultComponentContext(lifecycle = decomposeLifecycle),
            appContext = applicationContext,
        )

        setContent {
            ExperimentsApp(component)
        }
    }

    override fun onDestroy() {
        component.close()
        super.onDestroy()
    }
}
