package ru.pavlig43.rootnocombro.api.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.document.api.ui.DocumentScreen
import ru.pavlig43.rootnocombro.api.component.IRootNocombroComponent
import ru.pavlig43.rootnocombro.api.component.TabScreen
import ru.pavlig43.signroot.api.ui.RootSignScreen

@Composable
fun RootNocombroScreen(rootNocombroComponent: IRootNocombroComponent) {
    val stack by rootNocombroComponent.stack.subscribeAsState()
    Surface() {
            Children(
                stack = stack,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .windowInsetsPadding(WindowInsets.systemBars)
            ) { child: Child.Created<Any, IRootNocombroComponent.Child> ->
                Column(Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                    when (val instance = child.instance) {
                        is IRootNocombroComponent.Child.RootSign -> RootSignScreen(instance.component)
                        is IRootNocombroComponent.Child.Document -> DocumentScreen(instance.component)
                        is IRootNocombroComponent.Child.Tab -> TabScreen()
                    }
                }


            }
        }

    }




