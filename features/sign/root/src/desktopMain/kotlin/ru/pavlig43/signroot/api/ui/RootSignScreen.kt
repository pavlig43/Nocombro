package ru.pavlig43.signroot.api.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.signroot.api.component.IRootSignComponent
import ru.pavlig43.signsignin.api.ui.SignInScreen
import ru.pavlig43.signsignup.api.ui.SignUpScreen

@Composable
fun  RootSignScreen(
    rootSignComponent: IRootSignComponent,
    modifier: Modifier = Modifier
    ){
    val stack by rootSignComponent.stack.subscribeAsState()

    Children(
        stack = stack,
        modifier = modifier
    ){child ->
        when(val instance = child.instance){
            is IRootSignComponent.Child.SignIn -> SignInScreen(instance.component)
            is IRootSignComponent.Child.SignUp -> SignUpScreen(instance.component)
        }

    }

}