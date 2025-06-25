package ru.pavlig43.signsignup.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.scope.Scope
import ru.pavlig43.signcommon.logopass.api.components.ILogoPassComponent
import ru.pavlig43.signcommon.logopass.api.components.LogoPassComponent
import ru.pavlig43.signcommon.social.api.components.ISocialSignComponent
import ru.pavlig43.signcommon.social.api.components.SocialSignComponent
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.signsignup.api.ISignUpDependencies
import ru.pavlig43.signsignup.internal.di.createSignUpModule

class SignUpComponent(
    componentContext: ComponentContext,
    signUpDependencies: ISignUpDependencies,
    private val signUp: () -> Unit,
    private val navigateToSignIn: () -> Unit,
) : ComponentContext by componentContext, ISignUpComponent {
//    private val coroutineScope = componentCoroutineScope()

    private val _signUpState = MutableStateFlow<SignUpState>(SignUpState.Initial())


    private fun handleSignInState(signUpState: SignUpState) {
        when (signUpState) {

            is SignUpState.Error -> {
                println("error ${signUpState.message}")
            }

            is SignUpState.Initial -> {}
            is SignUpState.Loading -> {
                println("loading signUn")
            }

            is SignUpState.Success -> {signUp()}
        }
    }

    private fun trySignIn(result: RequestResult<*>) {

        _signUpState.update { result.toSignUpState() }
        handleSignInState(_signUpState.value)
    }

    override val signUpState = _signUpState.asStateFlow()

    override fun onSignInClick() {
        navigateToSignIn()
    }

    private val koinContext = instanceKeeper.getOrCreate {
        ComponentKoinContext()
    }
    private val scope: Scope =
        koinContext.getOrCreateKoinScope(createSignUpModule(signUpDependencies))

    /**
     * TODO() проверить на пересоздание репозитория при повороте
     *  private val logoPassRepository:ILogoPassRepository = instanceKeeper.getOrCreate { scope.get() }
     */
    override val logoPassComponent: ILogoPassComponent = LogoPassComponent(
        componentContext = childContext("logoPass"),
        logoPassRepository = scope.get(),
        sendLogoPassRequest = { result -> trySignIn(result) },
    )
    override val socialSignComponent: ISocialSignComponent =
        SocialSignComponent(componentContext = childContext("social"))



}

private fun RequestResult<*>.toSignUpState(): SignUpState {
    return when (this) {
        is RequestResult.Error<*> -> SignUpState.Error(" ${throwable?.message ?: "Unknown error"}")
        is RequestResult.InProgress -> SignUpState.Loading()
        is RequestResult.Initial<*> -> SignUpState.Initial()
        is RequestResult.Success<*> -> SignUpState.Success()
    }
}

interface SignUpState {
    class Initial : SignUpState
    class Loading : SignUpState
    class Success : SignUpState
    class Error(val message: String) : SignUpState
}
