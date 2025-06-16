package ru.pavlig43.signsignin.api.component

import kotlinx.coroutines.flow.StateFlow
import ru.pavlig43.signcommon.logopass.api.components.ILogoPassComponent
import ru.pavlig43.signcommon.social.api.components.ISocialSignComponent

interface ISignInComponent {
    val logoPassComponent: ILogoPassComponent
    val socialSignComponent: ISocialSignComponent

    val signInState: StateFlow<SignInState>

    fun onSignUpClick()

}
interface SignInState {
    class Initial : SignInState
    class Loading : SignInState
    class Success : SignInState
    class Error(val message: String) : SignInState
}


