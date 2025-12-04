package ru.pavlig43.signcommon.logopass.api.components

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.signcommon.logopass.api.data.ILogoPassRepository
import ru.pavlig43.signcommon.logopass.api.data.LogoPass
import ru.pavlig43.signcommon.logopass.api.data.LogoPassResult

class LogoPassComponent(
    componentContext: ComponentContext,
    private val logoPassRepository: ILogoPassRepository,
    private val sendLogoPassRequest: (RequestResult<LogoPassResult>) -> Unit,
    ) : ComponentContext by componentContext, ILogoPassComponent {

    private val coroutineScope = componentCoroutineScope()

    private val _logoPassState = MutableStateFlow<LogoPassState>(LogoPassState.Initial())

    override val logoPassState: StateFlow<LogoPassState> = _logoPassState.asStateFlow()


    private val _login = MutableStateFlow("")


    override val login = _login.asStateFlow()

    private fun validateLogin(login: String): Boolean {
        println(login)
        return true
    }

    override val isValidLogin: StateFlow<Boolean> = _login.map { validateLogin(it) }
        .stateIn(
            coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    private val _password = MutableStateFlow("")
    override val password = _password.asStateFlow()

    private fun validatePassword(password: String): Boolean {
        println(password)
        return true
    }

    override val isValidPassword: StateFlow<Boolean> = _password.map { validatePassword(it) }
        .stateIn(
            coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )
    override val isValidLogoPass: StateFlow<Boolean> =
        isValidLogin.combine(isValidPassword) { isValidLogin, isValidPassword -> isValidLogin && isValidPassword }
            .stateIn(
                coroutineScope,
                started = SharingStarted.Eagerly,
                initialValue = false
            )


    override fun onLoginChanged(login: String) {
        _login.update { login }
    }

    override fun onPasswordChanged(password: String) {
        _password.update { password }
    }

    override fun sendLogoPass() {
        coroutineScope.launch {
            val result = logoPassRepository.sendLogoPass(LogoPass(_login.value, _password.value))
            sendLogoPassRequest(result)

        }
    }


}




