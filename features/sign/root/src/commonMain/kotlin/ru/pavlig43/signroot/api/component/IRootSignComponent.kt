package ru.pavlig43.signroot.api.component

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import ru.pavlig43.signsignin.api.component.ISignInComponent
import ru.pavlig43.signsignup.api.component.ISignUpComponent

interface IRootSignComponent {
    val stack: Value<ChildStack<*, Child>>


    sealed class  Child{
        class SignIn(val component: ISignInComponent): Child()
        class SignUp(val component: ISignUpComponent): Child()
    }

}