package ru.pavlig43.signcommon.logopass.api.components

import kotlinx.coroutines.flow.StateFlow

interface ILogoPassComponent {

    val logoPassState: StateFlow<LogoPassState>

    val login: StateFlow<String>

    val isValidLogin: StateFlow<Boolean>

    val password: StateFlow<String>

    val isValidPassword: StateFlow<Boolean>

    val isValidLogoPass: StateFlow<Boolean>

    fun onLoginChanged(login: String)

    fun onPasswordChanged(password: String)

    fun sendLogoPass()


}

sealed interface LogoPassState {
    class Initial : LogoPassState

    class Loading : LogoPassState

    class Success : LogoPassState

    class Error(val message: String) : LogoPassState

}
