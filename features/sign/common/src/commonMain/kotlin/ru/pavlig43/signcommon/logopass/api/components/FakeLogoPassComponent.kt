package ru.pavlig43.signcommon.logopass.api.components

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeLogoPassComponent : ILogoPassComponent {
    override val logoPassState: StateFlow<LogoPassState> = MutableStateFlow(LogoPassState.Initial())


    override val login: StateFlow<String> = MutableStateFlow("login")

    override val isValidLogin: StateFlow<Boolean> = MutableStateFlow(true)
    override val password: StateFlow<String> = MutableStateFlow("passwor7d")

    override val isValidPassword: StateFlow<Boolean> = MutableStateFlow(true)
    override val isValidLogoPass: StateFlow<Boolean> = MutableStateFlow(true)

    override fun onLoginChanged(login: String) = Unit

    override fun onPasswordChanged(password: String) = Unit
    override fun sendLogoPass() = Unit

}