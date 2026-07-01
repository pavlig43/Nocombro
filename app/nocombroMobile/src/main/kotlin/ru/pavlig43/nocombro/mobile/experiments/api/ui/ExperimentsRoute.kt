package ru.pavlig43.nocombro.mobile.experiments.api.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import ru.pavlig43.nocombro.mobile.experiments.api.component.ExperimentsMobileChild
import ru.pavlig43.nocombro.mobile.experiments.api.component.ExperimentsMobileComponent
import ru.pavlig43.nocombro.mobile.experiments.internal.ui.ExperimentDetailsScreen
import ru.pavlig43.nocombro.mobile.experiments.internal.ui.ExperimentEntryScreen
import ru.pavlig43.nocombro.mobile.experiments.internal.ui.ExperimentsListScreen

@Composable
fun ExperimentsRoute(
    component: ExperimentsMobileComponent,
    onOpenMenu: () -> Unit,
) {
    Children(
        stack = component.stack,
        modifier = Modifier.fillMaxSize(),
    ) { child ->
        when (val instance = child.instance) {
            is ExperimentsMobileChild.List -> ExperimentsListScreen(
                listComponent = instance.component,
                onSelectExperiment = component::selectExperiment,
                onOpenMenu = onOpenMenu,
            )

            is ExperimentsMobileChild.Details -> ExperimentDetailsScreen(
                detailsComponent = instance.component,
                onBack = component::closeCurrentScreen,
            )

            is ExperimentsMobileChild.Entry -> ExperimentEntryScreen(
                entryComponent = instance.component,
                onBack = component::closeCurrentScreen,
            )
        }
    }
}
