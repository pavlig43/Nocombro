package ru.pavlig43.signcommon.logopass.api.components

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshotFlow
import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.signcommon.logopass.api.data.ILogoPassRepository
import ru.pavlig43.signcommon.logopass.api.data.LogoPass
import ru.pavlig43.signcommon.logopass.api.data.LogoPassResult

class LogoPassComponent(
    componentContext: ComponentContext,
    private val logoPassRepository: ILogoPassRepository,
    private val sendLogoPassRequest: (Result<LogoPassResult>) -> Unit,
    ) : ComponentContext by componentContext, ILogoPassComponent {

    private val coroutineScope = componentCoroutineScope()

    private val _logoPassState = MutableStateFlow<LogoPassState>(LogoPassState.Initial())

    override val logoPassState: StateFlow<LogoPassState> = _logoPassState.asStateFlow()


    override val login = TextFieldState()

    private fun validateLogin(login: String): Boolean {
        return true
    }

    override val isValidLogin: StateFlow<Boolean> = snapshotFlow { validateLogin(login.text.toString()) }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    override val password = TextFieldState()

    private fun validatePassword(password: String): Boolean {
        return true
    }

    override val isValidPassword: StateFlow<Boolean> = snapshotFlow { validatePassword(password.text.toString()) }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )
    override val isValidLogoPass: StateFlow<Boolean> =
        isValidLogin.combine(isValidPassword) { isValidLogin, isValidPassword -> isValidLogin && isValidPassword }
            .stateIn(
                scope = coroutineScope,
                started = SharingStarted.Eagerly,
                initialValue = false
            )


    override fun onLoginChanged(login: String) {
        this.login.setTextAndPlaceCursorAtEnd(login)
    }

    override fun onPasswordChanged(password: String) {
        this.password.setTextAndPlaceCursorAtEnd(password)
    }

    override fun sendLogoPass() {
        val request = LogoPass(login.text.toString(), password.text.toString())
        coroutineScope.launch {
            val result = logoPassRepository.sendLogoPass(request)
            sendLogoPassRequest(result)

        }
    }


}




