package ru.pavlig43.sampletable.api.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.pavlig43.sampletable.api.component.SampleTableComponent
import ru.pavlig43.sampletable.api.component.SampleTableComponentMain
import ru.pavlig43.sampletable.app.SampleApp

@Composable
fun SampleTableScreen(
    component: SampleTableComponent,
    modifier: Modifier = Modifier,
) {
    val componentMain = component as SampleTableComponentMain
    SampleApp(componentMain, modifier)
}
