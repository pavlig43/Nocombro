package ru.pavlig43.signsignup.api.component

import kotlinx.coroutines.flow.StateFlow
import ru.pavlig43.signcommon.logopass.api.components.ILogoPassComponent
import ru.pavlig43.signcommon.social.api.components.ISocialSignComponent

interface ISignUpComponent {
    val logoPassComponent: ILogoPassComponent
    val socialSignComponent: ISocialSignComponent

    val signUpState: StateFlow<SignUpState>

    fun onSignInClick()




}


