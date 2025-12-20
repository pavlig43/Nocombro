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
    data object  Initial : SignInState
    data object Loading : SignInState
    data object Success : SignInState
    data class Error(val message: String) : SignInState
}


